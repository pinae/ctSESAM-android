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
                "5XJcX8Ju/KY9P17gSZWbvsMCxazUyWVS3SmGpwOqOJkBQU1Cyu0n9RkxbNJ1CJSoF8BPTH5d5xy6\n" +
                "IGIUb3kN6EdWBppTk/PAHbgQX/tBiW2Uwi7DMEg6GGebqr+Dj94Ur9JnpiCRUTZfDgUyTpy5GQQ3\n" +
                "TQUlDTqpvs+5n6cpdombXBnpcfl2ddQhawJLOFpGtED0h4LZtW0nEc7mvvSSRosXHohRNScrUCg0\n" +
                "Jm5/J29HVXQmFEyNmHYt2Wckk1gCP+Mt34klaNnuk3yFDLtQmg==\n",
                Base64.encodeToString(ciphertext, Base64.DEFAULT));
    }

    public void testDecrypt() {
        String cyphertext =
                "5XJcX8Ju/KY9P17gSZWbvsMCxazUyWVS3SmGpwOqOJkBQU1Cyu0n9RkxbNJ1CJSoF8BPTH5d5xy6\n" +
                "IGIUb3kN6EdWBppTk/PAHbgQX/tBiW2Uwi7DMEg6GGebqr+Dj94Ur9JnpiCRUTZfDgUyTpy5GQQ3\n" +
                "TQUlDTqpvs+5n6cpdombXBnpcfl2ddQhawJLOFpGtED0h4LZtW0nEc7mvvSSRosXHohRNScrUCg0\n" +
                "Jm5/J29HVXQmFEyNmHYt2Wckk1gCP+Mt34klaNnuk3yFDLtQmg==\n";
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
