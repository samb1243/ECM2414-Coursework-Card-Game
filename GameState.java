import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public final class GameState {
    public static final int NO_WINNER = 0;

    private static final long POLL_MILLIS = 50;

    private final AtomicInteger winner = new AtomicInteger(NO_WINNER);
    private final CountDownLatch startGate;
    private volatile boolean aborted;

    public GameState (int players){
        if (players < 1){
            throw new IllegalArgumentException("At least one player is required: " + players);
        }
        this.startGate = new CountDownLatch(players);

    }

    public boolean declareWinner (int playerID){
        if(playerID < 1){
            throw new IllegalArgumentException("Player id must be positive: " + playerID);
        }
        return !aborted && winner.compareAndSet(NO_WINNER, playerID);
    }

    public int getWinner() {
        return winner.get();
    }

    public boolean isOver() {
        return aborted || winner.get() != NO_WINNER;
    }

    public boolean isAborted() {
        return aborted;
    }

    public void abort() {
        aborted = true;
    }

    public void markReady() {
        startGate.countDown();
    }
    
    public void awaitsStart() throws InterruptedException {
        while (!isOver() && !startGate.await(POLL_MILLIS, TimeUnit.MILLISECONDS)){
            //re-checks isOver() so an aborted game cannot leave threads stuck here
        }
    }

    
}
