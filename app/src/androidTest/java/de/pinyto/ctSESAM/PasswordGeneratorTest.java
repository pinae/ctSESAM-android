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
        PasswordGenerator pg = new PasswordGenerator(domain, username, kgk, salt);
        try {
            pg.hash(2);
            PasswordSetting setting = new PasswordSetting("unit.test");
            setting.setLength(10);
            assertEquals("b}f_Qoszh+", pg.getPassword(setting));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashTwiceAndGetPassword () {
        byte[] domain = UTF8.encode("unit.test");
        byte[] username = UTF8.encode("hugo");
        byte[] kgk = UTF8.encode("secret");
        byte[] salt = UTF8.encode("pepper");
        PasswordGenerator pg = new PasswordGenerator(domain, username, kgk, salt);
        try {
            pg.hash(2);
            pg.hash(5);
            PasswordSetting setting = new PasswordSetting("unit.test");
            setting.setLength(10);
            assertEquals("3YJAn$M*y8", pg.getPassword(setting));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordEmptyInputs () {
        PasswordGenerator pg = new PasswordGenerator(
                new byte[] {},
                new byte[] {},
                new byte[] {},
                new byte[] {});
        try {
            pg.hash(2);
            PasswordSetting setting = new PasswordSetting("unit.test");
            setting.setLength(10);
            assertEquals("Y*mRn.N8t7", pg.getPassword(setting));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordLengthZero () {
        byte[] domain = UTF8.encode("unit.test");
        byte[] username = UTF8.encode("hugo");
        byte[] kgk = UTF8.encode("secret");
        byte[] salt = UTF8.encode("pepper");
        PasswordGenerator pg = new PasswordGenerator(domain, username, kgk, salt);
        try {
            pg.hash(2);
            PasswordSetting setting = new PasswordSetting("unit.test");
            setting.setLength(0);
            assertEquals("", pg.getPassword(setting));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordLengthMax () {
        PasswordGenerator pg = new PasswordGenerator(
                new byte[] {},
                new byte[] {},
                new byte[] {},
                new byte[] {});
        try {
            pg.hash(2);
            PasswordSetting setting = new PasswordSetting("unit.test");
            setting.setLength(1000);
            assertEquals(81, pg.getPassword(setting).length());
            assertEquals(
                "Y*mRn.N8t784pP$e!RG-*v{Q%pdUp4#\">q6b{eH.jjc7;\"%8x3" +
                "{xh4V(G{#TpfGL&v03GsQQ:FnR]*GXc",
                pg.getPassword(setting));
        } catch (NotHashedException e) {
            e.printStackTrace();
        }
    }

    public void testHashOnceAndGetPasswordEmptyInputsZeroHashes () {
        PasswordGenerator pg = new PasswordGenerator(
                new byte[] {},
                new byte[] {},
                new byte[] {},
                new byte[] {});
        boolean thrown = false;
        try {
            pg.hash(0);
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
        PasswordGenerator pg = new PasswordGenerator(domain, username, kgk, salt);
        boolean thrown = false;
        try {
            PasswordSetting setting = new PasswordSetting("unit.test");
            setting.setLength(1000);
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
        PasswordGenerator pg = new PasswordGenerator(domain, username, kgk, salt);
        boolean thrown = false;
        try {
            pg.hash(-3);
        } catch (NotHashedException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    public void testHashTwiceWithPositiveAndNegativeIterationCountRaisesError () {
        byte[] domain = UTF8.encode("unit.test");
        byte[] username = UTF8.encode("hugo");
        byte[] kgk = UTF8.encode("secret");
        byte[] salt = UTF8.encode("pepper");
        PasswordGenerator pg = new PasswordGenerator(domain, username, kgk, salt);
        boolean thrown = false;
        try {
            pg.hash(2);
            pg.hash(-1);
            PasswordSetting setting = new PasswordSetting("unit.test");
            setting.setLength(1000);
            pg.getPassword(setting);
        } catch (NotHashedException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }

}
