package de.pinyto.ctSESAM;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encrypt with AES. Optionally creates a key.
 */
public class Crypter {
    private byte[] key;
    private byte[] iv;

    public Crypter(byte[] keyIv) {
        switch (keyIv.length) {
            case 32:
                this.key = keyIv;
                this.iv = new byte[]{(byte) 0xb5, 0x4f, (byte) 0xcf, (byte) 0xb0,
                        (byte) 0x88, 0x09, 0x55, (byte) 0xe5, (byte) 0xbf, 0x79, (byte) 0xaf, 0x37,
                        0x71, 0x1c, 0x28, (byte) 0xb6};
                break;
            case 48:
                this.key = Arrays.copyOfRange(keyIv, 0, 32);
                this.iv = Arrays.copyOfRange(keyIv, 32, 48);
                for (int i = 0; i < keyIv.length; i++) {
                    keyIv[i] = 0x00;
                }
                break;
            default:
                this.key = Crypter.createKey(keyIv, "pepper".getBytes());
                this.iv = new byte[]{(byte) 0xb5, 0x4f, (byte) 0xcf, (byte) 0xb0,
                        (byte) 0x88, 0x09, 0x55, (byte) 0xe5, (byte) 0xbf, 0x79, (byte) 0xaf, 0x37,
                        0x71, 0x1c, 0x28, (byte) 0xb6};
                break;
        }
    }

    public static byte[] createKey(byte[] password, byte[] salt) {
        return PBKDF2.hmac("SHA256", password, salt, 1024);
    }

    public static byte[] createIvKey(byte[] password, byte[] salt) {
        return PBKDF2.hmac("SHA384", password, salt, 32768);
    }

    public static byte[] createSalt() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[32];
        sr.nextBytes(salt);
        return salt;
    }

    public static byte[] createIv() {
        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);
        return salt;
    }

    public byte[] encrypt(byte[] data) {
        return this.encrypt(data, "PKCS7Padding");
    }

    public byte[] encrypt(byte[] data, String padding) {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/" + padding);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(this.iv));
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            Log.d("Encryption error", "AES/CBC is not implemented.");
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            Log.d("Encryption error", padding + " is not implemented.");
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            Log.d("Encryption error", "Invalid IV.");
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            Log.d("Encryption error", "Invalid key.");
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            Log.d("Encryption error", "Illegal block size of the data.");
            e.printStackTrace();
        } catch (BadPaddingException e) {
            Log.d("Encryption error", "Bad padding of the data.");
            e.printStackTrace();
        }
        return new byte[] {};
    }

    public byte[] decrypt(byte[] data) {
        try {
            return this.decrypt(data, "PKCS7Padding");
        } catch (NoSuchPaddingException paddingError) {
            Log.d("Encryption error", "PKCS7Padding is not implemented.");
            paddingError.printStackTrace();
            return new byte[] {};
        }
    }

    public byte[] decrypt(byte[] encryptedData, String padding) throws NoSuchPaddingException {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/" + padding);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(iv));
            return cipher.doFinal(encryptedData);
        } catch (NoSuchAlgorithmException e) {
            Log.d("Encryption error", "AES/CBC is not implemented.");
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            Log.d("Encryption error", "Invalid IV.");
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            Log.d("Encryption error", "Invalid key.");
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            Log.d("Encryption error", "Illegal block size of the data.");
            e.printStackTrace();
        } catch (BadPaddingException e) {
            Log.d("Encryption error", "Bad padding of the data.");
            e.printStackTrace();
        }
        return new byte[] {};
    }
}
