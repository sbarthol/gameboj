package ch.epfl.gameboj.gui;

import static ch.epfl.gameboj.bits.Bits.reverse8;
import static ch.epfl.gameboj.bits.Bits.test;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.lcd.LcdImage;
import ch.epfl.gameboj.component.lcd.LcdImageLine;


public class SpriteViewer implements Viewer {
    
    private final GameBoy gameBoy;

    /**
     * Construit un SpriteViewer pour la gameboy donnée
     * 
     * @param gameBoy
     */
    public SpriteViewer(GameBoy gb) {
        gameBoy = gb;
    }
    
    private LcdImageLine getLine(int line) {
        
        LcdImageLine ans = new LcdImageLine(32);
        
        for(int i=0;i<4;i++) {
            
            LcdImageLine.Builder b = new LcdImageLine.Builder(32);
            
            int index = (line / 16) * 4 + i;
            
            int lcdc = gameBoy.lcdController().read(AddressMap.REGS_LCDC_START);
            int spriteLine = line % 16;
            int size = Bits.test(lcdc, 2) ? 16: 8;

            if (test(gameBoy.lcdController().read(0xfe00+index * 4 + 3), 6)) {
                spriteLine = 16 - spriteLine - 1;
            }
            
            if(size == 8 && spriteLine >= 8) {
                continue;
            }
            
            int tile = gameBoy.lcdController().read(0xfe00+index * 4 + 2);
            if (spriteLine >= 8)
                tile++;

            int lsb = gameBoy.lcdController().read(0x8000 + tile * 16 + 2 * (spriteLine % 8));
            int msb = gameBoy.lcdController().read(0x8000 + tile * 16 + 2 * (spriteLine % 8) + 1);

            if (!test(gameBoy.lcdController().read(0xfe00+index * 4 + 3), 5)) {
                lsb = reverse8(lsb);
                msb = reverse8(msb);
            }

            b.setBytes(i, lsb, msb);
            
            int obp0 = gameBoy.lcdController().read(AddressMap.REGS_LCDC_START + 8);
            int obp1 = gameBoy.lcdController().read(AddressMap.REGS_LCDC_START + 9);
            
            LcdImageLine l = b.build().mapColors(test(gameBoy.lcdController().read(0xfe00+4 * index + 3), 4)
                    ? obp1 : obp0);
            ans = ans.below(l);
        }
        
        return ans;
    }

    @Override
    public LcdImage currentImage() {
       
        LcdImage.Builder b = new LcdImage.Builder(32,160);
        
        for(int i=0;i<160;i++) {
            
            b.setLine(i, getLine(i));
        }
        
        return b.build();
    }

}
