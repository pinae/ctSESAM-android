package de.pinyto.ctSESAM;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Calculate a password and display it.
 */
class GeneratePasswordTask extends AsyncTask<byte[], Void, JSONObject> {
    private PasswordSetting setting;
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    private WeakReference<MainActivity> mainActivityWeakRef;
    private boolean applyCheckboxLetters;
    private boolean applyCheckboxDigits;
    private boolean applyCheckboxExtra;

    GeneratePasswordTask(MainActivity mainActivity,
                         PasswordSetting setting,
                         KgkManager kgkManager,
                         PasswordSettingsManager settingsManager,
                         boolean applyCheckboxLetters,
                         boolean applyCheckboxDigits,
                         boolean applyCheckboxExtra) {
        super();
        this.mainActivityWeakRef = new WeakReference<>(mainActivity);
        this.setting = setting;
        this.kgkManager = kgkManager;
        this.settingsManager = settingsManager;
        this.applyCheckboxLetters = applyCheckboxLetters;
        this.applyCheckboxDigits = applyCheckboxDigits;
        this.applyCheckboxExtra = applyCheckboxExtra;
    }

    @Override
    protected JSONObject doInBackground(byte[]... params) {
        byte[] domain = params[0];
        byte[] username = params[1];
        byte[] kgk = params[2];
        byte[] salt = params[3];
        int iterations = ByteBuffer.wrap(Arrays.copyOfRange(params[4], 0, 4)).getInt();
        int length = ByteBuffer.wrap(Arrays.copyOfRange(params[4], 0, 4)).getInt();
        boolean checkBoxLettersIsChecked = params[5][0] == 0x01;
        boolean checkBoxDigitsIsChecked = params[6][0] == 0x01;
        boolean checkBoxExtraIsChecked = params[7][0] == 0x01;
        boolean forceLetters = params[8][0] == 0x01;
        boolean forceDigits = params[9][0] == 0x01;
        boolean forceExtra = params[10][0] == 0x01;
        String generatedPassword;
        do {
            PasswordGenerator generator = new PasswordGenerator(
                    domain,
                    username,
                    kgk,
                    salt);
            try {
                generator.hash(iterations);
                if (applyCheckboxLetters) {
                    this.setting.setUseLetters(checkBoxLettersIsChecked);
                }
                if (applyCheckboxDigits) {
                    this.setting.setUseDigits(checkBoxDigitsIsChecked);
                }
                if (applyCheckboxExtra) {
                    this.setting.setUseExtra(checkBoxExtraIsChecked);
                }
                this.setting.setIterations(iterations);
                this.setting.setLength(length);
                generatedPassword = generator.getPassword(setting);
            } catch (NotHashedException e) {
                e.printStackTrace();
                generatedPassword = "Not hashed.";
            }
            iterations++;
        } while (!PasswordAnalyzer.contains(generatedPassword,
                forceLetters, forceDigits, forceExtra));
        JSONObject result = new JSONObject();
        try {
            result.put("generatedPassword", generatedPassword);
            result.put("iterations", iterations - 1);
        } catch (JSONException jsonError) {
            jsonError.printStackTrace();
        }
        return result;
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        MainActivity activity = mainActivityWeakRef.get();
        if (activity != null && !activity.isFinishing()) {
            TextView textViewPassword = (TextView) activity.findViewById(R.id.textViewPassword);
            try {
                this.setting.setIterations(result.getInt("iterations"));
                textViewPassword.setText(result.getString("generatedPassword"));
            } catch (JSONException jsonError) {
                jsonError.printStackTrace();
            }
            CheckBox checkBoxLetters =
                    (CheckBox) activity.findViewById(R.id.checkBoxLetters);
            CheckBox checkBoxDigits =
                    (CheckBox) activity.findViewById(R.id.checkBoxDigits);
            CheckBox checkBoxSpecialCharacters =
                    (CheckBox) activity.findViewById(R.id.checkBoxSpecialCharacter);
            if (applyCheckboxLetters) {
                this.setting.setUseLetters(checkBoxLetters.isChecked());
            }
            if (applyCheckboxDigits) {
                this.setting.setUseDigits(checkBoxDigits.isChecked());
            }
            if (applyCheckboxExtra) {
                this.setting.setUseExtra(checkBoxSpecialCharacters.isChecked());
            }
            SeekBar seekBarLength =
                    (SeekBar) activity.findViewById(R.id.seekBarLength);
            this.setting.setLength(seekBarLength.getProgress() + 4);
            this.setting.setModificationDateToNow();
            settingsManager.setSetting(this.setting);
            settingsManager.storeLocalSettings(kgkManager);
            activity.setIsGenerated(true);
            activity.invalidateOptionsMenu();
            Button generateButton = (Button) activity.findViewById(R.id.generatorButton);
            generateButton.setText(activity.getResources().getString(R.string.re_generator_button));
            TextView textViewIterationCount =
                    (TextView) activity.findViewById(R.id.iterationCount);
            try {
                textViewIterationCount.setText(Integer.toString(result.getInt("iterations")));
                activity.setIterationCountVisibility(View.VISIBLE);
            } catch (JSONException jsonError) {
                jsonError.printStackTrace();
            }
            // load settings because the domain might be new
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity.getBaseContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    settingsManager.getDomainList());
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) activity.findViewById(R.id.autoCompleteTextViewDomain);
            autoCompleteTextViewDomain.setAdapter(adapter);
        }
    }
}
