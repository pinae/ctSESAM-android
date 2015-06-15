package de.pinyto.passwordgenerator;

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
    private boolean syced;
    private String notes;

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

    public boolean useLetters() {
        return this.useLowerCase && this.useUpperCase;
    }

    public void setUseLetters(boolean useLetters) {
        this.useLowerCase = useLetters;
        this.useUpperCase = useLetters;
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

    public void setModificationDate(String mDate) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        try {
            this.mDate = df.parse(mDate);
        } catch (ParseException e) {
            System.out.println("This date has a wrong format: " + mDate);
            e.printStackTrace();
            this.cDate = Calendar.getInstance().getTime();
        }
        if (this.cDate.compareTo(this.mDate) > 0) {
            System.out.println("The modification date was before the creation Date. " +
                    "Set the creation date to the earlier date.");
            this.cDate = this.mDate;
        }
    }

    public void setModificationDateToNow() {
        this.mDate = Calendar.getInstance().getTime();
        if (this.cDate.compareTo(this.mDate) > 0) {
            System.out.println("The modification date was before the creation Date. " +
                    "Set the creation date to the earlier date.");
            this.cDate = this.mDate;
        }
    }
}
