// Gameboj stage 1

package ch.epfl.gameboj.component.memory;

import static ch.epfl.test.TestRandomizer.RANDOM_ITERATIONS;
import static ch.epfl.test.TestRandomizer.newRandom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.ComponentTest;

class RamControllerTest implements ComponentTest {
    @Override
    public Component newComponent() {
        return new RamController(new Ram(1), 0);
    }

    Ram newRamFF() {
        Ram r = new Ram(0xFF + 1);
        for (int i = 0; i <= 0xFF; ++i)
            r.write(i, i);
        return r;
    }

    @Test
    void constructorFailsForNullRam() {
        assertThrows(NullPointerException.class,
                () -> new RamController(null, 0));
    }

    @Test
    void constructorFailsForInvalidRange() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            Ram r = newRamFF();
            int a = rng.nextInt(0x1000);
            assertThrows(IllegalArgumentException.class,
                    () -> new RamController(r, a, a + r.size() + 1 + rng.nextInt(10)));
        }
    }

    @Test
    void readCorrectlyTranslatesAddresses() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            Ram r = newRamFF();
            int a = rng.nextInt(0xFFFF - r.size());
            RamController c = new RamController(r, a);
            for (int j = 0; j < 256; ++j)
               assertEquals(j, c.read(a + j));
        }
    }

    @Test
    void readHandlesAddressesOutsideRange() {
        Random rng = newRandom();
        Ram r = newRamFF();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int a = rng.nextInt(0xFF00);
            RamController rc = new RamController(r, a);
            assertEquals(Component.NO_DATA, rc.read(a - 1));
            assertNotEquals(Component.NO_DATA, rc.read(a));
            assertNotEquals(Component.NO_DATA, rc.read(a + r.size() - 1));
            assertEquals(Component.NO_DATA, rc.read(a + r.size()));
        }
    }

    @Test
    void writeCorrectlyTranslatesAddresses() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            Ram r = newRamFF();
            int a = rng.nextInt(0xFFFF - r.size());
            RamController c = new RamController(r, a);
            int xor = 0b1010_0101;
            for (int j = 0; j < 256; ++j)
                c.write(a + j, j ^ xor);
            for (int j = 0; j < 256; ++j)
                assertEquals(j ^ xor, r.read(j));
        }
    }

    @Test
    void writeHandlesAddressesOutsideRange() {
        Random rng = newRandom();
        Ram r = newRamFF();
        int o = 0x1000;
        RamController c = new RamController(r, o);
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int a = rng.nextInt(o);
            c.write(a, 0xDA);
        }
        for (int i = 0; i < r.size(); ++i)
            assertEquals(i, r.read(i));
    }
}
