package de.pinyto.ctSESAM;

import junit.framework.TestCase;

/**
 * Tests the Analyzer
 */
public class PasswordAnalyzerTest extends TestCase {

    public void testContainsLowerCaseLetters() {
        assertTrue(PasswordAnalyzer.containsLowerCaseLetters("1354$/o8435768"));
        assertFalse(PasswordAnalyzer.containsLowerCaseLetters("1354$/T8435768"));
    }

    public void testContainsUpperCaseLetters() {
        assertTrue(PasswordAnalyzer.containsUpperCaseLetters("1354$/T8435768"));
        assertFalse(PasswordAnalyzer.containsUpperCaseLetters("1354$/w8435768"));
    }

    public void testContainsDigits() {
        assertTrue(PasswordAnalyzer.containsDigits("1354$/O8435768"));
        assertFalse(PasswordAnalyzer.containsDigits("a,iesnoitaern/$"));
    }

    public void testContainsExtra() {
        assertTrue(PasswordAnalyzer.containsExtra("1354$/O8aeo768"));
        assertFalse(PasswordAnalyzer.containsExtra("a56794iesnoita"));
    }

    public void testContains() {
        assertTrue(PasswordAnalyzer.contains("a1$B", true, true, true));
        assertFalse(PasswordAnalyzer.contains("a1$", true, true, true));
        assertTrue(PasswordAnalyzer.contains("a1$", false, true, true));
        assertTrue(PasswordAnalyzer.contains("", false, false, false));
        assertFalse(PasswordAnalyzer.contains(null, true, true, true));
    }

}
