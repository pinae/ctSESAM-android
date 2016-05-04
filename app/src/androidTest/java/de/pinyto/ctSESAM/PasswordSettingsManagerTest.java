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
    private KgkManager kgkManager;

    public PasswordSettingsManagerTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        settingsManager = new PasswordSettingsManager(getActivity().getBaseContext());
        kgkManager = new KgkManager(getActivity().getBaseContext());
    }

    public void testSetSetting() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setTemplate("nnnn");
        settingsManager.setSetting(setting);
        PasswordSetting checkSetting = settingsManager.getSetting("unit.test");
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
        setting.setExtraCharacterSet("ABCKWXkwx345/$#");
        setting.setTemplate("ooooooo");
        setting.setLegacyPassword("Insecure");
        setting.setUsername("hugo");
        setting.setSalt("hmpf".getBytes());
        setting.setNotes("12 is more than 5.");
        settingsManager.setSetting(setting);
        PasswordSetting checkSetting = settingsManager.getSetting("unit.test");
        assertEquals("ABCKWXkwx345/$#", checkSetting.getCharacterSetAsString());
        assertEquals("Insecure", checkSetting.getLegacyPassword());
        assertEquals("hugo", checkSetting.getUsername());
        assertEquals("hmpf".getBytes().length, checkSetting.getSalt().length);
        for (int i = 0; i < checkSetting.getSalt().length; i++) {
            assertEquals("hmpf".getBytes()[i], checkSetting.getSalt()[i]);
        }
        assertEquals("12 is more than 5.", checkSetting.getNotes());
        settingsManager.deleteSetting(checkSetting.getDomain());
    }

    private Crypter getSettingsCrypter(KgkManager kgkManager) {
        byte[] salt2 = kgkManager.getSalt2();
        byte[] iv2 = kgkManager.getIv2();
        byte[] kgk = kgkManager.getKgk();
        byte[] settingsKey = Crypter.createKey(kgk, salt2);
        byte[] settingsKeyIv = new byte[48];
        for (int i = 0; i < settingsKey.length; i++) {
            settingsKeyIv[i] = settingsKey[i];
            settingsKey[i] = 0x00;
        }
        System.arraycopy(iv2, 0, settingsKeyIv, settingsKey.length, iv2.length);
        return new Crypter(settingsKeyIv);
    }

    public void testGetBlob() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setTemplate("nnnn");
        settingsManager.setSetting(setting);
        byte[] password = "some secret".getBytes();
        kgkManager.decryptKgk(password,
                kgkManager.getKgkCrypterSalt(), kgkManager.gelLocalKgkBlock());
        byte[] blob = settingsManager.getExportData(kgkManager);
        assertEquals(0x01, blob[0]);
        byte[] encryptedSettings = Arrays.copyOfRange(blob, 145, blob.length);
        for (int i = 0; i < blob.length; i++) {
            blob[i] = 0x00;
        }
        Crypter settingsCrypter = this.getSettingsCrypter(kgkManager);
        byte[] decrypted = settingsCrypter.decrypt(encryptedSettings);
        try {
            JSONObject data = new JSONObject(Packer.decompress(decrypted));
            boolean found = false;
            Iterator<String> keysIterator = data.keys();
            while (keysIterator.hasNext()) {
                JSONObject dataset = data.getJSONObject(keysIterator.next());
                if (dataset.getString("domain").equals("unit.test")) {
                    found = true;
                    assertTrue(dataset.has("extras"));
                    assertEquals("#!\"~|@^°$%&/()[]{}=-_+*<>;:.", dataset.getString("extras"));
                    assertTrue(dataset.has("passwordTemplate"));
                    assertEquals("nnnn", dataset.getString("passwordTemplate"));
                    assertTrue(dataset.has("iterations"));
                    assertEquals(4096, dataset.getInt("iterations"));
                }
            }
            assertTrue(found);
        } catch (JSONException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void testUpdateBlob() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setTemplate("nnnn");
        setting.setCreationDate("2001-01-01T02:14:12");
        setting.setModificationDate("2001-01-01T02:14:13");
        settingsManager.setSetting(setting);
        byte[] password = "some secret".getBytes();
        try {
            kgkManager.decryptKgk(password,
                    kgkManager.getKgkCrypterSalt(), kgkManager.gelLocalKgkBlock());
            byte[] blob = settingsManager.getExportData(kgkManager);
            byte[] encryptedSettings = Arrays.copyOfRange(blob, 145, blob.length);
            for (int i = 0; i < blob.length; i++) {
                blob[i] = 0x00;
            }
            Crypter settingsCrypter = this.getSettingsCrypter(kgkManager);
            byte[] decrypted = settingsCrypter.decrypt(encryptedSettings);
            JSONObject data = new JSONObject(Packer.decompress(decrypted));
            JSONObject remoteDataset = new JSONObject();
            remoteDataset.put("domain", "unit.test");
            remoteDataset.put("length", 12);
            remoteDataset.put("iterations", 4097);
            remoteDataset.put("usedCharacters", "0123456789");
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
            byte[] salt = kgkManager.getKgkCrypterSalt();
            byte[] kgkBlock = kgkManager.getEncryptedKgk();
            byte[] remoteBlob = new byte[1 + salt.length + kgkBlock.length + encryptedData.length];
            remoteBlob[0] = 0x01;
            System.arraycopy(salt, 0, remoteBlob, 1, salt.length);
            for (int i = 0; i < kgkBlock.length; i++) {
                remoteBlob[1 + salt.length + i] = kgkBlock[i];
            }
            for (int i = 0; i < encryptedData.length; i++) {
                remoteBlob[1 + salt.length + kgkBlock.length + i] = encryptedData[i];
            }
            settingsManager.updateFromExportData(kgkManager, remoteBlob);
            PasswordSetting updated = settingsManager.getSetting("unit.test");
            assertEquals("2012-04-13T11:45:10", updated.getModificationDate());
            assertEquals("2001-01-01T02:14:12", updated.getCreationDate());
            assertEquals(4097, updated.getIterations());
            assertEquals(12, updated.getLength());
        } catch (JSONException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        settingsManager.deleteSetting("unit.test");
    }

    public void testSetAllSettingsToSynced() {
        PasswordSetting setting = new PasswordSetting("unit.test");
        setting.setTemplate("nnnn");
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
        setting.setTemplate("nnnn");
        setting.setCreationDate("2001-01-01T02:14:12");
        setting.setModificationDate("2001-01-01T02:14:13");
        settingsManager.setSetting(setting);
        byte[] password = "some secret".getBytes();
        kgkManager.decryptKgk(password,
                kgkManager.getKgkCrypterSalt(), kgkManager.gelLocalKgkBlock());
        settingsManager.storeLocalSettings(kgkManager);
        PasswordSettingsManager settingsManager2 = new PasswordSettingsManager(
                getActivity().getBaseContext());
        try {
            settingsManager2.loadLocalSettings(kgkManager);
        } catch (WrongPasswordException ex) {
            ex.printStackTrace();
            assertTrue(false);
        }
        PasswordSetting setting2 = settingsManager2.getSetting("unit.test");
        assertEquals(setting.getLength(), setting2.getLength());
        assertEquals("2001-01-01T02:14:12", setting2.getCreationDate());
        assertEquals("2001-01-01T02:14:13", setting2.getModificationDate());
    }

}
