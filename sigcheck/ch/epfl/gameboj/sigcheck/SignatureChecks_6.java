package ch.epfl.gameboj.sigcheck;

import java.io.File;
import java.io.IOException;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.Timer;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cartridge.MBC0;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.memory.BootRom;
import ch.epfl.gameboj.component.memory.BootRomController;
import ch.epfl.gameboj.component.memory.Rom;

public final class SignatureChecks_6 {
    private SignatureChecks_6() {}

    void checkMBC0() {
        Rom r = null;
        MBC0 m = new MBC0(r);
        Component c = m;
        System.out.println(c);
    }
    
    void checkCartridge() throws IOException {
        Cartridge c = Cartridge.ofFile(new File(""));
        Component c2 = c;
        System.out.println(c2);
    }
    
    void checkBootRomController() {
        Cartridge c = null;
        BootRomController b = new BootRomController(c);
        Component c2 = b;
        System.out.println(c2);
    }
    
    void checkTimer() {
        Cpu c = null;
        Timer t = new Timer(c);
        Component c2 = t;
        Clocked c3 = t;
        System.out.println(c2 + "" + c3);
    }
    
    void checkGameBoy() {
        Cartridge c = null;
        GameBoy g = new GameBoy(c);
        Timer t = g.timer();
        System.out.println(t);
    }
    
    void checkBootRomImport() {
        byte[] d = BootRom.DATA;
        System.out.println(d);
    }
}