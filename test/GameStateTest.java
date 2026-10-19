import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import src.GameState;

public class GameStateTest { //tests for the GameState class

  @Test
  void rejectsNonPositivePlayerCounts() {
    assertThrows(IllegalArgumentException.class, () -> new GameState(0));
  }

  @Test
  void startsWithNoWinner() {
    GameState state = new GameState(2);
    assertEquals(GameState.NO_WINNER, state.getWinner());
    assertFalse(state.isOver());
    assertFalse(state.isAborted());
  }

  @Test
  void firstPlayerToDeclareWins() {
    GameState state = new GameState(3);
    assertTrue(state.declareWinner(2));
    assertFalse(state.declareWinner(1));
    assertFalse(state.declareWinner(3));
    assertEquals(2, state.getWinner());
    assertTrue(state.isOver());
  }

  @Test
  void rejectsNonPositiveWinnerIds() {
    assertThrows(IllegalArgumentException.class, () -> new GameState(1).declareWinner(0));
  }

  @Test
  void nobodyCanWinAnAbortedGame() {
    GameState state = new GameState(2);
    state.abort();
    assertTrue(state.isAborted());
    assertTrue(state.isOver());
    assertFalse(state.declareWinner(1));
    assertEquals(GameState.NO_WINNER, state.getWinner());
  }

  @Test
  void awaitsStartReturnsOnceEveryPlayerIsReady() {
    GameState state = new GameState(2);
    state.markReady();
    state.markReady();
    assertTimeoutPreemptively(Duration.ofSeconds(1), state::awaitsStart);
  }

  @Test
  void awaitsStartReturnsWhenTheGameIsAborted() {
    GameState state = new GameState(2);
    state.markReady(); //second player never becomes ready
    state.abort();
    assertTimeoutPreemptively(Duration.ofSeconds(1), state::awaitsStart);
  }

  @Test
  void awaitsStartBlocksUntilTheLastPlayerIsReady() throws InterruptedException {
    GameState state = new GameState(2);
    state.markReady();
    Thread waiter = new Thread(() -> {
      try {
        state.awaitsStart();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    waiter.start();
    waiter.join(200);
    assertTrue(waiter.isAlive(), "should still be waiting for the second player");

    state.markReady();
    waiter.join(1_000);
    assertFalse(waiter.isAlive());
  }

  @Test
  void endOfRoundReportsWhetherTheGameIsOver() {
    GameState state = new GameState(1);
    assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
      assertFalse(state.awaitEndOfRound());
      state.declareWinner(1);
      assertTrue(state.awaitEndOfRound());
    });
  }

  @Test
  void endOfRoundWaitsForEveryPlayer() throws InterruptedException {
    GameState state = new GameState(2);
    boolean[] result = new boolean[1];
    Thread first = new Thread(() -> result[0] = state.awaitEndOfRound());
    first.start();
    first.join(200);
    assertTrue(first.isAlive(), "should wait for the second player to finish the round");

    state.declareWinner(2);
    assertTrue(state.awaitEndOfRound());
    first.join(1_000);
    assertFalse(first.isAlive());
    assertTrue(result[0], "both players should see that the game is over");
  }

  @Test
  void playersWhoLeaveDoNotHoldUpTheRound() {
    GameState state = new GameState(2);
    state.leaveRounds();
    assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertFalse(state.awaitEndOfRound()));
  }
}
