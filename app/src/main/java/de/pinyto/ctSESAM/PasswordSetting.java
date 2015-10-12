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
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PasswordSetting wraps a set of settings for one domain.
 */
public class PasswordSetting {
    private String domain;
    private String username;
    private String legacyPassword;
    private final String defaultCharacterSetLowerCase = "abcdefghijklmnopqrstuvwxyz";
    private final String defaultCharacterSetUpperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final String defaultCharacterSetDigits = "0123456789";
    private final String defaultCharacterSetExtra = "#!\"~|@^°$%&/()[]{}=-_+*<>;:.";
    private String characterSet;
    private String characterSetExtra;
    private int iterations = 4096;
    private int length = 10;
    private byte[] salt;
    private Date cDate;
    private Date mDate;
    private String notes;
    private String url;
    private String template;
    private boolean synced = false;

    PasswordSetting(String domain) {
        this.domain = domain;
        this.salt = Crypter.createSalt();
        this.cDate = Calendar.getInstance().getTime();
        this.mDate = this.cDate;
        this.characterSet = this.getDefaultCharacterSet();
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
        this.legacyPassword = legacyPassword;
    }

    public boolean useLetters() {
        return this.useLowerCase() && this.useUpperCase();
    }

    private void removeFromCharacterSet(String remstr) {
        String newSet = "";
        for (int i = 0; i < this.characterSet.length(); i++) {
            boolean addThisChar = true;
            for (int j = 0; j < remstr.length(); j++) {
                if (this.characterSet.charAt(i) == remstr.charAt(j)) {
                    addThisChar = false;
                }
            }
            if (addThisChar) {
                newSet += this.characterSet.charAt(i);
            }
        }
        this.characterSet = newSet;
    }

    private void removeFromCharacterSetExcept(String exceptStr) {
        String newSet = "";
        for (int i = 0; i < this.characterSet.length(); i++) {
            boolean addThisChar = false;
            for (int j = 0; j < exceptStr.length(); j++) {
                if (this.characterSet.charAt(i) == exceptStr.charAt(j)) {
                    addThisChar = true;
                }
            }
            if (addThisChar) {
                newSet += this.characterSet.charAt(i);
            }
        }
        this.characterSet = newSet;
    }

    public void setUseLetters(boolean useLetters) {
        boolean createNewTemplate = this.useLetters() != useLetters;
        this.removeFromCharacterSet(this.defaultCharacterSetLowerCase +
                this.defaultCharacterSetUpperCase);
        if (useLetters) {
            this.characterSet = this.defaultCharacterSetLowerCase +
                    this.defaultCharacterSetUpperCase + this.characterSet;
        }
        if (createNewTemplate) {
            this.calculateTemplate();
        }
    }

    public boolean useLowerCase() {
        return this.characterSet == null ||
                this.characterSet.length() >= this.defaultCharacterSetLowerCase.length() &&
                        this.characterSet.substring(0,
                                this.defaultCharacterSetLowerCase.length()).equals(
                                this.defaultCharacterSetLowerCase);
    }

    public void setUseLowerCase(boolean useLowerCase) {
        boolean createNewTemplate = this.useLowerCase() != useLowerCase;
        this.removeFromCharacterSet(this.defaultCharacterSetLowerCase);
        if (useLowerCase) {
            this.characterSet = this.defaultCharacterSetLowerCase + this.characterSet;
        }
        if (createNewTemplate) {
            this.calculateTemplate();
        }
    }

    public boolean useUpperCase() {
        if (this.useLowerCase()) {
            return this.characterSet.length() >= this.defaultCharacterSetLowerCase.length() +
                    this.defaultCharacterSetUpperCase.length() &&
                    this.characterSet.substring(this.defaultCharacterSetLowerCase.length(),
                            this.defaultCharacterSetLowerCase.length() +
                                    this.defaultCharacterSetUpperCase.length()).equals(
                            this.defaultCharacterSetUpperCase);
        } else {
            return this.characterSet.length() >= this.defaultCharacterSetUpperCase.length() &&
                this.characterSet.substring(0,
                        this.defaultCharacterSetUpperCase.length()).equals(
                        this.defaultCharacterSetUpperCase);
        }
    }

    public void setUseUpperCase(boolean useUpperCase) {
        boolean createNewTemplate = this.useUpperCase() != useUpperCase;
        this.removeFromCharacterSet(this.defaultCharacterSetUpperCase);
        if (useUpperCase) {
            if (this.useLowerCase()) {
                this.characterSet = this.characterSet.substring(0,
                        this.defaultCharacterSetLowerCase.length()) +
                        this.defaultCharacterSetUpperCase +
                        this.characterSet.substring( this.defaultCharacterSetLowerCase.length());
            } else {
                this.characterSet = this.defaultCharacterSetUpperCase + this.characterSet;
            }
        }
        if (createNewTemplate) {
            this.calculateTemplate();
        }
    }

    public boolean useDigits() {
        if (this.useLetters()) {
            return this.characterSet.length() >= this.defaultCharacterSetLowerCase.length() +
                    this.defaultCharacterSetUpperCase.length() +
                    this.defaultCharacterSetDigits.length() &&
                    this.characterSet.substring(this.defaultCharacterSetLowerCase.length() +
                                    this.defaultCharacterSetUpperCase.length(),
                            this.defaultCharacterSetLowerCase.length() +
                                    this.defaultCharacterSetUpperCase.length() +
                                    this.defaultCharacterSetDigits.length()).equals(
                            this.defaultCharacterSetDigits);
        } else {
            return this.characterSet.length() >= this.defaultCharacterSetDigits.length() &&
                    this.characterSet.substring(0,
                            this.defaultCharacterSetDigits.length()).equals(
                            this.defaultCharacterSetDigits);
        }
    }

    public void setUseDigits(boolean useDigits) {
        boolean createNewTemplate = this.useDigits() != useDigits;
        this.removeFromCharacterSet(this.defaultCharacterSetDigits);
        if (useDigits) {
            if (this.useLetters()) {
                this.characterSet = this.characterSet.substring(0,
                        this.defaultCharacterSetLowerCase.length() +
                                this.defaultCharacterSetUpperCase.length()) +
                        this.defaultCharacterSetDigits +
                        this.characterSet.substring( this.defaultCharacterSetLowerCase.length() +
                                this.defaultCharacterSetUpperCase.length());
            } else {
                this.characterSet = this.defaultCharacterSetDigits + this.characterSet;
            }
        }
        if (createNewTemplate) {
            this.calculateTemplate();
        }
    }

    public boolean useExtra() {
        if (this.useLetters() && this.useDigits()) {
            return this.characterSet.length() >= this.defaultCharacterSetLowerCase.length() +
                    this.defaultCharacterSetUpperCase.length() +
                    this.defaultCharacterSetDigits.length() +
                    this.getExtraCharacterSetAsString().length() &&
                    this.characterSet.substring(this.defaultCharacterSetLowerCase.length() +
                                    this.defaultCharacterSetUpperCase.length() +
                                    this.defaultCharacterSetDigits.length(),
                            this.defaultCharacterSetLowerCase.length() +
                                    this.defaultCharacterSetUpperCase.length() +
                                    this.defaultCharacterSetDigits.length() +
                                    this.getExtraCharacterSetAsString().length()).equals(
                            this.getExtraCharacterSetAsString());
        } else if (this.useLetters()) {
            return this.characterSet.length() >= this.defaultCharacterSetLowerCase.length() +
                    this.defaultCharacterSetUpperCase.length() +
                    this.getExtraCharacterSetAsString().length() &&
                    this.characterSet.substring(this.defaultCharacterSetLowerCase.length() +
                                    this.defaultCharacterSetUpperCase.length(),
                            this.defaultCharacterSetLowerCase.length() +
                                    this.defaultCharacterSetUpperCase.length() +
                                    this.getExtraCharacterSetAsString().length()).equals(
                            this.getExtraCharacterSetAsString());
        } else if (this.useDigits()) {
            return this.characterSet.length() >= this.defaultCharacterSetDigits.length() +
                    this.getExtraCharacterSetAsString().length() &&
                    this.characterSet.substring(this.defaultCharacterSetDigits.length(),
                            this.defaultCharacterSetDigits.length() +
                                    this.getExtraCharacterSetAsString().length()).equals(
                            this.getExtraCharacterSetAsString());
        } else {
            return this.characterSet.length() >= this.getExtraCharacterSetAsString().length() &&
                    this.characterSet.substring(0,
                            this.getExtraCharacterSetAsString().length()).equals(
                            this.getExtraCharacterSetAsString());
        }
    }

    public void setUseExtra(boolean useExtra) {
        boolean createNewTemplate = this.useExtra() != useExtra;
        this.removeFromCharacterSetExcept(this.defaultCharacterSetLowerCase +
                this.defaultCharacterSetUpperCase +
                this.defaultCharacterSetDigits);
        if (useExtra) {
            if (this.useLetters() && useDigits()) {
                this.characterSet = this.characterSet.substring(0,
                        this.defaultCharacterSetLowerCase.length() +
                                this.defaultCharacterSetUpperCase.length() +
                                this.defaultCharacterSetDigits.length()) +
                        this.getExtraCharacterSetAsString() +
                        this.characterSet.substring(this.defaultCharacterSetLowerCase.length() +
                                this.defaultCharacterSetUpperCase.length() +
                                this.defaultCharacterSetDigits.length());
            } else if (this.useLetters()) {
                this.characterSet = this.characterSet.substring(0,
                        this.defaultCharacterSetLowerCase.length() +
                                this.defaultCharacterSetUpperCase.length()) +
                        this.getExtraCharacterSetAsString() +
                        this.characterSet.substring(this.defaultCharacterSetLowerCase.length() +
                                this.defaultCharacterSetUpperCase.length());
            } else if (this.useDigits()) {
                this.characterSet = this.characterSet.substring(0,
                        this.defaultCharacterSetDigits.length()) +
                        this.getExtraCharacterSetAsString() +
                        this.characterSet.substring(this.defaultCharacterSetDigits.length());
            } else {
                this.characterSet = this.getExtraCharacterSetAsString() + this.characterSet;
            }
        }
        if (createNewTemplate) {
            this.calculateTemplate();
        }
    }

    public String getDefaultCharacterSet() {
        String set = "";
        set = set + this.defaultCharacterSetLowerCase;
        set = set + this.defaultCharacterSetUpperCase;
        set = set + this.defaultCharacterSetDigits;
        set = set + this.defaultCharacterSetExtra;
        return set;
    }

    public void setCharacterSet(String characterSet) {
        if (characterSet == null || characterSet.length() <= 0) {
            this.characterSet = this.getDefaultCharacterSet();
        } else {
            this.characterSet = characterSet;
        }
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
        if (this.characterSet != null) {
            return this.characterSet;
        } else {
            return this.getDefaultCharacterSet();
        }
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
        this.salt = salt;
    }

    public int getLength() {
        return this.length;
    }

    public void setLength(int length) {
        boolean createNewTemplate = this.length != length;
        this.length = length;
        if (createNewTemplate) {
            this.calculateTemplate();
        }
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

    public String getFullTemplate() {
        if (this.useDigits() && !this.useLowerCase() &&
                !this.useUpperCase() && !this.useExtra()) {
            return "0;" + this.getTemplate();
        } else if (!this.useDigits() && this.useLowerCase() &&
                !this.useUpperCase() && !this.useExtra()) {
            return "1;" + this.getTemplate();
        } else if (!this.useDigits() && !this.useLowerCase() &&
                this.useUpperCase() && !this.useExtra()) {
            return "2;" + this.getTemplate();
        } else if (this.useDigits() && this.useLowerCase() &&
                !this.useUpperCase() && !this.useExtra()) {
            return "3;" + this.getTemplate();
        } else if (!this.useDigits() && this.useLowerCase() &&
                this.useUpperCase() && !this.useExtra()) {
            return "4;" + this.getTemplate();
        } else if (this.useDigits() && this.useLowerCase() &&
                this.useUpperCase() && !this.useExtra()) {
            return "5;" + this.getTemplate();
        } else if (this.useDigits() && this.useLowerCase() &&
                this.useUpperCase() && this.useExtra()) {
            return "6;" + this.getTemplate();
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

    private void calculateTemplate() {
        this.template = "";
        boolean aInserted = false;
        boolean AInserted = false;
        boolean nInserted = false;
        boolean oInserted = false;
        for (int i = 0; i < this.getLength(); i++) {
            if (this.useLowerCase() && !aInserted) {
                this.template = this.template + "a";
                aInserted = true;
            } else if (this.useUpperCase() && !AInserted) {
                this.template = this.template + "A";
                AInserted = true;
            } else if (this.useDigits() && !nInserted) {
                this.template = this.template + "n";
                nInserted = true;
            } else if (this.useExtra() && !oInserted) {
                this.template = this.template + "o";
                oInserted = true;
            } else {
                this.template = this.template + "x";
            }
        }
        this.template = this.ShuffleString(this.template);
    }

    public String getTemplate() {
        if (this.template == null) {
            this.calculateTemplate();
        }
        return this.template;
    }

    public void setFullTemplate(String fullTemplate) {
        Matcher matcher = Pattern.compile("([0123456]);([aAnox]+)").matcher(fullTemplate);
        if (matcher.matches() && matcher.groupCount() >= 2) {
            int complexity = Integer.parseInt(matcher.group(1));
            switch (complexity) {
                case 0:
                    this.setUseDigits(true);
                    this.setUseLowerCase(false);
                    this.setUseUpperCase(false);
                    this.setUseExtra(false);
                    break;
                case 1:
                    this.setUseDigits(false);
                    this.setUseLowerCase(true);
                    this.setUseUpperCase(false);
                    this.setUseExtra(false);
                    break;
                case 2:
                    this.setUseDigits(false);
                    this.setUseLowerCase(false);
                    this.setUseUpperCase(true);
                    this.setUseExtra(false);
                    break;
                case 3:
                    this.setUseDigits(true);
                    this.setUseLowerCase(true);
                    this.setUseUpperCase(false);
                    this.setUseExtra(false);
                    break;
                case 4:
                    this.setUseDigits(false);
                    this.setUseLowerCase(true);
                    this.setUseUpperCase(true);
                    this.setUseExtra(false);
                    break;
                case 5:
                    this.setUseDigits(true);
                    this.setUseLowerCase(true);
                    this.setUseUpperCase(true);
                    this.setUseExtra(false);
                    break;
                case 6:
                    this.setUseDigits(true);
                    this.setUseLowerCase(true);
                    this.setUseUpperCase(true);
                    this.setUseExtra(true);
                    break;
            }
            this.template = matcher.group(2);
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
            domainObject.put("salt", Base64.encodeToString(this.getSalt(), Base64.DEFAULT));
            domainObject.put("length", this.getLength());
            domainObject.put("cDate", this.getCreationDate());
            domainObject.put("mDate", this.getModificationDate());
            domainObject.put("usedCharacters", this.getCharacterSetAsString());
            domainObject.put("extras", this.getExtraCharacterSetAsString());
            domainObject.put("passwordTemplate", this.getFullTemplate());
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
            this.setCharacterSet(loadedSetting.getString("usedCharacters"));
        }
        if (loadedSetting.has("extras")) {
            this.setExtraCharacterSet(loadedSetting.getString("extras"));
        }
        if (loadedSetting.has("passwordTemplate")) {
            this.setFullTemplate(loadedSetting.getString("passwordTemplate"));
        }
    }
}
