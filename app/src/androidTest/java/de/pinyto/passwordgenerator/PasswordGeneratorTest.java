package de.pinyto.passwordgenerator;

import junit.framework.TestCase;

/**
 * Tests for the PasswordGenerator class.
 */
public class PasswordGeneratorTest extends TestCase {

    public void testHashOnceAndGetPassword () {
        PasswordGenerator pg = new PasswordGenerator("test.de", "secret");
        try {
            pg.hash(2);
            assertEquals("U§G8+6}m[+", pg.getPassword(true, true, true, 10));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashTwiceAndGetPassword () {
        PasswordGenerator pg = new PasswordGenerator("test.de", "secret");
        try {
            pg.hash(2);
            pg.hash(5);
            assertEquals(":xv[{xqCa%", pg.getPassword(true, true, true, 10));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordEmptyInputs () {
        PasswordGenerator pg = new PasswordGenerator("", "");
        try {
            pg.hash(2);
            assertEquals("/ZsQWL>MJ$", pg.getPassword(true, true, true, 10));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordLengthZero () {
        PasswordGenerator pg = new PasswordGenerator("test.de", "secret");
        try {
            pg.hash(2);
            assertEquals("", pg.getPassword(true, true, true, 0));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordLengthMax () {
        PasswordGenerator pg = new PasswordGenerator("", "");
        try {
            pg.hash(2);
            assertEquals(80, pg.getPassword(true, true, true, 1000).length());
            assertEquals("/ZsQWL>MJ$f3h§;fBMQ_5u:;1DP4*EFZ[VkLUY2phD%\"i\"oJ\"GiD-4" +
                            "N./f%\"d##+JXPjb-:.bU*BZse/",
                    pg.getPassword(true, true, true, 1000));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordEmptyInputsZeroHashes () {
        PasswordGenerator pg = new PasswordGenerator("", "");
        boolean thrown = false;
        try {
            pg.hash(0);
        } catch (NotHashedException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    public void testNoHashRaisesError () {
        PasswordGenerator pg = new PasswordGenerator("test.de", "secret");
        boolean thrown = false;
        try {
            pg.getPassword(true, true, true, 1000);
        } catch (NotHashedException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    public void testHashWithNegativeIterationCountRaisesError () {
        PasswordGenerator pg = new PasswordGenerator("test.de", "secret");
        boolean thrown = false;
        try {
            pg.hash(-3);
        } catch (NotHashedException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    public void testHashTwiceWithPositiveAndNegativeIterationCountRaisesError () {
        PasswordGenerator pg = new PasswordGenerator("test.de", "secret");
        boolean thrown = false;
        try {
            pg.hash(2);
            pg.hash(-1);
            pg.getPassword(true, true, true, 1000);
        } catch (NotHashedException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

}
