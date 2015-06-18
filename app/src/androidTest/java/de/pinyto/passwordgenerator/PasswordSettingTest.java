package de.pinyto.passwordgenerator;

import junit.framework.TestCase;

/**
 * Testing the setting container class.
 */
public class PasswordSettingTest extends TestCase {

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

}
