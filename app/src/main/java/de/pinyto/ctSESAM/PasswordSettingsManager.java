package de.pinyto.ctSESAM;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Use this class to manage password settings. It will store them internally and it will also
 * pack them for synchronization.
 */
public class PasswordSettingsManager {
    private SharedPreferences savedDomains;
    private Context contentContext;
    private Set<PasswordSetting> settings;

    PasswordSettingsManager(Context contentContext) {
        this.contentContext = contentContext;
        this.savedDomains = contentContext.getSharedPreferences(
                "savedDomains", Context.MODE_PRIVATE);
        this.settings = new HashSet<>();
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

    public void loadLocalSettings(KgkManager kgkManager) throws WrongPasswordException {
        Crypter settingsCrypter = this.getSettingsCrypter(kgkManager);
        byte[] encrypted = Base64.decode(
                this.savedDomains.getString("encryptedSettings", ""),
                Base64.DEFAULT);
        if (encrypted.length < 40) {
            return;
        }
        byte[] decrypted = settingsCrypter.decrypt(encrypted);
        if (decrypted.length < 35) {
            throw new WrongPasswordException("wrong length: too short");
        }
        String decompressedSettings = Packer.decompress(decrypted);
        if (decompressedSettings.length() <= 0) {
            throw new WrongPasswordException("unable to decompress");
        }
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
        } catch (JSONException jsonError) {
            Log.d("Settings loading error", "The loaded settings are not in JSON format.");
            jsonError.printStackTrace();
        } catch (ParseException timeFormatError) {
            Log.d("Settings loading error",
                    "The loaded settings contain time information in a wrong format.");
            timeFormatError.printStackTrace();
        }
    }

    public void storeLocalSettings(KgkManager kgkManager) {
        kgkManager.freshSalt2();
        kgkManager.freshIv2();
        Crypter settingsCrypter = this.getSettingsCrypter(kgkManager);
        JSONObject storeStructure = new JSONObject();
        try {
            storeStructure.put("settings", this.getSettingsAsJSON());
            storeStructure.put("synced", this.getSyncedSettings());
        } catch (JSONException jsonError) {
            Log.d("Settings saving error", "Could not construct JSON structure for storage.");
            jsonError.printStackTrace();
        }
        SharedPreferences.Editor savedDomainsEditor = savedDomains.edit();
        Log.d("storing", storeStructure.toString());
        Log.d("compressed length", Integer.toString(Packer.compress(storeStructure.toString()).length));
        byte[] encryptedSettings = settingsCrypter.encrypt(
                Packer.compress(storeStructure.toString()));
        Log.d("cyphertext length", Integer.toString(encryptedSettings.length));
        savedDomainsEditor.putString("encryptedSettings",
                Base64.encodeToString(
                        encryptedSettings,
                        Base64.DEFAULT));
        savedDomainsEditor.apply();
        kgkManager.storeLocalKgkBlock();
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

    public byte[] getExportData(KgkManager kgkManager) {
        kgkManager.freshIv2();
        kgkManager.freshSalt2();
        byte[] kgkBlock = kgkManager.getEncryptedKgk();
        Crypter settingsCrypter = this.getSettingsCrypter(kgkManager);
        byte[] encryptedSettings = settingsCrypter.encrypt(
                Packer.compress(
                        this.getSettingsAsJSON().toString()
                )
        );
        byte[] salt = kgkManager.getKgkCrypterSalt();
        byte[] exportData = new byte[1 + salt.length + kgkBlock.length + encryptedSettings.length];
        exportData[0] = 0x01;
        System.arraycopy(salt, 0, exportData, 1, salt.length);
        System.arraycopy(kgkBlock, 0, exportData, 1 + salt.length, kgkBlock.length);
        System.arraycopy(encryptedSettings, 0,
                exportData, 1 + salt.length + kgkBlock.length, encryptedSettings.length);
        return exportData;
    }

    public boolean updateFromExportData(KgkManager kgkManager, byte[] blob) {
        if (!(blob[0] == 0x01)) {
            Log.d("Version error", "Wrong data format. Could not import anything.");
            return true;
        }
        byte[] encryptedSettings = Arrays.copyOfRange(blob, 145, blob.length);
        Crypter settingsCrypter = this.getSettingsCrypter(kgkManager);
        byte[] decryptedSettings = settingsCrypter.decrypt(encryptedSettings);
        if (decryptedSettings.length <= 0) {
            Toast.makeText(contentContext, R.string.sync_wrong_password,
                    Toast.LENGTH_SHORT).show();
            return false;
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
                        setting.setSynced(true);
                        break;
                    }
                }
                if (!found) {
                    PasswordSetting newSetting = new PasswordSetting(
                            loadedSetting.getString("domain"));
                    newSetting.loadFromJSON(loadedSetting);
                    newSetting.setSynced(true);
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
            this.storeLocalSettings(kgkManager);
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
    }

    public void setAllSettingsToSynced() {
        for (PasswordSetting setting : this.settings) {
            setting.setSynced(true);
        }
    }

    public void deleteAllSettings() {
        SharedPreferences.Editor savedDomainsEditor = savedDomains.edit();
        savedDomainsEditor.putString("encryptedSettings",
                Base64.encodeToString(
                        new byte[] {},
                        Base64.DEFAULT));
        savedDomainsEditor.apply();
    }
}
