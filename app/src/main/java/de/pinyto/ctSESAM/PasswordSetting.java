package de.pinyto.ctSESAM;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * PasswordSetting wraps a set of settings for one domain.
 */
public class PasswordSetting {
    private String domain;
    private String username;
    private String legacyPassword;
    private boolean useLowerCase = true;
    private boolean useUpperCase = true;
    private boolean useDigits = true;
    private boolean useExtra = true;
    private boolean useCustom = false;
    private boolean avoidAmbiguous = true;
    private final String defaultCharacterSetLowerCase = "abcdefghijklmnopqrstuvwxyz";
    private final String defaultCharacterSetUpperCase = "ABCDEFGHJKLMNPQRTUVWXYZ";
    private final String defaultCharacterSetDigits = "0123456789";
    private final String defaultCharacterSetExtra = "#!\"§$%&/()[]{}=-_+*<>;:.";
    private String customCharacterSet;
    private int iterations = 4096;
    private int length = 10;
    private final byte[] defaultSalt = new byte[] { 0x70, 0x65, 0x70, 0x70, 0x65, 0x72 };
    private byte[] salt;
    private Date cDate;
    private Date mDate;
    private String notes;
    private String url;
    private String reserved;
    private boolean synced = false;

    PasswordSetting(String domain) {
        this.domain = domain;
        this.salt = defaultSalt;
        this.cDate = Calendar.getInstance().getTime();
        this.mDate = this.cDate;
    }

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUsername() {
        if (this.username != null) {
            return this.username;
        } else {
            return "";
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLegacyPassword() {
        if (this.legacyPassword != null) {
            return this.legacyPassword;
        } else {
            return "";
        }
    }

    public void setLegacyPassword(String legacyPassword) {
        this.legacyPassword = legacyPassword;
    }

    public boolean useLetters() {
        return this.useLowerCase && this.useUpperCase;
    }

    public void setUseLetters(boolean useLetters) {
        this.useLowerCase = useLetters;
        this.useUpperCase = useLetters;
    }

    public boolean useLowerCase() {
        return this.useLowerCase;
    }

    public void setUseUpperCase(boolean useUpperCase) {
        this.useUpperCase = useUpperCase;
    }

    public boolean useUpperCase() {
        return this.useUpperCase;
    }

    public void setUseLowerCase(boolean useLowerCase) {
        this.useLowerCase = useLowerCase;
    }

    public boolean useDigits() {
        return this.useDigits;
    }

    public void setUseDigits(boolean useDigits) {
        this.useDigits = useDigits;
    }

    public boolean useExtra() {
        return this.useExtra;
    }

    public void setUseExtra(boolean useExtra) {
        this.useExtra = useExtra;
    }

    public boolean useCustomCharacterSet() {
        return this.useCustom;
    }

    public boolean avoidAmbiguousCharacters() {
        return this.avoidAmbiguous;
    }

    public void setAvoidAmbiguousCharacters(boolean avoidAmbiguous) {
        this.avoidAmbiguous = avoidAmbiguous;
    }

    public String getCharacterSetAsString() {
        if (this.customCharacterSet != null) {
            return this.customCharacterSet;
        } else {
            return this.getDefaultCharacterSet();
        }
    }

    public String getCustomCharacterSet() {
        if (this.customCharacterSet != null) {
            return this.customCharacterSet;
        } else {
            return "";
        }
    }

    public String getDefaultCharacterSet() {
        String set = "";
        if (useLowerCase) {
            set = set + this.defaultCharacterSetLowerCase;
        }
        if (useUpperCase) {
            set = set + this.defaultCharacterSetUpperCase;
        }
        if (useDigits) {
            set = set + this.defaultCharacterSetDigits;
        }
        if (useExtra) {
            set = set + this.defaultCharacterSetExtra;
        }
        return set;
    }

    public void setCustomCharacterSet(String characterSet) {
        if (characterSet.equals(this.getDefaultCharacterSet())) {
            this.customCharacterSet = null;
            this.useCustom = false;
        } else {
            this.useCustom = true;
            this.customCharacterSet = characterSet;
        }
    }

    public List<String> getCharacterSet() {
        List<String> characterSet = new ArrayList<>();
        String characters;
        if (this.useCustomCharacterSet()) {
            characters = this.getCustomCharacterSet();
        } else {
            characters = this.getDefaultCharacterSet();
        }
        for (int i = 0; i < characters.length(); i++) {
            characterSet.add(Character.toString(characters.charAt(i)));
        }
        return characterSet;
    }

    public byte[] getSalt() {
        return this.salt;
    }

    public byte[] getDefaultSalt() {
        return this.defaultSalt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public int getLength() {
        return this.length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getIterations() {
        return this.iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public String getCreationDate() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        return df.format(this.cDate);
    }

    public void setCreationDate(String cDate) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        try {
            this.cDate = df.parse(cDate);
        } catch (ParseException e) {
            System.out.println("This date has a wrong format: " + cDate);
            e.printStackTrace();
            this.cDate = Calendar.getInstance().getTime();
        }
        if (this.cDate.compareTo(this.mDate) > 0) {
            this.mDate = this.cDate;
        }
    }

    public String getModificationDate() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        return df.format(this.mDate);
    }

    public Date getMDate() {
        return this.mDate;
    }

    public void setModificationDate(String mDate) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        try {
            this.mDate = df.parse(mDate);
        } catch (ParseException e) {
            System.out.println("This date has a wrong format: " + mDate);
            e.printStackTrace();
            this.mDate = Calendar.getInstance().getTime();
        }
        if (this.cDate.compareTo(this.mDate) > 0) {
            System.out.println("The modification date was before the creation Date. " +
                    "Set the creation date to the earlier date.");
            this.cDate = this.mDate;
        }
        this.synced = false;
    }

    public void setModificationDateToNow() {
        this.mDate = Calendar.getInstance().getTime();
        if (this.cDate.compareTo(this.mDate) > 0) {
            System.out.println("The modification date was before the creation Date. " +
                    "Set the creation date to the earlier date.");
            this.cDate = this.mDate;
        }
        this.synced = false;
    }

    public String getNotes() {
        if (this.notes != null) {
            return this.notes;
        } else {
            return "";
        }
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getUrl() {
        if (this.url != null) {
            return this.url;
        } else {
            return "";
        }
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getReserved() {
        if (this.reserved != null) {
            return this.reserved;
        } else {
            return "";
        }
    }

    public void setReserved(String reserved) {
        this.reserved = reserved;
    }

    public boolean isSynced() {
        return this.synced;
    }

    public void setSynced(boolean isSynced) {
        this.synced = isSynced;
    }

    public JSONObject toJSON() {
        JSONObject domainObject = new JSONObject();
        try {
            domainObject.put("domain", this.getDomain());
            if (this.url != null && this.url.length() > 0) {
                domainObject.put("url", this.getUrl());
            }
            if (this.username != null && this.username.length() > 0) {
                domainObject.put("username", this.getUsername());
            }
            if (this.legacyPassword != null && this.legacyPassword.length() > 0) {
                domainObject.put("legacyPassword", this.getLegacyPassword());
            }
            if (this.notes != null && this.notes.length() > 0) {
                domainObject.put("notes", this.getNotes());
            }
            domainObject.put("iterations", this.getIterations());
            domainObject.put("salt", Base64.encodeToString(this.getSalt(), Base64.DEFAULT));
            domainObject.put("length", this.getLength());
            domainObject.put("cDate", this.getCreationDate());
            domainObject.put("mDate", this.getModificationDate());
            domainObject.put("usedCharacters", this.getCharacterSetAsString());
            if (this.reserved != null && this.reserved.length() > 0) {
                domainObject.put("reserved", this.getReserved());
            }
        } catch (JSONException e) {
            System.out.println("Settings packing error: Unable to pack the JSON data.");
        }
        return domainObject;
    }

    public void loadFromJSON(JSONObject loadedSetting) throws JSONException {
        if (loadedSetting.has("domain")) {
            this.setDomain(loadedSetting.getString("domain"));
        }
        if (loadedSetting.has("url")) {
            this.setUrl(loadedSetting.getString("url"));
        }
        if (loadedSetting.has("username")) {
            this.setUsername(loadedSetting.getString("username"));
        }
        if (loadedSetting.has("legacyPassword")) {
            this.setLegacyPassword(loadedSetting.getString("legacyPassword"));
        }
        if (loadedSetting.has("notes")) {
            this.setNotes(loadedSetting.getString("notes"));
        }
        if (loadedSetting.has("iterations")) {
            this.setIterations(loadedSetting.getInt("iterations"));
        }
        if (loadedSetting.has("salt")) {
            this.setSalt(Base64.decode(loadedSetting.getString("salt"), Base64.DEFAULT));
        }
        if (loadedSetting.has("length")) {
            this.setLength(loadedSetting.getInt("length"));
        }
        if (loadedSetting.has("cDate")) {
            this.setCreationDate(loadedSetting.getString("cDate"));
        }
        if (loadedSetting.has("mDate")) {
            this.setModificationDate(loadedSetting.getString("mDate"));
        }
        if (loadedSetting.has("usedCharacters")) {
            this.setCustomCharacterSet(loadedSetting.getString("usedCharacters"));
        }
        if (loadedSetting.has("reserved")) {
            this.setReserved(loadedSetting.getString("reserved"));
        }
    }
}
