package ch.epfl.gameboj;
import java.util.ArrayList;
import java.util.Objects;

import ch.epfl.gameboj.component.Component;
import static ch.epfl.gameboj.Preconditions.*;

public final class Bus {
    
    private final ArrayList<Component> components;
    
    /**
     * Initialise la liste des composants
     */
    public Bus() {
        
        components = new ArrayList<Component>();
    }
    
    /**
     * Attache le composant donné au bus, 
     * ou lève l'exception NullPointerException 
     * si le composant vaut null
     * @param le composant
     * @throws NullPointerException
     */
    public void attach(Component component) {
        
        components.add(Objects.requireNonNull(component));
    }
    
    /**
     * Retourne la valeur stockée à 
     * l'adresse donnée si au moins un 
     * des composants attaché au bus possède 
     * une valeur à cette adresse, ou FF16 
     * sinon (!) ; lève l'exception 
     * IllegalArgumentException si l'adresse 
     * n'est pas une valeur 16 bits
     * @param l'adresse
     * @throws IllegalArgumentException
     */
    public int read(int address) {
        
        checkBits16(address);
        for (Component c: components) {
            
            if (c.read(address) != Component.NO_DATA) {
                return c.read(address);
            }
        }
        return 0xFF;
    }
    
    /**
     * Ecrit la valeur à l'adresse donnée 
     * dans tous les composants connectés
     * au bus ; lève l'exception 
     * IllegalArgumentException si 
     * l'adresse n'est pas une valeur 
     * 16 bits ou si la donnée n'est 
     * pas une valeur 8 bits
     * @param l'adresse et la donnée
     * @throws IllegalArgumentException
     */
    public void write(int address, int data) {
        
        checkBits16(address);
        checkBits8(data);
                
        for (Component c: components) {
            
            c.write(address, data);
        }
    }
}
