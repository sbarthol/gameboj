// Gameboj stage 3

package ch.epfl.gameboj.component.cpu;

import static ch.epfl.gameboj.component.cpu.Opcode.*;
import static ch.epfl.test.TestRandomizer.RANDOM_ITERATIONS;
import static ch.epfl.test.TestRandomizer.newRandom;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

class CpuTest {
    private static final int CPU_STATE_SIZE = 10;

    @Test
    void initialStateIsCorrect() {
        int[] initialState = new Cpu()._testGetPcSpAFBCDEHL();
        assertEquals(CPU_STATE_SIZE, initialState.length);
        assertCpuStateEquals(new int[CPU_STATE_SIZE], initialState);
    }

    @Test
    void nopsDoNothing() throws IOException {
        Opcode[] os = new Opcode[] {
                NOP,
                LD_A_A,
                LD_B_B,
                LD_C_C,
                LD_D_D,
                LD_E_E,
                LD_H_H,
                LD_L_L
        };
        int[] nopState = cpuState(1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        for (Opcode o: os) {
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emit(s, o);
                assertCpuStateEquals(nopState, stateAfter(s, o.cycles));
            }
        }
    }

    // Loads

    @Test
    void ldR8HlrWorks() throws IOException {
        Opcode[] os = new Opcode[] {
                LD_A_HLR,
                null,
                LD_B_HLR,
                LD_C_HLR,
                LD_D_HLR,
                LD_E_HLR,
                LD_H_HLR,
                LD_L_HLR,
        };
        for (int i = 0; i < os.length; ++i) {
            Opcode o = os[i];
            if (o == null)
                continue;
            int[] e = cpuState(1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            e[2 + i] = o.encoding;
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emit(s, o);
                assertCpuStateEquals(e, stateAfter(s, o.cycles));
            }
        }
    }

    @Test
    void ldAHlrIWorks() throws IOException {
        try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
            Opcode o = LD_A_HLRI;
            emit(s, o);
            int[] e = cpuState(1, 0, o.encoding, 0, 0, 0, 0, 0, 0, 1);
            assertCpuStateEquals(e, stateAfter(s, o.cycles));
        }
    }

    @Test
    void ldAHlrDWorks() throws IOException {
        try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
            Opcode o = LD_A_HLRD;
            emit(s, o);
            int[] e = cpuState(1, 0, o.encoding, 0, 0, 0, 0, 0, 0xFF, 0xFF);
            assertCpuStateEquals(e, stateAfter(s, o.cycles));
        }
    }

    @Test
    void ldAN8RWorks() throws IOException {
        Opcode o = LD_A_N8R;
        Random rng = newRandom();
        byte[] ramData = new byte[0x80];
        rng.nextBytes(ramData);
        Component ramC = ramAt(0xFF00, ramData);
        int[] e = cpuState(2, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        for (int n = 0; n < ramData.length; ++n) {
            if (n == 0xF)
                continue;  // Skip 0xFF0F, as this will be mapped to a CPU register later
            e[2] = ramC.read(0xFF00 + n);
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emitN8(s, o, n);
                assertCpuStateEquals(e, stateAfter(s, o.cycles, ramC));
            }
        }
    }

    @Test
    void ldACRWorks() throws IOException {
        Opcode[] os = new Opcode[] { LD_C_N8, LD_A_CR };
        Random rng = newRandom();
        byte[] ramData = new byte[0x80];
        rng.nextBytes(ramData);
        Component ramC = ramAt(0xFF00, ramData);
        int[] e = cpuState(totalBytes(os), 0, 0, 0, 0, 0, 0, 0, 0, 0);
        for (int n = 0; n < ramData.length; ++n) {
            if (n == 0xF)
                continue;  // Skip 0xFF0F, as this will be mapped to a CPU register later
            e[2] = Byte.toUnsignedInt(ramData[n]);
            e[5] = n;
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emitN8(s, os[0], n);
                emit(s, os[1]);
                assertCpuStateEquals(e, stateAfter(s, totalCycles(os), ramC));
            }
        }
    }

    @Test
    void ldAN16RWorks() throws IOException {
        Opcode o = LD_A_N16R;
        Random rng = newRandom();
        byte[] ramData = new byte[0x80];
        rng.nextBytes(ramData);
        int startAddress = 0x100;
        Component ramC = ramAt(startAddress, ramData);
        int[] e = cpuState(o.totalBytes, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int j = rng.nextInt(ramData.length);
            e[2] = Byte.toUnsignedInt(ramData[j]);
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emitN16(s, o, startAddress + j);
                assertCpuStateEquals(e, stateAfter(s, o.cycles, ramC));
            }
        }
    }

    @Test
    void ldABCRWorks() throws IOException {
        Opcode[] os = new Opcode[] { LD_BC_N16,  LD_A_BCR };
        Random rng = newRandom();
        byte[] ramData = new byte[0x80];
        rng.nextBytes(ramData);
        int startAddress = 0x100;
        Component ramC = ramAt(startAddress, ramData);
        int[] e = cpuState(totalBytes(os), 0, 0, 0, 0, 0, 0, 0, 0, 0);
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int j = rng.nextInt(ramData.length);
            int a = startAddress + j;
            e[2] = Byte.toUnsignedInt(ramData[j]);
            e[4] = (a >> 8);
            e[5] = a & 0xFF;
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emitN16(s, os[0], a);
                emitN16(s, os[1], a);
                assertCpuStateEquals(e, stateAfter(s, totalCycles(os), ramC));
            }
        }
    }

    @Test
    void ldADERWorks() throws IOException {
        Opcode[] os = new Opcode[] { LD_DE_N16, LD_A_DER };
        Random rng = newRandom();
        byte[] ramData = new byte[0x80];
        rng.nextBytes(ramData);
        int startAddress = 0x100;
        Component ramC = ramAt(startAddress, ramData);
        int[] e = cpuState(totalBytes(os), 0, 0, 0, 0, 0, 0, 0, 0, 0);
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int j = rng.nextInt(ramData.length);
            int a = startAddress + j;
            e[2] = Byte.toUnsignedInt(ramData[j]);
            e[6] = (a >> 8);
            e[7] = a & 0xFF;
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emitN16(s, os[0], a);
                emitN16(s, os[1], a);
                assertCpuStateEquals(e, stateAfter(s, totalCycles(os), ramC));
            }
        }
    }

    @Test
    void ldR8N8Works() throws IOException {
        Opcode[] os = new Opcode[] {
                LD_A_N8,
                null,
                LD_B_N8,
                LD_C_N8,
                LD_D_N8,
                LD_E_N8,
                LD_H_N8,
                LD_L_N8,
        };
        for (int i = 0; i < os.length; ++i) {
            Opcode o = os[i];
            if (o == null)
                continue;
            int[] e = cpuState(2, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            for (int n = 0; n <= 0xFF; ++n) {
                e[2 + i] = n;
                try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                    emitN8(s, o, n);
                    assertCpuStateEquals(e, stateAfter(s, o.cycles));
                }
            }
        }
    }

    @Test
    void ldR16N16Works() throws IOException {
        Opcode[] os = new Opcode[] {
                LD_SP_N16,
                LD_BC_N16,
                LD_DE_N16,
                LD_HL_N16,
        };
        try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
            emitN16(s, os[0], 0x1234);
            emitN16(s, os[1], 0x5678);
            emitN16(s, os[2], 0x9ABC);
            emitN16(s, os[3], 0xDEF0);
            int[] e = cpuState(totalBytes(os), 0x1234, 0, 0, 0x56, 0x78, 0x9A, 0xBC, 0xDE, 0xF0);
            assertCpuStateEquals(e, stateAfter(s, totalCycles(os)));
        }
    }

    @Test
    void popR16Works() throws IOException {
        Opcode[] os = new Opcode[] {
                LD_SP_N16,
                POP_AF,
                POP_BC,
                POP_DE,
                POP_HL
        };
        byte[] ramData = new byte[] {
                (byte) 0xF1, (byte) 0xE2, (byte) 0xD3, (byte) 0xC4, (byte) 0xB5, (byte) 0xA6, (byte) 0x97, (byte) 0x88
        };
        int startAddress = 0x100;
        Component ramC = ramAt(startAddress, ramData);
        try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
            emitN16(s, os[0], startAddress);
            for (int i = 1; i < os.length; ++i)
                emit(s, os[i]);
            int[] e = cpuState(totalBytes(os), startAddress + ramData.length, 0xE2, 0xF0, 0xC4, 0xD3, 0xA6, 0xB5, 0x88, 0x97);
            assertCpuStateEquals(e, stateAfter(s, totalCycles(os), ramC));
        }
    }

    // Stores

    @Test
    void ldHlrR8Works() throws IOException {
        Opcode[] os0 = new Opcode[] {
                LD_A_N8,
                LD_B_N8,
                LD_C_N8,
                LD_D_N8,
                LD_E_N8,
                LD_H_N8,
                LD_L_N8,
        };
        Opcode[] os1 = new Opcode[] {
                LD_HLR_A,
                LD_HLR_B,
                LD_HLR_C,
                LD_HLR_D,
                LD_HLR_E,
                LD_HLR_H,
                LD_HLR_L,
        };
        int[] rs = new int[] { 0xF1, 0xE2, 0xD3, 0xC4, 0xB5, 0xA6, 0x97 };
        int hl = 0xA697;
        byte[] ramData = new byte[] { 0 };
        Component ramC = ramAt(hl, ramData);
        for (int j = 0; j < os1.length; ++j) {
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                for (int i = 0; i < os0.length; ++i)
                    emitN8(s, os0[i], rs[i]);
                emit(s, os1[j]);
                stateAfter(s, totalCycles(os0) + os1[j].cycles, ramC);
                assertEquals(rs[j], ramC.read(hl));
            }
        }
    }

    @Test
    void ldHlrIAWorks() throws IOException {
        Opcode[] os = new Opcode[] { LD_L_N8, LD_HLRI_A };
        int startAddress = 100;
        byte[] ramData = new byte[] { 0x5A };
        Component ramC = ramAt(startAddress, ramData);
        try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
            emitN8(s, os[0], startAddress);
            emit(s, os[1]);
            assertCpuStateEquals(cpuState(totalBytes(os), 0, 0, 0, 0, 0, 0, 0, 0, startAddress + 1), stateAfter(s, totalCycles(os), ramC));
            assertEquals(0, ramC.read(startAddress));
        }
    }

    @Test
    void ldHlrDAWorks() throws IOException {
        Opcode[] os = new Opcode[] { LD_L_N8, LD_HLRD_A };
        int startAddress = 100;
        byte[] ramData = new byte[] { 0x5A };
        Component ramC = ramAt(startAddress, ramData);
        try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
            emitN8(s, os[0], startAddress);
            emit(s, os[1]);
            assertCpuStateEquals(cpuState(totalBytes(os), 0, 0, 0, 0, 0, 0, 0, 0, startAddress - 1), stateAfter(s, totalCycles(os), ramC));
            assertEquals(0, ramC.read(startAddress));
        }
    }

    @Test
    void ldN8RAWorks() throws IOException {
        Opcode o = LD_N8R_A;
        Random rng = newRandom();
        byte[] ramData = new byte[0x80];
        rng.nextBytes(ramData);
        Component ramC = ramAt(0xFF00, ramData);
        for (int n = 0; n < ramData.length; ++n) {
            if (n == 0xF)
                continue;  // Skip 0xFF0F, as this will be mapped to a CPU register later
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emitN8(s, o, n);
                assertCpuStateEquals(cpuState(o.totalBytes, 0, 0, 0, 0, 0, 0, 0, 0, 0), stateAfter(s, o.cycles, ramC));
                assertEquals(0, ramC.read(0xFF00 + n));
            }
        }
    }

    @Test
    void ldCRAWorks() throws IOException {
        Opcode[] os = new Opcode[] { LD_C_N8, LD_CR_A };
        Random rng = newRandom();
        byte[] ramData = new byte[0x80];
        rng.nextBytes(ramData);
        Component ramC = ramAt(0xFF00, ramData);
        for (int n = 0; n < ramData.length; ++n) {
            if (n == 0xF)
                continue;  // Skip 0xFF0F, as this will be mapped to a CPU register later
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emitN8(s, os[0], n);
                emit(s, os[1]);
                assertCpuStateEquals(cpuState(totalBytes(os), 0, 0, 0, 0, n, 0, 0, 0, 0), stateAfter(s, totalCycles(os), ramC));
                assertEquals(0, ramC.read(0xFF00 + n));
            }
        }
    }

    @Test
    void ldN16RAWorks() throws IOException {
        Opcode[] os = new Opcode[] { LD_A_N8, LD_N16R_A };
        Random rng = newRandom();
        byte[] ramData = new byte[0x80];
        rng.nextBytes(ramData);
        int startAddress = 0x100;
        Component ramC = ramAt(startAddress, ramData);
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int n = rng.nextInt(0x100);
            int a = startAddress + rng.nextInt(ramData.length);
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emitN8(s, os[0], n);
                emitN16(s, os[1], a);
                assertCpuStateEquals(cpuState(totalBytes(os), 0, n, 0, 0, 0, 0, 0, 0, 0), stateAfter(s, totalCycles(os), ramC));
                assertEquals(n, ramC.read(a));
            }
        }
    }

    @Test
    void ldBCRAWorks() throws IOException {
        Opcode[] os = new Opcode[] { LD_BC_N16, LD_A_N8, LD_BCR_A };
        Random rng = newRandom();
        byte[] ramData = new byte[0x80];
        rng.nextBytes(ramData);
        int startAddress = 0x100;
        Component ramC = ramAt(startAddress, ramData);
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int j = rng.nextInt(ramData.length);
            int addr = startAddress + j;
            int a = rng.nextInt(0xFF);
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emitN16(s, os[0], addr);
                emitN8(s, os[1], a);
                emit(s, os[2]);
                int b = (addr >> 8), c = addr & 0xFF;
                assertCpuStateEquals(cpuState(totalBytes(os), 0, a, 0, b, c, 0, 0, 0, 0), stateAfter(s, totalCycles(os), ramC));
                assertEquals(a, ramC.read(addr));
            }
        }
    }

    @Test
    void ldDERAWorks() throws IOException {
        Opcode[] os = new Opcode[] { LD_DE_N16, LD_A_N8, LD_DER_A };
        Random rng = newRandom();
        byte[] ramData = new byte[0x80];
        rng.nextBytes(ramData);
        int startAddress = 0x100;
        Component ramC = ramAt(startAddress, ramData);
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int j = rng.nextInt(ramData.length);
            int addr = startAddress + j;
            int a = rng.nextInt(0xFF);
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emitN16(s, os[0], addr);
                emitN8(s, os[1], a);
                emit(s, os[2]);
                int d = (addr >> 8), e = addr & 0xFF;
                assertCpuStateEquals(cpuState(totalBytes(os), 0, a, 0, 0, 0, d, e, 0, 0), stateAfter(s, totalCycles(os), ramC));
                assertEquals(a, ramC.read(addr));
            }
        }
    }

    @Test
    void ldHLRN8Works() throws IOException {
        Opcode[] os = new Opcode[] { LD_HL_N16, LD_HLR_N8 };
        Random rng = newRandom();
        byte[] ramData = new byte[0x80];
        rng.nextBytes(ramData);
        int startAddress = 0x100;
        Component ramC = ramAt(startAddress, ramData);
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int j = rng.nextInt(ramData.length);
            int addr = startAddress + j;
            int n = rng.nextInt(0xFF);
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emitN16(s, os[0], addr);
                emitN8(s, os[1], n);
                int h = (addr >> 8), l = addr & 0xFF;
                assertCpuStateEquals(cpuState(totalBytes(os), 0, 0, 0, 0, 0, 0, 0, h, l), stateAfter(s, totalCycles(os), ramC));
                assertEquals(n, ramC.read(addr));
            }
        }
    }

    @Test
    void ldN16RSPWorks() throws IOException {
        Opcode[] os = new Opcode[] { LD_SP_N16, LD_N16R_SP };
        Random rng = newRandom();
        byte[] ramData = new byte[0x80];
        rng.nextBytes(ramData);
        int startAddress = 0x100;
        Component ramC = ramAt(startAddress, ramData);
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int j = rng.nextInt(ramData.length - 1);
            int addr = startAddress + j;
            int n = rng.nextInt(0xFFFF);
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emitN16(s, os[0], n);
                emitN16(s, os[1], addr);
                assertCpuStateEquals(cpuState(totalBytes(os), n, 0, 0, 0, 0, 0, 0, 0, 0), stateAfter(s, totalCycles(os), ramC));
                int nl = (n & 0xFF), nh = (n >> 8);
                assertEquals(nl, ramC.read(addr));
                assertEquals(nh, ramC.read(addr + 1));
            }
        }
    }

    @Test
    void pushR16Works() throws IOException {
        Opcode[] os = new Opcode[] {
                LD_A_N8,
                LD_B_N8,
                LD_C_N8,
                LD_D_N8,
                LD_E_N8,
                LD_H_N8,
                LD_L_N8,
                LD_SP_N16,
                PUSH_AF,
                PUSH_BC,
                PUSH_DE,
                PUSH_HL
        };
        int[] rs = new int[] { 0xF1, 0xE2, 0xD3, 0xC4, 0xB5, 0xA6, 0x97 };
        int startAddress = 0x100;
        byte[] ramData = new byte[8];
        ramData[0] = (byte) 0xA5;
        Component ramC = ramAt(startAddress, ramData);
        try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
            for (int i = 0; i < rs.length; ++i)
                emitN8(s, os[i], rs[i]);
            emitN16(s, os[7], startAddress + ramData.length);
            for (int i = 8; i < os.length; ++i)
                emit(s, os[i]);
            int[] e = cpuState(totalBytes(os), startAddress, 0xF1, 0x00, 0xE2, 0xD3, 0xC4, 0xB5, 0xA6, 0x97);
            assertCpuStateEquals(e, stateAfter(s, totalCycles(os), ramC));
            for (int i = 1; i < rs.length; ++i)
                assertEquals(rs[rs.length - i], ramC.read(startAddress + i - 1));
            assertEquals(0, ramC.read(startAddress + rs.length - 1)); // F
        }
    }

    // Move
    @Test
    void ldR8R8Works() throws IOException {
        Opcode[] os0 = new Opcode[] {
                LD_A_N8,
                LD_B_N8,
                LD_C_N8,
                LD_D_N8,
                LD_E_N8,
                LD_H_N8,
                LD_L_N8,
        };

        Opcode[] os = {
                LD_A_B, LD_A_C, LD_A_D, LD_A_E, LD_A_H, LD_A_L,
                LD_B_A, LD_B_C, LD_B_D, LD_B_E, LD_B_H, LD_B_L,
                LD_C_A, LD_C_B, LD_C_D, LD_C_E, LD_C_H, LD_C_L,
                LD_D_A, LD_D_B, LD_D_C, LD_D_E, LD_D_H, LD_D_L,
                LD_E_A, LD_E_B, LD_E_C, LD_E_D, LD_E_H, LD_E_L,
                LD_H_A, LD_H_B, LD_H_C, LD_H_D, LD_H_E, LD_H_L,
                LD_L_A, LD_L_B, LD_L_C, LD_L_D, LD_L_E, LD_L_H
        };
        int[][] move = new int[][] {
            { 2, 4 }, { 2, 5 }, { 2, 6 }, { 2, 7 }, { 2, 8 }, { 2, 9 },
            { 4, 2 }, { 4, 5 }, { 4, 6 }, { 4, 7 }, { 4, 8 }, { 4, 9 },
            { 5, 2 }, { 5, 4 }, { 5, 6 }, { 5, 7 }, { 5, 8 }, { 5, 9 },
            { 6, 2 }, { 6, 4 }, { 6, 5 }, { 6, 7 }, { 6, 8 }, { 6, 9 },
            { 7, 2 }, { 7, 4 }, { 7, 5 }, { 7, 6 }, { 7, 8 }, { 7, 9 },
            { 8, 2 }, { 8, 4 }, { 8, 5 }, { 8, 6 }, { 8, 7 }, { 8, 9 },
            { 9, 2 }, { 9, 4 }, { 9, 5 }, { 9, 6 }, { 9, 7 }, { 9, 8 },
        };

        int[] rs = new int[] { 0xF0, 0xE1, 0xD2, 0xC3, 0xB4, 0xA5, 0x96 };
        for (int j = 0; j < os.length; ++j) {
            Opcode o = os[j];
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                for (int i = 0; i < os0.length; ++i)
                    emitN8(s, os0[i], rs[i]);
                emit(s, o);
                int[] e = cpuState(totalBytes(os0) + o.totalBytes, 0, rs[0], 0, rs[1], rs[2], rs[3], rs[4], rs[5], rs[6]);
                e[move[j][0]] = e[move[j][1]];
                assertCpuStateEquals(e, stateAfter(s, totalCycles(os0) + o.cycles));
            }
        }
    }

    @Test
    void ldSpHlWorks() throws IOException {
        Opcode[] os = new Opcode[] { LD_HL_N16, LD_SP_HL };
        Random rng = newRandom();
        for (int i = 0; i < RANDOM_ITERATIONS; ++i) {
            int n = rng.nextInt(0xFFFF);
            try (ByteArrayOutputStream s = new ByteArrayOutputStream()) {
                emitN16(s, os[0], n);
                emit(s, os[1]);
                int nh = (n >> 8), nl = n & 0xFF;
                assertCpuStateEquals(cpuState(totalBytes(os), n, 0, 0, 0, 0, 0, 0, nh, nl), stateAfter(s, totalCycles(os)));
            }
        }
    }

    private static int combine(int h, int l) {
        return (h << 8) | l;
    }

    private int[] cpuState(int pc, int sp, int a, int f, int b, int c, int d, int e, int h, int l) {
        return new int[] { pc, sp, a, f, b, c, d, e, h, l };
    }

    private static String cpuStateToString(int[] s) {
        if (s.length < CPU_STATE_SIZE)
            return cpuStateToString(Arrays.copyOf(s, CPU_STATE_SIZE));
        else
            return String.format("PC: %04X SP: %04X AF: %04X BC: %04X DE: %04X HL: %04X",
                    s[0], s[1],
                    combine(s[2], s[3]),
                    combine(s[4], s[5]),
                    combine(s[6], s[7]),
                    combine(s[8], s[9]));
    }

    private static void assertCpuStateEquals(int[] expected, int[] actual) {
        int[] fixedActual = actual.length == CPU_STATE_SIZE ? actual : Arrays.copyOf(actual, CPU_STATE_SIZE);
        assertArrayEquals(expected, actual, () -> String.format("Expected state: [%s], actual: [%s]", cpuStateToString(expected), cpuStateToString(fixedActual)));
    }

    private Component ramAt(int startAddress, byte[] initialContents) {
        Ram r = new Ram(initialContents.length);
        for (int i = 0; i < initialContents.length; ++i)
            r.write(i, Byte.toUnsignedInt(initialContents[i]));
        return new RamController(r, startAddress);
    }

    private int[] stateAfter(ByteArrayOutputStream program, int cycles, Component... components) {
        Component p = new ProgRom(program.toByteArray());
        Cpu c = new Cpu();
        Bus b = new Bus();
        p.attachTo(b);
        c.attachTo(b);
        for (Component c2: components)
            c2.attachTo(b);
        for (int i = 0; i < cycles; ++i) {
            c.cycle(i);
        }
        return c._testGetPcSpAFBCDEHL();
    }

    private static void emit(ByteArrayOutputStream s, Opcode op) {
        s.write(op.encoding);
    }

    private static void emitN8(ByteArrayOutputStream s, Opcode op, int n) {
        s.write(op.encoding);
        s.write(n);
    }

    private static void emitN16(ByteArrayOutputStream s, Opcode op, int n) {
        s.write(op.encoding);
        s.write(n & 0xFF);
        s.write(n >> 8);
    }

    private int totalBytes(Opcode[] os) {
        int b = 0;
        for (Opcode o: os)
            b += o.totalBytes;
        return b;
    }

    private int totalCycles(Opcode[] os) {
        int c = 0;
        for (Opcode o: os)
            c += o.cycles;
        return c;
    }
}

class ProgRom implements Component {
    private final byte[] p;

    public ProgRom(byte[] p) {
        this.p = Arrays.copyOf(p, p.length);
    }

    @Override
    public int read(int address) {
        if (0 <= address && address < p.length)
            return Byte.toUnsignedInt(p[address]);
        else
            return Component.NO_DATA;
    }

    @Override
    public void write(int address, int data) { }
}
