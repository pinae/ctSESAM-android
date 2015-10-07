package de.pinyto.ctSESAM;

import android.os.AsyncTask;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * Create kgk and password at once.
 */
class CreateKgkAndPasswordTask extends AsyncTask<byte[], Void, byte[]> {
    int iterations;
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    private WeakReference<MainActivity> mainActivityWeakRef;
    private boolean applyCheckboxLetters;
    private boolean applyCheckboxDigits;
    private boolean applyCheckboxExtra;

    CreateKgkAndPasswordTask(MainActivity mainActivity,
                             int iterations,
                             KgkManager kgkManager,
                             PasswordSettingsManager settingsManager,
                             boolean applyCheckboxLetters,
                             boolean applyCheckboxDigits,
                             boolean applyCheckboxExtra) {
        super();
        this.mainActivityWeakRef = new WeakReference<>(mainActivity);
        this.iterations = iterations;
        this.kgkManager = kgkManager;
        this.settingsManager = settingsManager;
        this.applyCheckboxLetters = applyCheckboxLetters;
        this.applyCheckboxDigits = applyCheckboxDigits;
        this.applyCheckboxExtra = applyCheckboxExtra;
    }

    @Override
    protected byte[] doInBackground(byte[]... params) {
        byte[] password = params[0];
        byte[] salt = Crypter.createSalt();
        byte[] ivKey = Crypter.createIvKey(password, salt);
        for (int i = 0; i < password.length; i++) {
            password[i] = 0x00;
        }
        return ivKey;
    }

    @Override
    protected void onPostExecute(byte[] ivKey) {
        kgkManager.createAndStoreNewKgkBlock(new Crypter(ivKey));
        MainActivity activity = mainActivityWeakRef.get();
        if (activity != null && !activity.isFinishing()) {
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) activity.findViewById(R.id.autoCompleteTextViewDomain);
            String domainStr = autoCompleteTextViewDomain.getText().toString();
            byte[] domain = UTF8.encode(autoCompleteTextViewDomain.getText());
            PasswordSetting setting = settingsManager.getSetting(domainStr);
            GeneratePasswordTask generatePasswordTask = new GeneratePasswordTask(
                    activity,
                    setting,
                    kgkManager,
                    settingsManager,
                    applyCheckboxLetters,
                    applyCheckboxDigits,
                    applyCheckboxExtra);
            EditText editTextUsername =
                    (EditText) activity.findViewById(R.id.editTextUsername);
            byte[] username = UTF8.encode(editTextUsername.getText());
            byte[] kgk = kgkManager.getKgk();
            CheckBox checkBoxLetters =
                    (CheckBox) activity.findViewById(R.id.checkBoxLetters);
            CheckBox checkBoxDigits =
                    (CheckBox) activity.findViewById(R.id.checkBoxDigits);
            CheckBox checkBoxExtra =
                    (CheckBox) activity.findViewById(R.id.checkBoxSpecialCharacter);
            CheckBox checkBoxLettersForce =
                    (CheckBox) activity.findViewById(R.id.checkBoxLettersForce);
            CheckBox checkBoxDigitsForce =
                    (CheckBox) activity.findViewById(R.id.checkBoxDigitsForce);
            CheckBox checkBoxExtraForce =
                    (CheckBox) activity.findViewById(R.id.checkBoxSpecialCharacterForce);
            generatePasswordTask.execute(domain, username, kgk, setting.getSalt(),
                    ByteBuffer.allocate(4).putInt(iterations).array(),
                    new byte[]{(byte) (checkBoxLetters.isChecked() ? 1 : 0)},
                    new byte[]{(byte) (checkBoxDigits.isChecked() ? 1 : 0)},
                    new byte[]{(byte) (checkBoxExtra.isChecked() ? 1 : 0)},
                    new byte[]{(byte) (checkBoxLettersForce.isChecked() ? 1 : 0)},
                    new byte[]{(byte) (checkBoxDigitsForce.isChecked() ? 1 : 0)},
                    new byte[]{(byte) (checkBoxExtraForce.isChecked() ? 1 : 0)});
        }
    }
}
