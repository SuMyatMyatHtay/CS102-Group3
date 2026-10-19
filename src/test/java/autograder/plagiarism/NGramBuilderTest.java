package autograder.plagiarism;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NGramBuilderTest {
    @Test
    void buildsOneWindowPerStartingPosition() {
        List<Token> tokens = List.of(new Token("a", 1), new Token("b", 1), new Token("c", 2),
                new Token("d", 3));

        Map<String, LineRange> grams = new NGramBuilder(3).build("Q1a.java", tokens);

        assertEquals(List.of("a b c", "b c d"), List.copyOf(grams.keySet()));
        assertEquals(new LineRange("Q1a.java", 1, 2), grams.get("a b c"));
        assertEquals(new LineRange("Q1a.java", 1, 3), grams.get("b c d"));
    }

    @Test
    void repeatedWindowKeepsItsFirstLocation() {
        List<Token> tokens = List.of(new Token("x", 1), new Token("y", 1), new Token("x", 5),
                new Token("y", 5));

        Map<String, LineRange> grams = new NGramBuilder(2).build("A.java", tokens);

        assertEquals(new LineRange("A.java", 1, 1), grams.get("x y"));
    }

    @Test
    void fewerTokensThanTheWindowGivesNoGrams() {
        assertTrue(new NGramBuilder(5).build("A.java", List.of(new Token("a", 1))).isEmpty());
    }

    @Test
    void rejectsWindowSmallerThanOne() {
        assertThrows(IllegalArgumentException.class, () -> new NGramBuilder(0));
    }
}
