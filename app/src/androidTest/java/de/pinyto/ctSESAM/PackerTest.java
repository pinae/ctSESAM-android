package de.pinyto.ctSESAM;

import android.util.Base64;

import junit.framework.TestCase;

/**
 * Test the Deflater-based Packer
 */
public class PackerTest extends TestCase {

    public void testCompress() {
        Packer p = new Packer();
        byte[] packed_data = p.compress("Some packable information");
        assertEquals(
                "AAAAGXjaC87PTVUoSEzOTkzKSVXIzEvLL8pNLMnMzwMAedUJrg==\n",
                Base64.encodeToString(packed_data, Base64.DEFAULT));
        assertEquals(
                "AAAAAXjaSwQAAGIAYg==\n",
                Base64.encodeToString(p.compress("a"), Base64.DEFAULT));
    }

    public void testDecompress() {
        Packer p = new Packer();
        byte[] compressed = Base64.decode(
                "AAAAGXjaC87PTVUoSEzOTkzKSVXIzEvLL8pNLMnMzwMAedUJrg==",
                Base64.DEFAULT);
        assertEquals("Some packable information", p.decompress(compressed));
    }

}
