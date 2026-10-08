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

/*
get valid inputs

set up
  initialise pack/cards
  initialise plahyers/hands
  intialise decks

run each turn
  start threads
  each player draws
  each player determins preferred value?
  each player discards card
  write to file?
*/
public class Game{

  private final List<Player> players;
  private final List<CardDeck> decks;
  private String GameState = " "; 
  private final Path outputDirectory;
  private final List<Thread> threads = new ArrayList<Thread>();
  
  


}