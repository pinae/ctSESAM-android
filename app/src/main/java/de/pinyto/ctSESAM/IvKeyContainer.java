package de.pinyto.ctSESAM;

/**
 * Created by jme on 16.09.15.
 */
public class IvKeyContainer {
    private byte[] ivKey;

    IvKeyContainer(byte[] ivKey) {
        this.ivKey = ivKey;
    }

    public byte[] getIvKey() {
        return this.ivKey;
    }
}
