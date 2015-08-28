package de.pinyto.ctSESAM;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by jme on 02.06.15.
 */
public class Crypter {
    final static byte[] iv = new byte[] { (byte)0xb5, 0x4f, (byte)0xcf, (byte)0xb0,
            (byte)0x88, 0x09, 0x55, (byte)0xe5, (byte)0xbf, 0x79, (byte)0xaf, 0x37,
            0x71, 0x1c, 0x28, (byte)0xb6 };

    private byte[] key;

    public Crypter(byte[] password) {
        byte[] salt;
        try {
            salt = "pepper".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d("Key generation error", "UTF-8 is not supported.");
            e.printStackTrace();
            salt = new byte[] {};
        }
        key = Arrays.copyOfRange(PBKDF2.hmac("SHA512", password, salt, 4096), 0, 32);
    }

    public byte[] encrypt(byte[] data) {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(iv));
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            Log.d("Encryption error", "AES/CBC is not implemented.");
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            Log.d("Encryption error", "PKCS7Padding is not implemented.");
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

    public byte[] decrypt(byte[] encryptedData) {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(iv));
            return cipher.doFinal(encryptedData);
        } catch (NoSuchAlgorithmException e) {
            Log.d("Encryption error", "AES/CBC is not implemented.");
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            Log.d("Encryption error", "PKCS7Padding is not implemented.");
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
