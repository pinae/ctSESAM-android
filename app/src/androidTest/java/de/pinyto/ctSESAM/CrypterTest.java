package de.pinyto.ctSESAM;

import android.util.Base64;
import android.util.Log;

import junit.framework.TestCase;

import java.io.UnsupportedEncodingException;

/**
 * Testing AES encryption
 */
public class CrypterTest extends TestCase {

    public void testEncrypt() {
        String messageString = "Important information with quite some length. " +
                "This message is as long as this because otherwise only one cipher block would " +
                "be encrypted. This long message insures that more than one block is needed.";
        byte[] password;
        byte[] message;
        try {
            password = "secret".getBytes("UTF-8");
            message = messageString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d("Key generation error", "UTF-8 is not supported.");
            password = "secret".getBytes();
            message = messageString.getBytes();
        }
        Crypter crypter = new Crypter(password);
        byte[] ciphertext = crypter.encrypt(message);
        assertEquals(
                "FRQFCWa38eSIrPnhELojAPrOb8oKzs2yoAbNqVONBEuac3OhUKY12mP+TNyZs1MRUbY9hnqvIG18\n" +
                "7MqTAVTzI0fCJhmR4stc/k4YpS+HptmzcTgEfXeli56davPUkmJ59yz2vvF3t/pCUOk0qWNQ2vv9\n" +
                "dU2sJhvOdQ7RVKzbw2DJAFtEM2BxJq8Oqa4mB4sBC/GpIP3xtNxANJPyN8xTSL2F4Ktt5hIcX3AV\n" +
                "UrnGYSjGeDHGua8iKNFohYtaPj3vvzaSVpGyzAfmlVEdN5/8zQ==\n",
                Base64.encodeToString(ciphertext, Base64.DEFAULT));
    }

    public void testDecrypt() {
        String cyphertext =
                "FRQFCWa38eSIrPnhELojAPrOb8oKzs2yoAbNqVONBEuac3OhUKY12mP+TNyZs1MRUbY9hnqvIG18\n" +
                "7MqTAVTzI0fCJhmR4stc/k4YpS+HptmzcTgEfXeli56davPUkmJ59yz2vvF3t/pCUOk0qWNQ2vv9\n" +
                "dU2sJhvOdQ7RVKzbw2DJAFtEM2BxJq8Oqa4mB4sBC/GpIP3xtNxANJPyN8xTSL2F4Ktt5hIcX3AV\n" +
                "UrnGYSjGeDHGua8iKNFohYtaPj3vvzaSVpGyzAfmlVEdN5/8zQ==\n";
        byte[] password;
        try {
            password = "secret".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d("Key generation error", "UTF-8 is not supported.");
            password = "secret".getBytes();
        }
        Crypter crypter = new Crypter(password);
        String decrypted = "";
        try {
            decrypted = new String(crypter.decrypt(
                    Base64.decode(cyphertext, Base64.DEFAULT)), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d("Decryption error", "UTF-8 is not supported.");
        }
        assertEquals(
                "Important information with quite some length. " +
                "This message is as long as this because otherwise only one cipher block would " +
                "be encrypted. This long message insures that more than one block is needed.",
                decrypted);
    }

}
