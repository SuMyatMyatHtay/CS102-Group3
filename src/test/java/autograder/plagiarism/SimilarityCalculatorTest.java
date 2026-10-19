package autograder.plagiarism;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimilarityCalculatorTest {
    private final SimilarityCalculator calculator = new SimilarityCalculator();

    @Test
    void identicalSetsScoreOne() {
        assertEquals(1.0, calculator.jaccard(Set.of("a", "b"), Set.of("a", "b")));
    }

    @Test
    void disjointSetsScoreZero() {
        assertEquals(0.0, calculator.jaccard(Set.of("a"), Set.of("b")));
    }

    @Test
    void partialOverlapIsSharedDividedByUnion() {
        assertEquals(0.5, calculator.jaccard(Set.of("a", "b", "c"), Set.of("b", "c", "d")));
        assertEquals(2, calculator.sharedCount(Set.of("a", "b", "c"), Set.of("b", "c", "d")));
    }

    @Test
    void twoEmptySetsScoreZeroInsteadOfDividingByZero() {
        assertEquals(0.0, calculator.jaccard(Set.of(), Set.of()));
    }
}
