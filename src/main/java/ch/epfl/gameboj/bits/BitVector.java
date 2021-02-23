package ch.epfl.gameboj.bits;

import ch.epfl.gameboj.Preconditions;

import java.util.Arrays;

public final class BitVector {

    private final int[] bits;

    /**
     * Prend une taille (en bits) et une valeur initiale (sous forme d'un
     * booléen) et construit un vecteur de bits de la taille donnée, dont tous
     * les bits ont la valeur donnée. lève une exception si la taille est
     * négative ou n'est pas un multiple de 32.
     * 
     * @param size
     * @param value
     * @throws IllegalArgumentExcpeption
     * 
     */
    public BitVector(int size, boolean value) {

        Preconditions.checkArgument(size > 0 && size % Integer.SIZE == 0);
        bits = new int[size / Integer.SIZE];
        Arrays.fill(bits, (value ? -1 : 0));
    }

    /**
     * Ne prend qu'une taille en argument et initialise tous les bits à 0. Lève
     * une exception si la taille est négative ou n'est pas un multiple de 32.
     * 
     * @throws IllegalArgumentExcpeption
     * @param size
     */
    public BitVector(int size) {
        this(size, false);
    }

    private BitVector(int[] bits) {
        this.bits = bits;
    }

    /**
     * Permet de connaitre la taille du vecteur, en bits
     * 
     * @return la taille du vecteur, en bits
     */
    public int size() {
        return bits.length * Integer.SIZE;
    }

    /**
     * Permet de déterminer si le bit d'index donné est vrai ou faux
     * 
     * @param index
     * @return
     */
    public boolean testBit(int index) {

        Preconditions.checkArgument(index >= 0 && index < size());
        return Bits.test(bits[index / Integer.SIZE], index % Integer.SIZE);
    }

    /**
     * Permet de calculer le complément du vecteur de bits
     * 
     * @return
     */
    public BitVector not() {
        int complement[] = new int[bits.length];
        for (int i = 0; i * Integer.SIZE < size(); i++) {
            complement[i] = ~bits[i];
        }
        return new BitVector(complement);
    }

    /**
     * Permet de calculer la disjoncion bit à bit avec un autre vecteur de même
     * taille (or)
     * 
     * @param bv
     * @return
     */
    public BitVector or(BitVector bv) {
        Preconditions.checkArgument(bv.size() == size());

        int or[] = new int[bits.length];
        for (int i = 0; i * Integer.SIZE < size(); i++) {
            or[i] = bits[i] | bv.bits[i];
        }
        return new BitVector(or);
    }

    /**
     * Permet de calculer la conjonction bit à bit avec un autre vecteur de même
     * taille (and)
     * 
     * @param bv
     * @return
     */
    public BitVector and(BitVector bv) {
        Preconditions.checkArgument(bv.size() == size());

        int and[] = new int[bits.length];
        for (int i = 0; i * Integer.SIZE < size(); i++) {
            and[i] = bits[i] & bv.bits[i];
        }
        return new BitVector(and);
    }

    /**
     * Permet de calculer le ou exclusif bit à bit avec un autre vecteur de même
     * taille (xor)
     * 
     * @param bv
     * @return
     */
    public BitVector xor(BitVector bv) {
        Preconditions.checkArgument(bv.size() == size());

        return this.not().and(bv).or(this.and(bv.not()));
    }

    /**
     * Permet de décaler le vecteur d'une distance quelconque, en utilisant la
     * convention habituelle qu'une distance positive représente un décalage à
     * gauche, une distance négative un décalage à droite (shift)
     * 
     * @param dist
     * @return
     */
    public BitVector shift(int dist) {

        return extractZeroExtended(-dist, size());
    }

    private int bitsAtIndex(boolean wrapped, int index) {

        if ((index >= 0 && index + Integer.SIZE <= size()) || wrapped) {

            index = Math.floorMod(index, size());
            int k = index / Integer.SIZE;

            int l = Bits.extract(bits[k], index % Integer.SIZE,
                    Integer.SIZE - (index % Integer.SIZE));
            int h = Bits.extract(bits[(k + 1) % bits.length], 0,
                    index % Integer.SIZE);

            return (h << (Integer.SIZE - index % Integer.SIZE)) | l;
        }

        else if (index > -Integer.SIZE && index < 0) {

            int h = Bits.extract(bits[0], 0, index + Integer.SIZE);
            return h << (-index);

        } else if (index < size() && index + Integer.SIZE > size()) {

            return Bits.extract(bits[bits.length - 1], index % Integer.SIZE,
                    Integer.SIZE - index % Integer.SIZE);
        }
        return 0;
    }

    private BitVector extract(boolean wrapped, int extractSize, int index) {

        Preconditions.checkArgument(extractSize > 0 && extractSize % Integer.SIZE == 0);
        int extract[] = new int[extractSize / Integer.SIZE];

        for (int i = 0; i * Integer.SIZE < extractSize; i++) {
            extract[i] = bitsAtIndex(wrapped, index + i * Integer.SIZE);
        }

        return new BitVector(extract);
    }

    /**
     * Permet d'extraire un vecteur de taille donnée de l'extension par 0 du
     * vecteur (extractZeroExtended)
     * 
     * @param extractSize
     * @param index
     * @return
     */
    public BitVector extractZeroExtended(int index, int extractSize) {

        return extract(false, extractSize, index);
    }

    /**
     * Permet d'extraire un vecteur de taille donnée de l'extension par
     * enroulement du vecteur (extractWrapped)
     * 
     * @param extractSize
     * @param index
     * @return
     */
    public BitVector extractWrapped(int index, int extractSize) {

        return extract(true, extractSize, index);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (!(other instanceof BitVector))
            return false;

        return Arrays.equals(bits, ((BitVector) other).bits);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bits);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < size(); i++) {
            b.append(testBit(i) ? "1" : "0");
        }
        return b.reverse().toString();
    }

    public final static class Builder {

        private int[] bits;

        /**
         * Le constructeur de la classe Builder prend en argument la taille du
         * vecteur de bits à construire, qui doit être un multiple de 32 non
         * négatif. La totalité des bits du vecteur à construire valent
         * initialement 0.
         * 
         * @param size
         */
        public Builder(int size) {
            Preconditions.checkArgument(size % Integer.SIZE == 0 && size > 0);
            bits = new int[size / Integer.SIZE];
        }

        /**
         * Permet de définir la valeur d'un octet désigné par son index
         * 
         * @param index
         * @param byte
         * @return le bâtisseur afin de pouvoir chaîner les appels
         */
        public Builder setByte(int index, int b) {
            if (bits == null)
                throw new IllegalStateException();
            if (index < 0 || index >= bits.length * 4)
                throw new IndexOutOfBoundsException();
            Preconditions.checkBits8(b);

            bits[index / 4] &= ~(0xff << (Byte.SIZE * (index % 4)));
            bits[index / 4] |= b << (Byte.SIZE * (index % 4));

            return this;
        }

        /**
         * Permet de construire le vecteur de bits
         * 
         * @return le vecteur de bits
         */
        public BitVector build() {
            if (bits == null)
                throw new IllegalStateException();
            BitVector bv = new BitVector(bits);
            bits = null;
            return bv;
        }
    }
}
