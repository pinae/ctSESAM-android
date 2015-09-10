package de.pinyto.ctSESAM;

import android.test.ActivityInstrumentationTestCase2;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Iterator;

import javax.crypto.NoSuchPaddingException;

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

    public void testSetSetting() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setUseDigits(true);
        setting.setUseLetters(false);
        setting.setUseExtra(false);
        setting.setLength(4);
        settingsManager.setSetting(setting);
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

    public void testSaveUnsupportedSettings() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setUseLowerCase(true);
        setting.setUseUpperCase(false);
        setting.setUseExtra(true);
        setting.setUseDigits(false);
        setting.setCustomCharacterSet("ABCKWXkwx345/$#");
        setting.setLegacyPassword("Insecure");
        setting.setUsername("hugo");
        setting.setAvoidAmbiguousCharacters(true);
        setting.setSalt("hmpf".getBytes());
        setting.setNotes("12 is more than 5.");
        settingsManager.setSetting(setting);
        PasswordSetting checkSetting = settingsManager.getSetting("unit.test");
        assertTrue(checkSetting.useLowerCase());
        assertFalse(checkSetting.useUpperCase());
        assertTrue(checkSetting.useExtra());
        assertFalse(checkSetting.useDigits());
        assertTrue(checkSetting.useCustomCharacterSet());
        assertEquals("ABCKWXkwx345/$#", checkSetting.getCustomCharacterSet());
        assertEquals("Insecure", checkSetting.getLegacyPassword());
        assertEquals("hugo", checkSetting.getUsername());
        assertTrue(checkSetting.avoidAmbiguousCharacters());
        assertEquals("hmpf".getBytes().length, checkSetting.getSalt().length);
        for (int i = 0; i < checkSetting.getSalt().length; i++) {
            assertEquals("hmpf".getBytes()[i], checkSetting.getSalt()[i]);
        }
        assertEquals("12 is more than 5.", checkSetting.getNotes());
        settingsManager.deleteSetting(checkSetting.getDomain());
    }

    public void testGetBlob() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setUseDigits(true);
        setting.setUseLetters(false);
        setting.setUseExtra(false);
        setting.setLength(4);
        settingsManager.setSetting(setting);
        byte[] password = "some secret".getBytes();
        byte[] blob = settingsManager.getExportData(password);
        assertEquals(0x01, blob[0]);
        byte[] salt = Arrays.copyOfRange(blob, 1, 33);
        byte[] kgkBlock =  Arrays.copyOfRange(blob, 33, 145);
        byte[] encryptedSettings = Arrays.copyOfRange(blob, 145, blob.length);
        for (int i = 0; i < blob.length; i++) {
            blob[i] = 0x00;
        }
        byte[] kgkKeyIv = Crypter.createIvKey(password, salt);
        Crypter kgkCrypter = new Crypter(kgkKeyIv);
        try {
            byte[] kgkData = kgkCrypter.decrypt(kgkBlock, "NoPadding");
            byte[] salt2 = Arrays.copyOfRange(kgkData, 0, 32);
            byte[] iv2 = Arrays.copyOfRange(kgkData, 32, 48);
            byte[] kgk = Arrays.copyOfRange(kgkData, 48, 112);
            byte[] settingsKey = Crypter.createKey(kgk, salt2);
            byte[] settingsKeyIv = new byte[48];
            for (int i = 0; i < settingsKey.length; i++) {
                settingsKeyIv[i] = settingsKey[i];
            }
            for (int i = 0; i < iv2.length; i++) {
                settingsKeyIv[settingsKey.length + i] = iv2[i];
            }
            Crypter settingsCrypter = new Crypter(settingsKeyIv);
            byte[] decrypted = settingsCrypter.decrypt(encryptedSettings);
            try {
                JSONObject data = new JSONObject(Packer.decompress(decrypted));
                boolean found = false;
                Iterator keysIterator = data.keys();
                while (keysIterator.hasNext()) {
                    JSONObject dataset = data.getJSONObject((String) keysIterator.next());
                    if (dataset.getString("domain").equals("unit.test")) {
                        found = true;
                        assertTrue(dataset.getBoolean("useDigits"));
                        assertFalse(dataset.getBoolean("useUpperCase"));
                        assertFalse(dataset.getBoolean("useLowerCase"));
                        assertFalse(dataset.getBoolean("useExtra"));
                        assertEquals(4, dataset.getInt("length"));
                        assertEquals(4096, dataset.getInt("iterations"));
                    }
                }
                assertTrue(found);
            } catch (JSONException e) {
                e.printStackTrace();
                assertTrue(false);
            }
        } catch (NoSuchPaddingException paddingError) {
            paddingError.printStackTrace();
        }
    }

    public void testUpdateBlob() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setUseDigits(true);
        setting.setUseLetters(false);
        setting.setUseExtra(false);
        setting.setLength(4);
        setting.setCreationDate("2001-01-01T02:14:12");
        setting.setModificationDate("2001-01-01T02:14:13");
        settingsManager.setSetting(setting);
        byte[] password = "some secret".getBytes();
        try {
            byte[] blob = settingsManager.getExportData(password);
            byte[] salt = Arrays.copyOfRange(blob, 1, 33);
            byte[] kgkBlock =  Arrays.copyOfRange(blob, 33, 145);
            byte[] encryptedSettings = Arrays.copyOfRange(blob, 145, blob.length);
            for (int i = 0; i < blob.length; i++) {
                blob[i] = 0x00;
            }
            byte[] kgkKeyIv = Crypter.createIvKey(password, salt);
            Crypter kgkCrypter = new Crypter(kgkKeyIv);
            byte[] kgkData = kgkCrypter.decrypt(kgkBlock, "NoPadding");
            byte[] salt2 = Arrays.copyOfRange(kgkData, 0, 32);
            byte[] iv2 = Arrays.copyOfRange(kgkData, 32, 48);
            byte[] kgk = Arrays.copyOfRange(kgkData, 48, 112);
            byte[] settingsKey = Crypter.createKey(kgk, salt2);
            byte[] settingsKeyIv = new byte[48];
            for (int i = 0; i < settingsKey.length; i++) {
                settingsKeyIv[i] = settingsKey[i];
            }
            for (int i = 0; i < iv2.length; i++) {
                settingsKeyIv[settingsKey.length + i] = iv2[i];
            }
            Crypter settingsCrypter = new Crypter(settingsKeyIv);
            byte[] decrypted = settingsCrypter.decrypt(encryptedSettings);
            JSONObject data = new JSONObject(Packer.decompress(decrypted));
            JSONObject remoteDataset = new JSONObject();
            remoteDataset.put("domain", "unit.test");
            remoteDataset.put("length", 12);
            remoteDataset.put("iterations", 4097);
            remoteDataset.put("useDigits", false);
            remoteDataset.put("useUpperCase", true);
            remoteDataset.put("useLowerCase", true);
            remoteDataset.put("useExtra", true);
            remoteDataset.put("mDate", "2012-04-13T11:45:10");
            Iterator<String> keysIterator = data.keys();
            while (keysIterator.hasNext()) {
                String domain = keysIterator.next();
                if (remoteDataset.getString("domain").equals(
                        data.getJSONObject(domain).getString("domain"))) {
                    data.put(domain, remoteDataset);
                }
            }
            byte[] encryptedData = settingsCrypter.encrypt(Packer.compress(data.toString()));
            byte[] remoteBlob = new byte[1 + salt.length + kgkBlock.length + encryptedData.length];
            remoteBlob[0] = 0x01;
            for (int i = 0; i < salt.length; i++) {
                remoteBlob[1 + i] = salt[i];
            }
            for (int i = 0; i < kgkBlock.length; i++) {
                remoteBlob[1 + salt.length + i] = kgkBlock[i];
            }
            for (int i = 0; i < encryptedData.length; i++) {
                remoteBlob[1 + salt.length + kgkBlock.length + i] = encryptedData[i];
            }
            settingsManager.updateFromExportData(password, remoteBlob);
            PasswordSetting updated = settingsManager.getSetting("unit.test");
            assertEquals("2012-04-13T11:45:10", updated.getModificationDate());
            assertEquals("2001-01-01T02:14:12", updated.getCreationDate());
            assertEquals(4097, updated.getIterations());
            assertEquals(12, updated.getLength());
            assertFalse(updated.useDigits());
            assertTrue(updated.useLetters());
            assertTrue(updated.useExtra());
        } catch (JSONException | NoSuchPaddingException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        settingsManager.deleteSetting("unit.test");
    }

    public void testSetAllSettingsToSynced() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setUseDigits(true);
        setting.setUseLetters(false);
        setting.setUseExtra(false);
        setting.setLength(4);
        setting.setCreationDate("2001-01-01T02:14:12");
        setting.setModificationDate("2001-01-01T02:14:13");
        settingsManager.setSetting(setting);
        assertFalse(setting.isSynced());
        String[] domainList = settingsManager.getDomainList();
        PasswordSetting[] allSettings = new PasswordSetting[domainList.length];
        for (int i = 0; i < domainList.length; i++) {
            allSettings[i] = settingsManager.getSetting(domainList[i]);
        }
        settingsManager.setAllSettingsToSynced();
        assertTrue(settingsManager.getSetting("unit.test").isSynced());
        for (String domain : domainList) {
            assertTrue(settingsManager.getSetting(domain).isSynced());
        }
        // restoring
        for (PasswordSetting s : allSettings) {
            settingsManager.setSetting(s);
        }
        settingsManager.deleteSetting("unit.test");
    }

    public void testSaveAndLoadLocally() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setUseDigits(true);
        setting.setUseLetters(false);
        setting.setUseExtra(false);
        setting.setLength(4);
        setting.setCreationDate("2001-01-01T02:14:12");
        setting.setModificationDate("2001-01-01T02:14:13");
        settingsManager.setSetting(setting);
        byte[] password = "some secret".getBytes();
        settingsManager.storeLocalSettings(password);
        PasswordSettingsManager settingsManager2 = new PasswordSettingsManager(getActivity().getBaseContext());
        settingsManager2.loadLocalSettings(password);
        PasswordSetting setting2 = settingsManager2.getSetting("unit.test");
        assertEquals(setting.getLength(), setting2.getLength());
        assertEquals("2001-01-01T02:14:12", setting2.getCreationDate());
        assertEquals("2001-01-01T02:14:13", setting2.getModificationDate());
    }

}
