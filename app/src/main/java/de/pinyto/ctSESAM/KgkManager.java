package de.pinyto.ctSESAM;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.NoSuchPaddingException;

/**
 * Stores and manages the key-generation-key.
 */
public class KgkManager {
    private Context contentContext;
    private SharedPreferences savedDomains;
    byte[] kgk;
    byte[] iv2;
    byte[] salt2;
    Crypter kgkCrypter;

    KgkManager(Context contentContext) {
        this.contentContext = contentContext;
        this.savedDomains = contentContext.getSharedPreferences(
                "savedDomains", Context.MODE_PRIVATE);
    }

    public byte[] getKgkCrypterSalt() {
        byte[] salt = Base64.decode(
                this.savedDomains.getString("salt", ""),
                Base64.DEFAULT);
        if (salt.length != 32) {
            salt = Crypter.createSalt();
            this.storeSalt(salt);
        }
        return salt;
    }

    private void storeSalt(byte[] salt) {
        SharedPreferences.Editor savedDomainsEditor = this.savedDomains.edit();
        savedDomainsEditor.putString("salt", Base64.encodeToString(
                salt,
                Base64.DEFAULT));
        savedDomainsEditor.apply();
    }

    private Crypter getKgkCrypter(byte[] password, byte[] salt) {
        this.kgkCrypter = new Crypter(Crypter.createIvKey(password, salt));
        return this.kgkCrypter;
    }

    public byte[] createNewKgk() {
        Clearer.zero(this.salt2);
        Clearer.zero(this.iv2);
        Clearer.zero(this.kgk);
        SecureRandom sr = new SecureRandom();
        this.kgk = new byte[64];
        sr.nextBytes(this.kgk);
        this.iv2 = Crypter.createIv();
        this.salt2 = Crypter.createSalt();
        return this.kgk;
    }

    public byte[] gelLocalKgkBlock() {
        String kgkBase64 = this.savedDomains.getString("KGK", "");
        if (kgkBase64.length() < 152) {
            return new byte[]{};
        }
        return Base64.decode(kgkBase64, Base64.DEFAULT);
    }

    public void decryptKgk(byte[] password, byte[] salt, byte[] encryptedKgk) {
        this.getKgkCrypter(password, salt);
        if (encryptedKgk.length != 112) {
            createNewKgk();
        } else {
            try {
                byte[] kgkBlock = this.kgkCrypter.decrypt(encryptedKgk, "NoPadding");
                Clearer.zero(this.salt2);
                Clearer.zero(this.iv2);
                Clearer.zero(this.kgk);
                this.salt2 = Arrays.copyOfRange(kgkBlock, 0, 32);
                this.iv2 = Arrays.copyOfRange(kgkBlock, 32, 48);
                this.kgk = Arrays.copyOfRange(kgkBlock, 48, 112);
            } catch (NoSuchPaddingException paddingError) {
                paddingError.printStackTrace();
            }
        }
    }

    public byte[] getKgk() {
        return this.kgk;
    }

    public boolean hasKgk() {
        return this.kgk != null && this.kgk.length == 64;
    }

    public byte[] getSalt2() {
        return this.salt2;
    }

    public byte[] getIv2() {
        return this.iv2;
    }

    public void freshSalt2() {
        Clearer.zero(this.salt2);
        this.salt2 = Crypter.createSalt();
    }

    public void freshIv2() {
        Clearer.zero(this.iv2);
        this.iv2 = Crypter.createIv();
    }

    public byte[] getFreshEncryptedKgk() {
        byte[] kgkBlock = new byte[112];
        this.freshIv2();
        this.freshSalt2();
        System.arraycopy(this.salt2, 0, kgkBlock, 0, this.salt2.length);
        System.arraycopy(this.iv2, 0, kgkBlock, salt2.length, this.iv2.length);
        System.arraycopy(this.kgk, 0, kgkBlock, salt2.length + iv2.length, this.kgk.length);
        byte[] encryptedKgk = this.kgkCrypter.encrypt(kgkBlock);
        Clearer.zero(kgkBlock);
        return encryptedKgk;
    }

    public void createAndSaveNewKgkBlock(byte[] password) {
        this.createNewKgk();
        byte[] salt = Crypter.createSalt();
        SharedPreferences.Editor savedDomainsEditor = this.savedDomains.edit();
        savedDomainsEditor.putString("salt", Base64.encodeToString(
                salt,
                Base64.DEFAULT));
        this.getKgkCrypter(password, salt);
        byte[] encryptedKgkBlock = this.getFreshEncryptedKgk();
        Clearer.zero(salt);
        savedDomainsEditor.putString("KGK", Base64.encodeToString(
                encryptedKgkBlock,
                Base64.DEFAULT));
        savedDomainsEditor.apply();
    }

    public void updateFromBlob(byte[] password, byte[] blob) {
        if (!(blob[0] == 0x01)) {
            Log.d("Version error", "Wrong data format. Could not import anything.");
            return;
        }
        byte[] salt = Arrays.copyOfRange(blob, 1, 33);
        byte[] kgkBlock =  Arrays.copyOfRange(blob, 33, 145);
        this.decryptKgk(password, salt, kgkBlock);
    }
}
