package de.pinyto.ctSESAM;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
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

    public PasswordGenerator(byte[] domain, byte[] username, byte[] kgk, byte[] salt) {
        hashValue = new byte[domain.length + username.length + kgk.length];
        int i = 0;
        while (i < domain.length) {
            hashValue[i] = domain[i];
            i++;
        }
        while (i < domain.length + username.length) {
            hashValue[i] = username[i - domain.length];
            i++;
        }
        while (i < domain.length + username.length + kgk.length) {
            hashValue[i] = kgk[i - domain.length - username.length];
            i++;
        }
        this.salt = salt;
        this.iterations = 0;
    }

    public void hash(int iterations) throws NotHashedException {
        this.iterations = this.iterations + iterations;
        if (this.iterations > 0) {
            if (iterations >= 0) {
                byte[] newHashValue = PBKDF2.hmac("SHA512", hashValue, salt, iterations);
                for (int i = 0; i < hashValue.length; i++) {
                    hashValue[i] = 0x00;
                }
                hashValue = newHashValue;
            } else {
                throw new NotHashedException("Negative iterations.");
            }
        } else {
            throw new NotHashedException(Integer.toString(this.iterations) +
                    " iterations means the password is not hashed at all.");
        }
    }

    public String getPassword(PasswordSetting setting) throws NotHashedException {
        if (this.iterations <= 0) {
            throw new NotHashedException(Integer.toString(this.iterations) +
                    " iterations means the password is not hashed at all.");
        }
        byte[] positiveHashValue = new byte[hashValue.length + 1];
        positiveHashValue[0] = 0;
        System.arraycopy(hashValue, 0, positiveHashValue, 1, hashValue.length);
        BigInteger hashNumber = new BigInteger(positiveHashValue);
        for (int i = 0; i < positiveHashValue.length; i++) {
            positiveHashValue[i] = 0x00;
        }
        String password = "";
        List<String> characterSet = setting.getCharacterSet();
        if (characterSet.size() > 0) {
            BigInteger setSize = BigInteger.valueOf(characterSet.size());
            while (hashNumber.compareTo(setSize) >= 0 && password.length() < setting.getLength()) {
                BigInteger[] divAndMod = hashNumber.divideAndRemainder(setSize);
                hashNumber = divAndMod[0];
                int mod = divAndMod[1].intValue();
                password += characterSet.get(mod);
            }
            if (hashNumber.compareTo(setSize) < 0 && password.length() < setting.getLength()) {
                password += characterSet.get(hashNumber.intValue());
            }
        }
        return password;
    }

    protected void finalize() throws Throwable {
        for (int i = 0; i < hashValue.length; i++) {
            hashValue[i] = 0x00;
        }
        super.finalize();
    }
}
