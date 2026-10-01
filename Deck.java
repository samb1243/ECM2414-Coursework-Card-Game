import java.util.Stack;

public class Deck {
  private final int deckID;
  private Stack<Card> cardsInDeck;

  Deck(int deckID) {
      this.deckID = deckID;
      this.cardsInDeck = new Stack<Card>();
    }
  
  public Stack<Card> getCardsInDeck() {
      return this.cardsInDeck;
    }
  public int getDeckID() {
    return this.deckID;

    
  }

