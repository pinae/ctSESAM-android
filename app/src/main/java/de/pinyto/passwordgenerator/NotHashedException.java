package de.pinyto.passwordgenerator;

/**
 * Exception for 0 iteration count or no call to hash in PasswordGenerator.
 */
public class NotHashedException extends Exception {

    public NotHashedException(String msg) {
        super(msg);
    }

}
