package de.pinyto.ctSESAM;

import android.text.Editable;

/**
 * Clears arrays and Editables.
 */
public class Clearer {

    public static void zero(byte[] a) {
        if (a != null) {
            for (int i = 0; i < a.length; i++) {
                a[i] = 0x00;
            }
        }
    }

    public static void zero(Editable e) {
        CharSequence zero = "0";
        for (int i = 0; i < e.length(); i++) {
            e.replace(i, i+1, zero);
        }
    }

}
