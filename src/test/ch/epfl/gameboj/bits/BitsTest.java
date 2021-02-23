// Gameboj stage 1

package ch.epfl.gameboj.bits;

import static ch.epfl.test.TestRandomizer.RANDOM_ITERATIONS;
import static ch.epfl.test.TestRandomizer.newRandom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class BitsTest {
    private static int[] ALL_MASKS = new int[] {
            0b00000000000000000000000000000001,
            0b00000000000000000000000000000010,
            0b00000000000000000000000000000100,
            0b00000000000000000000000000001000,
            0b00000000000000000000000000010000,
            0b00000000000000000000000000100000,
            0b00000000000000000000000001000000,
            0b00000000000000000000000010000000,
            0b00000000000000000000000100000000,
            0b00000000000000000000001000000000,
            0b00000000000000000000010000000000,
            0b00000000000000000000100000000000,
            0b00000000000000000001000000000000,
            0b00000000000000000010000000000000,
            0b00000000000000000100000000000000,
            0b00000000000000001000000000000000,
            0b00000000000000010000000000000000,
            0b00000000000000100000000000000000,
            0b00000000000001000000000000000000,
            0b00000000000010000000000000000000,
            0b00000000000100000000000000000000,
            0b00000000001000000000000000000000,
            0b00000000010000000000000000000000,
            0b00000000100000000000000000000000,
            0b00000001000000000000000000000000,
            0b00000010000000000000000000000000,
            0b00000100000000000000000000000000,
            0b00001000000000000000000000000000,
            0b00010000000000000000000000000000,
            0b00100000000000000000000000000000,
            0b01000000000000000000000000000000,
            0b10000000000000000000000000000000,
    };

    private enum AllBits implements Bit {
        B00, B01, B02, B03, B04, B05, B06, B07,
        B08, B09, B10, B11, B12, B13, B14, B15,
        B16, B17, B18, B19, B20, B21, B22, B23,
        B24, B25, B26, B27, B28, B29, B30, B31,
    };

    @Test
    void maskFailsForInvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> Bits.mask(-1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> Bits.mask(Integer.SIZE));
    }

    @Test
    void maskWorksBetween0And31() {
        for (int i = 0; i < Integer.SIZE; ++i)
            assertEquals(ALL_MASKS[i], Bits.mask(i));
    }

    @Test
    void testFailsForInvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> Bits.test(0, -1));
        assertThrows(IndexOutOfBoundsException.class,
                () -> Bits.test(0, Integer.SIZE));
    }

    @Test
    void testWorksForValuesWithSingleBitSet() {
        for (int i = 0; i < Integer.SIZE; ++i) {
            int m = ALL_MASKS[i];
            for (int j = 0; j < Integer.SIZE; ++j) {
                assertEquals(i == j, Bits.test(m, j));
            }
        }
    }

    @Test
    void test2WorksForValuesWithSingleBitSet() {
        for (int i = 0; i < Integer.SIZE; ++i) {
            int m = ALL_MASKS[i];
            for (Bit b: AllBits.values()) {
                assertEquals(i == b.ordinal(), Bits.test(m, b));
            }
        }
    }

    @Test
    void setFailsForInvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> Bits.set(0, -1, true));
        assertThrows(IndexOutOfBoundsException.class,
                () -> Bits.set(0, Integer.SIZE, true));
    }

    @Test
    void setDoesNotChangeValueWhenItDoesNotChangeBit() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v = rng.nextInt();
            int j = rng.nextInt(Integer.SIZE);
            assertEquals(v, Bits.set(v, j, Bits.test(v, j)));
        }
    }

    @Test
    void setCanSetAndClearAllIndividualBits() {
        for (int i = 0; i < Integer.SIZE; ++i) {
            int m = ALL_MASKS[i];
            assertEquals(m, Bits.set(0, i, true));
            assertEquals(0, Bits.set(m, i, false));
        }
    }

    @Test
    void clipFailsForInvalidSize() {
        assertThrows(IllegalArgumentException.class,
                () -> Bits.clip(-1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Bits.clip(Integer.SIZE + 1, 0));
    }

    @Test
    void clipWorksForMaximumSize() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v = rng.nextInt();
            assertEquals(v, Bits.clip(Integer.SIZE, v));
            assertEquals(~v, Bits.clip(Integer.SIZE, ~v));
        }
    }

    @Test
    void clipWorksForNonMaximumSizes() {
        Random rng = newRandom();
        for (int i = 0; i < Integer.SIZE; ++i) {
            for (int j = 0; j < RANDOM_ITERATIONS; ++j) {
                int b = Bits.clip(i, rng.nextInt());
                assertTrue(0 <= b && b <= ALL_MASKS[i] - 1);
            }
        }
    }

    @Test
    void extractFailsForInvalidRanges() {
        int[][] invalidRanges = new int[][] {
            { -1, 1 },
            { 0, Integer.SIZE + 1 },
            { 1, Integer.SIZE }
        };
        for (int[] r: invalidRanges) {
            assertThrows(IndexOutOfBoundsException.class,
                    () -> Bits.extract(0, r[0], r[1]));
        }
    }

    @Test
    void extractAndTestAreCompatible() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v = rng.nextInt();
            int b = rng.nextInt(Integer.SIZE);
            int e = Bits.test(v, b) ? 1 : 0;
            assertEquals(e, Bits.extract(v, b, 1));
        }
    }

    @Test
    void extractCanExtractEverything() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v = rng.nextInt();
            assertEquals(v, Bits.extract(v, 0, Integer.SIZE));
        }
    }

    @Test
    void extractCanExtractNothing() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v = rng.nextInt();
            int b = rng.nextInt(Integer.SIZE);
            assertEquals(0, Bits.extract(v, b, 0));
        }
    }

    @Test
    void extractWorksOnKnownValues() {
        assertEquals(0xF, Bits.extract(0xFEDCBA98, 28, 4));
        assertEquals(0xE, Bits.extract(0xFEDCBA98, 24, 4));
        assertEquals(0xD, Bits.extract(0xFEDCBA98, 20, 4));
        assertEquals(0xC, Bits.extract(0xFEDCBA98, 16, 4));
        assertEquals(0xB, Bits.extract(0xFEDCBA98, 12, 4));
        assertEquals(0xA, Bits.extract(0xFEDCBA98,  8, 4));
        assertEquals(0x9, Bits.extract(0xFEDCBA98,  4, 4));
        assertEquals(0x8, Bits.extract(0xFEDCBA98,  0, 4));
    }

    @Test
    void rotateFailsForInvalidSize() {
        assertThrows(IllegalArgumentException.class,
                () -> Bits.rotate(0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Bits.rotate(Integer.SIZE + 1, 0, 0));
    }

    @Test
    void rotateWorksOnFullInt() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v = rng.nextInt();
            int d = rng.nextInt();
            assertEquals(Integer.rotateLeft(v, d), Bits.rotate(Integer.SIZE, v, d));
            assertEquals(Integer.rotateLeft(~v, d), Bits.rotate(Integer.SIZE, ~v, d));
        }
    }

    @Test
    void rotateDoesNothingWhenAllBitsAreSet() {
        Random rng = newRandom();
        for (int s = 1; s < Integer.SIZE; ++s) {
            int v = ALL_MASKS[s] - 1;
            for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
                int d = rng.nextInt();
                assertEquals(v, Bits.rotate(s, v, d));
            }
        }
    }

    @Test
    void rotateDoesNothingWhenDistanceIsAMultipleOfSize() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v8 = rng.nextInt(0b1_0000_0000);
            int k = rng.nextInt(11) - 5;
            assertEquals(v8, Bits.rotate(8, v8, k * 8));
        }
    }

    @Test
    void rotateIsPeriodic() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v24 = rng.nextInt(0x100_0000);
            int d = rng.nextInt(24);
            int k = rng.nextInt(11) - 5;
            assertEquals(Bits.rotate(24, v24, d), Bits.rotate(24, v24, d + 24 * k));
        }
    }

    @Test
    void rotateWorksOnKnownValues() {
        assertEquals(0b1, Bits.rotate(1, 0b1, 1));
        assertEquals(0b1, Bits.rotate(1, 0b1, -1));

        assertEquals(0b10, Bits.rotate(2, 0b1, 1));
        assertEquals(0b10, Bits.rotate(2, 0b1, -1));

        assertEquals(0b010, Bits.rotate(3, 0b1, 1));
        assertEquals(0b100, Bits.rotate(3, 0b1, 2));
        assertEquals(0b100, Bits.rotate(3, 0b1, -1));

        assertEquals(0b1010, Bits.rotate(4, 0b0101, 1));
        assertEquals(0b1010, Bits.rotate(4, 0b0101, -3));
        assertEquals(0b0101, Bits.rotate(4, 0b0101, 2));
        assertEquals(0b0101, Bits.rotate(4, 0b0101, -2));
        assertEquals(0b1010, Bits.rotate(4, 0b0101, 3));

        assertEquals(0b11010, Bits.rotate(5, 0b01101, 1));
        assertEquals(0b10101, Bits.rotate(5, 0b01101, 2));
        assertEquals(0b01011, Bits.rotate(5, 0b01101, 3));
        assertEquals(0b10110, Bits.rotate(5, 0b01101, 4));
    }

    @Test
    void signExtend8FailsOnInvalidValue() {
        assertThrows(IllegalArgumentException.class,
                () -> Bits.signExtend8(0x100));
        assertThrows(IllegalArgumentException.class,
                () -> Bits.signExtend8(-1));
    }

    @Test
    void signExtend8WorksOnAllValidValues() {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i) {
            assertEquals(i, Bits.signExtend8(i & 0xFF));
        }
    }

    @Test
    void reverse8FailsOnInvalidValue() {
        assertThrows(IllegalArgumentException.class,
                () -> Bits.reverse8(0x100));
        assertThrows(IllegalArgumentException.class,
                () -> Bits.reverse8(-1));
    }

    @Test
    void reverse8IsItsOwnInverse() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v8 = rng.nextInt(0x100);
            assertEquals(v8, Bits.reverse8(Bits.reverse8(v8)));
        }
    }

    @Test
    void reverse8WorksOnKnownValues() {
        assertEquals(0b0000_0000, Bits.reverse8(0b0000_0000));
        assertEquals(0b1111_1111, Bits.reverse8(0b1111_1111));
        assertEquals(0b0100_1000, Bits.reverse8(0b0001_0010));
        assertEquals(0b0011_0101, Bits.reverse8(0b1010_1100));
    }

    @Test
    void complement8FailsOnInvalidValue() {
        assertThrows(IllegalArgumentException.class,
                () -> Bits.complement8(0x100));
        assertThrows(IllegalArgumentException.class,
                () -> Bits.complement8(-1));
    }

    @Test
    void complement8IsItsOwnInverse() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int v8 = rng.nextInt(0x100);
            assertEquals(v8, Bits.complement8(Bits.complement8(v8)));
        }
    }

    @Test
    void complement8WorksOnKnownValues() {
        assertEquals(0b1111_1111, Bits.complement8(0b0000_0000));
        assertEquals(0b1010_1010, Bits.complement8(0b0101_0101));
        assertEquals(0b0101_0101, Bits.complement8(0b1010_1010));
        assertEquals(0b1100_0110, Bits.complement8(0b0011_1001));
    }

    @Test
    void make16FailsOnInvalidValues() {
        assertThrows(IllegalArgumentException.class,
                () -> Bits.make16(0x100, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Bits.make16(-1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> Bits.make16(0, 0x100));
        assertThrows(IllegalArgumentException.class,
                () -> Bits.make16(0, -1));
    }

    @Test
    void make16WorksOnKnownValues() {
        assertEquals(0xAA55, Bits.make16(0xAA, 0x55));
        assertEquals(0x0000, Bits.make16(0x00, 0x00));
        assertEquals(0x55AA, Bits.make16(0x55, 0xAA));
        assertEquals(0x1234, Bits.make16(0x12, 0x34));
    }

    @Test
    void make16AndExtractAreCompatible() {
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int h = rng.nextInt(0x100);
            int l = rng.nextInt(0x100);
            int v = Bits.make16(h, l);
            assertEquals(h, Bits.extract(v, 8, 8));
            assertEquals(l, Bits.extract(v, 0, 8));
        }
    }
}

