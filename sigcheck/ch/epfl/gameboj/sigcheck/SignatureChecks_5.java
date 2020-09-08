package ch.epfl.gameboj.sigcheck;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.cpu.Cpu;

public final class SignatureChecks_5 {
    private SignatureChecks_5() {}

    void checkCpu() {
        Cpu c = new Cpu();
        Cpu.Interrupt i = Cpu.Interrupt.VBLANK;
        i = Cpu.Interrupt.LCD_STAT;
        i = Cpu.Interrupt.TIMER;
        i = Cpu.Interrupt.SERIAL;
        i = Cpu.Interrupt.JOYPAD;
        c.requestInterrupt(i);
    }
    
    void checkGameBoy() {
        GameBoy g = new GameBoy(null);
        long c = 0;
        g.runUntil(c);
        Cpu cpu = g.cpu();
        cpu.cycle(c);
        c = g.cycles();
    }
}