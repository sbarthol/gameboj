package ch.epfl.gameboj;

public interface Register {
    
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
}