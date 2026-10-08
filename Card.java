public class Card {
  private final int cardValue;

  Card(int cardValue) {
    if (cardValue < 0) {
      throw new IllegalArgumentException("Card value must be non-negative: " + cardValue);
    }
      this.cardValue = cardValue;
    }
  public int getCardValue() {
        return this.cardValue;
    }
}

