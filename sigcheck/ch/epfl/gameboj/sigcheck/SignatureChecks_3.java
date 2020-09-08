package ch.epfl.gameboj.sigcheck;

import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;

public final class SignatureChecks_3 {
    private SignatureChecks_3() {}

    private enum Regs implements Register { R1, R2, R3, R4 };
    private enum R1Bits implements Bit { B0, B1, B2 };
    
    void checkRegister() {
        System.out.println(Regs.R1.index());
    }
    
    void checkRegisterFile() {
        int x;
        RegisterFile<Regs> r = new RegisterFile<>(Regs.values());
        x = r.get(Regs.R1);
        r.set(Regs.R1, x);
        boolean b = r.testBit(Regs.R1, R1Bits.B0);
        r.setBit(Regs.R1, R1Bits.B1, !b);
     }
    
    void checkClocked(Clocked c) {
        long cy = 0;
        c.cycle(cy);
    }
    
    void checkCpu() {
        Cpu c = new Cpu();
        Component cp = c;
        Clocked cl = c;
        int[] rs = c._testGetPcSpAFBCDEHL();
        System.out.println("" + c + cp + cl + rs);
    }
}