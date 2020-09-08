package ch.epfl.gameboj.sigcheck;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.component.cpu.Opcode;

public final class SignatureChecks_4 {
    private SignatureChecks_4() {}

    void checkAddressMapImport() {
        System.out.println(AddressMap.WORK_RAM_START);
    }
    
    void checkOpcodeImport() {
        System.out.println(Opcode.Family.LD_HLSP_S8);
    }
}