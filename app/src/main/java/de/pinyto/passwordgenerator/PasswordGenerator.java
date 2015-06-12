package de.pinyto.passwordgenerator;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles the hashing and the creation of passwords. Please initialize first.
 * Do not forget to hash at least once because otherwise the password might look not very
 * random. It is safe to hash often because an attacker has to hash as often as you did for
 * every try of a brute-force attack. getPassword creates a password string out of the hash
 * digest.
 */
public class PasswordGenerator {

    private byte[] hashValue;
    private byte[] salt;
    private int iterations;

    public PasswordGenerator(String domain, String masterPassword) {
        try {
            this.hashValue = (domain + masterPassword).getBytes("UTF-8");
            this.salt = "pepper".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d("pwd generation error",
                    "UTF-8 is not supported. Using default encoding instead.");
            this.hashValue = (domain + masterPassword).getBytes();
            this.salt = "pepper".getBytes();
        }
        this.iterations = 0;
    }

    public void hash(int iterations) throws NotHashedException {
        this.iterations = this.iterations + iterations;
        if (this.iterations > 0) {
            if (iterations >= 0) {
                hashValue = PBKDF2_HMAC.sha512(hashValue, salt, iterations);
            } else {
                throw new NotHashedException("Negative iterations.");
            }
        } else {
            throw new NotHashedException(Integer.toString(this.iterations) +
                    " iterations means the password is not hashed at all.");
        }
    }

    public String getPassword(boolean specialCharacters, boolean letters,
                            boolean numbers, int length) throws NotHashedException {
        if (this.iterations <= 0) {
            throw new NotHashedException(Integer.toString(this.iterations) +
                    " iterations means the password is not hashed at all.");
        }
        byte[] positiveHashValue = new byte[hashValue.length + 1];
        positiveHashValue[0] = 0;
        System.arraycopy(hashValue, 0, positiveHashValue, 1, hashValue.length);
        BigInteger hashNumber = new BigInteger(positiveHashValue);
        String password = "";
        if (specialCharacters || letters || numbers) {
            List<String> characterSet = new ArrayList<>();
            if (letters) {
                characterSet.add("a");
                characterSet.add("b");
                characterSet.add("c");
                characterSet.add("d");
                characterSet.add("e");
                characterSet.add("f");
                characterSet.add("g");
                characterSet.add("h");
                characterSet.add("i");
                characterSet.add("j");
                characterSet.add("k");
                characterSet.add("l");
                characterSet.add("m");
                characterSet.add("n");
                characterSet.add("o");
                characterSet.add("p");
                characterSet.add("q");
                characterSet.add("r");
                characterSet.add("s");
                characterSet.add("t");
                characterSet.add("u");
                characterSet.add("v");
                characterSet.add("w");
                characterSet.add("x");
                characterSet.add("y");
                characterSet.add("z");
                characterSet.add("A");
                characterSet.add("B");
                characterSet.add("C");
                characterSet.add("D");
                characterSet.add("E");
                characterSet.add("F");
                characterSet.add("G");
                characterSet.add("H");
                characterSet.add("J");
                characterSet.add("K");
                characterSet.add("L");
                characterSet.add("M");
                characterSet.add("N");
                characterSet.add("P");
                characterSet.add("Q");
                characterSet.add("R");
                characterSet.add("T");
                characterSet.add("U");
                characterSet.add("V");
                characterSet.add("W");
                characterSet.add("X");
                characterSet.add("Y");
                characterSet.add("Z");
            }
            if (numbers) {
                characterSet.add("0");
                characterSet.add("1");
                characterSet.add("2");
                characterSet.add("3");
                characterSet.add("4");
                characterSet.add("5");
                characterSet.add("6");
                characterSet.add("7");
                characterSet.add("8");
                characterSet.add("9");
            }
            if (specialCharacters) {
                characterSet.add("#");
                characterSet.add("!");
                characterSet.add("\"");
                characterSet.add("§");
                characterSet.add("$");
                characterSet.add("%");
                characterSet.add("&");
                characterSet.add("/");
                characterSet.add("(");
                characterSet.add(")");
                characterSet.add("[");
                characterSet.add("]");
                characterSet.add("{");
                characterSet.add("}");
                characterSet.add("=");
                characterSet.add("-");
                characterSet.add("_");
                characterSet.add("+");
                characterSet.add("*");
                characterSet.add("<");
                characterSet.add(">");
                characterSet.add(";");
                characterSet.add(":");
                characterSet.add(".");
            }
            BigInteger setSize = BigInteger.valueOf(characterSet.size());
            while (hashNumber.compareTo(setSize) >= 0 && password.length() < length) {
                BigInteger[] divAndMod = hashNumber.divideAndRemainder(setSize);
                hashNumber = divAndMod[0];
                int mod = divAndMod[1].intValue();
                password += characterSet.get(mod);
            }
            if (hashNumber.compareTo(setSize) < 0 && password.length() < length) {
                password += characterSet.get(hashNumber.intValue());
            }
        }
        return password;
    }

}
