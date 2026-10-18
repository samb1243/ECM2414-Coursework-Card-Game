import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import scr.InvalidPackException;
import scr.Pack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PackTest {

    @TempDir
    Path dir;

    private Path write(String content) throws Exception {
        Path file = dir.resolve("pack.txt");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private static String rows(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(Integer::toString).collect(Collectors.joining("\n")) + "\n";
    }

    /** Builds a pack straight from card values, without a pack file. */
    private static Pack pack(int players, int... values) throws InvalidPackException {
        return Pack.parse(IntStream.of(values).mapToObj(Integer::toString).collect(Collectors.toList()), players);
    }

    @Test
    void loadsAValidPackInFileOrder() throws Exception {
        Pack pack = Pack.load(write(rows(16)), 2);
        assertEquals(16, pack.getCards().size());
        assertEquals(1, pack.getCards().get(0).getCardValue());
        assertEquals(16, pack.getCards().get(15).getCardValue());
        assertEquals(2, pack.getPlayers());
    }

    @Test
    void acceptsZeroAndValuesGreaterThanN() throws Exception {
        Pack pack = pack(1, 0, 0, 0, 0, 99, 99, 99, 99);
        assertEquals(0, pack.getCards().get(0).getCardValue());
        assertEquals(99, pack.getCards().get(7).getCardValue());
    }

    @Test
    void toleratesSurroundingWhitespaceWindowsLineEndingsAndAByteOrderMark() throws Exception {
        String content = "\uFEFF 1 \r\n2\r\n3\r\n4\r\n5\r\n6\r\n7\r\n8\r\n\r\n";
        assertEquals(8, Pack.load(write(content), 1).getCards().size());
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 9, 15, 17, 0})
    void rejectsTheWrongNumberOfRows(int rows) throws Exception {
        Path file = write(rows == 0 ? "" : rows(rows));
        int players = rows < 12 ? 1 : 2;
        InvalidPackException e = assertThrows(InvalidPackException.class, () -> Pack.load(file, players));
        assertTrue(e.getMessage().contains("must have " + 8 * players + " rows"), e.getMessage());
    }

    @Test
    void rejectsANegativeValue() throws Exception {
        Path file = write("1\n2\n3\n-4\n5\n6\n7\n8\n");
        InvalidPackException e = assertThrows(InvalidPackException.class, () -> Pack.load(file, 1));
        assertTrue(e.getMessage().contains("Row 4"), e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"x", "1.5", "1 2", "", "99999999999"})
    void rejectsRowsThatAreNotASingleInteger(String badRow) throws Exception {
        Path file = write("1\n2\n" + badRow + "\n4\n5\n6\n7\n8\n");
        assertThrows(InvalidPackException.class, () -> Pack.load(file, 1));
    }

    @Test
    void rejectsAMissingFileOrADirectory() {
        assertThrows(InvalidPackException.class, () -> Pack.load(dir.resolve("missing.txt"), 1));
        assertThrows(InvalidPackException.class, () -> Pack.load(dir, 1));
    }

    @Test
    void rejectsANonPositivePlayerCount() {
        assertThrows(IllegalArgumentException.class, () -> Pack.parse(List.of(), 0));
    }

    @Test
    void reportsWhetherAWinIsPossible() throws Exception {
        assertTrue(pack(1, 5, 1, 5, 2, 5, 3, 5, 4).canProduceWinner());
        assertFalse(pack(1, 1, 1, 1, 2, 2, 2, 3, 3).canProduceWinner());
    }
}
