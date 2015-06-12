package de.pinyto.passwordgenerator;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class creates PBKDF2_HMAC with sha256.
 */
public class PBKDF2_HMAC {
    public static byte[] sha512HMAC(byte[] key, byte[] password) {
        if (key.length == 0) {
            key = new byte[] { 0x00 };
        }
        try {
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            sha512_HMAC.init(new SecretKeySpec(key, "HmacSHA512"));
            return sha512_HMAC.doFinal(password);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return password;
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return password;
        }
    }

    private static byte[] F (byte[] password, byte[] salt, int iterations, int i)
    {
        byte[] Si = new byte[salt.length+4];
        System.arraycopy(salt, 0, Si, 0, salt.length);
        byte[] iByteArray = ByteBuffer.allocate(4).putInt(i).array();
        System.arraycopy(iByteArray, 0, Si, salt.length, iByteArray.length);
        byte[] U = sha512HMAC(password, Si);
        byte[] T = new byte[U.length];
        System.arraycopy(U, 0, T, 0, T.length);
        for (int c = 1; c < iterations; c++) {
            U = sha512HMAC(password, U);
            for (int k = 0; k < U.length; k++) {
                T[k] = (byte) (((int) T[k]) ^ ((int) U[k]));
            }
        }
        return T;
    }

    public static byte[] sha512 (byte[] hashString, byte[] salt, int iterations)
    {
        int dkLen = 64;
        int hLen = 64;
        int l = (int) Math.ceil(dkLen / hLen);
        int r = dkLen - (l - 1) * hLen;
        byte[] dk = new byte[dkLen];
        for (int i = 1; i <= l; i++) {
            byte[] T = F(hashString, salt, iterations, i);
            for (int k = 0; k < T.length; k++) {
                if (i-1+k < dk.length) {
                    dk[i-1+k] = T[k];
                }
            }
        }
        return dk;
    }
}
