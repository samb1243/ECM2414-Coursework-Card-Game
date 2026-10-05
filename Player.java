import java.util.ArrayList;

public class Player {
  private final int playerID;
  private ArrayList<Card> cardsInHand;
  private int prefferedCard;
  private final deck deckToDrawFromID;
  private final deck deckToDiscardToID;

  Player(int playerID) {
    this.playerID = playerID;
    this.cardsInHand = new ArrayList<Card>();
    this.deckToDrawFrom;
    this.deckToDiscardTo;
    }

  public void setDecks() {
    this.deckToDrawFrom = playerID;  //use get deck using deckID?
      if (playerID = n) {
        this.deckToDiscardTo = 0;  //use get deck using deckID?
      } else {
        this.deckToDiscardTo = playerID;  //use get deck using deckID?
      }
  }
  
  public ArrayList<Card> getCardsInHand() {
    return this.cardsInHand;
    }
  public int getPlayerID() {
    return this.playerID;
  }

  public int getPrefferedCardValue(){
    return this.prefferedCard;
  }

  public void setNewPrefferedCardValue() {
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
    this.prefferedCard = maxValue;
  }

  public void drawCard(){    //fix
    this.ArrayList<Card>(4) = deck.deckToDrawFromID (0)
  }
}
