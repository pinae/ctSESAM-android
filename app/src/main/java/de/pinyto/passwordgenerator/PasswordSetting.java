package de.pinyto.passwordgenerator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
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
    private byte[] salt;
    private boolean forceLowerCase = false;
    private boolean forceUpperCase = false;
    private boolean forceDigits = false;
    private boolean forceExtra = false;
    private boolean forceRegexValidation = false;
    private String validatorRegEx;
    private Date cDate;
    private Date mDate;
    private String notes;
    private boolean synced = false;

    PasswordSetting(String domain) {
        this.domain = domain;
        try {
            this.salt = "pepper".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("UTF-8 is not supported. Using default encoding.");
            e.printStackTrace();
            this.salt = "pepper".getBytes();
        }
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

    public void setUseUpperCase(boolean useUpperCase) {
        this.useUpperCase = useUpperCase;
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

    public boolean isSynced() {
        return this.synced;
    }

    public void setSynced(boolean isSynced) {
        this.synced = isSynced;
    }

    public JSONObject toJSON() {
        JSONObject domainObject = new JSONObject();
        try {
            domainObject.put("domain", this.domain);
            domainObject.put("useLowerCase", this.useLowerCase);
            domainObject.put("useUpperCase", this.useUpperCase);
            domainObject.put("useDigits", this.useDigits);
            domainObject.put("useExtra", this.useExtra);
            domainObject.put("iterations", this.iterations);
            domainObject.put("length", this.length);
            domainObject.put("cDate", this.getCreationDate());
            domainObject.put("mDate", this.getModificationDate());
        } catch (JSONException e) {
            System.out.println("Settings packing error: Unable to pack the JSON data.");
        }
        return domainObject;
    }

    public void loadFromJSON(JSONObject loadedSetting) throws JSONException {
        if (loadedSetting.has("domain")) {
            this.setDomain(loadedSetting.getString("domain"));
        }
        if (loadedSetting.has("cDate")) {
            this.setCreationDate(loadedSetting.getString("cDate"));
        }
        if (loadedSetting.has("mDate")) {
            this.setModificationDate(loadedSetting.getString("mDate"));
        }
        if (loadedSetting.has("iterations")) {
            this.setIterations(loadedSetting.getInt("iterations"));
        }
        if (loadedSetting.has("length")) {
            this.setLength(loadedSetting.getInt("length"));
        }
        if (loadedSetting.has("useUpperCase")) {
            this.setUseUpperCase(loadedSetting.getBoolean("useUpperCase"));
        }
        if (loadedSetting.has("useLowerCase")) {
            this.setUseLowerCase(loadedSetting.getBoolean("useLowerCase"));
        }
        if (loadedSetting.has("useDigits")) {
            this.setUseDigits(loadedSetting.getBoolean("useDigits"));
        }
        if (loadedSetting.has("useExtra")) {
            this.setUseExtra(loadedSetting.getBoolean("useExtra"));
        }
    }
}
