package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;

import java.util.Objects;

public final class RamController implements Component {
    
    private final int startAddress;
    private final int endAddress;
    private final Ram ram;
    
    /**
     * construit un contrôleur pour la mémoire vive 
     * donnée, accessible entre l'adresse startAddress 
     * (inclue) et endAddress (exclue) ; lève l'exception 
     * NullPointerException si la mémoire donnée 
     * est nulle et l'exception IllegalArgumentException 
     * si l'une des deux adresses n'est pas une valeur 
     * 16 bits, ou si l'intervalle qu'elles décrivent a 
     * une taille négative ou supérieure à celle de la mémoire
     * @param ram
     * @param startAddress
     * @param endAddress
     * @throws IllegalArgumentException
     * @throws NullPointerException
     */
    public RamController(Ram ram, int startAddress, int endAddress) {
        Preconditions.checkBits16(startAddress);
        Preconditions.checkBits16(endAddress - 1);
        
        this.ram = Objects.requireNonNull(ram);
        Preconditions.checkArgument(startAddress < endAddress && endAddress - startAddress <= ram.size());

        this.startAddress = startAddress;
        this.endAddress = endAddress;
        
    }

    /**
     * appelle le premier constructeur en lui 
     * passant une adresse de fin telle que la
     * totalité de la mémoire vive soit 
     * accessible au travers du contrôleur
     * @param ram
     * @param startAddress
     */
    public RamController(Ram ram, int startAddress) {
        this(ram, startAddress, startAddress + ram.size());
    }
    
    @Override
    public int read(int address) {
        
        Preconditions.checkBits16(address);
        if(address >= startAddress && address < endAddress) {
            return ram.read(address - startAddress);
        }else {
            return Component.NO_DATA;
        }
    }

    @Override
    public void write(int address, int data) {
        
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        
        if(address >= startAddress && address < endAddress) {
            
            ram.write(address - startAddress, data);
        }
    }
}
