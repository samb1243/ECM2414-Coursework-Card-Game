import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

