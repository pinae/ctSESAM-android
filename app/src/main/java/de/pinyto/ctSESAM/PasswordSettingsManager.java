package de.pinyto.ctSESAM;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import javax.crypto.NoSuchPaddingException;

/**
 * Use this class to manage password settings. It will store them internally and it will also
 * pack them for synchronization.
 */
public class PasswordSettingsManager {
    private SharedPreferences savedDomains;
    private Context contentContext;
    private Set<PasswordSetting> settings;
    private boolean localSettingsLoaded;

    PasswordSettingsManager(Context contentContext) {
        this.contentContext = contentContext;
        this.savedDomains = contentContext.getSharedPreferences(
                "savedDomains", Context.MODE_PRIVATE);
        this.settings = new HashSet<>();
        this.localSettingsLoaded = false;
    }

    public void loadSettings(byte[] password) {
        this.loadLocalSettings(password);
        this.loadRemoteSettings(password);
        // Zero the password after use.
        for (int i = 0; i < password.length; i++) {
            password[i] = 0x00;
        }
    }

    private byte[] getKgkCrypterSalt() {
        byte[] salt = Base64.decode(
                this.savedDomains.getString("salt", ""),
                Base64.DEFAULT);
        if (salt.length != 32) {
            salt = Crypter.createSalt();
            SharedPreferences.Editor savedDomainsEditor = this.savedDomains.edit();
            savedDomainsEditor.putString("salt", Base64.encodeToString(
                    salt,
                    Base64.DEFAULT));
            savedDomainsEditor.apply();
        }
        return salt;
    }

    private Crypter getKgkCrypter(byte[] password) {
        return new Crypter(Crypter.createIvKey(
            password,
            this.getKgkCrypterSalt()));
    }

    private String createNewKgk(byte[] password) {
        Crypter kgkCrypter = this.getKgkCrypter(password);
        byte[] salt = Crypter.createSalt();
        byte[] iv = Crypter.createIv();
        SecureRandom sr = new SecureRandom();
        byte[] kgk = new byte[64];
        sr.nextBytes(kgk);
        byte[] kgkBlock = new byte[112];
        for (int i = 0; i < salt.length; i++) {
            kgkBlock[i] = salt[i];
            salt[i] = 0x00;
        }
        for (int i = 0; i < iv.length; i++) {
            kgkBlock[salt.length + i] = iv[i];
            iv[i] = 0x00;
        }
        for (int i = 0; i < kgk.length; i++) {
            kgkBlock[salt.length + iv.length + i] = kgk[i];
            kgk[i] = 0x00;
        }
        String kgkBlockBase64 = Base64.encodeToString(
                kgkCrypter.encrypt(kgkBlock, "NoPadding"),
                Base64.DEFAULT);
        SharedPreferences.Editor savedDomainsEditor = this.savedDomains.edit();
        savedDomainsEditor.putString("KGK", kgkBlockBase64);
        savedDomainsEditor.apply();
        return kgkBlockBase64;
    }

    private byte[] getKgkBlock(byte[] password) {
        String kgkBase64 = this.savedDomains.getString("KGK", "");
        if (kgkBase64.length() < 152) {
            kgkBase64 = this.createNewKgk(password);
        }
        Crypter kgkCrypter = this.getKgkCrypter(password);
        try {
            return kgkCrypter.decrypt(Base64.decode(
                    kgkBase64,
                    Base64.DEFAULT), "NoPadding");
        } catch (NoSuchPaddingException paddingError) {
            paddingError.printStackTrace();
            return new byte[]{};
        }
    }

    public byte[] getKgk(byte[] password) {
        byte[] kgkData = getKgkBlock(password);
        byte[] kgk = Arrays.copyOfRange(kgkData, 48, 112);
        for (int i = 0; i < kgkData.length; i++) {
            kgkData[i] = 0x00;
        }
        return kgk;
    }

    private Crypter getSettingsCrypter(byte[] password) {
        return this.getSettingsCrypter(password, new byte[]{}, new byte[]{});
    }

    private Crypter getSettingsCrypter(byte[] password, byte[] newSalt, byte[] newIv) {
        byte[] kgkData = getKgkBlock(password);
        byte[] salt2 = Arrays.copyOfRange(kgkData, 0, 32);
        byte[] iv2 = Arrays.copyOfRange(kgkData, 32, 48);
        byte[] kgk = Arrays.copyOfRange(kgkData, 48, 112);
        if (newSalt.length == 32 && newIv.length == 16) {
            for (int i = 0; i < salt2.length; i++) {
                salt2[i] = 0x00;
            }
            salt2 = newSalt;
            for (int i = 0; i < iv2.length; i++) {
                iv2[i] = 0x00;
            }
            iv2 = newIv;
            System.arraycopy(salt2, 0, kgkData, 0, salt2.length);
            System.arraycopy(iv2, 0, kgkData, salt2.length, iv2.length);
            System.arraycopy(kgk, 0, kgkData, salt2.length + iv2.length, kgk.length);
            byte[] newKgkSalt = Crypter.createSalt();
            Crypter kgkCrypter = new Crypter(Crypter.createIvKey(password, newKgkSalt));
            SharedPreferences.Editor savedDomainsEditor = this.savedDomains.edit();
            savedDomainsEditor.putString("salt",
                    Base64.encodeToString(
                            newKgkSalt,
                            Base64.DEFAULT));
            savedDomainsEditor.putString("KGK",
                    Base64.encodeToString(
                            kgkCrypter.encrypt(kgkData, "NoPadding"),
                            Base64.DEFAULT));
            savedDomainsEditor.apply();
        }
        for (int i = 0; i < kgkData.length; i++) {
            kgkData[i] = 0x00;
        }
        byte[] settingsKey = Crypter.createKey(kgk, salt2);
        for (int i = 0; i < salt2.length; i++) {
            salt2[i] = 0x00;
        }
        for (int i = 0; i < kgk.length; i++) {
            kgk[i] = 0x00;
        }
        byte[] settingsKeyIv = new byte[48];
        for (int i = 0; i < settingsKey.length; i++) {
            settingsKeyIv[i] = settingsKey[i];
            settingsKey[i] = 0x00;
        }
        for (int i = settingsKey.length; i < settingsKey.length + iv2.length; i++) {
            settingsKeyIv[i] = iv2[i - settingsKey.length];
            iv2[i - settingsKey.length] = 0x00;
        }
        return new Crypter(settingsKeyIv);
    }

    public void loadLocalSettings(byte[] password) {
        Crypter settingsCrypter = this.getSettingsCrypter(password);
        byte[] encrypted = Base64.decode(
                this.savedDomains.getString("encryptedSettings", ""),
                Base64.DEFAULT);
        if (encrypted.length < 40) {
            return;
        }
        byte[] decrypted = settingsCrypter.decrypt(encrypted);
        if (decrypted.length < 40) {
            Toast.makeText(contentContext,
                    R.string.local_wrong_password, Toast.LENGTH_SHORT).show();
            return;
        }
        String decompressedSettings = Packer.decompress(decrypted);
        try {
            JSONObject decryptedObject = new JSONObject(decompressedSettings);
            JSONObject decryptedSettings = decryptedObject.getJSONObject("settings");
            JSONArray syncedSettings = decryptedObject.getJSONArray("synced");
            Iterator<String> keys = decryptedSettings.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject settingObject = decryptedSettings.getJSONObject(key);
                boolean found = false;
                for (PasswordSetting setting : this.settings) {
                    if (setting.getDomain().contentEquals(key)) {
                        found = true;
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                                Locale.ENGLISH);
                        Date modifiedRemote = df.parse(settingObject.getString("mDate"));
                        if (modifiedRemote.after(setting.getMDate())) {
                            setting.loadFromJSON(settingObject);
                            boolean foundInSynced = false;
                            for (int i = 0; i < syncedSettings.length(); i++) {
                                if (syncedSettings.getString(i).contentEquals(key)) {
                                    foundInSynced = true;
                                }
                            }
                            setting.setSynced(foundInSynced);
                        }
                    }
                }
                if (!found) {
                    PasswordSetting newSetting = new PasswordSetting(key);
                    newSetting.loadFromJSON(settingObject);
                    boolean foundInSynced = false;
                    for (int i = 0; i < syncedSettings.length(); i++) {
                        if (syncedSettings.getString(i).contentEquals(key)) {
                            foundInSynced = true;
                        }
                    }
                    newSetting.setSynced(foundInSynced);
                    this.settings.add(newSetting);
                }
            }
            this.localSettingsLoaded = true;
        } catch (JSONException jsonError) {
            Log.d("Settings loading error", "The loaded settings are not in JSON format.");
            jsonError.printStackTrace();
        } catch (ParseException timeFormatError) {
            Log.d("Settings loading error",
                    "The loaded settings contain time information in a wrong format.");
            timeFormatError.printStackTrace();
        }
    }

    public boolean isLocalSettingsLoaded() {
        return this.localSettingsLoaded;
    }

    public void loadRemoteSettings(byte[] password) {

    }

    public void storeSettings(byte[] password) {
        this.storeLocalSettings(password);
        this.updateSyncServerIfNecessary(password);
        // Zero the password after use.
        for (int i = 0; i < password.length; i++) {
            password[i] = 0x00;
        }
    }

    public void storeLocalSettings(byte[] password) {
        byte[] newSalt = Crypter.createSalt();
        byte[] newIv = Crypter.createIv();
        Crypter settingsCrypter = this.getSettingsCrypter(password, newSalt, newIv);
        JSONObject storeStructure = new JSONObject();
        try {
            storeStructure.put("settings", this.getSettingsAsJSON());
            storeStructure.put("synced", this.getSyncedSettings());
        } catch (JSONException jsonError) {
            Log.d("Settings saving error", "Could not construct JSON structure for storage.");
            jsonError.printStackTrace();
        }
        SharedPreferences.Editor savedDomainsEditor = savedDomains.edit();
        if (settingsCrypter != null) {
            byte[] encryptedSettings = settingsCrypter.encrypt(
                    Packer.compress(storeStructure.toString()));
            savedDomainsEditor.putString("encryptedSettings",
                    Base64.encodeToString(
                            encryptedSettings,
                            Base64.DEFAULT));
            savedDomainsEditor.apply();
        }
    }

    public void updateSyncServerIfNecessary(byte[] password) {

    }

    public PasswordSetting getSetting(String domain) {
        for (PasswordSetting setting : this.settings) {
            if (setting.getDomain().contentEquals(domain)) {
                return setting;
            }
        }
        PasswordSetting newSetting = new PasswordSetting(domain);
        this.settings.add(newSetting);
        return newSetting;
    }

    public void setSetting(PasswordSetting changed) {
        for (PasswordSetting setting : this.settings) {
            if (setting.getDomain().contentEquals(changed.getDomain())) {
                this.settings.remove(setting);
                break;
            }
        }
        this.settings.add(changed);
    }

    public void deleteSetting(String domain) {
        for (PasswordSetting setting : this.settings) {
            if (setting.getDomain().contentEquals(domain)) {
                this.settings.remove(setting);
            }
        }
    }

    public String[] getDomainList() {
        String[] domainList = new String[this.settings.size()];
        int i = 0;
        for (PasswordSetting setting : this.settings) {
            domainList[i] = setting.getDomain();
            i++;
        }
        return domainList;
    }

    private JSONObject getSettingsAsJSON() {
        JSONObject settings = new JSONObject();
        try {
            for (PasswordSetting setting : this.settings) {
                settings.put(setting.getDomain(), setting.toJSON());
            }
        } catch (JSONException jsonError) {
            Log.d("Settings packing error", "Could not create json.");
            jsonError.printStackTrace();
        }
        return settings;
    }

    private JSONArray getSyncedSettings() {
        JSONArray syncedSettings = new JSONArray();
        for (PasswordSetting setting : this.settings) {
            if (setting.isSynced()) {
                syncedSettings.put(setting.getDomain());
            }
        }
        return syncedSettings;
    }

    public byte[] getExportData(byte[] password) {
        byte[] kgk = this.getKgk(password);
        byte[] salt2 = Crypter.createSalt();
        byte[] iv2 = Crypter.createIv();
        byte[] kgkData = new byte[112];
        System.arraycopy(salt2, 0, kgkData, 0, salt2.length);
        System.arraycopy(iv2, 0, kgkData, salt2.length, iv2.length);
        System.arraycopy(kgk, 0, kgkData, salt2.length + iv2.length, kgk.length);
        byte[] newSalt = Crypter.createSalt();
        Crypter kgkCrypter = new Crypter(Crypter.createIvKey(password, newSalt));
        byte[] kgkBlock = kgkCrypter.encrypt(kgkData, "NoPadding");
        byte[] settingsKey = Crypter.createKey(kgk, salt2);
        for (int i = 0; i < salt2.length; i++) {
            salt2[i] = 0x00;
        }
        for (int i = 0; i < kgk.length; i++) {
            kgk[i] = 0x00;
        }
        byte[] settingsKeyIv = new byte[48];
        for (int i = 0; i < settingsKey.length; i++) {
            settingsKeyIv[i] = settingsKey[i];
            settingsKey[i] = 0x00;
        }
        for (int i = settingsKey.length; i < settingsKey.length + iv2.length; i++) {
            settingsKeyIv[i] = iv2[i - settingsKey.length];
            iv2[i - settingsKey.length] = 0x00;
        }
        Crypter settingsCrypter = new Crypter(settingsKeyIv);
        byte[] encryptedSettings = settingsCrypter.encrypt(
                Packer.compress(
                        this.getSettingsAsJSON().toString()
                )
        );
        byte[] exportData = new byte[1 + newSalt.length + kgkBlock.length + encryptedSettings.length];
        exportData[0] = 0x01;
        for (int i = 0; i < newSalt.length; i++) {
            exportData[1 + i] = newSalt[i];
            newSalt[i] = 0x00;
        }
        for (int i = 0; i < kgkBlock.length; i++) {
            exportData[1 + newSalt.length + i] = kgkBlock[i];
            kgkBlock[i] = 0x00;
        }
        for (int i = 0; i < encryptedSettings.length; i++) {
            exportData[1 + newSalt.length + kgkBlock.length + i] = encryptedSettings[i];
            encryptedSettings[i] = 0x00;
        }
        return exportData;
    }

    public boolean updateFromExportData(byte[] password, byte[] blob) {
        if (!(blob[0] == 0x01)) {
            Log.d("Version error", "Wrong data format. Could not import anything.");
            return true;
        }
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
            for (int i = 0; i < kgkKeyIv.length; i++) {
                kgkKeyIv[i] = 0x00;
            }
            for (int i = 0; i < kgkBlock.length; i++) {
                kgkBlock[i] = 0x00;
            }
            for (int i = 0; i < salt.length; i++) {
                salt[i] = 0x00;
            }
            byte[] salt2 = Arrays.copyOfRange(kgkData, 0, 32);
            byte[] iv2 = Arrays.copyOfRange(kgkData, 32, 48);
            byte[] kgk = Arrays.copyOfRange(kgkData, 48, 112);
            for (int i = 0; i < kgkData.length; i++) {
                kgkData[i] = 0x00;
            }
            byte[] settingsKey = Crypter.createKey(kgk, salt2);
            for (int i = 0; i < salt2.length; i++) {
                salt2[i] = 0x00;
            }
            for (int i = 0; i < kgk.length; i++) {
                kgk[i] = 0x00;
            }
            byte[] settingsKeyIv = new byte[48];
            for (int i = 0; i < settingsKey.length; i++) {
                settingsKeyIv[i] = settingsKey[i];
                settingsKey[i] = 0x00;
            }
            for (int i = settingsKey.length; i < settingsKey.length + iv2.length; i++) {
                settingsKeyIv[i] = iv2[i - settingsKey.length];
                iv2[i - settingsKey.length] = 0x00;
            }
            Crypter settingsCrypter = new Crypter(settingsKeyIv);
            byte[] decryptedSettings = settingsCrypter.decrypt(encryptedSettings);
            if (decryptedSettings.length <= 0) {
                Toast.makeText(contentContext, R.string.sync_wrong_password,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            for (int i = 0; i < settingsKeyIv.length; i++) {
                settingsKeyIv[i] = 0x00;
            }
            String jsonString = Packer.decompress(decryptedSettings);
            try {
                JSONObject loadedSettings = new JSONObject(jsonString);
                boolean updateRemote = false;
                Iterator<String> loadedSettingsIterator = loadedSettings.keys();
                while (loadedSettingsIterator.hasNext()) {
                    JSONObject loadedSetting = loadedSettings.getJSONObject(
                            loadedSettingsIterator.next());
                    boolean found = false;
                    for (String domain : this.getDomainList()) {
                        PasswordSetting setting = this.getSetting(domain);
                        if (setting.getDomain().equals(loadedSetting.getString("domain"))) {
                            found = true;
                            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                                    Locale.ENGLISH);
                            Date modifiedRemote = df.parse(loadedSetting.getString("mDate"));
                            if (modifiedRemote.after(setting.getMDate())) {
                                setting.loadFromJSON(loadedSetting);
                                this.setSetting(setting);
                            } else {
                                updateRemote = true;
                            }
                            break;
                        }
                    }
                    if (!found) {
                        PasswordSetting newSetting = new PasswordSetting(
                                loadedSetting.getString("domain"));
                        newSetting.loadFromJSON(loadedSetting);
                        this.setSetting(newSetting);
                    }
                }
                for (String domain : this.getDomainList()) {
                    PasswordSetting setting = this.getSetting(domain);
                    boolean found = false;
                    Iterator<String> loadedSettingsIterator2 = loadedSettings.keys();
                    while (loadedSettingsIterator2.hasNext()) {
                        JSONObject loadedSetting = loadedSettings.getJSONObject(
                                loadedSettingsIterator2.next());
                        if (setting.getDomain().equals(loadedSetting.getString("domain"))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found && setting.isSynced()) {
                        updateRemote = true;
                    }
                }
                this.storeLocalSettings(password);
                return updateRemote;
            } catch (JSONException e) {
                Log.d("Update settings error", "Unable to read JSON data.");
                e.printStackTrace();
                return false;
            } catch (ParseException e) {
                Log.d("Update settings error", "Unable to parse the date.");
                e.printStackTrace();
                return false;
            }
        } catch (NoSuchPaddingException paddingError) {
            paddingError.printStackTrace();
            return false;
        }
    }

    public void setAllSettingsToSynced() {
        for (PasswordSetting setting : this.settings) {
            setting.setSynced(true);
        }
    }
}
