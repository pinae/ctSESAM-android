package de.pinyto.ctSESAM;

/**
 * Check passwords for their properties.
 */
public class PasswordAnalyzer {

    public static boolean contains(String password,
                                   boolean containsLetters,
                                   boolean containsDigits,
                                   boolean containsExtra) {
        boolean contains = true;
        if (containsLetters && !PasswordAnalyzer.containsUpperCaseLetters(password)) {
            contains = false;
        }
        if (containsLetters && !PasswordAnalyzer.containsLowerCaseLetters(password)) {
            contains = false;
        }
        if (containsDigits && !PasswordAnalyzer.containsDigits(password)) {
            contains = false;
        }
        if (containsExtra && !PasswordAnalyzer.containsExtra(password)) {
            contains = false;
        }
        return contains;
    }

    public static boolean containsLowerCaseLetters(String password) {
        if (password == null) {
            return false;
        }
        String defaultCharacterSetLowerCase = "abcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i < password.length(); i++) {
            for (int j = 0; j < defaultCharacterSetLowerCase.length(); j++) {
                if (password.charAt(i) == defaultCharacterSetLowerCase.charAt(j)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean containsUpperCaseLetters(String password) {
        if (password == null) {
            return false;
        }
        String defaultCharacterSetUpperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        for (int i = 0; i < password.length(); i++) {
            for (int j = 0; j < defaultCharacterSetUpperCase.length(); j++) {
                if (password.charAt(i) == defaultCharacterSetUpperCase.charAt(j)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean containsDigits(String password) {
        if (password == null) {
            return false;
        }
        String defaultCharacterSetDigits = "0123456789";
        for (int i = 0; i < password.length(); i++) {
            for (int j = 0; j < defaultCharacterSetDigits.length(); j++) {
                if (password.charAt(i) == defaultCharacterSetDigits.charAt(j)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean containsExtra(String password) {
        if (password == null) {
            return false;
        }
        String defaultCharacterSetExtra = "#!\"~|@^°$%&/()[]{}=-_+*<>;:.";
        for (int i = 0; i < password.length(); i++) {
            for (int j = 0; j < defaultCharacterSetExtra.length(); j++) {
                if (password.charAt(i) == defaultCharacterSetExtra.charAt(j)) {
                    return true;
                }
            }
        }
        return false;
    }

}
