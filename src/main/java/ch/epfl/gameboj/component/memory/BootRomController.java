package ch.epfl.gameboj.component.memory;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cartridge.Cartridge;

public final class BootRomController implements Component {

    private final Cartridge cartridge;
    private final Rom rom;
    private boolean bootRomDisabled;

    /**
     * construit un contrôleur de mémoire de démarrage auquel la cartouche
     * donnée est attachée ; lève l'exception NullPointerException si cette
     * cartouche est nulle.
     * 
     * @param cartridge
     * @throws NullPointerException
     */
    public BootRomController(Cartridge cartridge) {

        this.cartridge = Objects.requireNonNull(cartridge);
        bootRomDisabled = false;
        rom = new Rom(BootRom.DATA);
    }

    @Override
    public int read(int address) {

        Preconditions.checkBits16(address);
        if (address >= AddressMap.BOOT_ROM_START
                && address < AddressMap.BOOT_ROM_END && !bootRomDisabled) {

            return rom.read(address - AddressMap.BOOT_ROM_START);

        } else {
            return cartridge.read(address);
        }
    }

    @Override
    public void write(int address, int data) {

        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);

        if (address == AddressMap.REG_BOOT_ROM_DISABLE) {

            bootRomDisabled = true;
        }else {
            cartridge.write(address, data);
        }
    }

}
