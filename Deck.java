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

  public void addCardsToDeck(Card card) { //adding all the cards initally to the deck
    this.cardsInDeck.push(card); //change to add all cards when we have done the unpacked the pack
  }

  //pushing and popping cards during the game
  public void pushCardToDeck(Card card) {
    this.cardsInDeck.push(card);
  }

  public void popCardFromDeck() {
    this.cardsInDeck.pop();
  }

}
