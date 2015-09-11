package de.pinyto.ctSESAM;

import junit.framework.TestCase;

/**
 * Tests the clearer
 */
public class ClearerTest extends TestCase {

    public void testZero() {
        byte[] a = new byte[100];
        for (int i = 0; i < a.length; i++) {
            a[i] = 0x37;
        }
        Clearer.zero(a);
        for (int i = 0; i < a.length; i++) {
            assertEquals(0x00, a[i]);
        }
    }

}
