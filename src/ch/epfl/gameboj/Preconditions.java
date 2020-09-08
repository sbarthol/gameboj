package ch.epfl.gameboj;

public interface Preconditions {

    /**
     * l'exception IllegalArgumentException si son argument est faux, et ne fait
     * rien sinon
     * 
     * @param b
     * @throws IllegalArgumentException
     */
    public static void checkArgument(boolean b) {

        if (!b) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * retourne son argument si celui-ci est compris entre 0 et FF16 (inclus),
     * ou lève l'exception IllegalArgumentException sinon
     * 
     * @param v
     * @throws IllegalArgumentException
     */
    public static int checkBits8(int v) {
        
        checkArgument(v >= 0 && v <= 0xFF);
        return v;

    }

    /**
     * retourne son argument si celui-ci est compris entre 0 et FFFF16 (inclus),
     * ou lève l'exception IllegalArgumentException sinon
     * 
     * @param v
     * @return
     * @throws IllegalArgumentException
     */
    public static int checkBits16(int v) {

        checkArgument(v >= 0 && v <= 0xFFFF);
        return v;
    }
}
