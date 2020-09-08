// Gameboj stage 1

package ch.epfl.gameboj;

import static ch.epfl.test.TestRandomizer.RANDOM_ITERATIONS;
import static ch.epfl.test.TestRandomizer.newRandom;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import org.junit.jupiter.api.Test;

public class PreconditionsTest {
    @Test
    void checkArgumentSucceedsForTrue() {
        Preconditions.checkArgument(true);
    }

    @Test
    void checkArgumentFailsForFalse() {
        assertThrows(IllegalArgumentException.class,
                () -> Preconditions.checkArgument(false));
    }

    @Test
    void checkBits8SucceedsFor8BitValues() {
        for (int i = 0; i <= 0xFF; ++i)
            Preconditions.checkBits8(i);
    }

    @Test
    void chechBits8FailsForNegativeValues() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v = - rng.nextInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class,
                    () -> Preconditions.checkBits8(v));
        }
    }

    @Test
    void checkBits8FailsForTooBigValues() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v = 0x100 + rng.nextInt(1000);
            assertThrows(IllegalArgumentException.class,
                    () -> Preconditions.checkBits8(v));
        }
    }

    @Test
    void checkBits16SucceedsFor16BitValues() {
        for (int i = 0; i <= 0xFFFF; ++i)
            Preconditions.checkBits16(i);
    }

    @Test
    void chechBits16FailsForNegativeValues() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v = - rng.nextInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class,
                    () -> Preconditions.checkBits16(v));
        }
    }

    @Test
    void checkBits16FailsForTooBigValues() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v = 0x10000 + rng.nextInt(1000);
            assertThrows(IllegalArgumentException.class,
                    () -> Preconditions.checkBits16(v));
        }
    }
}
