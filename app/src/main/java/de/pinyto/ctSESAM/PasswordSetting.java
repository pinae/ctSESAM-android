package de.pinyto.ctSESAM;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PasswordSetting wraps a set of settings for one domain.
 */
public class PasswordSetting {
    private String domain;
    private String url;
    private String username;
    private String legacyPassword;
    private String notes;
    private int iterations = 4096;
    private byte[] salt;
    private Date cDate;
    private Date mDate;
    private final String defaultCharacterSetDigits = "0123456789";
    private final String defaultCharacterSetLowerCase = "abcdefghijklmnopqrstuvwxyz";
    private final String defaultCharacterSetUpperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final String defaultCharacterSetExtra = "#!\"~|@^°$%&/()[]{}=-_+*<>;:.";
    private String characterSetExtra;
    private String template = "aAnoxxxxxx";
    private boolean synced = false;

    PasswordSetting(String domain) {
        this.domain = domain;
        this.salt = Crypter.createSalt();
        this.cDate = Calendar.getInstance().getTime();
        this.mDate = this.cDate;
        this.characterSetExtra = this.defaultCharacterSetExtra;
        this.calculateTemplate();
    }

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
        this.synced = false;
    }

    public boolean hasUsername() {
        return this.username != null && this.username.length() > 0;
    }

    public String getUsername() {
        if (this.username != null) {
            return this.username;
        } else {
            return "";
        }
    }

    public void setUsername(String username) {
        if (!username.equals(this.username)) {
            this.synced = false;
        }
        this.username = username;
    }

    public boolean hasLegacyPassword() {
        return this.legacyPassword != null && this.legacyPassword.length() > 0;
    }

    public String getLegacyPassword() {
        if (this.legacyPassword != null) {
            return this.legacyPassword;
        } else {
            return "";
        }
    }

    public void setLegacyPassword(String legacyPassword) {
        if (!legacyPassword.equals(this.legacyPassword)) {
            this.synced = false;
        }
        this.legacyPassword = legacyPassword;
    }

    public String getDefaultCharacterSet() {
        String set = "";
        set = set + this.defaultCharacterSetDigits;
        set = set + this.defaultCharacterSetLowerCase;
        set = set + this.defaultCharacterSetUpperCase;
        set = set + this.defaultCharacterSetExtra;
        return set;
    }

    public String getLowerCaseCharacterSetAsString() {
        return this.defaultCharacterSetLowerCase;
    }

    public String getUpperCaseCharacterSetAsString() {
        return this.defaultCharacterSetUpperCase;
    }

    public String getDigitsCharacterSetAsString() {
        return this.defaultCharacterSetDigits;
    }

    public List<String> getCharacterSet() {
        List<String> characterSet = new ArrayList<>();
        String characters = this.getCharacterSetAsString();
        for (int i = 0; i < characters.length(); i++) {
            characterSet.add(Character.toString(characters.charAt(i)));
        }
        return characterSet;
    }

    public String getCharacterSetAsString() {
        String set = "";
        if (this.getTemplate().contains("n")) {
            set = set + this.getDigitsCharacterSetAsString();
        }
        if (this.getTemplate().contains("a")) {
            set = set + this.getLowerCaseCharacterSetAsString();
        }
        if (this.getTemplate().contains("A")) {
            set = set + this.getUpperCaseCharacterSetAsString();
        }
        if (this.getTemplate().contains("o")) {
            set = set + this.getExtraCharacterSetAsString();
        }
        if (set.length() <= 0) {
            set = this.getDefaultCharacterSet();
        }
        return set;
    }

    public void setExtraCharacterSet(String extraCharacterSet) {
        if (extraCharacterSet == null || extraCharacterSet.length() <= 0) {
            this.characterSetExtra = this.defaultCharacterSetExtra;
        } else {
            this.characterSetExtra = extraCharacterSet;
        }
    }

    public List<String> getExtraCharacterSet() {
        List<String> extraCharacterSet = new ArrayList<>();
        String characters = this.getExtraCharacterSetAsString();
        for (int i = 0; i < characters.length(); i++) {
            extraCharacterSet.add(Character.toString(characters.charAt(i)));
        }
        return extraCharacterSet;
    }

    public String getExtraCharacterSetAsString() {
        if (this.characterSetExtra != null) {
            return this.characterSetExtra;
        } else {
            return this.defaultCharacterSetExtra;
        }
    }

    public List<String> getDigitsCharacterSet() {
        List<String> digitsCharacterSet = new ArrayList<>();
        String characters = this.defaultCharacterSetDigits;
        for (int i = 0; i < characters.length(); i++) {
            digitsCharacterSet.add(Character.toString(characters.charAt(i)));
        }
        return digitsCharacterSet;
    }

    public List<String> getLowerCaseLettersCharacterSet() {
        List<String> lowerCaseLettersCharacterSet = new ArrayList<>();
        String characters = this.defaultCharacterSetLowerCase;
        for (int i = 0; i < characters.length(); i++) {
            lowerCaseLettersCharacterSet.add(Character.toString(characters.charAt(i)));
        }
        return lowerCaseLettersCharacterSet;
    }

    public List<String> getUpperCaseLettersCharacterSet() {
        List<String> upperCaseLettersCharacterSet = new ArrayList<>();
        String characters = this.defaultCharacterSetUpperCase;
        for (int i = 0; i < characters.length(); i++) {
            upperCaseLettersCharacterSet.add(Character.toString(characters.charAt(i)));
        }
        return upperCaseLettersCharacterSet;
    }

    public byte[] getSalt() {
        return this.salt;
    }

    public void setSalt(byte[] salt) {
        if (!Arrays.equals(salt, this.salt)) {
            this.synced = false;
        }
        this.salt = salt;
    }

    public int getLength() {
        return this.getTemplate().length();
    }

    public int getIterations() {
        return this.iterations;
    }

    public void setIterations(int iterations) {
        if (iterations != this.iterations) {
            this.synced = false;
        }
        this.iterations = iterations;
    }

    public Date getCDate() {
        return this.cDate;
    }

    public String getCreationDate() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        return df.format(this.cDate);
    }

    public void setCreationDate(String cDate) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        try {
            if (df.parse(cDate).compareTo(this.cDate) != 0) {
                this.synced = false;
            }
            this.cDate = df.parse(cDate);
        } catch (ParseException e) {
            System.out.println("This date has a wrong format: " + cDate);
            e.printStackTrace();
        }
        if (this.cDate.compareTo(this.mDate) > 0) {
            this.mDate = this.cDate;
            this.synced = false;
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
            if (df.parse(mDate).compareTo(this.mDate) != 0) {
                this.synced = false;
            }
            this.mDate = df.parse(mDate);
        } catch (ParseException e) {
            System.out.println("This date has a wrong format: " + mDate);
            e.printStackTrace();
        }
        if (this.cDate.compareTo(this.mDate) > 0) {
            System.out.println("The modification date was before the creation Date. " +
                    "Set the creation date to the earlier date.");
            this.cDate = this.mDate;
            this.synced = false;
        }
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
        if (!notes.equals(this.notes)) {
            this.synced = false;
        }
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
        if (!url.equals(this.url)) {
            this.synced = false;
        }
        this.url = url;
    }

    public String getFullTemplate() {
        int complexity = this.getComplexity();
        if (complexity >= 0) {
            return Integer.toString(complexity) + ";" + this.getTemplate();
        } else {
            return "";
        }
    }

    private String ShuffleString(String s)
    {
        int index;
        char temp;
        char[] array = s.toCharArray();
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--)
        {
            index = random.nextInt(i + 1);
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
        return String.valueOf(array);
    }

    private void calculateTemplate(boolean useLowerCase,
                                   boolean useUpperCase,
                                   boolean useDigits,
                                   boolean useExtra) {
        int targetLength = this.getLength();
        this.template = "";
        boolean aInserted = false;
        boolean AInserted = false;
        boolean nInserted = false;
        boolean oInserted = false;
        for (int i = 0; i < targetLength; i++) {
            if (useLowerCase && !aInserted) {
                this.template = this.template + "a";
                aInserted = true;
            } else if (useUpperCase && !AInserted) {
                this.template = this.template + "A";
                AInserted = true;
            } else if (useDigits && !nInserted) {
                this.template = this.template + "n";
                nInserted = true;
            } else if (useExtra && !oInserted) {
                this.template = this.template + "o";
                oInserted = true;
            } else {
                this.template = this.template + "x";
            }
        }
        this.template = this.ShuffleString(this.template);
    }

    private void calculateTemplate() {
        boolean use_lower_case = this.getTemplate().contains("a");
        boolean use_upper_case = this.getTemplate().contains("A");
        boolean use_digits = this.getTemplate().contains("n");
        boolean use_extra = this.getTemplate().contains("o");
        if (!use_lower_case && !use_upper_case && !use_digits && !use_extra) {
            use_lower_case = true;
            use_upper_case = true;
            use_digits = true;
            use_extra = true;
        }
        this.calculateTemplate(use_lower_case, use_upper_case, use_digits, use_extra);
    }

    public String getTemplate() {
        return this.template;
    }

    public void setTemplate(String template) {
        Matcher matcher = Pattern.compile("(([01234567]);)?([aAnox]+)").matcher(template);
        if (matcher.matches() && matcher.groupCount() >= 3) {
            if (matcher.group(2) != null) {
                this.setComplexity(Integer.parseInt(matcher.group(2)));
            }
            this.template = matcher.group(3);
        }
    }

    public int getComplexity() {
        if (this.getTemplate().contains("n") && !this.getTemplate().contains("a") &&
                !this.getTemplate().contains("A") && !this.getTemplate().contains("o")) {
            return 0;
        } else if (!this.getTemplate().contains("n") && this.getTemplate().contains("a") &&
                !this.getTemplate().contains("A") && !this.getTemplate().contains("o")) {
            return 1;
        } else if (!this.getTemplate().contains("n") && !this.getTemplate().contains("a") &&
                this.getTemplate().contains("A") && !this.getTemplate().contains("o")) {
            return 2;
        } else if (this.getTemplate().contains("n") && this.getTemplate().contains("a") &&
                !this.getTemplate().contains("A") && !this.getTemplate().contains("o")) {
            return 3;
        } else if (!this.getTemplate().contains("n") && this.getTemplate().contains("a") &&
                this.getTemplate().contains("A") && !this.getTemplate().contains("o")) {
            return 4;
        } else if (this.getTemplate().contains("n") && this.getTemplate().contains("a") &&
                this.getTemplate().contains("A") && !this.getTemplate().contains("o")) {
            return 5;
        } else if (this.getTemplate().contains("n") && this.getTemplate().contains("a") &&
                this.getTemplate().contains("A") && this.getTemplate().contains("o")) {
            return 6;
        } else {
            return -1;
        }
    }

    public void setComplexity(int complexity) {
        if (complexity < 0 || complexity > 7) {
            Log.e("Complexity error", "Illegal complexity: " + Integer.toString(complexity));
        }
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
            if (this.salt != null && this.salt.length > 0) {
                domainObject.put("salt", Base64.encodeToString(this.getSalt(), Base64.DEFAULT));
            }
            domainObject.put("cDate", this.getCreationDate());
            domainObject.put("mDate", this.getModificationDate());
            domainObject.put("extras", this.getExtraCharacterSetAsString());
            domainObject.put("passwordTemplate", this.getTemplate());
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
        if (loadedSetting.has("cDate")) {
            this.setCreationDate(loadedSetting.getString("cDate"));
        }
        if (loadedSetting.has("mDate")) {
            this.setModificationDate(loadedSetting.getString("mDate"));
        }
        if (loadedSetting.has("extras")) {
            this.setExtraCharacterSet(loadedSetting.getString("extras"));
        }
        if (loadedSetting.has("passwordTemplate")) {
            this.setTemplate(loadedSetting.getString("passwordTemplate"));
        }
        if (loadedSetting.has("length") && loadedSetting.has("usedCharacters") &&
                !loadedSetting.has("passwordTemplate")) {
            String conversionTemplate = "o";
            for (int i = 1; i < loadedSetting.getInt("length"); i++) {
                conversionTemplate = conversionTemplate + "x";
            }
            this.template = conversionTemplate;
            this.setExtraCharacterSet(loadedSetting.getString("usedCharacters"));
            this.calculateTemplate(false, false, false, true);
        }
    }
}
