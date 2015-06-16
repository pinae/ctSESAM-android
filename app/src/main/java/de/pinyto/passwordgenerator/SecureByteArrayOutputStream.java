package de.pinyto.passwordgenerator;

import java.io.ByteArrayOutputStream;

/**
 * This class lets you empty the buffer of an ByteArrayOutputStream.
 */
public class SecureByteArrayOutputStream extends ByteArrayOutputStream {
    public void emptyBuffer() {
        for (int i = 0; i < this.buf.length; i++) {
            this.buf[i] = 0x00;
        }
    }

    protected void finalize() throws Throwable {
        for (int i = 0; i < this.buf.length; i++) {
            this.buf[i] = 0x00;
        }
        super.finalize();
    }
}
