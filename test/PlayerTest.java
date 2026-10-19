import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import src.Card;
import src.CardDeck;
import src.GameState;
import src.Player;

/**
 * Behaviour of a single player, driven through {@link Player#playTurn()} so
 * that the strategy can be tested deterministically without threads.
 */
class PlayerTest {

    @TempDir
    Path dir;

    private CardDeck left;
    private CardDeck right;
    private GameState state;

    @BeforeEach
    void setUp() {
        left = new CardDeck(2);
        right = new CardDeck(3);
        state = new GameState(1);
    }

    private Player player(int id, int... hand) {
        Player p = new Player(id, left, right, state, dir);
        for (int v : hand) {
            p.recieveCard(new Card(v));
        }
        return p;
    }

    private void fillLeft(int... values) {
        for (int v : values) {
            left.addCard(new Card(v));
        }
    }

    private static List<Integer> values(List<Card> cards) {
        List<Integer> result = new ArrayList<>();
        cards.forEach(c -> result.add(c.getCardValue()));
        return result;
    }

    @Test
    void aTurnDrawsFromTheLeftTopAndDiscardsToTheRightBottom() {
        Player p = player(2, 2, 5, 2, 7);
        fillLeft(9, 4);
        right.addCard(new Card(1));

        Player.TurnResult turn = p.playTurn();

        assertEquals(9, turn.getCardDrawn().getCardValue());
        assertEquals(5, turn.getCardDisCarded().getCardValue());
        assertEquals(List.of(4), values(left.getContents()));
        assertEquals(List.of(1, 5), values(right.getContents()));
    }

    @Test
    void handHoldsExactlyFourCardsAfterEveryTurn() {
        Player p = player(2, 1, 3, 4, 5);
        fillLeft(6, 7, 8, 9, 1, 3, 2, 2);
        for (int i = 0; i < 8; i++) {
            assertEquals(Player.HAND_SIZE, p.playTurn().getPlayersHand().size());
            assertEquals(Player.HAND_SIZE, p.getHand().size());
        }
    }

    @Test
    void neverDiscardsAPreferredCard() {
        Player p = player(2, 2, 1, 1, 1);
        fillLeft(2, 3, 2, 4, 5, 6);
        for (int i = 0; i < 6; i++) {
            assertNotEquals(2, p.playTurn().getCardDisCarded().getCardValue());
        }
    }

    @Test
    void discardsTheOldestNonPreferredCard() {
        Player p = player(2, 5, 2, 6, 7);
        fillLeft(8);
        assertEquals(5, p.playTurn().getCardDisCarded().getCardValue());
        assertEquals(List.of(2, 6, 7, 8), values(p.getHand()));
    }

    @Test
    void aDrawnNonPreferredCardIsDiscardedIfItIsTheOnlyOne() {
        Player p = player(2, 2, 2, 2, 9);
        fillLeft(3);
        Player.TurnResult turn = p.playTurn();
        assertEquals(9, turn.getCardDisCarded().getCardValue());
        assertEquals(List.of(2, 2, 2, 3), values(turn.getPlayersHand()));
    }

    
    @Test
    void doesNotHoldANonPreferredCardIndefinitely() {
        Player p = player(1, 7, 8, 9, 6);
        fillLeft(2, 3, 4, 5, 2, 3, 4, 5);
        List<Integer> discarded = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            discarded.add(p.playTurn().getCardDisCarded().getCardValue());
        }
        assertEquals(List.of(7, 8, 9, 6), discarded);
    }

    @Test
    void drawingTheFourthPreferredCardProducesAWinningHand() {
        Player p = player(2, 2, 2, 8, 2);
        fillLeft(2);
        Player.TurnResult turn = p.playTurn();
        assertTrue(turn.isWinningHand());
        assertTrue(p.hasWinningHand());
        assertEquals(List.of(2, 2, 2, 2), values(turn.getPlayersHand()));
    }

    @Test
    void fourOfANonPreferredValueIsAlsoAWinningHand() {
        assertTrue(player(1, 6, 6, 6, 6).hasWinningHand());
        assertFalse(player(1, 6, 6, 6, 1).hasWinningHand());
    }

    @Test
    void aTurnFailsOnceTheLeftDeckRunsOut() {
        Player p = player(2, 1, 3, 4, 5);
        fillLeft(6, 7);
        p.playTurn();
        p.playTurn();
        assertEquals(List.of(4, 5, 6, 7), values(p.getHand()));
        assertThrows(IllegalStateException.class, p::playTurn);
        assertEquals(List.of(4, 5, 6, 7), values(p.getHand()), "a failed turn must not change the hand");
    }

    @Test
    void cardsAreConservedAcrossATurn() {
        Player p = player(2, 1, 2, 3, 4);
        fillLeft(5, 6);
        int before = p.getHand().size() + left.size() + right.size();
        p.playTurn();
        assertEquals(before, p.getHand().size() + left.size() + right.size());
    }

    @Test
    void aTurnOnAnEmptyDeckFailsWithoutChangingTheHand() {
        Player p = player(2, 1, 2, 3, 4);
        assertThrows(IllegalStateException.class, p::playTurn);
        assertEquals(List.of(1, 2, 3, 4), values(p.getHand()));
        assertEquals(0, right.size());
    }

    @Test
    void cannotBeDealtMoreThanFourCards() {
        Player p = player(1, 1, 2, 3, 4);
        assertThrows(IllegalStateException.class, () -> p.recieveCard(new Card(5)));
    }

    @Test
    void worksWhenDrawingAndDiscardingToTheSameDeck() {
        CardDeck only = new CardDeck(1);
        Player p = new Player(1, only, only, new GameState(1), dir);
        for (int v : new int[] {1, 1, 1, 5}) {
            p.recieveCard(new Card(v));
        }
        only.addCard(new Card(1));
        Player.TurnResult turn = p.playTurn();
        assertTrue(turn.isWinningHand());
        assertEquals(List.of(5), values(only.getContents()));
    }

    @Test
    void immediateWinnerWritesTheFourLineFileAndAnnouncesTheWin() {
        Player p = player(1, 3, 3, 3, 3);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            Thread t = new Thread(p);
            t.start();
            t.join();
        });
        assertNull(p.getFailure());
        assertEquals(1, state.getWinner());
        assertEquals(List.of(
                "player 1 initial hand 3 3 3 3",
                "player 1 wins",
                "player 1 exits",
                "player 1 final hand: 3 3 3 3"), read(p.getOutputFile()));
        assertEquals(0, left.size() + right.size(), "no card should move after an immediate win");
    }

    @Test
    void aPlayerToldOfAnotherWinnerStopsAndReportsIt() {
        Player p = player(1, 1, 2, 3, 4);
        fillLeft(5, 6, 7);
        state.declareWinner(4);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            Thread t = new Thread(p);
            t.start();
            t.join();
        });
        assertEquals(List.of(
                "player 1 initial hand 1 2 3 4",
                "player 4 has informed player 1 that player 4 has won",
                "player 1 exits",
                "player 1 hand: 1 2 3 4"), read(p.getOutputFile()));
    }

    @Test
    void aPlayerThreadLogsEachActionAndDeclaresAMidGameWin() {
        Player p = player(2, 2, 2, 5, 2);
        fillLeft(7, 2);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            Thread t = new Thread(p);
            t.start();
            t.join();
        });
        assertEquals(2, state.getWinner());
        assertEquals(List.of(
                "player 2 initial hand 2 2 5 2",
                "player 2 draws a 7 from deck 2",
                "player 2 discards a 5 to deck 3",
                "player 2 current hand is 2 2 2 7",
                "player 2 draws a 2 from deck 2",
                "player 2 discards a 7 to deck 3",
                "player 2 current hand is 2 2 2 2",
                "player 2 wins",
                "player 2 exits",
                "player 2 final hand: 2 2 2 2"), read(p.getOutputFile()));
    }

    @Test
    void anUnwritableOutputFileAbortsTheGameInsteadOfHangingIt() throws Exception {
        Path notADirectory = Files.writeString(dir.resolve("file"), "x");
        Player p = new Player(1, left, right, state, notADirectory);
        for (int v : new int[] {1, 2, 3, 4}) {
            p.recieveCard(new Card(v));
        }
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            Thread t = new Thread(p);
            t.start();
            t.join();
        });
        assertTrue(state.isAborted());
        assertTrue(p.getFailure() != null);
    }

    private static List<String> read(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
