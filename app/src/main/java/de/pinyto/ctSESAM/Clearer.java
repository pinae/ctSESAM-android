package de.pinyto.ctSESAM;

/**
 * Clears arrays.
 */
public class Clearer {

    public static void zero(byte[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = 0x00;
        }
    }

}
