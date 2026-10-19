import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import src.Card;
import src.CardDeck;

class CardDeckTest {

    @TempDir
    Path dir;

    @Test
    void drawsFromTheTopAndDiscardsToTheBottom() {
        CardDeck deck = new CardDeck(1);
        deck.addCard(new Card(1));
        deck.addCard(new Card(2));
        deck.addCard(new Card(3));

        assertEquals(1, deck.drawCard().getCardValue());
        deck.addCard(new Card(9));
        assertEquals(List.of(2, 3, 9), deck.getContents().stream().map(Card::getCardValue).toList());
        assertEquals(2, deck.drawCard().getCardValue());
        assertEquals(3, deck.drawCard().getCardValue());
        assertEquals(9, deck.drawCard().getCardValue());
    }

    @Test
    void drawingFromAnEmptyDeckReturnsNull() {
        CardDeck deck = new CardDeck(1);
        assertNull(deck.drawCard());
        assertEquals(0, deck.size());
    }

    @Test
    void rejectsNullCardsAndInvalidIds() {
        assertThrows(NullPointerException.class, () -> new CardDeck(1).addCard(null));
        assertThrows(IllegalArgumentException.class, () -> new CardDeck(0));
    }

    @Test
    void contentsAreASnapshotNotALiveView() {
        CardDeck deck = new CardDeck(1);
        deck.addCard(new Card(5));
        List<Card> snapshot = deck.getContents();
        deck.addCard(new Card(6));
        snapshot.clear();
        assertEquals(2, deck.size());
    }

    @Test
    void writesContentsInTheRequiredFormat() throws Exception {
        CardDeck deck = new CardDeck(2);
        for (int v : new int[] {1, 3, 3, 7}) {
            deck.addCard(new Card(v));
        }
        Path file = deck.writeContents(dir);
        assertEquals(dir.resolve("deck2_output.txt"), file);
        assertEquals(List.of("deck2 contents: 1 3 3 7"), Files.readAllLines(file));
    }

    @Test
    void writesAnEmptyDeckWithoutTrailingSpace() throws Exception {
        Path file = new CardDeck(4).writeContents(dir);
        assertEquals(List.of("deck4 contents:"), Files.readAllLines(file));
    }

    // Many producers and consumers share one deck. Every card added must be drawn exactly once: none lost and none duplicated.
    
    @Test
    void concurrentAddAndDrawConservesEveryCard() {
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            CardDeck deck = new CardDeck(1);
            int producers = 4;
            int perProducer = 5_000;
            List<Card> drawn = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch go = new CountDownLatch(1);
            AtomicBoolean producing = new AtomicBoolean(true);
            List<Thread> threads = new ArrayList<>();
            for (int p = 0; p < producers; p++) {
                int base = p * perProducer;
                threads.add(new Thread(() -> {
                    await(go);
                    for (int i = 0; i < perProducer; i++) {
                        deck.addCard(new Card(base + i));
                    }
                }));
            }
            List<Thread> consumers = new ArrayList<>();
            for (int c = 0; c < 4; c++) {
                consumers.add(new Thread(() -> {
                    await(go);
                    while (producing.get() || deck.size() > 0) {
                        Card card = deck.drawCard();
                        if (card != null) {
                            drawn.add(card);
                        }
                    }
                }));
            }
            threads.forEach(Thread::start);
            consumers.forEach(Thread::start);
            go.countDown();
            for (Thread t : threads) {
                t.join();
            }
            producing.set(false);
            for (Thread t : consumers) {
                t.join();
            }

            assertEquals(producers * perProducer, drawn.size());
            assertEquals(producers * perProducer, drawn.stream().map(Card::getCardValue).distinct().count());
            assertEquals(0, deck.size());
        });
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
