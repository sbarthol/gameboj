package ch.epfl.gameboj.component;

import java.io.File;
import java.io.IOException;
import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;

public final class DebugMain {

    public static void main(String[] args) throws IOException {

        File romFile = new File("07-jr,jp,call,ret,rst.gb");
        long cycles = 30000000;

        GameBoy gb = new GameBoy(Cartridge.ofFile(romFile));

        Component printer = new DebugPrintComponent();
        printer.attachTo(gb.bus());

        while (gb.cycles() < cycles) {
            long nextCycles = Math.min(gb.cycles() + 17556, cycles);

            gb.runUntil(nextCycles);
            gb.cpu().requestInterrupt(Cpu.Interrupt.VBLANK);
        }
    }
}