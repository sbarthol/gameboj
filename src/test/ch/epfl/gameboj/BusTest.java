// Gameboj stage 1

package ch.epfl.gameboj;

import static ch.epfl.test.TestRandomizer.RANDOM_ITERATIONS;
import static ch.epfl.test.TestRandomizer.newRandom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.component.Component;

class BusTest {
    private static SimpleComponent[] newComponents(int n) {
        SimpleComponent[] cs = new SimpleComponent[n];
        for (int i = 0; i < cs.length; ++i) {
            cs[i] = new SimpleComponent(i, i);
        }
        return cs;
    }

    @Test
    void attachFailsForNullComponent() {
        Bus b = new Bus();
        assertThrows(NullPointerException.class,
                () -> b.attach(null));
    }

    @Test
    void readFailsForInvalidAddress() {
        Bus b = new Bus();
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int a0 = rng.nextInt();
            while (0 <= a0 && a0 <= 0xFFFF)
                a0 += 0xFFFF;
            int a = a0;
            assertThrows(IllegalArgumentException.class,
                    () -> b.read(a));
        }
    }

    @Test
    void readReturnsCorrectValue() {
        Component[] cs = newComponents(20);
        Collections.shuffle(Arrays.asList(cs), newRandom());
        Bus b = new Bus();
        for (Component c: cs)
            b.attach(c);
        for (int i = 0; i < cs.length; ++i)
            assertEquals(i, b.read(i));
    }

    @Test
    void readReturnsCorrectDefaultValue() {
        Random rng = newRandom();
        Bus b = new Bus();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int a = rng.nextInt(0x1_0000);
            assertEquals(0xFF, b.read(a));
        }
    }

    @Test
    void writeWritesToAllComponents() {
        SimpleComponent[] cs = newComponents(20);
        Bus b = new Bus();
        for (Component c: cs)
            b.attach(c);
        b.write(0, 42);
        for (SimpleComponent c: cs)
            assertTrue(c.wasWritten());
    }

    @Test
    void writeWritesCorrectValue() {
        SimpleComponent[] cs = newComponents(20);
        Bus b = new Bus();
        for (Component c: cs)
            b.attach(c);
        for (int i = 0; i < cs.length; ++i)
            b.write(i, (i * 2018) & 0xFF);
        for (int i = 0; i < cs.length; ++i)
            assertEquals((i * 2018) & 0xFF, b.read(i));
    }

    @Test
    void writeFailsForInvalidAddress() {
        Random rng = newRandom();
        Bus b = new Bus();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int a0 = rng.nextInt();
            while (0 <= a0 && a0 <= 0xFFFF)
                a0 += 0xFFFF;
            int a = a0;
            assertThrows(IllegalArgumentException.class,
                    () -> b.write(a, 0));
        }
    }

    @Test
    void writeFailsForInvalidData() {
        Random rng = newRandom();
        Bus b = new Bus();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int d0 = rng.nextInt();
            while (0 <= d0 && d0 <= 0xFF)
                d0 += 0xFF;
            int d = d0;
            assertThrows(IllegalArgumentException.class,
                    () -> b.write(0, d));
        }
    }
}

class SimpleComponent implements Component {
    private final int address;
    private int value;
    private boolean wasRead, wasWritten;

    public SimpleComponent(int address, int initialValue) {
        this.address = address;
        this.value = initialValue;
    }

    boolean wasRead() { return wasRead; }
    boolean wasWritten() { return wasWritten; }

    @Override
    public int read(int a) {
        wasRead = true;
        return a == address ? value : Component.NO_DATA;
    }

    @Override
    public void write(int a, int d) {
        wasWritten = true;
        if (a == address)
            value = d;
    }
}
