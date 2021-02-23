package ch.epfl.gameboj.component.lcd;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;
import ch.epfl.gameboj.bits.Bits;

public final class LcdImageLine {

    private final BitVector lsb, msb, opacity;
    private static final int IDENTITY_MAP = 0b11_10_01_00;

    /**
     * Construit une ligne d'image, composée de trois vecteurs de bits de même
     * longueur : le premier contient les bits de poids fort (msb), le second
     * les bits de poids faible (lsb) et le dernier l'opacité (opacity)
     * 
     * @param lsb
     * @param msb
     * @param opacity
     */
    public LcdImageLine(BitVector lsb, BitVector msb, BitVector opacity) {

        Preconditions.checkArgument(lsb.size() == msb.size() && lsb.size() == opacity.size());
        this.msb = msb;
        this.lsb = lsb;
        this.opacity = opacity;
    }

    /**
     * Construit une ligne d'image de taille size dont les 3 vecteurs de bits
     * sont nuls
     * 
     * @param size
     */
    public LcdImageLine(int size) {

        this(new BitVector(size), new BitVector(size), new BitVector(size));
    }

    /**
     * 
     * @param ligne
     * @return la longueur, en pixels, de la ligne
     */
    public int size() {
        return lsb.size();
    }

    /**
     * 
     * @return le vecteur des bits de poids fort (msb)
     */
    public BitVector msb() {
        return msb;
    }

    /**
     * 
     * @return le vecteur des bits de poids faible (lsb)
     */
    public BitVector lsb() {
        return lsb;
    }

    /**
     * 
     * @return le vecteur d'opacité
     */
    public BitVector opacity() {
        return opacity;
    }

    /**
     * Décale la ligne d'un nombre de pixels donné, en préservant sa longueur
     * 
     * @param i
     * @return
     */
    public LcdImageLine shift(int i) {

        return new LcdImageLine(lsb().shift(i), msb().shift(i),
                opacity().shift(i));
    }

    /**
     * Permet d'extraire de l'extension infinie par enroulement, à partir d'un
     * pixel donné, une ligne de longueur donnée
     * 
     * @param size
     * @return une ligne de longeueur 'extractSize'
     */
    public LcdImageLine extractWrapped(int index, int extractSize) {

        return new LcdImageLine(lsb().extractWrapped(index, extractSize),
                msb().extractWrapped(index, extractSize),
                opacity().extractWrapped(index, extractSize));
    }

    /**
     * Transformer les couleurs de la ligne en fonction d'une palette, donnée
     * sous la forme d'un octet
     * 
     * @param color
     * @return ligne
     */
    public LcdImageLine mapColors(int color) {

        Preconditions.checkBits8(color);

        if (color == IDENTITY_MAP)
            return this;

        BitVector new_lsb = new BitVector(size());
        BitVector new_msb = new BitVector(size());

        for (int i = 0; i < 4; i++) {

            new_lsb = new_lsb.or(new BitVector(size(), Bits.test(color, 2 * i))
                    .and(lsb.xor(new BitVector(size(), !Bits.test(i, 0))))
                    .and(msb.xor(new BitVector(size(), !Bits.test(i, 1)))));

            new_msb = new_msb.or(new BitVector(size(), Bits.test(color, 2 * i + 1))
                    .and(lsb.xor(new BitVector(size(), !Bits.test(i, 0))))
                    .and(msb.xor(new BitVector(size(), !Bits.test(i, 1)))));
        }

        return new LcdImageLine(new_lsb, new_msb, opacity);
    }

    /**
     * Compose la ligne avec une seconde de même longueur, placée au-dessus
     * d'elle, en utilisant l'opacité de la ligne supérieure pour effectuer la
     * composition
     * 
     * @param that
     * @return ligne
     */
    public LcdImageLine below(LcdImageLine upper) {

        return below(upper, upper.opacity());
    }

    /**
     * Compose la ligne avec une seconde de même longueur, placée au-dessus
     * d'elle, en utilisant un vecteur d'opacité passé en argument pour
     * effectuer la composition, celui de la ligne supérieure étant ignoré
     * 
     * @param that
     * @param opacity
     * @return
     */
    public LcdImageLine below(LcdImageLine upper, BitVector opacity) {

        Preconditions.checkArgument(upper.size() == size() && opacity.size() == size());

        BitVector new_lsb = lsb.and(opacity.not()).or(upper.lsb.and(opacity));
        BitVector new_msb = msb.and(opacity.not()).or(upper.msb.and(opacity));
        return new LcdImageLine(new_lsb, new_msb, this.opacity.or(opacity));
    }

    /**
     * Joint la ligne avec une autre de même longueur, à partir d'un pixel
     * d'index donné
     * 
     * @param index
     * @return
     */
    public LcdImageLine join(LcdImageLine other, int index) {
        
        Preconditions.checkArgument(other.size() == size() && index >= 0 && index <= size());

        BitVector ones = new BitVector(size(), true).shift(index);

        BitVector new_lsb = this.lsb.and(ones.not()).or(other.lsb().and(ones));
        BitVector new_msb = this.msb.and(ones.not()).or(other.msb().and(ones));
        BitVector new_opacity = this.opacity.and(ones.not())
                .or(other.opacity().and(ones));

        return new LcdImageLine(new_lsb, new_msb, new_opacity);
    }

    @Override
    public boolean equals(Object other) {

        if (other == null)
            return false;
        if (!(other instanceof LcdImageLine))
            return false;

        return (msb.equals(((LcdImageLine) other).msb())
                && lsb.equals(((LcdImageLine) other).lsb()))
                && opacity.equals(((LcdImageLine) other).opacity());
    }

    @Override
    public int hashCode() {

        return Objects.hash(lsb, msb, opacity);
    }
    
    @Override
    public String toString() {
        
        return String.format("lsb:\t%s\nlsm:\t%s\nopacity:\t%s", lsb,msb,opacity);
    }

    public static final class Builder {

        private final BitVector.Builder lsbBuilder, msbBuilder;

        /**
         * Créé 2 bâtisseurs pour les vecteurs de bits de poids faible et fort
         * en les initialisant avec size
         * 
         * @param height
         * @param width
         */
        public Builder(int size) {
            lsbBuilder = new BitVector.Builder(size);
            msbBuilder = new BitVector.Builder(size);
        }

        /**
         * Permet de définir la valeur des octets de poids fort et de poids
         * faible de la ligne, à un index donné
         * 
         * @param index
         * @return
         */
        public Builder setBytes(int index, int lsbByte, int msbByte) {

            Preconditions.checkBits8(lsbByte);
            Preconditions.checkBits8(msbByte);

            lsbBuilder.setByte(index, lsbByte);
            msbBuilder.setByte(index, msbByte);
            return this;
        }

        /**
         * Permet de construire la ligne avec les octets définis jusqu'à
         * présent, dans laquelle tous les pixels de couleur 0 sont
         * transparents, les autres opaques
         * 
         * @return
         */
        public LcdImageLine build() {
            BitVector lsb = lsbBuilder.build();
            BitVector msb = msbBuilder.build();
            return new LcdImageLine(lsb, msb, lsb.or(msb));
        }
    }
}
