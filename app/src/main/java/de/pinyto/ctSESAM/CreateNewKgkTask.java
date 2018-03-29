package de.pinyto.ctSESAM;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

/**
 * Create a new kgk block.
 */
class CreateNewKgkTask extends AsyncTask<byte[], byte[], byte[]> {
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    private WeakReference<OnNewKgkFinishedListener> finishedListenerWeakRef;

    CreateNewKgkTask(OnNewKgkFinishedListener finishedListener,
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
        kgkManager.createAndStoreNewKgkBlock(new Crypter(ivKey));
        OnNewKgkFinishedListener finishedListener = finishedListenerWeakRef.get();
        try {
            settingsManager.loadLocalSettings(kgkManager);
            if (finishedListener != null) {
                finishedListener.onFinished(true);
            }
        } catch (WrongPasswordException passwordError) {
            passwordError.printStackTrace();
            if (finishedListener != null) {
                finishedListener.onFinished(false);
            }
        }
    }

    public interface OnNewKgkFinishedListener {
        void onFinished(boolean success);
    }
}
