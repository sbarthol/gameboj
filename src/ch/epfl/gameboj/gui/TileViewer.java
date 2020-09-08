package ch.epfl.gameboj.gui;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.lcd.LcdImage;
import ch.epfl.gameboj.component.lcd.LcdImageLine;

public final class TileViewer implements Viewer {
    

    private final GameBoy gameBoy;

    /**
     * Construit un TileViewer pour la gameboy donnée
     * 
     * @param gameBoy
     */
    public TileViewer(GameBoy gameBoy) {
        this.gameBoy = gameBoy;
    }

    private LcdImageLine getLine(int line) {

        LcdImageLine.Builder b = new LcdImageLine.Builder(256);
        for (int i = 0; i < 32; i++) {

            int index = (line / 8)*32 + i;

            int address = 0x8000 + index * 16 + 2 * (line % 8);
            int lsb = gameBoy.lcdController().read(address);
            int msb = gameBoy.lcdController().read(address + 1);

            b.setBytes(i, Bits.reverse8(lsb), Bits.reverse8(msb));

        }

        return b.build().mapColors(gameBoy.lcdController().read(AddressMap.REGS_LCDC_START+7));

    }

    @Override
    public LcdImage currentImage() {

        LcdImage.Builder b = new LcdImage.Builder(256, 96);
        for (int i = 0; i < 96; i++) {

            b.setLine(i, getLine(i));
        }

        return b.build();
    }

}
