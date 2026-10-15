import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Pack{ //class for the pack of cards, contains a list of cards and methods to load and parse the pack from a file

    public static final int CARDS_PER_PLAYER = 8;

    private final List<Card> cards;

    private Pack(List<Card> cards) { //constructor for the pack class, takes in a list of cards as an argument
        this.cards = cards;
    }

    public static Pack load(Path file, int players) throws InvalidPackException{ //method to load a pack of cards from a file, takes in the path to the file and the number of players as arguments, returns a Pack object
        requirePositive(players);
        if (file == null || !Files.isRegularFile (file)){
            throw new InvalidPackException("Invalid file path: " + file);
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            throw new InvalidPackException("Could not read pack file " + file + ": " + e.getMessage(), e);
        }
        return parse (lines, players);
    }

    public static Pack parse(List<String> lines, int players) throws InvalidPackException { //method to parse a pack of cards from a list of strings, takes in the list of strings and the number of players as arguments, returns a Pack object
        requirePositive(players);
        int end = lines.size();
        while (end > 0 && lines.get(end - 1).isBlank()) {
            end--;
        }
        int expected = CARDS_PER_PLAYER * players;
        if (end != expected) {
            throw new InvalidPackException("A Pack for " + players + " players must have " + expected + " rows, but this one has " + end + ".");
        }
        List<Card> cards = new ArrayList<>(expected);
        for (int i = 0; i < end; i++){
            String row = lines.get(i).replace("\uFEFF", "").strip();
            int value;
            try {
                value = Integer.parseInt(row);
            } catch (NumberFormatException e){
                throw new InvalidPackException ("Row " + (i + 1) +  " is not a single integer: \"" + row + "\"");
            }
            if (value < 0) {
                throw new InvalidPackException ("Row " + (i + 1) + " is negative: " + value);
            }
            cards.add(new Card(value));
        }
        return new Pack(cards);
    }

    private static void requirePositive(int players) { //method to check if the number of players is positive, takes in the number of players as an argument, throws an IllegalArgumentException if the number of players is not positive
        if (players < 1) {
            throw new IllegalArgumentException("Number of players must be positive: " + players);
        }
    }

    public int getPlayers(){ //method to get the number of players, returns the number of players in the pack
        return cards.size() / CARDS_PER_PLAYER;
    }

    public List<Card> getCards() {  //method to get the list of cards, returns the list of cards in the pack
        return this.cards;
    }

    public boolean canProduceWinner(){ //method to check if the pack can produce a winner, returns true if the pack can produce a winner, false otherwise
        Map<Integer, Integer> counts = new HashMap<>();
        for (Card card : cards){
            if (counts.merge(card.getCardValue(), 1, Integer::sum) >= Player.HAND_SIZE) {
                return true;
            }
        }
        return false;
    }

}