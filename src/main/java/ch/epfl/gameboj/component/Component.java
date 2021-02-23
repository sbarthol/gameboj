package ch.epfl.gameboj.component;

import ch.epfl.gameboj.Bus;

public interface Component {
    
    public static final int NO_DATA = 0x100;
    
    /**
     * retourne l'octet stocké à l'adresse 
     * donnée par le composant, ou NO_DATA 
     * si le composant ne possède aucune valeur 
     * à cette adresse ; lève l'exception 
     * IllegalArgumentException si l'adresse 
     * n'est pas une valeur 16 bits
     * @param un entier: l'adresse
     * @throws IllegalArgumentException
     */
    public abstract int read(int address) ;
    
    /**
     * stocke la valeur donnée à l'adresse donnée 
     * dans le composant, ou ne fait rien si le 
     * composant ne permet pas de stocker de valeur à 
     * cette adresse ; lève l'exception IllegalArgumentException 
     * si l'adresse n'est pas une valeur 16 bits ou si la 
     * donnée n'est pas une valeur 8 bits.
     * @param 2 entiers: l'adress et la valeur
     * @throws IllegalArgumentException
     */
    public abstract void write(int address, int data);
    
    /**
     * attache le composant au bus donné, 
     * en appelant simplement la méthode 
     * attach de celui-ci avec le composant en argument
     * @param le bus
     * @throws IllegalArgumentException
     */
    public default void attachTo(Bus bus) {
        
        bus.attach(this);
    }
}
