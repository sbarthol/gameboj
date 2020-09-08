package ch.epfl.gameboj.component.cartridge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static ch.epfl.gameboj.Preconditions.*;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

public final class Cartridge implements Component {

    private final Component bankController;
    private static final int MBC_TYPE = 0x147;
    private static final int SIZE = 0x149;
    private static final int TITLE_START = 0x134;
    private static final int TITLE_END = 0x143;
    private static final int RAM_SIZE[] = new int[] { 0, 2048, 8192, 32768 };
    private final String name;

    private Cartridge(Component bankController) {

        this.bankController = bankController;

        StringBuilder b = new StringBuilder();
        for (int i = TITLE_START; i <= TITLE_END; i++) {
            b.append(Character.toChars(bankController.read(i)));
        }
        name = b.toString();
    }

    /**
     * retourne une cartouche dont la mémoire morte contient les octets du
     * fichier donné ; lève l'exception IOException en cas d'erreur
     * d'entrée-sortie, y compris si le fichier donné n'existe pas, et
     * l'exception IllegalArgumentException si le fichier en question ne
     * contient pas 0 à la position 147
     * 
     * @param romFile
     * @return
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public static Cartridge ofFile(File romFile) throws IOException {

        byte data[] = Files.readAllBytes(romFile.toPath());

        checkArgument(data.length > MBC_TYPE);
        checkArgument(data[MBC_TYPE] >= 0 && data[MBC_TYPE] <= 3);

        if (data[MBC_TYPE] == 0)
            return new Cartridge(new MBC0(new Rom(data)));
        else
            return new Cartridge(new MBC1(new Rom(data), RAM_SIZE[data[SIZE]]));
    }

    /**
     * retourne le nom du jeu que contient la cartouche
     * 
     * @return
     */
    public String name() {
        return name;
    }

    @Override
    public int read(int address) {

        checkBits16(address);
        return bankController.read(address);
    }

    @Override
    public void write(int address, int data) {

        checkBits16(address);
        checkBits8(data);

        bankController.write(address, data);
    }

}
