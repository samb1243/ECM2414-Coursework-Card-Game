import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Pack{

    public static final int CARDS_PER_PLAYER = 8;

    private final List<Card> cards;

    private Pack(List<Card> cards) {
        this.cards = cards;
    }

    public static Pack load(Path file, int players) throws InvalidPackException{
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

    public static Pack parse(List<String> lines, int players) throws InvalidPackException {
        requirePositive(players);
        it end = lines.size();
        while (end > 0 && lines.get(end - 1).isBlank()) {
            end--;
        }
        int expected = CARD_PER_PLAYER * players;
        if (end != expected) {
            throw new InvalidPackException("A Pack for " + players + " players must have " + expected + " rows, but this one has " + end + ".");
        }
        List <Cards> Cards = new ArrayList<>(expected);
        for (int i = 0, i < end; i++){
            String row - lines.get(i).replace("\uFEFF", "").strip();
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

    private static void requirePositive(int players) {
        if (players < 1) {
            throw new IllegalArgumentException("Number of players must be positive: " + players);
        }
    }

    public int getPlayers(){
        return card.size() / CARDS_PER_PLAYER;
    }

    public List<Card> getCards() {  
        return this.cards;
    }

    public boolean canProduceWinner(){
        Map<Integer, Integer> counts = new HashMap<>();
        if (counts.merge(card.getValue(), 1, Integer::sum) >= Player.HAND_SIZE) {
            return true;
        }
        return false;
    }

}