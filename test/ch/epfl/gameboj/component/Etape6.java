package ch.epfl.gameboj.component;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;

class Etape6 {
    
    byte rom[] = new byte[1<<15];
    GameBoy gb;
    
    
    @Test
    void etape6() {
        
        exec("etape6.gb");
        assertEquals(0xff,gb.cpu()._testGetPcSpAFBCDEHL()[2]);
    }

    @Disabled
    @Test
    void testGen() throws IOException {
        
        rom[0x05]=(byte)0x3E;
        rom[0x06]=(byte)0xFF;
        rom[0x100]=(byte)0xC3;
        rom[0x101]=(byte)0x05;
        rom[0x102]=(byte)0x00;
        
        FileOutputStream stream = new FileOutputStream("/Users/Sacha/Desktop/etape6.gb");
        try {
            stream.write(rom);
        } finally {
            stream.close();
        }
    }
    
    private void exec(String s) {
        
        File romFile = new File(s);
        long cycles = 30000000;

        try{
            gb = new GameBoy(Cartridge.ofFile(romFile));
        }
        catch (Exception e) {
            
        }

        while (gb.cycles() < cycles) {
            long nextCycles = Math.min(gb.cycles() + 17556, cycles);

            gb.runUntil(nextCycles);
            gb.cpu().requestInterrupt(Cpu.Interrupt.VBLANK);
        }
    }

}
