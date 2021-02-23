package ch.epfl.gameboj.component.lcd;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.*;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.BitVector;

import static ch.epfl.gameboj.bits.Bits.*;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;
import ch.epfl.gameboj.component.memory.Ram;

public final class LcdController implements Component, Clocked {

    private enum Reg implements Register {
        LCDC, STAT, SCY, SCX, LY, LYC, DMA, BGP, OBP0, OBP1, WY, WX
    }

    private enum LCDC implements Bit {
        BG, OBJ, OBJ_SIZE, BG_AREA, TILE_SOURCE, WIN, WIN_AREA, LCD_STATUS
    }

    private enum STAT implements Bit {
        MODE0, MODE1, LYC_EQ_LY, INT_MODE0, INT_MODE1, INT_MODE2, INT_LYC
    }

    private enum Sprite implements Bit {
        UNUSED_0, UNUSED_1, UNUSED_2, UNUSED_3, PALETTE, FLIP_H, FLIP_V, BEHIND_BG
    }

    private RegisterFile<Reg> registerFile;

    private static final int WX_OFFSET = 7;
    private static final int SPRITE_MAX_HEIGHT = 16;
    private static final int SPRITE_MIN_HEIGHT = 8;
    private static final int SPRITE_WIDTH = 8;
    private static final int TOTAL_SPRITES = 40;
    private static final int MAX_SPRITES = 10;

    private static final int LCD_WIDTH = 160;
    private static final int LCD_HEIGHT = 144;
    private static final int IMAGE_WIDTH = 256;
    private static final int IMAGE_HEIGHT = IMAGE_WIDTH;
    private static final int TILE_WIDTH = 8;
    private static final int TILE_HEIGHT = TILE_WIDTH;

    private static final int TOTAL_CYCLES = 17556;
    private static final int IMAGE_CYCLES = 16416;
    private static final int LINE_CYCLES = 114;
    private static final int MODE2_CYCLES = 20;
    private static final int MODE3_CYCLES = 43;
    private static final int MODE0_CYCLES = 51;

    private Cpu cpu;
    private Ram videoRam, oam;
    private Bus bus;
    private int winY, dmaIndex;
    private boolean dma;

    LcdImage.Builder nextImageBuilder;
    LcdImage currentImage;

    private long nextNonIdleCycle, lcdOnCycle;

    /**
     * Permet de créer un controller LCD en prenant en argument le processeur du
     * Game Boy auquel il appartient,
     * 
     * @param cpu
     */
    public LcdController(Cpu cpu) {

        this.cpu = Objects.requireNonNull(cpu);
        nextNonIdleCycle = 0;
        lcdOnCycle = 0;
        dmaIndex = 0;
        videoRam = new Ram(AddressMap.VIDEO_RAM_SIZE);
        oam = new Ram(AddressMap.OAM_RAM_SIZE);
        registerFile = new RegisterFile<>(Reg.values());
        dma = false;
    }

    @Override
    public void cycle(long cycle) {

        if (dma) {
            oam.write(dmaIndex,
                    bus.read(make16(registerFile.get(Reg.DMA), dmaIndex)));
            if (++dmaIndex == AddressMap.OAM_RAM_SIZE)
                dma = false;
        }

        if (nextNonIdleCycle == Integer.MAX_VALUE
                && registerFile.testBit(Reg.LCDC, LCDC.LCD_STATUS)) {

            nextNonIdleCycle = cycle;
            lcdOnCycle = cycle;

        }

        if (cycle < nextNonIdleCycle) {
            return;
        }

        reallyCycle(cycle);
    }

    @Override
    public int read(int address) {

        Preconditions.checkBits16(address);

        if (address >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END) {
            return videoRam.read(address - AddressMap.VIDEO_RAM_START);
        }

        if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
            return oam.read(address - AddressMap.OAM_START);
        }

        if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {
            return registerFile.get(Reg.values()[address - AddressMap.REGS_LCDC_START]);
        }

        return NO_DATA;
    }

    @Override
    public void write(int address, int data) {

        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);

        if (address >= AddressMap.VIDEO_RAM_START && address < AddressMap.VIDEO_RAM_END) {
            videoRam.write(address - AddressMap.VIDEO_RAM_START, data);
        }

        if (address >= AddressMap.OAM_START && address < AddressMap.OAM_END) {
            oam.write(address - AddressMap.OAM_START, data);
        }

        if (address >= AddressMap.REGS_LCDC_START && address < AddressMap.REGS_LCDC_END) {

            Reg r = Reg.values()[address - AddressMap.REGS_LCDC_START];

            if (r == Reg.LY)
                return;

            else if (r == Reg.DMA) {

                registerFile.set(r, data);
                dma = true;
                dmaIndex = 0;
            }

            else if (r == Reg.LCDC && !test(data, LCDC.LCD_STATUS)) {

                registerFile.set(r, data);
                registerFile.set(Reg.LY, 0);
                LYchanged();
                setMode(0);
                if (registerFile.testBit(Reg.STAT, STAT.INT_MODE0)) {
                    cpu.requestInterrupt(Interrupt.LCD_STAT);
                }

                nextNonIdleCycle = Integer.MAX_VALUE;

            } else if (r == Reg.STAT) {

                registerFile.set(Reg.STAT,
                        (data & 0xf8) | (registerFile.get(Reg.STAT) & 0x07));

            } else if (r == Reg.LYC) {

                registerFile.set(r, data);
                LYchanged();

            } else {

                registerFile.set(r, data);
            }
        }

    }

    @Override
    public void attachTo(Bus bus) {
        this.bus = bus;
        bus.attach(this);
    }

    /**
     * retourne l'image actuellement affichée à l'écran, de type LcdImage.
     * méthode retourne toujours une image non nulle de 160×144 pixels, même si
     * la simulation n'a pas encore été effectuée assez longtemps pour que la
     * première image ait été dessinée. Dans ce dernier cas, l'image retournée
     * sera simplement vide, c-à-d que tous ses pixels seront de couleur 0.
     * 
     * @return l'image actuellement affichée à l'écran
     */
    public LcdImage currentImage() {

        return currentImage == null ? new LcdImage(LCD_WIDTH, LCD_HEIGHT)
                : currentImage;
    }

    private void reallyCycle(long cycle) {

        long elapsed = (cycle - lcdOnCycle) % TOTAL_CYCLES;

        if (elapsed < IMAGE_CYCLES) {

            if (elapsed % LINE_CYCLES == 0) {

                if (elapsed == 0) {

                    winY = 0;
                    registerFile.set(Reg.LY, 0);
                    nextImageBuilder = new LcdImage.Builder(LCD_WIDTH,
                            LCD_HEIGHT);
                } else {

                    registerFile.set(Reg.LY, registerFile.get(Reg.LY) + 1);
                }

                setMode(2);
                LYchanged();
                nextNonIdleCycle += MODE2_CYCLES;

                if (registerFile.testBit(Reg.STAT, STAT.INT_MODE2)) {
                    cpu.requestInterrupt(Interrupt.LCD_STAT);
                }

            } else if (elapsed % LINE_CYCLES == MODE2_CYCLES) {

                setMode(3);
                int index = registerFile.get(Reg.LY);
                nextImageBuilder.setLine(index, computeLine(index));
                nextNonIdleCycle += MODE3_CYCLES;

            } else if (elapsed % LINE_CYCLES == MODE2_CYCLES + MODE3_CYCLES) {

                setMode(0);
                nextNonIdleCycle += MODE0_CYCLES;
                if (registerFile.testBit(Reg.STAT, STAT.INT_MODE0)) {
                    cpu.requestInterrupt(Interrupt.LCD_STAT);
                }
            }

        } else {

            if ((elapsed - IMAGE_CYCLES) == 0) {

                setMode(1);
                currentImage = nextImageBuilder.build();
                cpu.requestInterrupt(Interrupt.VBLANK);
                if (registerFile.testBit(Reg.STAT, STAT.INT_MODE1)) {
                    cpu.requestInterrupt(Interrupt.LCD_STAT);
                }

                nextNonIdleCycle += LINE_CYCLES;
                registerFile.set(Reg.LY, registerFile.get(Reg.LY) + 1);
                LYchanged();

            } else if ((elapsed - IMAGE_CYCLES) % LINE_CYCLES == 0) {

                nextNonIdleCycle += LINE_CYCLES;
                registerFile.set(Reg.LY, registerFile.get(Reg.LY) + 1);
                LYchanged();
            }
        }
    }

    private LcdImageLine computeLine(int line) {

        LcdImageLine backGround;

        if (line < registerFile.get(Reg.WY)
                || !registerFile.testBit(Reg.LCDC, LCDC.WIN)
                || !(registerFile.get(Reg.WX) - WX_OFFSET < LCD_WIDTH)) {

            backGround = computeBg(line);

        } else {

            backGround = computeBg(line).join(computeWin(),
                    Math.max(0, registerFile.get(Reg.WX) - WX_OFFSET));
        }

        if (!registerFile.testBit(Reg.LCDC, LCDC.OBJ)) {
            return backGround;
        }

        LcdImageLine backSprites = lineWithSprites(line, true);
        LcdImageLine frontSprites = lineWithSprites(line, false);

        BitVector opacity = backGround.opacity()
                .or(backSprites.opacity().not());
        return backSprites.below(backGround, opacity).below(frontSprites);

    }

    private LcdImageLine computeBg(int lineIndex) {

        if (!registerFile.testBit(Reg.LCDC, LCDC.BG)) {

            return new LcdImageLine(LCD_WIDTH);
        }

        LcdImageLine.Builder bgBuilder = new LcdImageLine.Builder(IMAGE_WIDTH);

        for (int i = 0; i * TILE_WIDTH < IMAGE_WIDTH; i++) {

            int lsbAddress = lsbAddress(lineIndex, i * TILE_WIDTH, true);
            int lsb = read(lsbAddress);
            int msb = read(lsbAddress + 1);
            bgBuilder.setBytes(i, reverse8(lsb), reverse8(msb));
        }

        return bgBuilder.build()
                .extractWrapped(registerFile.get(Reg.SCX), LCD_WIDTH)
                .mapColors(registerFile.get(Reg.BGP));
    }

    private LcdImageLine computeWin() {

        LcdImageLine.Builder winBuilder = new LcdImageLine.Builder(LCD_WIDTH);

        for (int i = 0; i * TILE_WIDTH < LCD_WIDTH; i++) {

            int lsbAddress = lsbAddress(winY, i * TILE_WIDTH, false);
            int lsb = read(lsbAddress);
            int msb = read(lsbAddress + 1);
            winBuilder.setBytes(i, reverse8(lsb), reverse8(msb));
        }

        winY++;
        return winBuilder.build().shift(registerFile.get(Reg.WX) - WX_OFFSET)
                .mapColors(registerFile.get(Reg.BGP));
    }

    private int lsbAddress(int row, int col, boolean background) {

        int r = background ? (row + registerFile.get(Reg.SCY)) % IMAGE_HEIGHT
                : winY;

        int indexAddress = (r / TILE_HEIGHT) * (IMAGE_HEIGHT / TILE_HEIGHT)
                + col / TILE_WIDTH
                + AddressMap.BG_DISPLAY_DATA[registerFile.testBit(Reg.LCDC,
                        background ? LCDC.BG_AREA : LCDC.WIN_AREA) ? 1 : 0];

        int tileIndex = read(indexAddress);

        boolean source = registerFile.testBit(Reg.LCDC, LCDC.TILE_SOURCE);
        int tileAddress = AddressMap.TILE_SOURCE[source ? 1 : 0] + tileIndex * 0x10
                + (source ? 0
                        : tileIndex >= AddressMap.TILE_SOURCE[1] / 0x100
                                ? -AddressMap.TILE_SOURCE[1] / 0x10
                                : AddressMap.TILE_SOURCE[1] / 0x10);

        return tileAddress + 2 * (r % TILE_WIDTH);
    }

    private void LYchanged() {

        boolean eq = registerFile.get(Reg.LY) == registerFile.get(Reg.LYC);
        registerFile.setBit(Reg.STAT, STAT.LYC_EQ_LY, eq);

        if (eq && registerFile.testBit(Reg.STAT, STAT.INT_LYC)) {
            cpu.requestInterrupt(Interrupt.LCD_STAT);
        }
    }

    private void setMode(int m) {

        registerFile.setBit(Reg.STAT, STAT.MODE0, test(m, 0));
        registerFile.setBit(Reg.STAT, STAT.MODE1, test(m, 1));
    }

    private int[] spritesIntersectingLine(int line) {

        int coordinateIndex[] = new int[10];
        int cnt = 0;

        for (int i = 0; i < TOTAL_SPRITES && cnt < MAX_SPRITES; i++) {

            int y = oam.read(4 * i) - SPRITE_MAX_HEIGHT;
            if (line >= y
                    && line < y + (registerFile.testBit(Reg.LCDC, LCDC.OBJ_SIZE)
                            ? SPRITE_MAX_HEIGHT
                            : SPRITE_MIN_HEIGHT)) {

                coordinateIndex[cnt++] = make16(oam.read(4 * i + 1), i);
            }
        }

        Arrays.sort(coordinateIndex, 0, cnt);
        int sprites[] = new int[cnt];

        for (int i = 0; i < cnt; i++) {
            sprites[i] = clip(8, coordinateIndex[i]);
        }

        return sprites;
    }

    private LcdImageLine lineWithSprite(int spriteIndex, int line) {

        LcdImageLine.Builder b = new LcdImageLine.Builder(LCD_WIDTH);

        int spriteLine;

        if (test(oam.read(spriteIndex * 4 + 3), Sprite.FLIP_V)) {

            int size = registerFile.testBit(Reg.LCDC, LCDC.OBJ_SIZE)
                    ? SPRITE_MAX_HEIGHT
                    : SPRITE_MIN_HEIGHT;
            int y = oam.read(4 * spriteIndex) - SPRITE_MAX_HEIGHT;
            spriteLine = size - line + y - 1;

        } else {

            int y = oam.read(4 * spriteIndex) - SPRITE_MAX_HEIGHT;
            spriteLine = line - y;
        }

        int tile = oam.read(spriteIndex * 4 + 2);
        if (spriteLine >= SPRITE_MIN_HEIGHT)
            tile++;

        int lsb = videoRam
                .read(tile * 16 + 2 * (spriteLine % SPRITE_MIN_HEIGHT));
        int msb = videoRam
                .read(tile * 16 + 2 * (spriteLine % SPRITE_MIN_HEIGHT) + 1);

        if (!test(oam.read(spriteIndex * 4 + 3), Sprite.FLIP_H)) {
            lsb = reverse8(lsb);
            msb = reverse8(msb);
        }

        b.setBytes(0, lsb, msb);

        return b.build().shift(oam.read(4 * spriteIndex + 1) - SPRITE_WIDTH)
                .mapColors(registerFile
                        .get(test(oam.read(4 * spriteIndex + 3), Sprite.PALETTE)
                                ? Reg.OBP1
                                : Reg.OBP0));

    }

    private LcdImageLine lineWithSprites(int line, boolean behind) {

        int sprites[] = spritesIntersectingLine(line);
        LcdImageLine l = new LcdImageLine(LCD_WIDTH);
        for (int sprite : sprites) {

            if (test(oam.read(sprite * 4 + 3), Sprite.BEHIND_BG) == behind) {

                l = lineWithSprite(sprite, line).below(l);
            }
        }
        return l;
    }
}