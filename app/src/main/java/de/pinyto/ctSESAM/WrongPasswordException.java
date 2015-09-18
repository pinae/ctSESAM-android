package de.pinyto.ctSESAM;

/**
 * This is thrown if the password is wrong.
 */
public class WrongPasswordException extends Exception {

    public WrongPasswordException(String msg) {
        super(msg);
    }
}
