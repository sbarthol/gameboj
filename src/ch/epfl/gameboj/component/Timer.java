package ch.epfl.gameboj.component;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.cpu.Cpu;

public final class Timer implements Component, Clocked {

    private final Cpu cpu;
    private int tima, tac, tma, div;

    /**
     * construit un minuteur associé au processeur donné, ou lève l'exception
     * NullPointerException si celui-ci est nul.
     * 
     * @param cpu
     * @throws NullPointerException
     */
    public Timer(Cpu cpu) {

        this.cpu = Objects.requireNonNull(cpu);

        div = 0;
        tma = 0;
        tima = 0;
        tac = 0;
    }

    @Override
    public void cycle(long cycle) {

        boolean s0 = state();
        div = Bits.clip(16, div + 4);
        incIfChange(s0);
    }

    @Override
    public int read(int address) {

        Preconditions.checkBits16(address);

        switch (address) {
        case AddressMap.REG_DIV:
            return Bits.extract(div, 8, 8);

        case AddressMap.REG_TIMA:
            return tima;

        case AddressMap.REG_TMA:
            return tma;

        case AddressMap.REG_TAC:
            return tac;

        default:
            return NO_DATA;
        }
    }

    @Override
    public void write(int address, int data) {

        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);

        boolean s0 = state();

        switch (address) {
        case AddressMap.REG_DIV: {
            div = 0;
            incIfChange(s0);
        }
            break;

        case AddressMap.REG_TIMA: {
            tima = data;
        }
            break;

        case AddressMap.REG_TMA: {
            tma = data;
        }
            break;

        case AddressMap.REG_TAC: {
            tac = data;
            incIfChange(s0);
        }
            break;
        }
        
    }

    private boolean state() {

        int bit[] = { 9, 3, 5, 7 };
        return Bits.test(tac, 2) && Bits.test(div, bit[Bits.clip(2, tac)]);
    }

    private void incIfChange(boolean lastState) {

        if (lastState && !state()) {

            if (tima == 0xFF) {
                tima = tma;
                cpu.requestInterrupt(Cpu.Interrupt.TIMER);
            } else {
                tima++;
            }
        }
    }
}