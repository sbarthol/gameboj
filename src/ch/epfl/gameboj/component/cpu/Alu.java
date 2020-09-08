package ch.epfl.gameboj.component.cpu;

import static ch.epfl.gameboj.Preconditions.*;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

public final class Alu {
    
    public enum Flag implements Bit {
        UNUSED_0, UNUSED_1, UNUSED_2, UNUSED_3,
        C, H, N, Z
    } 
    
    public enum RotDir {
        LEFT, RIGHT
    } 

    private Alu() {}
    
    /**
     * @return une valeur dont les bits correspondant 
     * aux différents fanions valent 1
     * ssi l'argument correspondant est vrai
     */
    public static int maskZNHC(boolean z, boolean n, boolean h, boolean c) {
        
        int result = 0;
        if (z) {
            result |= Flag.Z.mask();
        }
        if (n) {
            result |= Flag.N.mask();
        }
        if (h) {
            result |= Flag.H.mask();
        }
        if (c) {
            result |= Flag.C.mask();
        }
        return result;
    }
    
    /**
     * @param la paquet d'entrée
     * @return la valeur contenue dans le paquet valeur/fanion donné
     */
    public static int unpackValue(int valueFlags) {
        checkBits16(valueFlags >> 8);
        return valueFlags >> 8; 
    }
    
    
    /**
     * 
     * @param valueFlags
     * @return les fanions contenus dans le paquet valeur/fanion donné
     */
    public static int unpackFlags(int valueFlags) {
        return Bits.clip(8, valueFlags);
    }
    
    
    /**
     * @return la somme des deux valeurs 
     * 8 bits données et du bit de retenue 
     * initial c0, et les fanions Z0HC,
     */
    public static int add(int l, int r, boolean c0) {
        
        checkBits8(l);
        checkBits8(r);
        
        int c = c0 ? 1 : 0;
        int v = Bits.clip(8, l+r+c);
        
        return packValueZNHC(v, v == 0, false, Bits.clip(4, l) + Bits.clip(4, r) + c > 0xf, (l + r + c) > 0xff);
    }
    
    
    /**
     * 
     * @return identique à la méthode précédente
     * mais avec false comme dernier argument
     */
    public static int add(int l, int r) {
        return add(l, r, false);
    }
    
    
    /**
     * 
     * @return la somme des deux valeurs 
     * 16 bits données et les fanions 00HC, 
     * où H et C sont les fanions correspondant 
     * à l'addition des 8 bits de poids faible
     */
    public static int add16L(int l, int r) {
        checkBits16(l);
        checkBits16(r);         
          
        int a = add(Bits.clip(8, l),Bits.clip(8, r));
        int b = add(Bits.extract(l, 8, 8),Bits.extract(r, 8, 8),Bits.test(a, 4));
        
        a = Bits.set(a, Flag.Z.index(), false);
        
        return (unpackValue(b) << 16) | a;

    }
    
    
    /**
     *
     * @return identique à add16L,
     *  si ce n'est que les fanions H et C
     *  correspondent à l'addition des 8 bits 
     *  de poids fort
     */
    public static int add16H(int l, int r) {
        checkBits16(l);
        checkBits16(r);
        
        int a = add(Bits.clip(8, l),Bits.clip(8, r));
        int b = add(Bits.extract(l, 8, 8),Bits.extract(r, 8, 8),Bits.test(a, 4));
        
        int flags = Bits.set(unpackFlags(b), Flag.Z.index(), false);
        
        return (unpackValue(b) << 16) | (unpackValue(a) << 8) | flags;
    }
    
    
    /**
     * 
     * @return la différence des valeurs 
     * de 8 bits données et du bit d'emprunt 
     * initial b0, et les fanions Z1HC
     */
    public static int sub(int l, int r, boolean b0) {
        checkBits8(l);
        checkBits8(r);
        int b = b0 ? 1 : 0;
        
        int v = Bits.clip(8, l-r-b);
        return packValueZNHC(v, v==0, true, Bits.clip(4, l) - Bits.clip(4, r) - b < 0, l - r - b < 0);
    }
    
    /**
     * 
     * @return identique à la méthode précédente, 
     * mais avec 'false' comme dernier argument
     */
    public static int sub(int l, int r) {
        return sub(l, r, false);
    }
    
    
    /**
     * 
     * @return l'ajustement de la valeur 8 bits 
     * donnée en argument afin qu'elle soit au format DCB
     */
    public static int bcdAdjust(int v, boolean n, boolean h, boolean c) {
        checkBits8(v);
        boolean fixL = h || (!n && (Bits.clip(4, v) > 9));
        boolean fixH = c || (!n && (v > 0x99));
        int fix = 0x60 * (fixH ? 1 : 0) + 0x06 * (fixL ? 1 : 0);
        
        int va = n ? v - fix : v + fix;
        va = Bits.clip(8, va);
        return packValueZNHC(va, va == 0, n, false, fixH);
    }
    
    
    /**
     * 
     * @return le « et » bit à bit des deux 
     * valeurs 8 bits données et les fanions Z010
     */
    public static int and(int l, int r) {
        checkBits8(l);
        checkBits8(r);
        return packValueZNHC(l & r, (l & r) == 0, false, true, false);
    }
    
    
    /**
     * 
     * @return le « ou inclusif » bit à bit 
     * des deux valeurs 8 bits données et les fanions Z000
     */
    public static int or(int l, int r) {
        checkBits8(l);
        checkBits8(r);
        return packValueZNHC(l | r, (l | r) == 0, false, false, false);
    }
    
    
    /**
     * 
     * @return le « ou exclusif » bit à bit 
     * des deux valeurs 8 bits données et les fanions Z000
     */
    public static int xor(int l, int r) {
        checkBits8(l);
        checkBits8(r);
        return packValueZNHC(l^r, (l^r) == 0, false, false, false);     }
    
    
    /**
     * 
     * @return la valeur 8 bits donnée décalée à gauche 
     * d'un bit, et les fanions Z00C où le fanion C 
     * contient le bit éjecté par le décalage 
     * (c-à-d que C est vrai ssi le bit en question valait 1)
     */
    public static int shiftLeft(int v) { 
        checkBits8(v);
        int a = Bits.clip(8, v << 1);
        return packValueZNHC(a, a == 0,false, false, Bits.test(v, 7));
    }
    
    
    /**
     * 
     * @return la valeur 8 bits donnée décalée à droite 
     * d'un bit, de manière arithmétique, et les fanions 
     * Z00C où C contient le bit éjecté par le décalage
     */
    public static int shiftRightA(int v) {
        checkBits8(v);
        
        int a = Bits.signExtend8(v);
        a >>>= 1;
        a = Bits.clip(8, a);
        
        return packValueZNHC(a, a == 0, false, false, Bits.test(v, 0));
    }
    
    
    /**
     * 
     * @return la valeur 8 bits donnée décalée à droite 
     * d'un bit, de manière logique, et les fanions Z00C 
     * où C contient le bit éjecté par le décalage
     */
    public static int shiftRightL(int v) {
        checkBits8(v);
        return packValueZNHC(v >>> 1, (v >>> 1) == 0, false, false, Bits.test(v, 0));
    }
    
    
    /**
     * 
     * @return la rotation de la valeur 8 bits donnée, d'une 
     * distance de un bit dans la direction donnée, et les 
     * fanions Z00C où C contient le bit qui est passé 
     * d'une extrémité à l'autre lors de la rotation
     */
    public static int rotate(RotDir d, int v) {
        checkBits8(v);
        if (d == RotDir.LEFT) {
            int a = Bits.rotate(8, v, 1);
            return packValueZNHC(a, a == 0, false, false, Bits.test(a, 0));
        }
        else {
            int a = Bits.rotate(8, v, -1);
            return packValueZNHC(a, a == 0, false, false, Bits.test(v, 0));
        }
    }

    
    /**
     * 
     * @return la rotation à travers la retenue, dans la direction donnée, 
     * de la combinaison de la valeur 8 bits et du fanion de retenue donnés,
     * ainsi que les fanions Z00C ; cette opération consiste à construire 
     * une valeur 9 bits à partir de la retenue et de la valeur 8 bits, 
     * la faire tourner dans la direction donnée, puis retourner les 
     * 8 bits de poids faible comme résultat, et le bit de poids le 
     * plus fort comme nouvelle retenue (fanion C)
     */
    public static int rotate(RotDir d, int v, boolean c) {
        checkBits8(v);
        if (d == RotDir.LEFT) {
            int a = Bits.rotate(9, (v | ((c ? 1 : 0) << 8)), 1);
            return packValueZNHC(Bits.clip(8, a), Bits.clip(8, a) == 0, false, false, Bits.test(a, 8));
        }
        else {
            int a = Bits.rotate(9, (v | ((c ? 1 : 0) << 8)), -1);
            return packValueZNHC(Bits.clip(8, a), Bits.clip(8, a) == 0, false, false, Bits.test(a, 8));
        }
    }
    
    
    /**
     * 
     * @return la valeur obtenue en échangeant les 4 bits de poids 
     * faible et de poids fort de la valeur 8 bits donnée, et les fanions Z000
     */
    public static int swap(int v) {
        checkBits8(v);
        int a = Bits.rotate(8, v, 4);
        return packValueZNHC(a, a == 0, false, false, false);
    }
    
    
    /**
     * @return la valeur 0 et les fanions Z010 
     * où Z est vrai ssi le bit d'index donné de 
     * la valeur 8 bits donnée vaut 0 
     * @throws IndexOutOfBoundsException si bitIndex
     * n'est pas compris entre 0 et 7 inclus
     */
    public static int testBit(int v, int bitIndex) {
        checkBits8(v);
        if (bitIndex < 0 || bitIndex > 7) {
            throw new IndexOutOfBoundsException();
        }
        return packValueZNHC(0, !Bits.test(v, bitIndex), false, true, false);
    }
    
    
    /**
     * 
     * @return un "paquet" contenant une valeur 8/16 bits
     * et les fanions
     */
    private static int packValueZNHC(int v, boolean z, boolean n, boolean h, boolean c) {

        return maskZNHC(z, n, h, c) | (v << 8);
    }
}

