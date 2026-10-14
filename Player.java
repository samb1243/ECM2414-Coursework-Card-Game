import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Player implements Runnable {
  public static final int HAND_SIZE = 4;

  private static final long WAIT_MILLIS = 20;
  
  private final int playerID;
  private final CardDeck deckToDrawFromID;
  private final CardDeck deckToDiscardToID;
  private final GameState state;
  private final Path outputDirectory;
  private List<Card> cardsInHand = new ArrayList<>(HAND_SIZE + 1);

  private volatile Throwable failure;
  

  public Player(int playerID, CardDeck deckToDrawFromID, CardDeck deckToDiscardToID, GameState state, Path outputDirectory){
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

  public int getPlayerID() {
    return playerID;
  }

  public int getPrefferedValue(){
    return playerID; 
  }


  public synchronized void recieveCard (Card card) {
    if (card == null){
      throw new IllegalStateException("player " + playerID + " already holds " + HAND_SIZE +  " cards");  
    }
    if (cardsInHand.size() >= HAND_SIZE){
      throw new IllegalStateException("player " + playerID + " already holds" + HAND_SIZE + " cards");
    }
    cardsInHand.add(card);
  }

  public synchronized List<Card> getHand(){
    return new ArrayList<>(cardsInHand);
  }

  public synchronized boolean hasWinningHand() {
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

  public Path getOutputFile() {
    return outputDirectory.resolve("player" + playerID + "_output.txt");
  } 

  public Throwable getFailure() {
    return failure;
  }

  public TurnResult playTurn(){
    CardDeck first = deckToDrawFromID.getDeckID() <= deckToDiscardToID.getDeckID() ? deckToDrawFromID : deckToDrawFromID;
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
          return new TurnResult(cardDrawn, cardDiscarded, cardsInHand, false);
        }
      }
    }
  }

  private int indexOfDiscard(){
    for (int i = 0; i < cardsInHand.size(); i++){
      if(cardsInHand.get(i).getCardValue() != playerID){
        return i;
      }
    }
    return 0;
  }



  @Override 
  public void run() {
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
      log(out, "player " + playerID + (winner == playerID ? " final hand: " : " hand:") + formatCards(getHand()));
      
    } catch (InterruptedException e ){
      Thread.currentThread().interrupt();
      fail(e);
    } catch (IOException  | RuntimeException e){
      fail(e);
    }
  }


  private void playUntilGameOver(BufferedWriter out) throws IOException, InterruptedException {
    while (!state.isOver()){
      if (!deckToDrawFromID.awaitCard(WAIT_MILLIS)){
        continue;
      }
      if(state.isOver()){
        break;
      }
      TurnResult turn = playTurn();
      log(out, "player " + playerID + " draws a " + turn.getCardDrawn() + " from deck " + deckToDrawFromID.getDeckID());
      log(out, "player " + playerID + " discards a " + turn.getCardDisCarded() + " to deck " + deckToDiscardToID.getDeckID());
      log(out, "player " + playerID + " current hand is " + formatCards(turn.getPlayersHand()));
      if(turn.isWinningHand()){
        announceWin(out);
      }
    }
  }

  private void announceWin(BufferedWriter out) throws IOException {
    if (state.declareWinner(playerID)){
      System.out.println("Player " + playerID + " wins");
      log(out, "player " + playerID + " wins");
    }
  }


  private void fail(Throwable e){
    failure = e;
    state.abort();
  }

  private static void log(BufferedWriter out, String line) throws IOException {
    out.write(line);
    out.newLine();
  }

  public static String formatCards(List<Card> cards) {
    return cards.stream().map(Card::toString).collect(Collectors.joining(" "));
  }

  @Override 
  public String toString(){
    return "player" + playerID;
  }



  public static final class TurnResult {
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

    public Card getCardDrawn(){
      return cardDrawn;
    }

    public Card getCardDisCarded(){
      return cardDiscarded;
    }

    public List<Card> getPlayersHand(){
      return playersHand;
    }

    public boolean isWinningHand(){
      return winningHand;
    }

  }
}
