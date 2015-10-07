package de.pinyto.ctSESAM;

import android.os.AsyncTask;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

/**
 * Create a new kgk block.
 */
class CreateNewKgkTask extends AsyncTask<byte[], byte[], byte[]> {
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    private WeakReference<MainActivity> mainActivityWeakRef;

    CreateNewKgkTask(MainActivity mainActivity,
                     KgkManager kgkManager,
                     PasswordSettingsManager settingsManager) {
        super();
        this.mainActivityWeakRef = new WeakReference<>(mainActivity);
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
    protected void onPreExecute() {
        MainActivity activity = mainActivityWeakRef.get();
        if (activity != null && !activity.isFinishing()) {
            TextView loadingMessage =
                    (TextView) activity.findViewById(R.id.textViewDecryptionMessage);
            loadingMessage.setText(activity.getString(R.string.creatingKgk));
        }
    }

    @Override
    protected void onPostExecute(byte[] ivKey) {
        kgkManager.createAndStoreNewKgkBlock(new Crypter(ivKey));
        MainActivity activity = mainActivityWeakRef.get();
        if (activity != null && !activity.isFinishing()) {
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) activity.findViewById(R.id.autoCompleteTextViewDomain);
            try {
                settingsManager.loadLocalSettings(kgkManager);
            } catch (WrongPasswordException passwordError) {
                passwordError.printStackTrace();
                autoCompleteTextViewDomain.dismissDropDown();
            }
            TextView loadingMessage =
                    (TextView) activity.findViewById(R.id.textViewDecryptionMessage);
            loadingMessage.setText("");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity.getBaseContext(),
                    android.R.layout.simple_dropdown_item_1line, settingsManager.getDomainList());
            autoCompleteTextViewDomain.setAdapter(adapter);
        }
    }
}
