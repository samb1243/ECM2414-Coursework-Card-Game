import java.util.Stack;

public class CardDeck {
  private final int cardDeckID;
  private Stack<Card> cardsInDeck;

  CardDeck(int deckID) {
      this.cardDeckID = deckID;
      this.cardsInDeck = new Stack<Card>();
    }
  
  public Stack<Card> getCardsInDeck() {
      return this.cardsInDeck;
    }
  public int getDeckID() {
    return this.cardDeckID;
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
