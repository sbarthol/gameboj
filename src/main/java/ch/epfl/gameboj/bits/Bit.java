package ch.epfl.gameboj.bits;

public interface Bit {
    
    /**
     *  Automatiquement fournie par le type 
     *  énuméré, étant donné que tous ces 
     *  types fournissent une telle méthode
     */
    public abstract int ordinal();
    
    /**
     *  @return la même valeur 
     *  que la méthode ordinal 
     *  mais dont le nom est plus parlant
     */
    public default int index() {
        
        return ordinal();
    }
    
    /**
     *  @return le masque correspondant 
     *  au bit, c-à-d une valeur dont seul 
     *  le bit de même index que celui du 
     *  récepteur vaut 1
     */
    public default int mask() {
        
        return Bits.mask(index());
    }
}
