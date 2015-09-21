package de.pinyto.ctSESAM;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    static final String syncAppName = "de.pinyto.ctSESAMsync";
    static final String syncServiceName = "SyncService";
    Messenger mService = null;
    boolean mBound;
    static final int REQUEST_SYNC = 1;
    static final int SEND_UPDATE = 2;
    static final int SYNC_RESPONSE = 1;
    static final int SEND_UPDATE_RESPONSE = 2;

    private PasswordSettingsManager settingsManager;
    private KgkManager kgkManager;
    private boolean isGenerated = false;
    private boolean showSettings = false;
    private boolean showLegacyPassword = false;
    private LoadLocalSettingsTask loadSettingsTask;
    private CreateNewKgkTask createNewKgkTask;
    private GeneratePasswordTask generatePasswordTask;
    private CreateKgkAndPasswordTask createKgkAndPasswordTask;
    private boolean applyCheckboxLetters = false;
    private boolean applyCheckboxDigits = false;
    private boolean applyCheckboxExtra = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            mBound = false;
        }
    };

    private class LoadLocalSettingsTask extends AsyncTask<byte[], Void, IvKeyContainer> {
        private KgkManager kgkManager;
        private PasswordSettingsManager settingsManager;

        LoadLocalSettingsTask(KgkManager kgkManager, PasswordSettingsManager settingsManager) {
            this.kgkManager = kgkManager;
            this.settingsManager = settingsManager;
        }

        @Override
        protected IvKeyContainer doInBackground(byte[]... params) {
            byte[] password = params[0];
            byte[] salt = params[1];
            byte[] ivKey = Crypter.createIvKey(password, salt);
            for (int i = 0; i < password.length; i++) {
                password[i] = 0x00;
            }
            return new IvKeyContainer(ivKey);
        }

        @Override
        protected void onPreExecute() {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    new String[]{getString(R.string.loading)});
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) findViewById(
                            R.id.autoCompleteTextViewDomain);
            autoCompleteTextViewDomain.setAdapter(adapter);
            autoCompleteTextViewDomain.showDropDown();
        }

        @Override
        protected void onPostExecute(IvKeyContainer ivKeyContainer) {
            byte[] encryptedKgkBlock = kgkManager.gelLocalKgkBlock();
            kgkManager.decryptKgk(
                    new Crypter(ivKeyContainer.getIvKey()),
                    encryptedKgkBlock);
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
            try {
                settingsManager.loadLocalSettings(kgkManager);
            } catch (WrongPasswordException passwordError) {
                Toast.makeText(getBaseContext(),
                        R.string.local_wrong_password, Toast.LENGTH_SHORT).show();
                autoCompleteTextViewDomain.dismissDropDown();
                kgkManager.reset();
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(),
                    android.R.layout.simple_dropdown_item_1line, settingsManager.getDomainList());
            autoCompleteTextViewDomain.setAdapter(adapter);
        }
    }

    private class CreateNewKgkTask extends AsyncTask<byte[], byte[], byte[]> {
        private KgkManager kgkManager;
        private PasswordSettingsManager settingsManager;

        CreateNewKgkTask(KgkManager kgkManager, PasswordSettingsManager settingsManager) {
            this.kgkManager = kgkManager;
            this.settingsManager = settingsManager;
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
        protected void onPreExecute() {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    new String[]{getString(R.string.creatingKgk)});
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) findViewById(
                            R.id.autoCompleteTextViewDomain);
            autoCompleteTextViewDomain.setAdapter(adapter);
            autoCompleteTextViewDomain.showDropDown();
        }

        @Override
        protected void onPostExecute(byte[] ivKey) {
            kgkManager.createAndSaveNewKgkBlock(new Crypter(ivKey));
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
            try {
                settingsManager.loadLocalSettings(kgkManager);
            } catch (WrongPasswordException passwordError) {
                passwordError.printStackTrace();
                autoCompleteTextViewDomain.dismissDropDown();
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(),
                    android.R.layout.simple_dropdown_item_1line, settingsManager.getDomainList());
            autoCompleteTextViewDomain.setAdapter(adapter);
            autoCompleteTextViewDomain.dismissDropDown();
        }
    }

    private class GeneratePasswordTask extends AsyncTask<byte[], Void, JSONObject> {
        PasswordSetting setting;

        GeneratePasswordTask(PasswordSetting setting) {
            this.setting = setting;
        }

        @Override
        protected JSONObject doInBackground(byte[]... params) {
            byte[] domain = params[0];
            byte[] username = params[1];
            byte[] kgk = params[2];
            byte[] salt = params[3];
            int iterations = ByteBuffer.wrap(Arrays.copyOfRange(params[4], 0, 4)).getInt();
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
            TextView textViewPassword = (TextView) findViewById(R.id.textViewPassword);
            try {
                this.setting.setIterations(result.getInt("iterations"));
                textViewPassword.setText(result.getString("generatedPassword"));
            } catch (JSONException jsonError) {
                jsonError.printStackTrace();
            }
            CheckBox checkBoxLetters =
                    (CheckBox) findViewById(R.id.checkBoxLetters);
            CheckBox checkBoxDigits =
                    (CheckBox) findViewById(R.id.checkBoxDigits);
            CheckBox checkBoxSpecialCharacters =
                    (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
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
                    (SeekBar) findViewById(R.id.seekBarLength);
            this.setting.setLength(seekBarLength.getProgress() + 4);
            this.setting.setModificationDateToNow();
            settingsManager.setSetting(this.setting);
            settingsManager.storeLocalSettings(kgkManager);
            isGenerated = true;
            invalidateOptionsMenu();
            Button generateButton = (Button) findViewById(R.id.generatorButton);
            generateButton.setText(getResources().getString(R.string.re_generator_button));
            TextView textViewIterationCount =
                    (TextView) findViewById(R.id.iterationCount);
            try {
                textViewIterationCount.setText(Integer.toString(result.getInt("iterations")));
                setIterationCountVisibility(View.VISIBLE);
            } catch (JSONException jsonError) {
                jsonError.printStackTrace();
            }
            // load settings because the domain might be new
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    settingsManager.getDomainList());
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
            autoCompleteTextViewDomain.setAdapter(adapter);
        }
    }

    private class CreateKgkAndPasswordTask extends AsyncTask<byte[], Void, byte[]> {
        int iterations;

        CreateKgkAndPasswordTask(int iterations) {
            this.iterations = iterations;
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
            kgkManager.createAndSaveNewKgkBlock(new Crypter(ivKey));
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
            String domainStr = autoCompleteTextViewDomain.getText().toString();
            byte[] domain = UTF8.encode(autoCompleteTextViewDomain.getText());
            PasswordSetting setting = settingsManager.getSetting(domainStr);
            generatePasswordTask = new GeneratePasswordTask(setting);
            EditText editTextUsername =
                    (EditText) findViewById(R.id.editTextUsername);
            byte[] username = UTF8.encode(editTextUsername.getText());
            byte[] kgk = kgkManager.getKgk();
            CheckBox checkBoxLetters =
                    (CheckBox) findViewById(R.id.checkBoxLetters);
            CheckBox checkBoxDigits =
                    (CheckBox) findViewById(R.id.checkBoxDigits);
            CheckBox checkBoxExtra =
                    (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
            CheckBox checkBoxLettersForce =
                    (CheckBox) findViewById(R.id.checkBoxLettersForce);
            CheckBox checkBoxDigitsForce =
                    (CheckBox) findViewById(R.id.checkBoxDigitsForce);
            CheckBox checkBoxExtraForce =
                    (CheckBox) findViewById(R.id.checkBoxSpecialCharacterForce);
            generatePasswordTask.execute(domain, username, kgk, setting.getSalt(),
                    ByteBuffer.allocate(4).putInt(iterations).array(),
                    new byte[]{(byte) (checkBoxLetters.isChecked() ? 1 : 0 )},
                    new byte[]{(byte) (checkBoxDigits.isChecked() ? 1 : 0 )},
                    new byte[]{(byte) (checkBoxExtra.isChecked() ? 1 : 0 )},
                    new byte[]{(byte) (checkBoxLettersForce.isChecked() ? 1 : 0 )},
                    new byte[]{(byte) (checkBoxDigitsForce.isChecked() ? 1 : 0 )},
                    new byte[]{(byte) (checkBoxExtraForce.isChecked() ? 1 : 0 )});
        }
    }

    class ResponseHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int respCode = msg.what;

            switch (respCode) {
                case SYNC_RESPONSE: {
                    String syncData = msg.getData().getString("respData");
                    try {
                        JSONObject syncDataObject = new JSONObject(syncData);
                        if (!syncDataObject.getString("status").equals("ok")) break;
                        EditText editTextMasterPassword = (EditText) findViewById(
                                R.id.editTextMasterPassword);
                        if (syncDataObject.has("result")) {
                            byte[] password = UTF8.encode(editTextMasterPassword.getText());
                            byte[] blob = Base64.decode(syncDataObject.getString("result"),
                                    Base64.DEFAULT);
                            kgkManager.updateFromBlob(password, blob);
                            boolean changed = settingsManager.updateFromExportData(
                                    kgkManager, blob);
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(),
                                    android.R.layout.simple_dropdown_item_1line,
                                    settingsManager.getDomainList());
                            AutoCompleteTextView autoCompleteTextViewDomain =
                                    (AutoCompleteTextView) findViewById(
                                            R.id.autoCompleteTextViewDomain);
                            autoCompleteTextViewDomain.setAdapter(adapter);
                            Toast.makeText(getApplicationContext(),
                                    R.string.sync_loaded, Toast.LENGTH_SHORT).show();
                            if (changed) {
                                byte[] encryptedBlob = settingsManager.getExportData(kgkManager);
                                if (mBound) {
                                    Message updateMsg = Message.obtain(null, SEND_UPDATE, 0, 0);
                                    updateMsg.replyTo = new Messenger(new ResponseHandler());
                                    Bundle bUpdateMsg = new Bundle();
                                    bUpdateMsg.putString("updatedData",
                                            Base64.encodeToString(encryptedBlob, Base64.DEFAULT));
                                    updateMsg.setData(bUpdateMsg);
                                    try {
                                        mService.send(updateMsg);
                                    } catch (RemoteException e) {
                                        Log.d("Sync error",
                                                "Could not send update message to sync service.");
                                        e.printStackTrace();
                                    }
                                }
                            }
                            Clearer.zero(password);
                        }
                    } catch (JSONException e) {
                        Log.d("Sync error", "The response is not valid JSON.");
                        e.printStackTrace();
                    }
                    break;
                }
                case SEND_UPDATE_RESPONSE: {
                    String updateRequestAnswer = msg.getData().getString("respData");
                    try {
                        JSONObject syncDataObject = new JSONObject(updateRequestAnswer);
                        if (syncDataObject.getString("status").equals("ok")) {
                            settingsManager.setAllSettingsToSynced();
                            Toast.makeText(getApplicationContext(),
                                    R.string.sync_successful, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    R.string.sync_error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.d("update Settings error", "Server response is not JSON.");
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    private void updateView() {
        ImageView containsIcon = (ImageView) findViewById(R.id.imageViewContains);
        ImageView forceContainsIcon = (ImageView) findViewById(R.id.imageViewForceContains);
        CheckBox checkBoxLettersForce =
                (CheckBox) findViewById(R.id.checkBoxLettersForce);
        CheckBox checkBoxLetters =
                (CheckBox) findViewById(R.id.checkBoxLetters);
        CheckBox checkBoxDigits =
                (CheckBox) findViewById(R.id.checkBoxDigits);
        CheckBox checkBoxDigitsForce =
                (CheckBox) findViewById(R.id.checkBoxDigitsForce);
        CheckBox checkBoxSpecialCharacters =
                (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
        CheckBox checkBoxSpecialCharactersForce =
                (CheckBox) findViewById(R.id.checkBoxSpecialCharacterForce);
        TextView lengthHeading = (TextView) findViewById(R.id.textViewLengthHeading);
        TextView lengthLabel = (TextView) findViewById(R.id.textViewLengthDisplay);
        SeekBar seekBarLength =
                (SeekBar) findViewById(R.id.seekBarLength);
        Button generateButton = (Button) findViewById(R.id.generatorButton);
        TextView passwordHeading = (TextView) findViewById(R.id.textViewPasswordHeading);
        TextView password = (TextView) findViewById(R.id.textViewPassword);
        TextView legacyPasswordHeading = (TextView) findViewById(R.id.textViewLegacyPasswordHeading);
        TextView legacyPassword = (TextView) findViewById(R.id.textViewLegacyPassword);
        if (this.showSettings) {
            containsIcon.setVisibility(View.VISIBLE);
            forceContainsIcon.setVisibility(View.VISIBLE);
            checkBoxLetters.setVisibility(View.VISIBLE);
            checkBoxLettersForce.setVisibility(View.VISIBLE);
            checkBoxDigits.setVisibility(View.VISIBLE);
            checkBoxDigitsForce.setVisibility(View.VISIBLE);
            checkBoxSpecialCharacters.setVisibility(View.VISIBLE);
            checkBoxSpecialCharactersForce.setVisibility(View.VISIBLE);
            lengthHeading.setVisibility(View.VISIBLE);
            lengthLabel.setVisibility(View.VISIBLE);
            seekBarLength.setVisibility(View.VISIBLE);
            generateButton.setVisibility(View.VISIBLE);
            passwordHeading.setVisibility(View.VISIBLE);
            password.setVisibility(View.VISIBLE);
        } else {
            containsIcon.setVisibility(View.INVISIBLE);
            forceContainsIcon.setVisibility(View.INVISIBLE);
            checkBoxLetters.setVisibility(View.INVISIBLE);
            checkBoxLettersForce.setVisibility(View.INVISIBLE);
            checkBoxDigits.setVisibility(View.INVISIBLE);
            checkBoxDigitsForce.setVisibility(View.INVISIBLE);
            checkBoxSpecialCharacters.setVisibility(View.INVISIBLE);
            checkBoxSpecialCharactersForce.setVisibility(View.INVISIBLE);
            lengthHeading.setVisibility(View.INVISIBLE);
            lengthLabel.setVisibility(View.INVISIBLE);
            seekBarLength.setVisibility(View.INVISIBLE);
            generateButton.setVisibility(View.INVISIBLE);
            passwordHeading.setVisibility(View.INVISIBLE);
            password.setVisibility(View.INVISIBLE);
        }
        if (this.showLegacyPassword) {
            legacyPasswordHeading.setVisibility(View.VISIBLE);
            legacyPassword.setVisibility(View.VISIBLE);
        } else {
            legacyPasswordHeading.setVisibility(View.INVISIBLE);
            legacyPassword.setVisibility(View.INVISIBLE);
        }
    }

    private void setIterationCountVisibility(int visible) {
        TextView textViewIterationCountBeginning =
                (TextView) findViewById(R.id.iterationCountBeginning);
        textViewIterationCountBeginning.setVisibility(visible);
        TextView textViewIterationCount =
                (TextView) findViewById(R.id.iterationCount);
        textViewIterationCount.setVisibility(visible);
        TextView textViewIterationCountEnd =
                (TextView) findViewById(R.id.iterationCountEnd);
        textViewIterationCountEnd.setVisibility(visible);
    }

    private void setDomainFieldFromClipboard() {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData clipDataCurrent = clipboard.getPrimaryClip();
            CharSequence pasteData = clipDataCurrent.getItemAt(0).getText();
            if (pasteData != null) {
                AutoCompleteTextView autoCompleteTextViewDomain =
                        (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
                autoCompleteTextViewDomain.setText(DomainExtractor.extract(pasteData.toString()));
            }
        }
    }

    private void generatePassword(int iterations) {
        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        String domainStr = autoCompleteTextViewDomain.getText().toString();
        byte[] domain = UTF8.encode(autoCompleteTextViewDomain.getText());
        PasswordSetting setting = this.settingsManager.getSetting(domainStr);
        if (kgkManager.hasKgk()) {
            generatePasswordTask = new GeneratePasswordTask(setting);
            EditText editTextUsername =
                    (EditText) findViewById(R.id.editTextUsername);
            byte[] username = UTF8.encode(editTextUsername.getText());
            byte[] kgk = kgkManager.getKgk();
            CheckBox checkBoxLetters =
                    (CheckBox) findViewById(R.id.checkBoxLetters);
            CheckBox checkBoxDigits =
                    (CheckBox) findViewById(R.id.checkBoxDigits);
            CheckBox checkBoxExtra =
                    (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
            CheckBox checkBoxLettersForce =
                    (CheckBox) findViewById(R.id.checkBoxLettersForce);
            CheckBox checkBoxDigitsForce =
                    (CheckBox) findViewById(R.id.checkBoxDigitsForce);
            CheckBox checkBoxExtraForce =
                    (CheckBox) findViewById(R.id.checkBoxSpecialCharacterForce);
            generatePasswordTask.execute(domain, username, kgk, setting.getSalt(),
                    ByteBuffer.allocate(4).putInt(iterations).array(),
                    new byte[]{(byte) (checkBoxLetters.isChecked() ? 1 : 0 )},
                    new byte[]{(byte) (checkBoxDigits.isChecked() ? 1 : 0 )},
                    new byte[]{(byte) (checkBoxExtra.isChecked() ? 1 : 0 )},
                    new byte[]{(byte) (checkBoxLettersForce.isChecked() ? 1 : 0 )},
                    new byte[]{(byte) (checkBoxDigitsForce.isChecked() ? 1 : 0 )},
                    new byte[]{(byte) (checkBoxExtraForce.isChecked() ? 1 : 0 )});
        } else {
            EditText editTextMasterPassword =
                    (EditText) findViewById(R.id.editTextMasterPassword);
            byte[] password = UTF8.encode(editTextMasterPassword.getText());
            createKgkAndPasswordTask = new CreateKgkAndPasswordTask(iterations);
            createKgkAndPasswordTask.execute(password);
        }
    }

    private void clearMasterPassword() {
        EditText editTextMasterPassword = (EditText) findViewById(R.id.editTextMasterPassword);
        Editable password = editTextMasterPassword.getText();
        CharSequence zero = "0";
        for (int i = 0; i < password.length(); i++) {
            password.replace(i, i+1, zero);
        }
        editTextMasterPassword.setText("", TextView.BufferType.EDITABLE);
    }

    private void setToNotGenerated() {
        isGenerated = false;
        Button generateButton = (Button) findViewById(R.id.generatorButton);
        generateButton.setText(getResources().getString(R.string.generator_button));
        setIterationCountVisibility(View.INVISIBLE);
        invalidateOptionsMenu();
        TextView textViewPassword = (TextView) findViewById(R.id.textViewPassword);
        textViewPassword.setText("");
    }

    private void setCheckboxChecked(CheckBox cb, boolean checked) {
        cb.setChecked(checked);
        if (checked) {
            cb.setBackgroundColor(Color.TRANSPARENT);
        } else {
            cb.setBackgroundColor(Color.MAGENTA);
        }
    }

    private void loadSettings() {
        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        String domain = autoCompleteTextViewDomain.getText().toString();
        EditText editTextUsername =
                (EditText) findViewById(R.id.editTextUsername);
        CheckBox checkBoxSpecialCharacters =
                (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
        CheckBox checkBoxLetters =
                (CheckBox) findViewById(R.id.checkBoxLetters);
        CheckBox checkBoxDigits =
                (CheckBox) findViewById(R.id.checkBoxDigits);
        SeekBar seekBarLength =
                (SeekBar) findViewById(R.id.seekBarLength);
        TextView lengthLabel =
                (TextView) findViewById(R.id.textViewLengthDisplay);
        TextView legacyPassword =
                (TextView) findViewById(R.id.textViewLegacyPassword);
        PasswordSetting passwordSetting = settingsManager.getSetting(domain);
        this.showSettings = !passwordSetting.hasLegacyPassword() && domain.length() > 0;
        this.showLegacyPassword = passwordSetting.hasLegacyPassword();
        legacyPassword.setText(passwordSetting.getLegacyPassword());
        editTextUsername.setText(passwordSetting.getUsername());
        this.setCheckboxChecked(checkBoxLetters, passwordSetting.useLetters());
        this.setCheckboxChecked(checkBoxDigits, passwordSetting.useDigits());
        this.setCheckboxChecked(checkBoxSpecialCharacters, passwordSetting.useExtra());
        applyCheckboxLetters = false;
        applyCheckboxDigits = false;
        applyCheckboxExtra = false;
        seekBarLength.setProgress(passwordSetting.getLength() - 4);
        lengthLabel.setText(Integer.toString(passwordSetting.getLength()));
    }

    private void setButtonEnabledByDomainLength() {
        Button generateButton = (Button) findViewById(R.id.generatorButton);
        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        generateButton.setEnabled(autoCompleteTextViewDomain.getText().length() >= 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        kgkManager = new KgkManager(getBaseContext());
        settingsManager = new PasswordSettingsManager(getBaseContext());
        setContentView(R.layout.activity_main);
        setIterationCountVisibility(View.INVISIBLE);
        setDomainFieldFromClipboard();
        setButtonEnabledByDomainLength();
        EditText editTextMasterPassword = (EditText) findViewById(R.id.editTextMasterPassword);
        editTextMasterPassword.setText("", TextView.BufferType.EDITABLE);
        setToNotGenerated();
        clearMasterPassword();
        updateView();

        editTextMasterPassword.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable editable) {
                setToNotGenerated();
            }
        });

        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        autoCompleteTextViewDomain.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable editable) {
                setButtonEnabledByDomainLength();
                setToNotGenerated();
                showSettings = editable.length() > 0;
                showLegacyPassword = false;
                for (String domain : settingsManager.getDomainList()) {
                    if (domain.contentEquals(editable)) {
                        loadSettings();
                        break;
                    }
                }
                updateView();
            }
        });
        autoCompleteTextViewDomain.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!kgkManager.hasKgk()) {
                    EditText editTextMasterPassword =
                            (EditText) findViewById(R.id.editTextMasterPassword);
                    byte[] password = UTF8.encode(editTextMasterPassword.getText());
                    if (kgkManager.gelLocalKgkBlock().length == 112) {
                        loadSettingsTask = new LoadLocalSettingsTask(kgkManager, settingsManager);
                        loadSettingsTask.execute(password, kgkManager.getKgkCrypterSalt());
                    } else {
                        byte[] salt = Crypter.createSalt();
                        SharedPreferences savedDomains = getBaseContext().getSharedPreferences(
                                "savedDomains", Context.MODE_PRIVATE);
                        SharedPreferences.Editor savedDomainsEditor = savedDomains.edit();
                        savedDomainsEditor.putString("salt", Base64.encodeToString(
                                salt,
                                Base64.DEFAULT));
                        savedDomainsEditor.apply();
                        createNewKgkTask = new CreateNewKgkTask(
                                kgkManager, settingsManager);
                        createNewKgkTask.execute(password, salt);
                    }
                }
                if (!hasFocus && kgkManager.hasKgk()) {
                    loadSettings();
                }
            }
        });
        autoCompleteTextViewDomain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                loadSettings();
            }
        });

        final CheckBox checkBoxLetters =
                (CheckBox) findViewById(R.id.checkBoxLetters);
        checkBoxLetters.setOnCheckedChangeListener(
                new CheckBox.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton compoundButton,
                            boolean isChecked) {
                        if (!isChecked) {
                            CheckBox checkBoxForce =
                                (CheckBox) findViewById(R.id.checkBoxLettersForce);
                            checkBoxForce.setChecked(false);
                        }
                        compoundButton.setBackgroundColor(Color.TRANSPARENT);
                        applyCheckboxLetters = true;
                        setToNotGenerated();
                    }
                });
        CheckBox checkBoxLettersForce =
                (CheckBox) findViewById(R.id.checkBoxLettersForce);
        checkBoxLettersForce.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        if (checked) {
                            checkBoxLetters.setChecked(true);
                            setToNotGenerated();
                        }
                    }
                });
        final CheckBox checkBoxDigits =
                (CheckBox) findViewById(R.id.checkBoxDigits);
        checkBoxDigits.setOnCheckedChangeListener(
                new CheckBox.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton compoundButton,
                            boolean isChecked) {
                        if (!isChecked) {
                            CheckBox checkBoxForce =
                                    (CheckBox) findViewById(R.id.checkBoxDigitsForce);
                            checkBoxForce.setChecked(false);
                        }
                        compoundButton.setBackgroundColor(Color.TRANSPARENT);
                        applyCheckboxDigits = true;
                        setToNotGenerated();
                    }
                });
        CheckBox checkBoxDigitsForce =
                (CheckBox) findViewById(R.id.checkBoxDigitsForce);
        checkBoxDigitsForce.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        if (checked) {
                            checkBoxDigits.setChecked(true);
                            setToNotGenerated();
                        }
                    }
                });
        final CheckBox checkBoxSpecialCharacters =
                (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
        checkBoxSpecialCharacters.setOnCheckedChangeListener(
                new CheckBox.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton compoundButton,
                            boolean isChecked) {
                        if (!isChecked) {
                            CheckBox checkBoxForce =
                                    (CheckBox) findViewById(R.id.checkBoxSpecialCharacterForce);
                            checkBoxForce.setChecked(false);
                        }
                        compoundButton.setBackgroundColor(Color.TRANSPARENT);
                        applyCheckboxExtra = true;
                        setToNotGenerated();
                    }
                });
        final CheckBox checkBoxSpecialCharactersForce =
                (CheckBox) findViewById(R.id.checkBoxSpecialCharacterForce);
        checkBoxSpecialCharactersForce.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                        if (checked) {
                            checkBoxSpecialCharacters.setChecked(true);
                            setToNotGenerated();
                        }
                    }
                });

        SeekBar seekBarLength = (SeekBar) findViewById(R.id.seekBarLength);
        seekBarLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView textViewLengthDisplay =
                        (TextView) findViewById(R.id.textViewLengthDisplay);
                textViewLengthDisplay.setText(Integer.toString(progress + 4));
                setToNotGenerated();
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Button generateButton = (Button) findViewById(R.id.generatorButton);
        generateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Load fields
                AutoCompleteTextView autoCompleteTextViewDomain =
                        (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
                String domain = autoCompleteTextViewDomain.getText().toString();
                // Load iteration count from settings
                int iterations = settingsManager.getSetting(domain).getIterations();
                if (isGenerated) {
                    iterations++;
                }
                // Generate password
                generatePassword(iterations);
            }
        });
    }

    private boolean isAppInstalled(String packageName) {
        PackageManager pm = getPackageManager();
        boolean installed;
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isAppInstalled(syncAppName)) {
            // Bind to the service
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    syncAppName,
                    syncAppName + "." + syncServiceName));
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        setToNotGenerated();
        clearMasterPassword();
        super.onPause();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main_actions, menu);
        MenuItem copyItem = menu.findItem(R.id.action_copy);
        copyItem.setVisible(isGenerated);
        MenuItem syncItem = menu.findItem(R.id.action_sync);
        EditText editTextMasterPassword = (EditText) findViewById(R.id.editTextMasterPassword);
        syncItem.setVisible(
                isAppInstalled(syncAppName) &&
                (editTextMasterPassword.getText().length() > 0));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_copy) {
            TextView textViewPassword = (TextView) findViewById(R.id.textViewPassword);
            ClipData clipDataPassword = ClipData.newPlainText(
                    "password",
                    textViewPassword.getText()
            );
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(clipDataPassword);
            return true;
        }

        if (id == R.id.action_sync) {
            if (!mBound) {
                Log.d("Sync error", "Sync service is not bound. This button should not be visible.");
                return true;
            }
            Message msg = Message.obtain(null, REQUEST_SYNC, 0, 0);
            msg.replyTo = new Messenger(new ResponseHandler());
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                Log.d("Sync error", "Could not send message to sync service.");
                e.printStackTrace();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
