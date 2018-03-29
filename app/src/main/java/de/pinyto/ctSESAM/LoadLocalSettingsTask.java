package de.pinyto.ctSESAM;

import android.os.AsyncTask;
import java.lang.ref.WeakReference;

/**
 * Asynchronously load and decrypt local settings.
 */
public class LoadLocalSettingsTask extends AsyncTask<byte[], Void, byte[]> {
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    private WeakReference<OnKgkDecryptionFinishedListener> finishedListenerWeakRef;

    LoadLocalSettingsTask(OnKgkDecryptionFinishedListener finishedListener,
                          KgkManager kgkManager,
                          PasswordSettingsManager settingsManager) {
        super();
        this.finishedListenerWeakRef = new WeakReference<>(finishedListener);
        this.kgkManager = kgkManager;
        this.settingsManager = settingsManager;
    }

    @Override
    protected byte[] doInBackground(byte[]... params) {
        byte[] password = params[0];
        byte[] salt = params[1];
        byte[] ivKey = Crypter.createIvKey(password, salt);
        for (int i = 0; i < password.length; i++) {
            password[i] = 0x00;
        }
        return ivKey;
    }

    @Override
    protected void onPostExecute(byte[] ivKey) {
        byte[] encryptedKgkBlock = kgkManager.gelLocalKgkBlock();
        kgkManager.decryptKgk(new Crypter(ivKey), encryptedKgkBlock);
        OnKgkDecryptionFinishedListener finishedListener = finishedListenerWeakRef.get();
        try {
            settingsManager.loadLocalSettings(kgkManager);
            if (finishedListener != null) {
                finishedListener.onFinished(true);
            }
        } catch (WrongPasswordException passwordError) {
            kgkManager.reset();
            if (finishedListener != null) {
                finishedListener.onFinished(false);
            }
        }
    }

    public interface OnKgkDecryptionFinishedListener {
        void onFinished(boolean success);
    }
}
