package de.pinyto.passwordgenerator;

import junit.framework.TestCase;

/**
 * Tests for the PasswordGenerator class.
 */
public class PasswordGeneratorTest extends TestCase {

    public void testHashOnceAndGetPassword () {
        byte[] domain = UTF8.encode("test.de");
        byte[] password = UTF8.encode("secret");
        PasswordGenerator pg = new PasswordGenerator(domain, password);
        try {
            pg.hash(2);
            PasswordSetting setting = new PasswordSetting("test.de");
            setting.setLength(10);
            assertEquals("U§G8+6}m[+", pg.getPassword(setting));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashTwiceAndGetPassword () {
        byte[] domain = UTF8.encode("test.de");
        byte[] password = UTF8.encode("secret");
        PasswordGenerator pg = new PasswordGenerator(domain, password);
        try {
            pg.hash(2);
            pg.hash(5);
            PasswordSetting setting = new PasswordSetting("test.de");
            setting.setLength(10);
            assertEquals(":xv[{xqCa%", pg.getPassword(setting));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordEmptyInputs () {
        PasswordGenerator pg = new PasswordGenerator(new byte[] {}, new byte[] {});
        try {
            pg.hash(2);
            PasswordSetting setting = new PasswordSetting("test.de");
            setting.setLength(10);
            assertEquals("/ZsQWL>MJ$", pg.getPassword(setting));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordLengthZero () {
        byte[] domain = UTF8.encode("test.de");
        byte[] password = UTF8.encode("secret");
        PasswordGenerator pg = new PasswordGenerator(domain, password);
        try {
            pg.hash(2);
            PasswordSetting setting = new PasswordSetting("test.de");
            setting.setLength(0);
            assertEquals("", pg.getPassword(setting));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordLengthMax () {
        PasswordGenerator pg = new PasswordGenerator(new byte[] {}, new byte[] {});
        try {
            pg.hash(2);
            PasswordSetting setting = new PasswordSetting("test.de");
            setting.setLength(1000);
            assertEquals(80, pg.getPassword(setting).length());
            assertEquals("/ZsQWL>MJ$f3h§;fBMQ_5u:;1DP4*EFZ[VkLUY2phD%\"i\"oJ\"GiD-4" +
                            "N./f%\"d##+JXPjb-:.bU*BZse/",
                    pg.getPassword(setting));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordEmptyInputsZeroHashes () {
        PasswordGenerator pg = new PasswordGenerator(new byte[] {}, new byte[] {});
        boolean thrown = false;
        try {
            pg.hash(0);
        } catch (NotHashedException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    public void testNoHashRaisesError () {
        byte[] domain = UTF8.encode("test.de");
        byte[] password = UTF8.encode("secret");
        PasswordGenerator pg = new PasswordGenerator(domain, password);
        boolean thrown = false;
        try {
            PasswordSetting setting = new PasswordSetting("test.de");
            setting.setLength(1000);
            pg.getPassword(setting);
        } catch (NotHashedException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    public void testHashWithNegativeIterationCountRaisesError () {
        byte[] domain = UTF8.encode("test.de");
        byte[] password = UTF8.encode("secret");
        PasswordGenerator pg = new PasswordGenerator(domain, password);
        boolean thrown = false;
        try {
            pg.hash(-3);
        } catch (NotHashedException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    public void testHashTwiceWithPositiveAndNegativeIterationCountRaisesError () {
        byte[] domain = UTF8.encode("test.de");
        byte[] password = UTF8.encode("secret");
        PasswordGenerator pg = new PasswordGenerator(domain, password);
        boolean thrown = false;
        try {
            pg.hash(2);
            pg.hash(-1);
            PasswordSetting setting = new PasswordSetting("test.de");
            setting.setLength(1000);
            pg.getPassword(setting);
        } catch (NotHashedException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

}
