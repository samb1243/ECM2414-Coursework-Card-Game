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
    
    this.prefferedCard = 0; //change to check for most common card in the list of cards in hand initally 
  }

  public void drawCard(){    //fix
    this.ArrayList<Card>(4) = deck.deckToDrawFromID (0)
  }
}
