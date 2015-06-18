package de.pinyto.passwordgenerator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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

    public boolean isSynced() {
        return this.synced;
    }

    public void setSynced(boolean isSynced) {
        this.synced = isSynced;
    }
}
