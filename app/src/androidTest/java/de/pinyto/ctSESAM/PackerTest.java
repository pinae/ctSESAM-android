package de.pinyto.ctSESAM;

import android.util.Base64;

import junit.framework.TestCase;

/**
 * Test the Deflater-based Packer
 */
public class PackerTest extends TestCase {

    public void testCompress() {
        byte[] packed_data = Packer.compress("Some packable information");
        assertEquals(
                "AAAAGXjaC87PTVUoSEzOTkzKSVXIzEvLL8pNLMnMzwMAedUJrg==\n",
                Base64.encodeToString(packed_data, Base64.DEFAULT));
        assertEquals(
                "AAAAAXjaSwQAAGIAYg==\n",
                Base64.encodeToString(Packer.compress("a"), Base64.DEFAULT));
    }

    public void testDecompress() {
        byte[] compressed = Base64.decode(
                "AAAAGXjaC87PTVUoSEzOTkzKSVXIzEvLL8pNLMnMzwMAedUJrg==",
                Base64.DEFAULT);
        assertEquals("Some packable information", Packer.decompress(compressed));
    }

}
