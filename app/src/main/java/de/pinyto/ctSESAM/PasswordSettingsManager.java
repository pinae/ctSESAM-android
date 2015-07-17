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

    PasswordSettingsManager(Context contentContext) {
        this.contentContext = contentContext;
        savedDomains = contentContext.getSharedPreferences("savedDomains", Context.MODE_PRIVATE);
    }

    public PasswordSetting getSetting(String domain) {
        Set<String> domainSet = savedDomains.getStringSet(
                "domainSet",
                new HashSet<String>()
        );
        PasswordSetting setting = new PasswordSetting(domain);
        if (domainSet != null) {
            if (domainSet.contains(domain)) {
                setting.setUseLowerCase(savedDomains.getBoolean(domain + "_useLowerCase", true));
                setting.setUseUpperCase(savedDomains.getBoolean(domain + "_useUpperCase", true));
                setting.setUseDigits(savedDomains.getBoolean(domain + "_digits", true));
                setting.setUseExtra(savedDomains.getBoolean(domain + "_special_characters", true));
                setting.setLength(savedDomains.getInt(domain + "_length", 10));
                setting.setIterations(savedDomains.getInt(domain + "_iterations", 4096));
                String customCharacterSet = savedDomains.getString(
                        domain + "_customCharacterSet", "");
                if (customCharacterSet != null && customCharacterSet.length() > 0) {
                    setting.setCustomCharacterSet(customCharacterSet);
                }
                String username = savedDomains.getString(domain + "_username", "");
                if (username != null && username.length() > 0) {
                    setting.setUsername(username);
                }
                String legacyPassword = savedDomains.getString(domain + "_legacyPassword", "");
                if (legacyPassword != null && legacyPassword.length() > 0) {
                    setting.setLegacyPassword(legacyPassword);
                }
                setting.setAvoidAmbiguousCharacters(savedDomains.getBoolean(
                        domain + "_avoidAmbiguousCharacters", true));
                String salt = savedDomains.getString(domain + "_salt", "");
                if (salt != null && salt.length() > 0) {
                    setting.setSalt(Base64.decode(salt, Base64.DEFAULT));
                }
                String notes = savedDomains.getString(domain + "_notes", "");
                if (notes != null && notes.length() > 0) {
                    setting.setNotes(notes);
                }
                String dateString = savedDomains.getString(domain + "_cDate", "");
                if (dateString != null && dateString.length() > 0) {
                    setting.setCreationDate(dateString);
                }
                dateString = savedDomains.getString(domain + "_mDate", "");
                if (dateString != null && dateString.length() > 0) {
                    setting.setModificationDate(dateString);
                }
                setting.setSynced(savedDomains.getBoolean(domain + "_synced", false));
            }
        }
        return setting;
    }

    public void saveSetting(PasswordSetting newSetting) {
        Set<String> domainSet = savedDomains.getStringSet(
                "domainSet",
                new HashSet<String>()
        );
        if ((domainSet != null) && (!domainSet.contains(newSetting.getDomain()))) {
            domainSet.add(newSetting.getDomain());
        }
        SharedPreferences.Editor savedDomainsEditor = savedDomains.edit();
        savedDomainsEditor.putStringSet("domainSet", domainSet);
        savedDomainsEditor.putBoolean(
                newSetting.getDomain() + "_useLowerCase",
                newSetting.useLowerCase()
        );
        savedDomainsEditor.putBoolean(
                newSetting.getDomain() + "_useUpperCase",
                newSetting.useUpperCase()
        );
        savedDomainsEditor.putBoolean(
                newSetting.getDomain() + "_digits",
                newSetting.useDigits()
        );
        savedDomainsEditor.putBoolean(
                newSetting.getDomain() + "_special_characters",
                newSetting.useExtra()
        );
        savedDomainsEditor.putInt(
                newSetting.getDomain() + "_length",
                newSetting.getLength()
        );
        savedDomainsEditor.putInt(
                newSetting.getDomain() + "_iterations",
                newSetting.getIterations()
        );
        savedDomainsEditor.putBoolean(
                newSetting.getDomain() + "_useCustomCharacterSet",
                newSetting.useCustomCharacterSet()
        );
        if (newSetting.useCustomCharacterSet()) {
            savedDomainsEditor.putString(
                    newSetting.getDomain() + "_customCharacterSet",
                    newSetting.getCustomCharacterSet()
            );
        }
        savedDomainsEditor.putString(
                newSetting.getDomain() + "_username",
                newSetting.getUsername()
        );
        savedDomainsEditor.putString(
                newSetting.getDomain() + "_legacyPassword",
                newSetting.getLegacyPassword()
        );
        savedDomainsEditor.putBoolean(
                newSetting.getDomain() + "_avoidAmbiguousCharacters",
                newSetting.avoidAmbiguousCharacters()
        );
        if (newSetting.getSalt().length > 0 &&
                newSetting.getSalt() != newSetting.getDefaultSalt()) {
            savedDomainsEditor.putString(
                    newSetting.getDomain() + "_salt",
                    Base64.encodeToString(newSetting.getSalt(), Base64.DEFAULT)
            );
        }
        savedDomainsEditor.putString(
                newSetting.getDomain() + "_notes",
                newSetting.getNotes()
        );
        savedDomainsEditor.putString(
                newSetting.getDomain() + "_cDate",
                newSetting.getCreationDate()
        );
        savedDomainsEditor.putString(
                newSetting.getDomain() + "_mDate",
                newSetting.getModificationDate()
        );
        savedDomainsEditor.putBoolean(
                newSetting.getDomain() + "_synced",
                newSetting.isSynced()
        );
        savedDomainsEditor.apply();
    }

    public void deleteSetting(String domain) {
        Set<String> domainSet = savedDomains.getStringSet(
                "domainSet",
                new HashSet<String>()
        );
        if ((domainSet != null) && (domainSet.contains(domain))) {
            domainSet.remove(domain);
        }
        SharedPreferences.Editor savedDomainsEditor = savedDomains.edit();
        savedDomainsEditor.putStringSet("domainSet", domainSet);
        if (savedDomains.contains(domain + "_useLowerCase")) {
            savedDomainsEditor.remove(domain + "_useLowerCase");
        }
        if (savedDomains.contains(domain + "_useUpperCase")) {
            savedDomainsEditor.remove(domain + "_useUpperCase");
        }
        if (savedDomains.contains(domain + "_digits")) {
            savedDomainsEditor.remove(domain + "_digits");
        }
        if (savedDomains.contains(domain + "_special_characters")) {
            savedDomainsEditor.remove(domain + "_special_characters");
        }
        if (savedDomains.contains(domain + "_length")) {
            savedDomainsEditor.remove(domain + "_length");
        }
        if (savedDomains.contains(domain + "_iterations")) {
            savedDomainsEditor.remove(domain + "_iterations");
        }
        if (savedDomains.contains(domain + "_useCustomCharacterSet")) {
            savedDomainsEditor.remove(domain + "_useCustomCharacterSet");
        }
        if (savedDomains.contains(domain + "_customCharacterSet")) {
            savedDomainsEditor.remove(domain + "_customCharacterSet");
        }
        if (savedDomains.contains(domain + "_username")) {
            savedDomainsEditor.remove(domain + "_username");
        }
        if (savedDomains.contains(domain + "_legacyPassword")) {
            savedDomainsEditor.remove(domain + "_legacyPassword");
        }
        if (savedDomains.contains(domain + "_avoidAmbiguousCharacters")) {
            savedDomainsEditor.remove(domain + "_avoidAmbiguousCharacters");
        }
        if (savedDomains.contains(domain + "_salt")) {
            savedDomainsEditor.remove(domain + "_salt");
        }
        if (savedDomains.contains(domain + "_notes")) {
            savedDomainsEditor.remove(domain + "_notes");
        }
        if (savedDomains.contains(domain + "_cDate")) {
            savedDomainsEditor.remove(domain + "_cDate");
        }
        if (savedDomains.contains(domain + "_mDate")) {
            savedDomainsEditor.remove(domain + "_mDate");
        }
        if (savedDomains.contains(domain + "_synced")) {
            savedDomainsEditor.remove(domain + "_synced");
        }
        savedDomainsEditor.apply();
    }

    public String[] getDomainList() {
        Set<String> domainSet = savedDomains.getStringSet("domainSet", new HashSet<String>());
        String[] domainList;
        if (domainSet != null) {
            domainList = new String[domainSet.size()];
            Iterator it = domainSet.iterator();
            int i = 0;
            while (it.hasNext()) {
                domainList[i] = (String) it.next();
                i++;
            }
        } else {
            domainList = new String[] {};
        }
        return domainList;
    }

    private JSONArray getSettingsAsJSON() {
        JSONArray settings = new JSONArray();
        Set<String> domainSet = savedDomains.getStringSet(
                "domainSet",
                new HashSet<String>()
        );
        if (domainSet != null) {
            for (String domain : domainSet) {
                PasswordSetting domainSetting = getSetting(domain);
                settings.put(domainSetting.toJSON());
            }
        }
        return settings;
    }

    public byte[] getExportData(byte[] password) {
        byte[] compressedData = Packer.compress(this.getSettingsAsJSON().toString());
        Crypter crypter = new Crypter(password);
        return crypter.encrypt(compressedData);
    }

    public boolean updateFromExportData(byte[] password, byte[] blob) {
        Crypter crypter = new Crypter(password);
        byte[] decryptedBlob = crypter.decrypt(blob);
        if (decryptedBlob.length <= 0) {
            Toast.makeText(contentContext, R.string.wrong_password,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        String jsonString = Packer.decompress(decryptedBlob);
        try {
            JSONArray loadedSettings = new JSONArray(jsonString);
            boolean updateRemote = false;
            for (int i = 0; i < loadedSettings.length(); i++) {
                JSONObject loadedSetting = (JSONObject) loadedSettings.get(i);
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
                            this.saveSetting(setting);
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
                    this.saveSetting(newSetting);
                }
            }
            for (String domain : this.getDomainList()) {
                PasswordSetting setting = this.getSetting(domain);
                boolean found = false;
                for (int j = 0; j < loadedSettings.length(); j++) {
                    JSONObject loadedSetting = loadedSettings.getJSONObject(j);
                    if (setting.getDomain().equals(loadedSetting.getString("domain"))) {
                        found = true;
                        break;
                    }
                }
                if (!found && setting.isSynced()) {
                    updateRemote = true;
                }
            }
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
        Set<String> domainSet = savedDomains.getStringSet("domainSet", new HashSet<String>());
        if (domainSet != null) {
            SharedPreferences.Editor savedDomainsEditor = savedDomains.edit();
            for (String domain : domainSet) {
                savedDomainsEditor.putBoolean(domain + "_synced", true);
            }
            savedDomainsEditor.apply();
        }
    }
}
