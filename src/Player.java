package src;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Player implements Runnable { //class for each individual player in the game, contains the player's id, the decks to draw from and discard to, the game state, the output directory, and the player's hand of cards
  public static final int HAND_SIZE = 4;

  private static final long WAIT_MILLIS = 20;
  
  private final int playerID;
  private final CardDeck deckToDrawFromID;
  private final CardDeck deckToDiscardToID;
  private final GameState state;
  private final Path outputDirectory;
  private List<Card> cardsInHand = new ArrayList<>(HAND_SIZE + 1);

  private volatile Throwable failure;
  

  public Player(int playerID, CardDeck deckToDrawFromID, CardDeck deckToDiscardToID, GameState state, Path outputDirectory){ //constructor for the player class, takes in the player's id, the decks to draw from and discard to, the game state, and the output directory as arguments
    if (playerID < 1){
      throw new IllegalArgumentException("Player id must be positive: " + playerID);
    }
    if (deckToDrawFromID == null || deckToDiscardToID == null || state == null || outputDirectory == null ){
      throw new NullPointerException("Player dependencies must not be null");
    }
    this.playerID = playerID;
    this.deckToDrawFromID = deckToDrawFromID;
    this.deckToDiscardToID = deckToDiscardToID;
    this.state = state;
    this.outputDirectory = outputDirectory;
  }

  public int getPlayerID() { //method to get the player's id, returns the player's id
    return playerID;
  }

  public int getPrefferedValue(){ //method to get the player's preferred value, returns the player's id as the preferred value
    return playerID; 
  }


  public synchronized void recieveCard (Card card) { //method to receive a card, takes in a card as an argument, adds the card to the player's hand, throws an exception if the player already has the maximum number of cards
    if (card == null){
      throw new IllegalStateException("player " + playerID + " already holds " + HAND_SIZE +  " cards");  
    }
    if (cardsInHand.size() >= HAND_SIZE){
      throw new IllegalStateException("player " + playerID + " already holds" + HAND_SIZE + " cards");
    }
    cardsInHand.add(card);
  }

  public synchronized List<Card> getHand(){ //method to get the player's hand, returns a copy of the player's hand
    return new ArrayList<>(cardsInHand);
  }

  public synchronized boolean hasWinningHand() { //method to check if the player has a winning hand, returns true if the player has a winning hand, false otherwise
    if (cardsInHand.size() != HAND_SIZE){
      return false;
    }
    int first = cardsInHand.get(0).getCardValue();
    for (Card card : cardsInHand) {
      if(card.getCardValue() != first) {
        return false;
      }
    }
    return true;
  }

  public Path getOutputFile() { //method to get the path to the player's output file, returns the path to the player's output file
    return outputDirectory.resolve("player" + playerID + "_output.txt");
  } 

  public Throwable getFailure() { //method to get the player's failure, returns the player's failure, or null if the player has not failed
    return failure;
  }

  public TurnResult playTurn(){ //method to play a turn, draws a card from the deck to draw from, discards a card to the deck to discard to, returns a TurnResult object containing the card drawn, the card discarded, the player's hand, and whether the player has a winning hand
    CardDeck first = deckToDrawFromID.getDeckID() <= deckToDiscardToID.getDeckID() ? deckToDrawFromID : deckToDiscardToID; //could be where it is buggint out witht he inconsistant turns 
    CardDeck second = first == deckToDrawFromID ? deckToDiscardToID : deckToDrawFromID; 
    synchronized(first){
      synchronized(second){
        synchronized(this){
          if (cardsInHand.size() != HAND_SIZE){
            throw new IllegalStateException("player " + playerID + " must hold " + HAND_SIZE + " cards");
          }
          Card cardDrawn = deckToDrawFromID.drawCard();
          if (cardDrawn == null){
            throw new IllegalStateException(deckToDrawFromID +" is empty");
          }
          cardsInHand.add(cardDrawn);
          Card cardDiscarded = cardsInHand.remove(indexOfDiscard());
          deckToDiscardToID.addCard(cardDiscarded);
          return new TurnResult(cardDrawn, cardDiscarded, cardsInHand, hasWinningHand());
        }
      }
    }
  }

  private int indexOfDiscard(){ //method to get the index of the card to discard, returns the index of the first card that does not match the player's preferred value, or 0 if all cards match the player's preferred value
    for (int i = 0; i < cardsInHand.size(); i++){
      if(cardsInHand.get(i).getCardValue() != playerID){
        return i;
      }
    }
    return 0;
  }



  @Override 
  public void run() { //method to run the player thread, writes the player's initial hand to the output file, plays turns until the game is over, writes the player's final hand to the output file, and handles any exceptions that occur during the game
    try(BufferedWriter out = Files.newBufferedWriter(getOutputFile(), StandardCharsets.UTF_8)){
      try {
        log(out, "player " + playerID + " initial hand " + formatCards(getHand()));
        if (hasWinningHand()) {
          announceWin(out);
        }
      } finally {
        state.markReady();
      }
      state.awaitsStart();
      playUntilGameOver(out);
      int winner = state.getWinner();
      if (winner != playerID && winner != GameState.NO_WINNER){
        log(out, "player " + winner + " has informed player " + playerID + " that player " + winner + " has won");
      }
      log(out, "player " + playerID + " exits");
      log(out, "player " + playerID + (winner == playerID ? " final hand: " : " hand: ") + formatCards(getHand()));
      
    } catch (InterruptedException e ){
      Thread.currentThread().interrupt();
      fail(e);
    } catch (IOException  | RuntimeException e){
      fail(e);
    } finally {
      state.leaveRounds(); //stop the other players waiting for this one at the end of a round
    }
  }


  private void playUntilGameOver(BufferedWriter out) throws IOException, InterruptedException { //method to play turns until the game is over, takes in a BufferedWriter as an argument, writes the player's actions to the output file, and handles any exceptions that occur during the game
    boolean gameOver = state.awaitEndOfRound(); //round 0: waits for every player, so nobody can win a turn before everyone has checked for a dealt winning hand
    while (!gameOver){
      while (!deckToDrawFromID.awaitCard(WAIT_MILLIS)){
        if (state.isAborted()){
          return;
        }
      }
      TurnResult turn = playTurn();
      log(out, "player " + playerID + " draws a " + turn.getCardDrawn() + " from deck " + deckToDrawFromID.getDeckID());
      log(out, "player " + playerID + " discards a " + turn.getCardDisCarded() + " to deck " + deckToDiscardToID.getDeckID());
      log(out, "player " + playerID + " current hand is " + formatCards(turn.getPlayersHand()));
      if(turn.isWinningHand()){
        announceWin(out);
      }
      gameOver = state.awaitEndOfRound(); //the game only ends between rounds, so everyone finishes the round a winner was found in
    }
  }

  private void announceWin(BufferedWriter out) throws IOException { //method to announce the player's win, takes in a BufferedWriter as an argument, writes the player's win to the output file, and declares the player as the winner in the game state
    if (state.declareWinner(playerID)){
      System.out.println("Player " + playerID + " wins");
      log(out, "player " + playerID + " wins");
    }
  }


  private void fail(Throwable e){ //method to handle the player's failure, takes in a Throwable as an argument, sets the player's failure to the Throwable, and aborts the game state
    failure = e;
    state.abort();
  }

  private static void log(BufferedWriter out, String line) throws IOException { //method to log a line to the output file, takes in a BufferedWriter and a string as arguments, writes the string to the output file, and adds a new line
    out.write(line);
    out.newLine();
  }

  public static String formatCards(List<Card> cards) { //method to format a list of cards as a string, takes in a list of cards as an argument, returns a string representation of the list of cards
    return cards.stream().map(Card::toString).collect(Collectors.joining(" "));
  }

  @Override 
  public String toString(){ //overrides the toString method to return the player's id as a string
    return "player" + playerID;
  }



  public static final class TurnResult { //class for the result of a turn, contains the card drawn, the card discarded, the player's hand, and whether the player has a winning hand
    private final Card cardDrawn;
    private final Card cardDiscarded;
    private final List<Card> playersHand;
    private final boolean winningHand;

    TurnResult(Card cardDrawn, Card cardDiscarded, List<Card> playersHand, boolean winningHand){
      this.cardDrawn = cardDrawn;
      this.cardDiscarded = cardDiscarded;
      this.playersHand = List.copyOf(playersHand);
      this.winningHand = winningHand;
    }

    public Card getCardDrawn(){ //method to get the card drawn, returns the card drawn
      return cardDrawn;
    }

    public Card getCardDisCarded(){ //method to get the card discarded, returns the card discarded
      return cardDiscarded;
    }

    public List<Card> getPlayersHand(){ //method to get the player's hand, returns a copy of the player's hand
      return playersHand;
    }

    public boolean isWinningHand(){ //method to check if the player has a winning hand, returns true if the player has a winning hand, false otherwise  
      return winningHand;
    }

  }
}
