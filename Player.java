import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Player {
  public static final int HAND_SIZE = 4;

  private static final long WAIT_MILLIS = 20;
  
  private final int playerID;
  private final CardDeck deckToDrawFromID;
  private final CardDeck deckToDiscardToID;
  private static final GameState state;
  private final Path outputDirectory;
  private List<Card> cardsInHand = new ArrayList<>(HAND_SIZE + 1);

  private int prefferedValue;
  

  Player(int playerID) {
    this.playerID = playerID;
    this.cardsInHand = new ArrayList<Card>();
    this.deckToDrawFromID = null; //will be changed when decks are set
    this.deckToDiscardToID = null; //same again
    }
  
  
  public List<Card> getCardsInHand() {
    return this.cardsInHand;
    }
  public int getPlayerID() {
    return this.playerID;
  }

  public int getPrefferedValue(){
    return this.prefferedValue;
  }

  //may not be needed but will keep here just in case 
  public void setNewPrefferedValue() {
    int[][] frequency = {{-1, -1, -1, -1, -1}, {-1, -1, -1, -1, -1}};
    int j = 0;
    for (Card card : cardsInHand) {
      int value = card.getCardValue();
      for (int i = 0; i < 5; i++) {
        if (value == frequency[0][i]) {
          frequency[1][i]++;
          break;
        } else if (i == 4){
          frequency[0][j] = value;
          frequency[1][j] = 1;
          j++;
        }
      }
    }
    int maxValue = frequency[0][0];
    int maxFrequency = frequency[1][0];
    for (int i = 1; i < 5; i++) {
      if (frequency[1][i] > maxFrequency) {
        maxValue = frequency[0][i];
        maxFrequency = frequency[1][i];
      }
    }
    this.prefferedValue = maxValue;
  }

  public void drawCard(){    //fix
    //this.ArrayList<Card>(4) = deck.deckToDrawFromID(0);
  }


  
}
