import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import src.Card;

class CardTest {

    @Test
    void holdsItsFaceValue() {
        assertEquals(7, new Card(7).getCardValue());
    }

    @Test
    void zeroIsAValidFaceValue() {
        assertEquals(0, new Card(0).getCardValue());
    }

    @Test
    void valuesLargerThanThePlayerCountAreAllowed() {
        assertEquals(Integer.MAX_VALUE, new Card(Integer.MAX_VALUE).getCardValue());
    }

    @Test
    void negativeFaceValueIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Card(-1));
    }

    @Test
    void printsAsItsValue() {
        assertEquals("12", new Card(12).toString());
    }
}
