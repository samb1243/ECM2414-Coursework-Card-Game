package src;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public final class GameState { //class for the state of the game, contains the winner of the game, a latch to wait for all players to be ready, and a flag to indicate if the game has been aborted
    public static final int NO_WINNER = 0;

    private static final long POLL_MILLIS = 50;

    private final AtomicInteger winner = new AtomicInteger(NO_WINNER);
    private final CountDownLatch startGate;
    private final Phaser roundBarrier; //makes every player finish a turn before anyone starts the next one, so all players take the same number of turns
    private volatile boolean aborted;
    private volatile boolean overAtEndOfRound; //whether the game was over when the last round finished, read by every player so they all stop after the same round

    public GameState (int players){ //constructor for the game state class, takes in the number of players as an argument
        if (players < 1){
            throw new IllegalArgumentException("At least one player is required: " + players);
        }
        this.startGate = new CountDownLatch(players);
        this.roundBarrier = new Phaser(players) {
            @Override
            protected boolean onAdvance(int round, int registeredParties) { //runs once, after the last player finishes their turn and before any player starts the next round
                overAtEndOfRound = isOver();
                return registeredParties == 0;
            }
        };

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

    public boolean awaitEndOfRound() { //method to wait until every player has finished their turn for this round, returns true if the game is over and no more rounds should be played
        roundBarrier.arriveAndAwaitAdvance();
        return overAtEndOfRound;
    }

    public void leaveRounds() { //method for a player that has stopped playing early, so the other players do not wait for it at the end of each round
        roundBarrier.arriveAndDeregister();
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
