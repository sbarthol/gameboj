package ch.epfl.gameboj.sigcheck;

import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.component.cpu.Alu;

public final class SignatureChecks_2 {
    private SignatureChecks_2() {}

    void checkAlu() {
        int i;
        Bit f = Alu.Flag.Z;
        f = Alu.Flag.N;
        f = Alu.Flag.H;
        f = Alu.Flag.C;
        i = f.mask();
        i = Alu.maskZNHC(false, false, false, false);
        i = Alu.unpackValue(i);
        i = Alu.unpackFlags(i);
        i = Alu.add(i, i, false);
        i = Alu.add(i, i);
        i = Alu.add16L(i, i);
        i = Alu.add16H(i, i);
        i = Alu.sub(i, i, false);
        i = Alu.sub(i, i);
        i = Alu.bcdAdjust(i, false, false, false);
        i = Alu.and(i, i);
        i = Alu.or(i, i);
        i = Alu.xor(i, i);
        i = Alu.shiftLeft(i);
        i = Alu.shiftRightA(i);
        i = Alu.shiftRightL(i);
        i = Alu.rotate(Alu.RotDir.LEFT, i);
        i = Alu.rotate(Alu.RotDir.RIGHT, i, false);
    }
    
    Bus checkGameBoy() {
        GameBoy g = new GameBoy(null);
        Bus b = g.bus();
        return b;
    }
}
