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
        Crypter crypter = new Crypter(Crypter.createKey(password, new byte[]{}));
        byte[] ciphertext = crypter.encrypt(message);
        assertEquals(
                "62fqFDS+SftUttn7LVPG+Jpd1TeW6+Z77iwQlgZItkp0Z8BBlhsiR/FB9WD7nL2gXK8tjAr/rgLe\n" +
                "HYrz5M4BztfWwZuYL7QZVD5PaEkLMBctNpUzvagAlopCCqmFQaFnd2S1nExtG6d5Isuj6wsQNBCh\n" +
                "ZlF4Yr2cx/tOQ50D9LenWa7k77UKIUKwMiactwCHt+BV/gQTd2bmlzBegioD1WKi0hqX8rAUnEm5\n" +
                "9cRVbUUn8Bkm4SiOz/AQyyfAlXx24GoTZZj42olJX8LqnXqe+g==\n",
                Base64.encodeToString(ciphertext, Base64.DEFAULT));
    }

    public void testDecrypt() {
        String cyphertext =
                "62fqFDS+SftUttn7LVPG+Jpd1TeW6+Z77iwQlgZItkp0Z8BBlhsiR/FB9WD7nL2gXK8tjAr/rgLe\n" +
                "HYrz5M4BztfWwZuYL7QZVD5PaEkLMBctNpUzvagAlopCCqmFQaFnd2S1nExtG6d5Isuj6wsQNBCh\n" +
                "ZlF4Yr2cx/tOQ50D9LenWa7k77UKIUKwMiactwCHt+BV/gQTd2bmlzBegioD1WKi0hqX8rAUnEm5\n" +
                "9cRVbUUn8Bkm4SiOz/AQyyfAlXx24GoTZZj42olJX8LqnXqe+g==\n";
        byte[] password;
        try {
            password = "secret".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d("Key generation error", "UTF-8 is not supported.");
            password = "secret".getBytes();
        }
        Crypter crypter = new Crypter(Crypter.createKey(password, new byte[]{}));
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
