package ch.epfl.gameboj.component.cartridge;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

public final class MBC0 implements Component {

    private final Rom rom;
    private static final int ROM_SIZE = 0x8000;

    /**
     * Construit un contrôleur de type 0 pour la mémoire donnée ; lève
     * l'exception NullPointerException si la mémoire est nulle, et
     * IllegalArgumentException si elle ne contient pas exactement 32 768
     * octets.
     * 
     * @param rom
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public MBC0(Rom rom) {

        this.rom = Objects.requireNonNull(rom);
        Preconditions.checkArgument(rom.size() == ROM_SIZE);

    }

    @Override
    public int read(int address) {
        
        Preconditions.checkBits16(address);
        
        if(address >= 0 && address < rom.size()) {
            return rom.read(address);
        }
        return NO_DATA;
    }

    @Override
    public void write(int address, int data) {

    }

}
