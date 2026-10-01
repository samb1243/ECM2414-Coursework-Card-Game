import java.util.ArrayList;

public class Player {
  private final int playerID;
  private ArrayList<Card> cardsInHand;
  private int prefferedCard;

  Player(int playerID) {
      this.playerID = playerID;
      this.cardsInHand = new ArrayList<Card>();
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

  public void setNewPrefferedCardValue(Card card) {
    
    this.prefferedCard = 0; //change to check for most common card in the list of cards in hand initally 
  }
}
