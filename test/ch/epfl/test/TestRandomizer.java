// Gameboj stage 1

package ch.epfl.test;

import java.util.Random;

public interface TestRandomizer {
    // Fix random seed to guarantee reproducibility.
    public final static long SEED = 2018;

    public final static int RANDOM_ITERATIONS = 100;

    public static Random newRandom() {
        return new Random(SEED);
    }
}
