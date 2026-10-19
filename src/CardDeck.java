package src;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


public class CardDeck { //class for each individual deck in the game, contains a deque of cards and methods to add, draw, and await cards
  private final int cardDeckID;
  private final Deque<Card> cardsInDeck = new ArrayDeque<>();

  public CardDeck(int cardDeckID) { //constructor for the card deck class, takes in the id of the deck as an argument
    if(cardDeckID < 1){
      throw new IllegalArgumentException("Deck id must be positive " + cardDeckID);
    }
    this.cardDeckID = cardDeckID;
  }
    
  public int getDeckID() { //method to get the id of the deck
    return this.cardDeckID;
  }

  public synchronized void addCard(Card card){ //method to add a card to the deck, takes in a card as an argument
    if (card == null){
      throw new NullPointerException("card");
    }
    cardsInDeck.addLast(card);
    notifyAll();
  }

  public synchronized Card drawCard(){ //method to draw a card from the deck, returns the card drawn
    return cardsInDeck.pollFirst();
  }

  public synchronized boolean awaitCard(long timeoutMillis) throws InterruptedException{ //method to wait for a card to be added to the deck, takes in a timeout in milliseconds as an argument, returns true if a card was added before the timeout, false otherwise
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

  public synchronized int size() { //method to get the size of the deck, returns the number of cards in the deck
    return cardsInDeck.size();
  }

  public synchronized List<Card> getContents() { //method to get the contents of the deck, returns a list of cards in the deck
    return new ArrayList<>(cardsInDeck);
  }

  public String describeContents(){ //method to describe the contents of the deck, returns a string describing the contents of the deck
    return ("deck" + cardDeckID + " contents: " + Player.formatCards(getContents())).stripTrailing(); 
  }

  public Path writeContents(Path directory) throws IOException { //method to write the contents of the deck to a file, takes in a directory as an argument, returns the path to the file written
        Path file = directory.resolve("deck" + cardDeckID + "_output.txt");
        Files.writeString(file, describeContents() + System.lineSeparator(), StandardCharsets.UTF_8);
        return file;
    }

    @Override
    public String toString() { //overrides the toString method to return the id of the deck as a string
        return "deck" + cardDeckID;
    }

}
