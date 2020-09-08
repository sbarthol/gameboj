// Gameboj stage 1

package ch.epfl.gameboj.component.memory;

import static ch.epfl.test.TestRandomizer.RANDOM_ITERATIONS;
import static ch.epfl.test.TestRandomizer.newRandom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.component.memory.Rom;

class RomTest {
    @Test
    void constructorFailsForNullArray() {
        assertThrows(NullPointerException.class,
                () -> new Rom(null));
    }

    @Test
    void sizeReturnsSize() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int s = rng.nextInt(10_000);
            Rom r = new Rom(new byte[s]);
            assertEquals(s, r.size());
        }
    }

    @Test
    void readReturnsDataGivenToConstructor() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            byte[] a = new byte[100];
            rng.nextBytes(a);
            byte[] a1 = Arrays.copyOf(a, a.length);
            Rom r = new Rom(a);
            for (int j = 0; j < a1.length; ++j)
                assertEquals(Byte.toUnsignedInt(a1[j]), r.read(j));
        }
    }

    @Test
    void readFailsForInvalidIndex() {
        Rom rom = new Rom(new byte[0]);
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int j = 0;
            while (j == 0)
                j = rng.nextInt();
            int k = j;
            assertThrows(IndexOutOfBoundsException.class,
                    () -> rom.read(k));
        }
    }
}
