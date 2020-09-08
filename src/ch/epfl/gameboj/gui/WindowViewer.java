package ch.epfl.gameboj.gui;

import static ch.epfl.gameboj.AddressMap.TILE_SOURCE;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.lcd.LcdImage;
import ch.epfl.gameboj.component.lcd.LcdImageLine;

public class WindowViewer implements Viewer {

    private final GameBoy gameBoy;

    /**
     * Construit un WindowViewer pour la gameboy donnée
     * 
     * @param gameBoy
     */
    public WindowViewer(GameBoy gb) {
        gameBoy = gb;
    }

    @Override
    public LcdImage currentImage() {

        LcdImage.Builder b = new LcdImage.Builder(256, 256);

        for (int i = 0; i < 256; i++) {

            b.setLine(i, getLine(i));
        }

        return b.build();

    }

    private LcdImageLine getLine(int line) {

        LcdImageLine.Builder b = new LcdImageLine.Builder(256);

        for (int i = 0; i < 32; i++) {

            int square = (line / 8) * 32 + i;
            boolean area = Bits.test(
                    gameBoy.lcdController().read(AddressMap.REGS_LCDC_START),
                    6);
            int index = gameBoy.lcdController()
                    .read(square + (area ? 0x9c00 : 0x9800));

            boolean source = Bits.test(
                    gameBoy.lcdController().read(AddressMap.REGS_LCDC_START),
                    4);

            int address = TILE_SOURCE[source ? 1 : 0] + index * 0x10
                    + (source ? 0
                            : index >= TILE_SOURCE[1] / 0x100
                                    ? -TILE_SOURCE[1] / 0x10
                                    : TILE_SOURCE[1] / 0x10);

            address += 2 * (line % 8);
            int lsb = gameBoy.lcdController().read(address);
            int msb = gameBoy.lcdController().read(address + 1);

            b.setBytes(i, Bits.reverse8(lsb), Bits.reverse8(msb));
        }

        return b.build().mapColors(
                gameBoy.lcdController().read(AddressMap.REGS_LCDC_START + 7));
    }

}
