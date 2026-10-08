import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


public class CardDeck {
  private final int cardDeckID;
  private final Deque<Card> cardsInDeck = new ArrayDeque<>();

  CardDeck(int cardDeckID) {
    if(cardDeckID < 1){
      throw new IllegalArgumentException("Deck id must be positive " + cardDeckID);
    }
    this.cardDeckID = cardDeckID;
  }
    
  public int getDeckID() {
    return this.cardDeckID;
  }

  public synchronized void addCard(Card card){
    if (card == null){
      throw new NullPointerException("card");
    }
    cardsInDeck.addLast(card);
    notifyAll();
  }

  public synchronized Card drawCard(){
    return cardsInDeck.pollFirst();
  }

  public synchronized boolean awaitCard(long timeoutMillis) throws InterruptedException{
    long deadline = System.currentTimeMillis() + timeoutMillis;
    while (cardsInDeck.isEmpty()){
      long remaining = deadline - System.currentTimeMillis();
      if (remaining <= 0){
        return false;
      }
      wait(remaining);
    }
    return true;
  }

  public synchronized int size() {
    return cardsInDeck.size();
  }

  public synchronized List<Card> getContents() {
    return new ArrayList<>(cardsInDeck);
  }

  public String describeContents(){
    return "deck" + cardDeckID + " contents: " + Player.formatCards(getContents())).stripTrailing(); //not been made yet
  }

  public Path writeContents(Path directory) throws IOException {
        Path file = directory.resolve("deck" + cardDeckID + "_output.txt");
        Files.writeString(file, describeContents() + System.lineSeparator(), StandardCharsets.UTF_8);
        return file;
    }

    @Override
    public String toString() {
        return "deck" + cardDeckID;
    }

}
