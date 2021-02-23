package ch.epfl.gameboj;

import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

public final class RegisterFile<E extends Register> {
    
    private final byte banc[];
    
    /**
     * Construit un banc de registres 8 bits 
     * dont la taille (c-à-d le nombre de registres) 
     * est égale à la taille du tableau donné
     * @param allRegs
     */
    public RegisterFile(E[] allRegs) {
        banc = new byte[allRegs.length]; 
    }
    
    /**
     * 
     * @return la valeur 8 bits contenue dans 
     * le registre donné, sous la forme d'un 
     * entier compris entre 0 (inclus) et FF16 (inclus)
     */
    public int get(E reg) {
       return Bits.clip(8, banc[reg.index()]);
    }
    
    
    /**
     * Modifie le contenu du registre donné pour 
     * qu'il soit égal à la valeur 8 bits donnée
     * 
     * @throws IllegalArgumentException si la valeur 
     * n'est pas une valeur 8 bits valide
     */
    public void set(E reg, int newValue) {
        Preconditions.checkBits8(newValue);
        banc[reg.index()] = (byte)newValue;
    }
    
    /**
     * 
     * @return vrai si et seulement si le 
     * bit donné du registre donné vaut 1
     */
    public boolean testBit(E reg, Bit b) {
        
        return Bits.test(get(reg), b);
    }
    
    /**
     * 
     * Modifie la valeur stockée dans le registre 
     * donné pour que le bit donné ait la 
     * nouvelle valeur donnée
     */
    public void setBit(E reg, Bit bit, boolean newValue) {
        
        set(reg, Bits.set(get(reg), bit.index(), newValue));
    }
}
