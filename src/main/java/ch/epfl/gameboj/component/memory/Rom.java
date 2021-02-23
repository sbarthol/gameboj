package ch.epfl.gameboj.component.memory;

import java.util.Arrays;

public final class Rom {
    
    private final byte[] data;
    
    /**
     * construit une mémoire morte dont 
     * le contenu et la taille sont ceux 
     * du tableau d'octets donné en argument, 
     * ou lève NullPointerException si 
     * celui-ci est nul
     * @param data
     * @throws NullPointerException
     */
    public Rom(byte[] data) {
        
        if(data == null) {
            throw new NullPointerException();
        }else {
            this.data=Arrays.copyOf(data, data.length);
        }
    }
    
    /**
     * retourne la taille, en octets, de 
     * la mémoire — qui n'est rien d'autre 
     * que la taille du tableau passé au 
     * constructeur
     * @return
     */
    public int size() {
        return data.length;
    }
    
    /**
     * retourne l'octet se trouvant à l'index 
     * donné, sous la forme d'une valeur comprise 
     * entre 0 et FF16, ou lève l'exception 
     * IndexOutOfBoundsException si l'index est invalide
     * @param index
     * @return
     * @throws IndexOutOfBoundsException
     */
    public int read(int index) {
        
        if(index >= 0 && index < data.length) {
            return Byte.toUnsignedInt(data[index]);
        }else {
            throw new IndexOutOfBoundsException();
        }
    }
        
}
