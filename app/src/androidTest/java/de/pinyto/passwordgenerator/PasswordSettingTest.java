package de.pinyto.passwordgenerator;

import android.util.Base64;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Testing the setting container class.
 */
public class PasswordSettingTest extends TestCase {

    public void testUsername() {
        PasswordSetting s = new PasswordSetting("unit.test");
        assertEquals("", s.getUsername());
        s.setUsername("Hugo");
        assertEquals("Hugo", s.getUsername());
    }

    public void testLegacyPassword() {
        PasswordSetting s = new PasswordSetting("unit.test");
        assertEquals("", s.getLegacyPassword());
        s.setLegacyPassword("K6x/vyG9(p");
        assertEquals("K6x/vyG9(p", s.getLegacyPassword());
    }

    public void testCharacterSet() {
        PasswordSetting s = new PasswordSetting("unit.test");
        assertFalse(s.useCustomCharacterSet());
        assertEquals("", s.getCustomCharacterSet());
        s.setCustomCharacterSet("&=Oo0wWsS$#uUvVzZ");
        assertTrue(s.useCustomCharacterSet());
        assertEquals("&=Oo0wWsS$#uUvVzZ", s.getCustomCharacterSet());
        s.setCustomCharacterSet(
                "abcdefghijklmnopqrstuvwxyz" +
                        "ABCDEFGHJKLMNPQRTUVWXYZ" +
                        "0123456789" +
                        "#!\"§$%&/()[]{}=-_+*<>;:.");
        assertFalse(s.useCustomCharacterSet());
        assertEquals("", s.getCustomCharacterSet());
    }

    public void testGetDefaultCharacterSet() {
        PasswordSetting s = new PasswordSetting("unit.test");
        s.setUseLetters(false);
        assertEquals("0123456789#!\"§$%&/()[]{}=-_+*<>;:.", s.getDefaultCharacterSet());
        s.setUseLetters(true);
        s.setUseDigits(false);
        s.setUseExtra(false);
        assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHJKLMNPQRTUVWXYZ",
                s.getDefaultCharacterSet());
    }

    public void testGetCharacterSet() {
        PasswordSetting s = new PasswordSetting("unit.test");
        assertEquals("c", s.getCharacterSet().get(2));
        s.setCustomCharacterSet("axFLp0");
        assertEquals(6, s.getCharacterSet().size());
        assertEquals("F", s.getCharacterSet().get(2));
        assertEquals("0", s.getCharacterSet().get(5));
    }

    public void testSalt() {
        byte[] expected;
        try {
            expected = "pepper".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            assertTrue(false);
            expected = "pepper".getBytes();
        }
        PasswordSetting s = new PasswordSetting("unit.test");
        byte[] actual = s.getSalt();
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
        expected = "somethingelse".getBytes();
        s.setSalt(expected);
        actual = s.getSalt();
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    public void testSetCreationDate() {
        PasswordSetting s = new PasswordSetting("unit.test");
        s.setModificationDate("1995-01-01T01:14:12");
        s.setCreationDate("2001-01-01T02:14:12");
        assertEquals("2001-01-01T02:14:12", s.getCreationDate());
        assertEquals("2001-01-01T02:14:12", s.getModificationDate());
    }

    public void testSetModificationDate() {
        PasswordSetting s = new PasswordSetting("unit.test");
        s.setCreationDate("2007-01-01T02:14:12");
        s.setModificationDate("2005-01-01T01:14:12");
        assertEquals("2005-01-01T01:14:12", s.getCreationDate());
        assertEquals("2005-01-01T01:14:12", s.getModificationDate());
    }

    public void testNotes() {
        PasswordSetting s = new PasswordSetting("unit.test");
        assertEquals("", s.getNotes());
        s.setNotes("Beware of the password!");
        assertEquals("Beware of the password!", s.getNotes());
    }

    public void testToJson() {
        PasswordSetting s = new PasswordSetting("unit.test");
        s.setModificationDate("2005-01-01T01:14:12");
        s.setCreationDate("2001-01-01T02:14:12");
        s.setSalt("something".getBytes());
        s.setIterations(213);
        s.setLength(14);
        s.setCustomCharacterSet("XVLCWKHGFQUIAEOSNRTDYÜÖÄPZBMJ");
        s.setNotes("Some note.");
        try {
            assertTrue(s.toJSON().has("domain"));
            assertEquals("unit.test", s.toJSON().getString("domain"));
            assertTrue(s.toJSON().has("cDate"));
            assertEquals("2001-01-01T02:14:12", s.toJSON().getString("cDate"));
            assertTrue(s.toJSON().has("mDate"));
            assertEquals("2005-01-01T01:14:12", s.toJSON().getString("mDate"));
            assertTrue(s.toJSON().has("salt"));
            assertEquals(
                    Base64.encodeToString("something".getBytes(), Base64.DEFAULT),
                    s.toJSON().getString("salt"));
            assertTrue(s.toJSON().has("iterations"));
            assertEquals(213, s.toJSON().getInt("iterations"));
            assertTrue(s.toJSON().has("length"));
            assertEquals(14, s.toJSON().getInt("length"));
            assertTrue(s.toJSON().has("useCustom"));
            assertTrue(s.toJSON().getBoolean("useCustom"));
            assertTrue(s.toJSON().has("customCharacterSet"));
            assertEquals("XVLCWKHGFQUIAEOSNRTDYÜÖÄPZBMJ",
                    s.toJSON().getString("customCharacterSet"));
            assertTrue(s.toJSON().has("notes"));
            assertEquals("Some note.", s.toJSON().getString("notes"));
        } catch (JSONException e) {
            assertTrue(false);
        }
    }

}
