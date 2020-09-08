package ch.epfl.gameboj.component;

import java.util.Objects;
import static ch.epfl.gameboj.AddressMap.*;
import static ch.epfl.gameboj.bits.Bits.*;
import static ch.epfl.gameboj.Preconditions.*;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.cpu.Cpu.Interrupt;

public final class Joypad implements Component {

    /**
     * 
     * Chaque élément de l'enum correspond à une touche de la GameBoy
     * 
     */
    public enum Key {
        RIGHT, LEFT, UP, DOWN, A, B, SELECT, START
    }

    private int p1, pressedKeys;
    private final Cpu cpu;

    /**
     * Simule un composant attaché au bus, représente le clavier
     * 
     * @param cpu
     */
    public Joypad(Cpu cpu) {
        this.cpu = Objects.requireNonNull(cpu);
        p1 = 0;
        pressedKeys = 0;
    }

    @Override
    public int read(int address) {

        checkBits16(address);

        if (address == REG_P1)
            return complement8(p1);
        return NO_DATA;
    }

    @Override
    public void write(int address, int data) {

        checkBits16(address);
        checkBits8(data);

        if (address == REG_P1) {
            p1 = ((complement8(data)) & 0x30) | (p1 & 0xcf);
            updateP1();
        }
    }

    /**
     * Simule la pression d'une touche; prend en paramètre la touche en question
     * 
     * @param k
     */
    public void keyPressed(Key k) {

        pressedKeys = set(pressedKeys, k.ordinal(), true);
    }

    /**
     * Simule le relâchement d'une touche; prend en paramètre la touche en
     * question
     * 
     * @param k
     */
    public void keyReleased(Key k) {

        pressedKeys = set(pressedKeys, k.ordinal(), false);
    }

    private void updateP1() {

        int old = p1;
        p1 &= 0x30;

        switch (extract(p1, 4, 2)) {

        case 0b00: {
            p1 |= 0;
        }
            break;
        case 0b01: {
            p1 |= extract(pressedKeys, 0, 4);
        }
            break;
        case 0b10: {
            p1 |= extract(pressedKeys, 4, 4);
        }
            break;
        case 0b11: {
            p1 |= extract(pressedKeys, 0, 4) | extract(pressedKeys, 4, 4);
        }
            break;
        }

        if (((~old) & p1) != 0) {
            cpu.requestInterrupt(Interrupt.JOYPAD);
        }
    }
}