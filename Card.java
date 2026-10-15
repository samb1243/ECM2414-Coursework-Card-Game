//class for each invidivual card in the deck, contains the value of the card and a method to get the value of the card
public class Card {
  private final int cardValue;

  Card(int cardValue) { //constructor for the card class, takes in the value of the card as an argument
    if (cardValue < 0) {
      throw new IllegalArgumentException("Card value must be non-negative: " + cardValue);
    }
      this.cardValue = cardValue;
    }
  public int getCardValue() { //method to get the value of the card
        return this.cardValue;
    }

  @Override
  public String toString() { //overrides the toString method to return the value of the card as a string
    return String.valueOf(cardValue);
  }
}
