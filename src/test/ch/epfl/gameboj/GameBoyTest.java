// Gameboj stage 2

package ch.epfl.gameboj;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GameBoyTest {
    @Test
    void workRamIsProperlyMapped() {
        Bus b = new GameBoy(null).bus();
        for (int a = 0; a <= 0xFFFF; ++a) {
            boolean inWorkRamOrEcho = (0xC000 <= a && a < 0xFE00);
            assertEquals(inWorkRamOrEcho ? 0 : 0xFF, b.read(a), String.format("at address 0x%04x", a));
        }
    }

    @Test
    void workRamCanBeReadAndWritten() {
        Bus b = new GameBoy(null).bus();
        for (int a = 0xC000; a < 0xE000; ++a)
            b.write(a, (a ^ 0xA5) & 0xFF);
        for (int a = 0xC000; a < 0xE000; ++a)
            assertEquals((a ^ 0xA5) & 0xFF, b.read(a));
    }

    @Test
    void echoAreaReflectsWorkRam() {
        Bus b = new GameBoy(null).bus();
        for (int a = 0xC000; a < 0xE000; ++a)
            b.write(a, (a ^ 0xA5) & 0xFF);
        for (int a = 0xE000; a < 0xFE00; ++a)
            assertEquals(((a - 0x2000) ^ 0xA5) & 0xFF, b.read(a));

        for (int a = 0xE000; a < 0xFE00; ++a)
            b.write(a, (a ^ 0xA5) & 0xFF);
        for (int a = 0xC000; a < 0xDE00; ++a)
            assertEquals(((a + 0x2000) ^ 0xA5) & 0xFF, b.read(a));
    }
}
