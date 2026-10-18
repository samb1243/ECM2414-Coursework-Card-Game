package scr;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public final class CardGame{ //class for the card game, contains a list of players, a list of decks, the game state, the output directory, and methods to start and play the game

  private final List<Player> players;
  private final List<CardDeck> decks;
  private final GameState state; 
  private final Path outputDirectory;
  private final List<Thread> threads = new ArrayList<>();
  
  public CardGame(Pack pack, Path outputDirectory) { //constructor for the card game class, takes in a pack of cards and the output directory as arguments, creates the players and decks, and deals the cards to the players and decks
    int n = pack.getPlayers();
    this.outputDirectory = outputDirectory;
    this.state = new GameState(n);
    List<CardDeck> deckList = new ArrayList<>(n);
    for (int i = 1; i <= n; i++){
      deckList.add(new CardDeck(i));
    }
    List <Player> playerlist = new ArrayList<>(n);
    for (int i = 1; i <= n; i++){
      playerlist.add(new Player(i, deckList.get(i-1), deckList.get(i % n), state, outputDirectory));
    }
    this.decks = List.copyOf(deckList);
    this.players = List.copyOf(playerlist);
    deal(pack.getCards());
  }

  private void deal(List<Card> cards) { //method to deal the cards to the players and decks, takes in a list of cards as an argument, deals the cards to the players and decks in a round-robin fashion
    int n = players.size();
    int handCards = Player.HAND_SIZE * n;
    for (int i = 0; i < cards.size(); i++){
      if (i < handCards){
        players.get(i % n).recieveCard(cards.get(i));
      } else {
        decks.get(i % n).addCard(cards.get(i));
      }
    }
  }

  public List<Player> getPlayers() { //method to get the list of players, returns a list of players
    return players;
  }

  public List<CardDeck> getDecks(){ //method to get the list of decks, returns a list of decks
    return decks;
  }

  public GameState getState() { //method to get the game state, returns the game state
    return state;
  }

  public synchronized void start() { //method to start the game, creates a thread for each player and starts the threads
    if (!threads.isEmpty()) {
      throw new IllegalStateException("Game already started");
    }
    for (Player player : players) {
      Thread thread = new Thread(player, player.toString());
      threads.add(thread);
    }
    threads.forEach(Thread::start);
  }

  public int awaitCompletion() throws InterruptedException, IOException { //method to wait for the game to complete, waits for all threads to finish, writes the contents of the decks to the output directory, and returns the id of the winner
    List<Thread> started;
    synchronized (this) {
      started = new ArrayList<>(threads);
    }
    for (Thread thread :  started){
      thread.join();
    }
    for (CardDeck deck : decks) {
      deck.writeContents(outputDirectory);
    }
    for (Player player : players) {
      if (player.getFailure() != null) {
        throw new IllegalStateException(player + " failed: " + player.getFailure(), player.getFailure());
      } 
    }
    return state.getWinner();
  }

  public int play() throws InterruptedException, IOException{ //method to play the game, starts the game and waits for it to complete, returns the id of the winner
    start();
    return awaitCompletion();
  }

  public List<Card> snapshotAllCards() { //method to get a snapshot of all cards in the game, returns a list of all cards in the players' hands and decks
    return snapshotFrom(0);
  }

  private List<Card> snapshotFrom(int index) { //method to get a snapshot of all cards in the game from a given index, takes in an index as an argument, returns a list of all cards in the players' hands and decks from the given index
    if (index == decks.size()) {
      List<Card> all = new ArrayList<>();
      for (Player player : players){
        all.addAll(player.getHand());
      }
      for (CardDeck deck : decks) {
        all.addAll(deck.getContents());
      }
      return all;
    }
    synchronized (decks.get(index)){
      return snapshotFrom(index + 1);
    }
  }
  


  // command line entry 

  public static void main (String[] args) { //method to run the game from the command line, takes in an array of strings as arguments, reads the number of players and the pack of cards from the console, and plays the game
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
    int status = runFromConsole(in, System.out, Paths.get(""));
    if (status != 0){
      System.exit(status);
    }

  }

  public static int runFromConsole(BufferedReader in, PrintStream out, Path outputDirectory){ //method to run the game from the console, takes in a BufferedReader, a PrintStream, and a Path as arguments, reads the number of players and the pack of cards from the console, and plays the game
    try{
      Integer players = readPlayers(in, out);
      if(players == null){
        out.println("no input recieved; exiting");
        return 1;
      }
      Pack pack = readPack(in, out, players);
      if (pack == null){
        out.println("No input recieved; exiting");
        return 1;
      }
      if (!pack.canProduceWinner()){
        out.println("no card value appears four times in this pack, therefore nobody can win the game." + "Press Ctrl + C to stop.");
      }
      new CardGame(pack, outputDirectory).play();
      return 0;
    } catch (IOException e) {
      out.println("I/O error: " + e.getMessage());
      return 1;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      out.println("Interrupted");
      return 1;
    } catch (IllegalStateException e) {
      out.println ("The game stopped unexpectedly: " + e.getMessage());
      return 1;
    }
  }

  private static Integer readPlayers(BufferedReader in, PrintStream out) throws IOException { //method to read the number of players from the console, takes in a BufferedReader and a PrintStream as arguments, returns the number of players as an Integer
    while (true) {
      out.println("Please enter the number of players");
      String line = readLine(in);
      if (line == null){
        return null;
      }
      try {
        int n =  Integer.parseInt(line);
        if (n >= 1) {
          return n;
        } 
      } catch (NumberFormatException e) {

      }
      out.println("Invalid number of players \"" + line + "\" :please enter a positive integer.");
    }
  }


  private static Pack readPack(BufferedReader in, PrintStream out, int players) throws IOException { //method to read the pack of cards from the console, takes in a BufferedReader, a PrintStream, and the number of players as arguments, returns a Pack object
    while (true){
      out.println("Please enter location of pack to load");
      String line = readLine(in);
      if(line == null){
        return null;
      }
      try {
        Path file = Paths.get(line);
        return Pack.load(file, players);
      } catch (InvalidPathException e) {
        out.println("Invalid pack: \"" + line + "\" is not a valid path.");
      } catch (InvalidPackException e){
        out.println("Invalid pack: " + e.getMessage());
      }

    }
  }

  private static String readLine(BufferedReader in) throws IOException { //method to read a line from the console, takes in a BufferedReader as an argument, returns the line read as a String
    String line = in.readLine();
    return line == null ? null : line.replace("\uFEFF","").strip();
  }



}