import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import src.Card;
import src.CardDeck;
import src.CardGame;
import src.InvalidPackException;
import src.Pack;
import src.Player;

//Whole-game behaviour: dealing, termination, invariants and output files
class CardGameTest {

    private static final Duration GAME_TIMEOUT = Duration.ofSeconds(30);

    @TempDir
    Path dir;

    // A shuffled pack with eight copies of each value 1..n, so a win is always reachable
    static Pack winnablePack(int n, long seed) throws InvalidPackException {
        List<Integer> values = new ArrayList<>();
        for (int v = 1; v <= n; v++) {
            for (int c = 0; c < Pack.CARDS_PER_PLAYER; c++) {
                values.add(v);
            }
        }
        Collections.shuffle(values, new Random(seed));
        return pack(n, values.stream().mapToInt(Integer::intValue).toArray());
    }

    // Builds a pack straight from card values, without a pack file 
    static Pack pack(int players, int... values) throws InvalidPackException {
        List<String> rows = new ArrayList<>();
        for (int v : values) {
            rows.add(Integer.toString(v));
        }
        return Pack.parse(rows, players);
    }

    private static List<Integer> sortedValues(List<Card> cards) {
        return cards.stream().map(Card::getCardValue).sorted().collect(Collectors.toList());
    }

    private static List<Integer> values(List<Card> cards) {
        return cards.stream().map(Card::getCardValue).collect(Collectors.toList());
    }

    private int play(CardGame game) {
        return assertTimeoutPreemptively(GAME_TIMEOUT, game::play);
    }

    @Test
    void dealsHandsThenDecksRoundRobinFromTheTopOfThePack() throws Exception {
        // n = 2: cards 1..8 go to hands, 9..16 to decks
        int[] values = new int[16];
        for (int i = 0; i < 16; i++) {
            values[i] = i + 1;
        }
        CardGame game = new CardGame(pack(2, values), dir);

        assertEquals(List.of(1, 3, 5, 7), values(game.getPlayers().get(0).getHand()));
        assertEquals(List.of(2, 4, 6, 8), values(game.getPlayers().get(1).getHand()));
        assertEquals(List.of(9, 11, 13, 15), values(game.getDecks().get(0).getContents()));
        assertEquals(List.of(10, 12, 14, 16), values(game.getDecks().get(1).getContents()));
    }

    @Test
    void ringConnectsPlayerIToDeckIAndDeckIPlusOne() throws Exception {
        // Player 3 (of 3) must draw from deck 3 and discard to deck 1
        // Hand of player 3 = cards 3, 6, 9, 12; deck 3 = cards 15, 18, 21, 24
        int[] values = new int[24];
        for (int i = 0; i < 24; i++) {
            values[i] = 100 + i;
        }
        values[2] = 3;
        values[5] = 3;
        values[8] = 3;
        values[11] = 50;   // player 3's only non-preferred card
        values[14] = 3;    // top of deck 3
        CardGame game = new CardGame(pack(3, values), dir);
        Player p3 = game.getPlayers().get(2);
        Player.TurnResult turn = p3.playTurn();
        assertEquals(3, turn.getCardDrawn().getCardValue());
        assertEquals(50, turn.getCardDisCarded().getCardValue());
        assertEquals(50, values(game.getDecks().get(0).getContents()).get(4), "discard goes to bottom of deck 1");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 6, 10})
    void gameEndsWithOneWinnerFourCardsEachAndNoCardsLost(int n) throws Exception {
        Pack pack = winnablePack(n, 42 + n);
        CardGame game = new CardGame(pack, dir);
        int winner = play(game);

        assertTrue(winner >= 1 && winner <= n, "winner " + winner);
        for (Player p : game.getPlayers()) {
            assertEquals(Player.HAND_SIZE, p.getHand().size(), p + " hand size");
        }
        assertTrue(game.getPlayers().get(winner - 1).hasWinningHand());
        assertEveryPlayerTookTheSameNumberOfTurns(game);
        assertEquals(sortedValues(pack.getCards()), sortedValues(game.snapshotAllCards()));
    }

    @Test
    void writesTwoNOutputFilesMatchingTheFinalState() throws Exception {
        int n = 4;
        CardGame game = new CardGame(winnablePack(n, 7), dir);
        int winner = play(game);

        for (Player p : game.getPlayers()) {
            List<String> lines = Files.readAllLines(dir.resolve("player" + p.getPlayerID() + "_output.txt"));
            int id = p.getPlayerID();
            assertTrue(lines.get(0).startsWith("player " + id + " initial hand "), lines.get(0));
            String hand = Player.formatCards(p.getHand());
            if (id == winner) {
                assertEquals(List.of("player " + id + " wins", "player " + id + " exits",
                        "player " + id + " final hand: " + hand), lines.subList(lines.size() - 3, lines.size()));
            } else {
                assertEquals(List.of("player " + winner + " has informed player " + id + " that player " + winner
                        + " has won", "player " + id + " exits", "player " + id + " hand: " + hand),
                        lines.subList(lines.size() - 3, lines.size()));
            }
            assertActionLinesAreWellFormed(id, n, lines.subList(1, lines.size() - 3));
        }
        for (CardDeck d : game.getDecks()) {
            List<String> lines = Files.readAllLines(dir.resolve("deck" + d.getDeckID() + "_output.txt"));
            assertEquals(List.of(d.describeContents()), lines);
        }
        try (var files = Files.list(dir)) {
            assertEquals(2 * n, files.count());
        }
    }

    // actions come in draw / discard / current-hand triples using the correct decks 
    private static void assertActionLinesAreWellFormed(int id, int n, List<String> actions) {
        assertEquals(0, actions.size() % 3, "actions must come in complete triples");
        int right = id % n + 1;
        for (int i = 0; i < actions.size(); i += 3) {
            assertTrue(actions.get(i).matches("player " + id + " draws a \\d+ from deck " + id), actions.get(i));
            assertTrue(actions.get(i + 1).matches("player " + id + " discards a \\d+ to deck " + right),
                    actions.get(i + 1));
            assertTrue(actions.get(i + 2).matches("player " + id + " current hand is \\d+ \\d+ \\d+ \\d+"),
                    actions.get(i + 2));
            String discarded = actions.get(i + 1).split(" ")[3];
            assertNotEquals(Integer.toString(id), discarded, "discarded a preferred card");
        }
    }

    @Test
    void immediateWinStopsEveryoneBeforeAnyCardMoves() throws Exception {
        // player 2 is dealt 2 2 2 2 (pack rows 2, 4, 6, 8 for n = 2)
        Pack pack = pack(2, 1, 2, 3, 2, 4, 2, 5, 2, 6, 7, 8, 9, 10, 11, 12, 13);
        CardGame game = new CardGame(pack, dir);
        List<List<Card>> decksBefore = new ArrayList<>();
        game.getDecks().forEach(d -> decksBefore.add(d.getContents()));

        assertEquals(2, play(game));

        assertEquals(List.of("player 2 initial hand 2 2 2 2", "player 2 wins", "player 2 exits",
                "player 2 final hand: 2 2 2 2"), Files.readAllLines(dir.resolve("player2_output.txt")));
        assertEquals(List.of("player 1 initial hand 1 3 4 5",
                "player 2 has informed player 1 that player 2 has won", "player 1 exits",
                "player 1 hand: 1 3 4 5"), Files.readAllLines(dir.resolve("player1_output.txt")));
        for (int i = 0; i < 2; i++) {
            assertEquals(decksBefore.get(i), game.getDecks().get(i).getContents());
        }
        for (Player p : game.getPlayers()) {
            assertEquals(0, turnsPlayed(p), p + " must not take a turn");
        }
    }

    // a player's turn count, taken from the draw lines in their output file
    private static long turnsPlayed(Player p) throws Exception {
        return Files.readAllLines(p.getOutputFile()).stream().filter(l -> l.contains(" draws a ")).count();
    }

    /**
     * Every player takes the same number of turns: the output files contain
     * the same number of draw lines, and because each full round takes one
     * card from and adds one card to every deck, every deck ends with the
     * four cards it was dealt
     */
    private static void assertEveryPlayerTookTheSameNumberOfTurns(CardGame game) throws Exception {
        long turns = turnsPlayed(game.getPlayers().get(0));
        for (Player p : game.getPlayers()) {
            assertEquals(turns, turnsPlayed(p), p + " turn count");
        }
        for (CardDeck d : game.getDecks()) {
            assertEquals(Player.HAND_SIZE, d.size(), d + " size");
        }
    }

    @Test
    void aWinInTheFirstRoundStillLetsEveryPlayerFinishThatRoundAndNoMore() throws Exception {
        // n = 2. Player 1 is dealt 1 1 1 5 and deck 1 starts with a 1, so player 1
        // wins in round 1; player 2 (hand 3 4 6 7, deck 2 top 8) cannot
        Pack pack = pack(2, 1, 3, 1, 4, 1, 6, 5, 7, 1, 8, 9, 10, 11, 12, 13, 14);
        CardGame game = new CardGame(pack, dir);

        assertEquals(1, play(game));

        assertEquals(1, turnsPlayed(game.getPlayers().get(0)));
        assertEquals(1, turnsPlayed(game.getPlayers().get(1)));
        assertEquals(List.of(
                "player 1 initial hand 1 1 1 5",
                "player 1 draws a 1 from deck 1",
                "player 1 discards a 5 to deck 2",
                "player 1 current hand is 1 1 1 1",
                "player 1 wins",
                "player 1 exits",
                "player 1 final hand: 1 1 1 1"), Files.readAllLines(dir.resolve("player1_output.txt")));
        assertEquals(List.of(
                "player 2 initial hand 3 4 6 7",
                "player 2 draws a 8 from deck 2",
                "player 2 discards a 3 to deck 1",
                "player 2 current hand is 4 6 7 8",
                "player 1 has informed player 2 that player 1 has won",
                "player 2 exits",
                "player 2 hand: 4 6 7 8"), Files.readAllLines(dir.resolve("player2_output.txt")));
        assertEquals(List.of("deck1 contents: 9 11 13 3"), Files.readAllLines(dir.resolve("deck1_output.txt")));
        assertEquals(List.of("deck2 contents: 10 12 14 5"), Files.readAllLines(dir.resolve("deck2_output.txt")));
    }

    @Test
    void cardsAreConservedInSnapshotsTakenWhileTheGameIsRunning() throws Exception {
        int n = 6;
        Pack pack = winnablePack(n, 99);
        List<Integer> expected = sortedValues(pack.getCards());
        CardGame game = new CardGame(pack, dir);
        assertTimeoutPreemptively(GAME_TIMEOUT, () -> {
            game.start();
            int snapshots = 0;
            while (!game.getState().isOver() || snapshots == 0) {
                List<Card> all = game.snapshotAllCards();
                assertEquals(expected, sortedValues(all));
                for (Player p : game.getPlayers()) {
                    assertEquals(Player.HAND_SIZE, p.getHand().size());
                }
                snapshots++;
            }
            game.awaitCompletion();
        });
        assertEquals(expected, sortedValues(game.snapshotAllCards()));
    }

    @Test
    void repeatedGamesAlwaysTerminateWithExactlyOneWinner() throws Exception {
        PrintStream original = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        try {
            for (int run = 0; run < 40; run++) {
                int n = 2 + run % 7;
                Path runDir = Files.createDirectory(dir.resolve("run" + run));
                CardGame game = new CardGame(winnablePack(n, run), runDir);
                int winner = play(game);
                long winners = game.getPlayers().stream().filter(p -> {
                    try {
                        return Files.readAllLines(p.getOutputFile()).contains("player " + p.getPlayerID() + " wins");
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                }).count();
                assertEquals(1, winners, "run " + run);
                assertTrue(winner >= 1 && winner <= n);
                assertFalse(game.getState().isAborted());
                assertEveryPlayerTookTheSameNumberOfTurns(game);
            }
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void winnerIsPrintedToTheTerminal() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        int winner;
        try {
            winner = play(new CardGame(winnablePack(3, 5), dir));
        } finally {
            System.setOut(original);
        }
        assertEquals("Player " + winner + " wins", captured.toString(StandardCharsets.UTF_8).strip());
    }

    // console

    private String runConsole(String input) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        PrintStream original = System.out;
        System.setOut(out);
        try {
            assertTimeoutPreemptively(GAME_TIMEOUT,
                    () -> CardGame.runFromConsole(new BufferedReader(new StringReader(input)), out, dir));
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private Path writePack(String name, Pack pack) throws Exception {
        Path file = dir.resolve(name);
        Files.write(file, pack.getCards().stream().map(Card::toString).collect(Collectors.toList()));
        return file;
    }

    @Test
    void consoleRepromptsUntilThePlayerCountAndPackAreValid() throws Exception {
        Path good = writePack("good.txt", winnablePack(2, 3));
        Path wrongSize = writePack("wrong.txt", winnablePack(3, 3));
        String input = String.join("\n", "zero", "-2", "0", "2",
                dir.resolve("missing.txt").toString(), wrongSize.toString(), good.toString()) + "\n";

        String output = runConsole(input);

        assertEquals(4, count(output, "Please enter the number of players"));
        assertEquals(3, count(output, "Invalid number of players"));
        assertEquals(3, count(output, "Please enter location of pack to load"));
        assertEquals(2, count(output, "Invalid pack:"));
        assertTrue(output.matches("(?s).*Player [12] wins.*"), output);
        assertTrue(Files.exists(dir.resolve("player2_output.txt")));
        assertTrue(Files.exists(dir.resolve("deck2_output.txt")));
    }

    @Test
    void consoleIgnoresAByteOrderMarkAndSurroundingSpaces() throws Exception {
        Path good = writePack("good.txt", winnablePack(2, 8));
        String output = runConsole((char) 0xFEFF + " 2 \n  " + good + "  \n");
        assertEquals(0, count(output, "Invalid"), output);
        assertTrue(Files.exists(dir.resolve("deck1_output.txt")));
    }

    @Test
    void consoleDoesNotStartAGameIfInputEndsBeforeAValidPack() throws Exception {
        String output = runConsole("2\nmissing.txt\n");
        assertTrue(output.contains("Invalid pack"));
        try (var files = Files.list(dir)) {
            assertEquals(0, files.count(), "no output files may be written");
        }
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + 1)) {
            count++;
        }
        return count;
    }
}
