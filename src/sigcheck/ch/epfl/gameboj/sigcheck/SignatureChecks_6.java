package ch.epfl.gameboj.sigcheck;

import java.io.File;
import java.io.IOException;

import main.java.gameboj.GameBoy;
import main.java.gameboj.component.Clocked;
import main.java.gameboj.component.Component;
import main.java.gameboj.component.Timer;
import main.java.gameboj.component.cartridge.Cartridge;
import main.java.gameboj.component.cartridge.MBC0;
import main.java.gameboj.component.cpu.Cpu;
import main.java.gameboj.component.memory.BootRom;
import main.java.gameboj.component.memory.BootRomController;
import main.java.gameboj.component.memory.Rom;

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