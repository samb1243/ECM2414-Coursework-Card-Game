import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public final class GameState { //class for the state of the game, contains the winner of the game, a latch to wait for all players to be ready, and a flag to indicate if the game has been aborted
    public static final int NO_WINNER = 0;

    private static final long POLL_MILLIS = 50;

    private final AtomicInteger winner = new AtomicInteger(NO_WINNER);
    private final CountDownLatch startGate;
    private volatile boolean aborted;

    public GameState (int players){ //constructor for the game state class, takes in the number of players as an argument
        if (players < 1){
            throw new IllegalArgumentException("At least one player is required: " + players);
        }
        this.startGate = new CountDownLatch(players);

    }

    public boolean declareWinner (int playerID){ //method to declare the winner of the game, takes in the id of the player as an argument, returns true if the player is the winner, false otherwise
        if(playerID < 1){
            throw new IllegalArgumentException("Player id must be positive: " + playerID);
        }
        return !aborted && winner.compareAndSet(NO_WINNER, playerID);
    }

    public int getWinner() { //method to get the winner of the game, returns the id of the winner, or NO_WINNER if there is no winner
        return winner.get();
    }

    public boolean isOver() { //method to check if the game is over, returns true if the game is over, false otherwise
        return aborted || winner.get() != NO_WINNER;
    }

    public boolean isAborted() { //method to check if the game has been aborted, returns true if the game has been aborted, false otherwise
        return aborted;
    }

    public void abort() { //method to abort the game, sets the aborted flag to true
        aborted = true;
    }

    public void markReady() { //method to mark a player as ready, decrements the latch count
        startGate.countDown();
    }
    
    public void awaitsStart() throws InterruptedException { //method to wait for all players to be ready, waits for the latch to reach zero or for the game to be over
        while (!isOver() && !startGate.await(POLL_MILLIS, TimeUnit.MILLISECONDS)){
            //re-checks isOver() so an aborted game cannot leave threads stuck here
        }
    }

    
}
