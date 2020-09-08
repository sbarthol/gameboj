// Gameboj stage 1

package ch.epfl.gameboj.component.memory;

import static org.junit.jupiter.api.Assertions.*;
import static ch.epfl.test.TestRandomizer.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.component.memory.Ram;

class RamTest {
    @Test
    void constructorFailsForNegativeSize() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v = - rng.nextInt(Integer.MAX_VALUE);
            assertThrows(IllegalArgumentException.class,
                    () -> new Ram(v));
        }
    }

    @Test
    void sizeReturnsSize() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int s = rng.nextInt(100_000);
            Ram r = new Ram(s);
            assertEquals(s, r.size());
        }
    }

    @Test
    void readReadsWhatWriteWrote() {
        Random rng = newRandom();
        int size = 10_000;
        Ram r = new Ram(size);
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int a = rng.nextInt(size);
            int b = rng.nextInt() & 0xFF;
            r.write(a, b);
            assertEquals(b, r.read(a));
        }
    }

    @Test
    void readFailsForInvalidIndex() {
        Ram ram = new Ram(0);
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int j = 0;
            while (j == 0)
                j = rng.nextInt();
            int k = j;
            assertThrows(IndexOutOfBoundsException.class,
                    () -> ram.read(k));
        }
    }

    @Test
    void writeFailsForInvalidIndex() {
        int size = 100;
        Ram ram = new Ram(size);
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int j = 0;
            while (0 <= j && j < size)
                j = rng.nextInt();
            int k = j;
            assertThrows(IndexOutOfBoundsException.class,
                    () -> ram.write(k, 0));
        }
    }
    
    @Test
    void writeFailsForInvalidValue() {
        Ram ram = new Ram(1);
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int j = 0;
            while (0 <= j && j <= 0xFF)
                j = rng.nextInt();
            int k = j;
            assertThrows(IllegalArgumentException.class,
                    () -> ram.write(0, k));
        }
    }
}
