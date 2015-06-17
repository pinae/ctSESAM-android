package de.pinyto.passwordgenerator;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Testing the management of password settings.
 */
public class PasswordSettingsManagerTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private PasswordSettingsManager settingsManager;

    public PasswordSettingsManagerTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        settingsManager = new PasswordSettingsManager(getActivity().getBaseContext());
    }

    public void testSaveSetting() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setUseDigits(true);
        setting.setUseLetters(false);
        setting.setUseExtra(false);
        setting.setLength(4);
        settingsManager.saveSetting(setting);
        PasswordSetting checkSetting = settingsManager.getSetting("unit.test");
        assertTrue(checkSetting.useDigits());
        assertFalse(checkSetting.useLetters());
        assertFalse(checkSetting.useExtra());
        assertEquals(4, checkSetting.getLength());
        assertEquals(4096, checkSetting.getIterations());
        settingsManager.deleteSetting(checkSetting.getDomain());
        boolean found = false;
        for (String domain : settingsManager.getDomainList()) {
            if (domain.equals("unit.test")) {
                found = true;
            }
        }
        assertFalse(found);
    }

}
