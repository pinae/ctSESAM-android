package de.pinyto.ctSESAM;

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
        s.setCharacterSet("&=Oo0wWsS$#uUvVzZ");
        assertEquals("&=Oo0wWsS$#uUvVzZ", s.getCharacterSetAsString());
        s.setCharacterSet(
                "abcdefghijklmnopqrstuvwxyz" +
                        "ABCDEFGHJKLMNPQRTUVWXYZ" +
                        "0123456789" +
                        "#!\"§$%&/()[]{}=-_+*<>;:.");
        assertTrue(s.useLetters());
        assertTrue(s.useDigits());
        assertTrue(s.useExtra());
    }

    public void testGetCharacterSetAsString() {
        PasswordSetting s = new PasswordSetting("unit.test");
        s.setUseLetters(false);
        assertEquals("0123456789#!\"§$%&/()[]{}=-_+*<>;:.", s.getCharacterSetAsString());
        s.setUseLetters(true);
        s.setUseDigits(false);
        s.setUseExtra(false);
        assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHJKLMNPQRTUVWXYZ",
                s.getCharacterSetAsString());
    }

    public void testGetCharacterSet() {
        PasswordSetting s = new PasswordSetting("unit.test");
        assertEquals("c", s.getCharacterSet().get(2));
        s.setCharacterSet("axFLp0");
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
        s.setCharacterSet("XVLCWKHGFQUIAEOSNRTDYÜÖÄPZBMJ");
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
            assertTrue(s.toJSON().has("usedCharacters"));
            assertEquals("XVLCWKHGFQUIAEOSNRTDYÜÖÄPZBMJ",
                    s.toJSON().getString("usedCharacters"));
            assertTrue(s.toJSON().has("notes"));
            assertEquals("Some note.", s.toJSON().getString("notes"));
        } catch (JSONException e) {
            assertTrue(false);
        }
    }

    public void testLoadFromJSON() {
        String json = "{\"domain\": \"unit.test\", \"username\": \"testilinius\", " +
                "\"notes\": \"interesting note\", \"legacyPassword\": \"rtSr?bS,mi\", " +
                "\"iterations\": 5341, " +
                "\"length\": 16, \"salt\": \"ZmFzY2luYXRpbmc=\", " +
                "\"usedCharacters\": \"abcdefghijklmnopqrstuvwxyz\", " +
                "\"cDate\": \"2001-01-01T02:14:12\", \"mDate\": \"2005-01-01T01:14:12\"}";
        try {
            JSONObject data = new JSONObject(json);
            PasswordSetting s = new PasswordSetting(data.getString("domain"));
            s.loadFromJSON(data);
            assertEquals("unit.test", s.getDomain());
            assertEquals("testilinius", s.getUsername());
            assertEquals("interesting note", s.getNotes());
            assertEquals("rtSr?bS,mi", s.getLegacyPassword());
            assertTrue(s.useLowerCase());
            assertFalse(s.useUpperCase());
            assertFalse(s.useDigits());
            assertFalse(s.useExtra());
            assertEquals("abcdefghijklmnopqrstuvwxyz", s.getCharacterSetAsString());
            assertEquals(5341, s.getIterations());
            assertEquals(16, s.getLength());
            byte[] expectedSalt;
            try {
                expectedSalt = "fascinating".getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                assertTrue(false);
                expectedSalt = "fascinating".getBytes();
            }
            assertEquals(expectedSalt.length, s.getSalt().length);
            for (int i = 0; i < expectedSalt.length; i++) {
                assertEquals(expectedSalt[i], s.getSalt()[i]);
            }
            assertEquals("2001-01-01T02:14:12", s.getCreationDate());
            assertEquals("2005-01-01T01:14:12", s.getModificationDate());
        } catch (JSONException e) {
            assertTrue(false);
        }
    }

}
