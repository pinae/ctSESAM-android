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

    public void testExtraCharacterSet() {
        PasswordSetting s = new PasswordSetting("unit.test");
        s.setExtraCharacterSet("&=Oo0wWsS$#uUvVzZ");
        assertEquals("&=Oo0wWsS$#uUvVzZ", s.getExtraCharacterSetAsString());
    }

    public void testGetCharacterSetAsString() {
        PasswordSetting s = new PasswordSetting("unit.test");
        s.setTemplate("noxxxxxx");
        assertEquals("0123456789#!\"~|@^°$%&/()[]{}=-_+*<>;:.", s.getCharacterSetAsString());
        s.setTemplate("xxaAxxxxxx");
        assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
                s.getCharacterSetAsString());
    }

    public void testGetCharacterSet() {
        PasswordSetting s = new PasswordSetting("unit.test");
        assertEquals("2", s.getCharacterSet().get(2));
        s.setExtraCharacterSet("axFLp0");
        s.setTemplate("oxxx");
        assertEquals(6, s.getCharacterSet().size());
        assertEquals("F", s.getCharacterSet().get(2));
        assertEquals("0", s.getCharacterSet().get(5));
    }

    public void testSalt() {
        PasswordSetting s = new PasswordSetting("unit.test");
        byte[] expected = "somethingelse".getBytes();
        s.setSalt(expected);
        byte[] actual = s.getSalt();
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

    public void testTemplate() {
        PasswordSetting s = new PasswordSetting("unit.test");
        assertEquals(10, s.getLength());
        assertTrue(s.getTemplate().contains("a"));
        assertTrue(s.getTemplate().contains("A"));
        assertTrue(s.getTemplate().contains("n"));
        assertTrue(s.getTemplate().contains("o"));
        assertTrue(s.getTemplate().contains("x"));
        s.setTemplate("xan");
        assertEquals(3, s.getLength());
        assertTrue(s.getTemplate().contains("a"));
        assertFalse(s.getTemplate().contains("A"));
        assertTrue(s.getTemplate().contains("n"));
        assertFalse(s.getTemplate().contains("o"));
        assertTrue(s.getTemplate().contains("x"));
        assertEquals("xan", s.getTemplate());
        assertEquals("3;xan", s.getFullTemplate());
        s.setTemplate("3;xan");
        assertEquals(3, s.getLength());
        assertTrue(s.getTemplate().contains("a"));
        assertFalse(s.getTemplate().contains("A"));
        assertTrue(s.getTemplate().contains("n"));
        assertFalse(s.getTemplate().contains("o"));
        assertTrue(s.getTemplate().contains("x"));
        assertEquals("xan", s.getTemplate());
    }

    public void testToJson() {
        PasswordSetting s = new PasswordSetting("unit.test");
        s.setModificationDate("2005-01-01T01:14:12");
        s.setCreationDate("2001-01-01T02:14:12");
        s.setUsername("Hugo");
        s.setLegacyPassword("Wamma");
        s.setSalt("something".getBytes());
        s.setIterations(213);
        s.setNotes("Some note.");
        try {
            assertTrue(s.toJSON().has("domain"));
            assertEquals("unit.test", s.toJSON().getString("domain"));
            assertTrue(s.toJSON().has("username"));
            assertEquals("Hugo", s.toJSON().getString("username"));
            assertTrue(s.toJSON().has("legacyPassword"));
            assertEquals("Wamma", s.toJSON().getString("legacyPassword"));
            assertTrue(s.toJSON().has("notes"));
            assertEquals("Some note.", s.toJSON().getString("notes"));
            assertTrue(s.toJSON().has("iterations"));
            assertEquals(213, s.toJSON().getInt("iterations"));
            assertTrue(s.toJSON().has("salt"));
            assertEquals(
                    Base64.encodeToString("something".getBytes(), Base64.DEFAULT),
                    s.toJSON().getString("salt"));
            assertTrue(s.toJSON().has("cDate"));
            assertEquals("2001-01-01T02:14:12", s.toJSON().getString("cDate"));
            assertTrue(s.toJSON().has("mDate"));
            assertEquals("2005-01-01T01:14:12", s.toJSON().getString("mDate"));
            assertTrue(s.toJSON().has("extras"));
            assertEquals("#!\"~|@^°$%&/()[]{}=-_+*<>;:.", s.toJSON().getString("extras"));
            assertTrue(s.toJSON().has("passwordTemplate"));
            assertEquals(10, s.toJSON().getString("passwordTemplate").length());
            assertTrue(s.toJSON().getString("passwordTemplate").contains("a"));
            assertTrue(s.toJSON().getString("passwordTemplate").contains("A"));
            assertTrue(s.toJSON().getString("passwordTemplate").contains("n"));
            assertTrue(s.toJSON().getString("passwordTemplate").contains("o"));
        } catch (JSONException e) {
            assertTrue(false);
        }
    }

    public void testLoadFromJSON() {
        String json = "{\"domain\": \"unit.test\", \"username\": \"testilinius\", " +
                "\"notes\": \"interesting note\", \"legacyPassword\": \"rtSr?bS,mi\", " +
                "\"iterations\": 5341, " +
                "\"passwordTemplate\": \"xnxxAxaoxx\", \"salt\": \"ZmFzY2luYXRpbmc=\", " +
                "\"extras\": \"#&{}[]()%\", " +
                "\"cDate\": \"2001-01-01T02:14:12\", \"mDate\": \"2005-01-01T01:14:12\"}";
        try {
            JSONObject data = new JSONObject(json);
            PasswordSetting s = new PasswordSetting(data.getString("domain"));
            s.loadFromJSON(data);
            assertEquals("unit.test", s.getDomain());
            assertEquals("testilinius", s.getUsername());
            assertEquals("interesting note", s.getNotes());
            assertEquals("rtSr?bS,mi", s.getLegacyPassword());
            assertEquals("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ#&{}[]()%",
                    s.getCharacterSetAsString());
            assertEquals(5341, s.getIterations());
            assertEquals(10, s.getLength());
            assertEquals("xnxxAxaoxx", s.getTemplate());
            assertEquals("#&{}[]()%", s.getExtraCharacterSetAsString());
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

    public void testSetCharacterSetExtra() {
        PasswordSetting s = new PasswordSetting("unit.test");
        s.setExtraCharacterSet("…ſ²³›ABC‹¢¥¥„“`´•");
        s.setTemplate("xox");
        assertEquals("…ſ²³›ABC‹¢¥¥„“`´•", s.getExtraCharacterSetAsString());
    }

}
