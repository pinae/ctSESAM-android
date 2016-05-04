package de.pinyto.ctSESAM;

import junit.framework.TestCase;

/**
 * Tests for the PasswordGenerator class.
 */
public class PasswordGeneratorTest extends TestCase {

    public void testHashOnceAndGetPassword () {
        byte[] domain = UTF8.encode("unit.test");
        byte[] username = UTF8.encode("hugo");
        byte[] kgk = UTF8.encode("secret");
        byte[] salt = UTF8.encode("pepper");
        try {
            PasswordGenerator pg = new PasswordGenerator(domain, username, kgk, salt, 2);
            PasswordSetting setting = new PasswordSetting("unit.test");
            setting.setTemplate("xxaxAnxoxx");
            assertEquals("a0b/Q3°[4_", pg.getPassword(setting));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordEmptyInputs () {
        try {
            PasswordGenerator pg = new PasswordGenerator(
                    new byte[] {},
                    new byte[] {},
                    new byte[] {},
                    new byte[] {},
                    2);
            PasswordSetting setting = new PasswordSetting("unit.test");
            setting.setTemplate("xxaxAnxoxx");
            assertEquals("5XmUG8z°_|", pg.getPassword(setting));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordLengthMax () {
        try {
            PasswordGenerator pg = new PasswordGenerator(
                    new byte[] {},
                    new byte[] {},
                    new byte[] {},
                    new byte[] {},
                    2);
            PasswordSetting setting = new PasswordSetting("unit.test");
            setting.setTemplate("Aoanxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                    "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                    "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                    "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                    "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                    "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                    "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                    "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                    "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                    "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                    "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
            assertEquals(81, pg.getPassword(setting).length());
            assertEquals(
                "X=l2&d$n_|Jx}!W_]Kn°=}/r0bBvsf%P(Cb{NIyY=a^zuCvex=fu^($T)7cVwcla{j°YrVyP!^!rZP@w1",
                pg.getPassword(setting));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordEmptyInputsZeroHashes () {
        boolean thrown = false;
        try {
            PasswordGenerator pg = new PasswordGenerator(
                    new byte[] {},
                    new byte[] {},
                    new byte[] {},
                    new byte[] {},
                    0);
            PasswordSetting setting = new PasswordSetting("unit.test");
            setting.setTemplate("axonA");
            pg.getPassword(setting);
        } catch (NotHashedException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    public void testNoHashRaisesError () {
        byte[] domain = UTF8.encode("unit.test");
        byte[] username = UTF8.encode("hugo");
        byte[] kgk = UTF8.encode("secret");
        byte[] salt = UTF8.encode("pepper");
        boolean thrown = false;
        try {
            PasswordGenerator pg = new PasswordGenerator(domain, username, kgk, salt, 0);
            PasswordSetting setting = new PasswordSetting("unit.test");
            setting.setTemplate("axonA");
            pg.getPassword(setting);
        } catch (NotHashedException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    public void testHashWithNegativeIterationCountRaisesError () {
        byte[] domain = UTF8.encode("unit.test");
        byte[] username = UTF8.encode("hugo");
        byte[] kgk = UTF8.encode("secret");
        byte[] salt = UTF8.encode("pepper");
        boolean thrown = false;
        try {
            PasswordGenerator pg = new PasswordGenerator(domain, username, kgk, salt, -3);
            PasswordSetting setting = new PasswordSetting("unit.test");
            setting.setTemplate("axonA");
            pg.getPassword(setting);
        } catch (NotHashedException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

}
