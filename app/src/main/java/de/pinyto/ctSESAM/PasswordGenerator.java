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

    public PasswordGenerator(byte[] domain,
                             byte[] username,
                             byte[] kgk,
                             byte[] salt,
                             int iterations) throws NotHashedException {
        byte[] startValue = new byte[domain.length + username.length + kgk.length];
        int i = 0;
        while (i < domain.length) {
            startValue[i] = domain[i];
            i++;
        }
        while (i < domain.length + username.length) {
            startValue[i] = username[i - domain.length];
            i++;
        }
        while (i < domain.length + username.length + kgk.length) {
            startValue[i] = kgk[i - domain.length - username.length];
            i++;
        }
        if (iterations <= 0) {
            throw new NotHashedException(Integer.toString(iterations) +
                    " iterations means the password is not hashed at all.");
        }
        this.hashValue = PBKDF2.hmac("SHA512", startValue, salt, iterations);
        Clearer.zero(startValue);
    }

    public String getPassword(PasswordSetting setting) {
        byte[] positiveHashValue = new byte[hashValue.length + 1];
        positiveHashValue[0] = 0;
        System.arraycopy(hashValue, 0, positiveHashValue, 1, hashValue.length);
        BigInteger hashNumber = new BigInteger(positiveHashValue);
        Clearer.zero(positiveHashValue);
        String password = "";
        List<String> characterSet = setting.getCharacterSet();
        List<String> digitsSet = setting.getDigitsCharacterSet();
        List<String> lowerSet = setting.getLowerCaseLettersCharacterSet();
        List<String> upperSet = setting.getUpperCaseLettersCharacterSet();
        List<String> extraSet = setting.getExtraCharacterSet();
        if (characterSet.size() > 0) {
            String template = setting.getTemplate();
            for (int i = 0; i < template.length(); i++) {
                if (hashNumber.compareTo(BigInteger.ZERO) > 0) {
                    List<String> set = characterSet;
                    if (template.charAt(i) == 'a') {
                        set = lowerSet;
                    } else if (template.charAt(i) == 'A') {
                        set = upperSet;
                    } else if (template.charAt(i) == 'n') {
                        set = digitsSet;
                    } else if (template.charAt(i) == 'o') {
                        set = extraSet;
                    } else if (template.charAt(i) == 'x') {
                        set = characterSet;
                    }
                    BigInteger setSize = BigInteger.valueOf(set.size());
                    BigInteger[] divAndMod = hashNumber.divideAndRemainder(setSize);
                    hashNumber = divAndMod[0];
                    int mod = divAndMod[1].intValue();
                    password += set.get(mod);
                }
            }
        }
        return password;
    }

    protected void finalize() throws Throwable {
        Clearer.zero(this.hashValue);
        super.finalize();
    }
}
