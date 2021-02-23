package ch.epfl.gameboj.component.cpu;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

class Fibo {
    
    byte fibo_it[] = new byte[] {
            (byte)0x06, (byte)0x00, (byte)0x3e, (byte)0x01,
            (byte)0x0e, (byte)0x0a, (byte)0x57, (byte)0x80,
            (byte)0x42, (byte)0x0d, (byte)0xc2, (byte)0x06,
            (byte)0x00, (byte)0x76
          };
    
    byte tribo_rapelle_toi_asfoury_la_question_au_final_d_aicc_il_fallait_deriver_une_generating_function_sur_le_nombre_de_manières_de_monter_les_escaliers_et_ben_c_est_exactement_cette_fonction_il_y_a_149_manières_de_monter_9_escaliers_en_prenant_1_2_ou_3_marches_a_la_fois[] = new byte[] {
            (byte)0x31, (byte)0xFF, (byte)0xFF, (byte)0x3E,
            (byte)0x0A, (byte)0xCD, (byte)0x09, (byte)0x00,
            (byte)0x76, (byte)0xFE, (byte)0x02, (byte)0xD8,
            (byte)0x28, (byte)0x1C, (byte)0x00, (byte)0x00, 
            (byte)0xC5, (byte)0xD5, (byte)0x3D, (byte)0x00,
            (byte)0x47, (byte)0xCD, (byte)0x09, (byte)0x00,
            (byte)0x4F, (byte)0x78, (byte)0x3D, (byte)0x47,
            (byte)0xCD, (byte)0x09, (byte)0x00, (byte)0x57,
            (byte)0x78, (byte)0x3D, (byte)0xCD, (byte)0x09,
            (byte)0x00, (byte)0x81, (byte)0x82, (byte)0xD1,
            (byte)0xC1, (byte)0xC9, (byte)0x3D, (byte)0xC9,
        };

    void $(byte[] program, int expect,int cycles) {
        
        GameBoy gameBoy = new GameBoy(null);
        
        Ram ram = new Ram(0x10000);
        RamController ramController = new RamController(ram,0);
        ramController.attachTo(gameBoy.bus());
        
        for(int i=0;i<program.length;i++) {
            gameBoy.bus().write(i, Byte.toUnsignedInt(program[i]));
        }
        
        
        gameBoy.runUntil(cycles);
        assertEquals(expect, gameBoy.cpu()._testGetPcSpAFBCDEHL()[2]);
    }
    
    @Test 
    void tuttiquanti(){
        
        $(fibo_it,89,300);
        $(tribo_rapelle_toi_asfoury_la_question_au_final_d_aicc_il_fallait_deriver_une_generating_function_sur_le_nombre_de_manières_de_monter_les_escaliers_et_ben_c_est_exactement_cette_fonction_il_y_a_149_manières_de_monter_9_escaliers_en_prenant_1_2_ou_3_marches_a_la_fois,149,8000);
    }
    
    @Test
    void testInterrupt() {
        
        GameBoy gameBoy = new GameBoy(null);
        
        Ram ram = new Ram(0x10000);
        RamController ramController = new RamController(ram,0);
        ramController.attachTo(gameBoy.bus());
        
        gameBoy.bus().write(AddressMap.REG_IE, 0b00100);
        gameBoy.cpu().requestInterrupt(Interrupt.TIMER);
        gameBoy.bus().write(0, 0xFB);
        gameBoy.bus().write(80, 0xD9);
        gameBoy.bus().write(1, 0x3E);
        gameBoy.bus().write(2, 0x11);
        
        gameBoy.runUntil(100);
        
        assertEquals(0,gameBoy.bus().read(AddressMap.REG_IF));
        assertEquals(0x11,gameBoy.cpu()._testGetPcSpAFBCDEHL()[2]);
    }

}
