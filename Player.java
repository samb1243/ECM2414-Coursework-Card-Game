import java.utils.ArrayList;

public class Player {
  private final int playerID;
  private ArrayList<Card> cardsInHand;

  Player(int playerID) {
      this.playerID = playerID;
      this.cardsInHand;
    }
  
  public int getCardsInHand() {
      return this.cardsInHand;
    }
  public int getPlayerID() {
    return this.playerID;
  }
}
