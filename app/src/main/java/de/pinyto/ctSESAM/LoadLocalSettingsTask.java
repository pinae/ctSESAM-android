package de.pinyto.ctSESAM;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

/**
 * Asynchronously load and decrypt local settings.
 */
public class LoadLocalSettingsTask extends AsyncTask<byte[], Void, byte[]> {
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    private WeakReference<MainActivity> mainActivityWeakRef;

    LoadLocalSettingsTask(MainActivity mainActivity,
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
    protected void onPostExecute(byte[] ivKey) {
        byte[] encryptedKgkBlock = kgkManager.gelLocalKgkBlock();
        Log.d("encrypted length", Integer.toString(encryptedKgkBlock.length));
        Log.d("encrypted kgk block", Hextools.bytesToHex(encryptedKgkBlock));
        kgkManager.decryptKgk(new Crypter(ivKey), encryptedKgkBlock);
        MainActivity activity = mainActivityWeakRef.get();
        if (activity != null && !activity.isFinishing()) {
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) activity.findViewById(R.id.autoCompleteTextViewDomain);
            try {
                settingsManager.loadLocalSettings(kgkManager);
            } catch (WrongPasswordException passwordError) {
                Toast.makeText(activity.getBaseContext(),
                        R.string.local_wrong_password, Toast.LENGTH_SHORT).show();
                autoCompleteTextViewDomain.dismissDropDown();
                kgkManager.reset();
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
