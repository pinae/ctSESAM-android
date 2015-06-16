package de.pinyto.passwordgenerator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;


public class MainActivity extends AppCompatActivity {

    static final String syncAppName = "de.pinyto.passwordsettingssync";
    static final String syncServiceName = "SyncService";
    Messenger mService = null;
    boolean mBound;
    static final int REQUEST_SYNC = 1;
    static final int SEND_UPDATE = 2;
    static final int SYNC_RESPONSE = 1;
    static final int SEND_UPDATE_RESPONSE = 2;

    private PasswordSettingsManager settingsManager;
    private boolean isGenerated = false;

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
                            CharBuffer passwordCharBuffer = CharBuffer.wrap(
                                    editTextMasterPassword.getText());
                            ByteBuffer passwordByteBuffer = Charset.forName("UTF-8").encode(
                                    passwordCharBuffer);
                            byte[] password = passwordByteBuffer.array();
                            boolean changed = settingsManager.updateFromExportData(
                                    password, Base64.decode(syncDataObject.getString("result"),
                                            Base64.DEFAULT));
                            if (changed) {
                                byte[] encryptedBlob = settingsManager.getExportData(password);
                                for (int i = 0; i < password.length; i++) {
                                    password[i] = 0x00;
                                }
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

    private void loadAutoCompleteFromSettings() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, settingsManager.getDomainList());
        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        autoCompleteTextViewDomain.setAdapter(adapter);
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

    private String generatePassword(int iterations) {
        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        byte[] domain = UTF8.encode(autoCompleteTextViewDomain.getText());
        EditText editTextMasterPassword =
                (EditText) findViewById(R.id.editTextMasterPassword);
        byte[] password = UTF8.encode(editTextMasterPassword.getText());
        String generatedPassword;
        PasswordGenerator generator = new PasswordGenerator(domain, password);
        try {
            generator.hash(iterations);
            CheckBox checkBoxSpecialCharacters =
                    (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
            CheckBox checkBoxLetters =
                    (CheckBox) findViewById(R.id.checkBoxLetters);
            CheckBox checkBoxDigits =
                    (CheckBox) findViewById(R.id.checkBoxDigits);
            SeekBar seekBarLength =
                    (SeekBar) findViewById(R.id.seekBarLength);
            generatedPassword = generator.getPassword(
                    checkBoxSpecialCharacters.isChecked(),
                    checkBoxLetters.isChecked(),
                    checkBoxDigits.isChecked(),
                    seekBarLength.getProgress() + 4);
        } catch (NotHashedException e) {
            e.printStackTrace();
            generatedPassword = "Not hashed.";
        }
        for (int i = 0; i < password.length; i++) {
            password[i] = 0x00;
        }
        return generatedPassword;
    }

    private void saveSettings(int iterations) {
        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        CheckBox checkBoxSpecialCharacters =
                (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
        CheckBox checkBoxLetters =
                (CheckBox) findViewById(R.id.checkBoxLetters);
        CheckBox checkBoxDigits =
                (CheckBox) findViewById(R.id.checkBoxDigits);
        SeekBar seekBarLength =
                (SeekBar) findViewById(R.id.seekBarLength);
        PasswordSetting newSetting = settingsManager.getSetting(
                autoCompleteTextViewDomain.getText().toString());
        newSetting.setUseLetters(checkBoxLetters.isChecked());
        newSetting.setUseDigits(checkBoxDigits.isChecked());
        newSetting.setUseExtra(checkBoxSpecialCharacters.isChecked());
        newSetting.setLength(seekBarLength.getProgress() + 4);
        newSetting.setIterations(iterations);
        newSetting.setModificationDateToNow();
        settingsManager.saveSetting(newSetting);
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

    private void loadSettings() {
        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        String domain = autoCompleteTextViewDomain.getText().toString();
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
        PasswordSetting passwordSetting = settingsManager.getSetting(domain);
        checkBoxLetters.setChecked(passwordSetting.useLetters());
        checkBoxDigits.setChecked(passwordSetting.useDigits());
        checkBoxSpecialCharacters.setChecked(passwordSetting.useExtra());
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
        settingsManager = new PasswordSettingsManager(getBaseContext());
        setContentView(R.layout.activity_main);
        setIterationCountVisibility(View.INVISIBLE);
        loadAutoCompleteFromSettings();
        setDomainFieldFromClipboard();
        loadSettings();
        setButtonEnabledByDomainLength();
        EditText editTextMasterPassword = (EditText) findViewById(R.id.editTextMasterPassword);
        editTextMasterPassword.setText("", TextView.BufferType.EDITABLE);
        setToNotGenerated();
        clearMasterPassword();

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
                TextView textViewPassword = (TextView) findViewById(R.id.textViewPassword);
                textViewPassword.setText(generatePassword(iterations));
                isGenerated = true;
                invalidateOptionsMenu();
                Button generateButton = (Button) findViewById(R.id.generatorButton);
                generateButton.setText(getResources().getString(R.string.re_generator_button));
                TextView textViewIterationCount =
                        (TextView) findViewById(R.id.iterationCount);
                textViewIterationCount.setText(Integer.toString(iterations));
                setIterationCountVisibility(View.VISIBLE);
                // Save domain and settings
                saveSettings(iterations);
                loadAutoCompleteFromSettings();
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
                loadSettings();
                setButtonEnabledByDomainLength();
                setToNotGenerated();
            }
        });

        editTextMasterPassword.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable editable) {
                setToNotGenerated();
            }
        });

        CheckBox.OnCheckedChangeListener settingCheckboxChange =
                new CheckBox.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton compoundButton,
                            boolean isChecked) {
                        setToNotGenerated();
                    }
                };
        CheckBox checkBoxSpecialCharacters =
                (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
        checkBoxSpecialCharacters.setOnCheckedChangeListener(settingCheckboxChange);
        CheckBox checkBoxLetters =
                (CheckBox) findViewById(R.id.checkBoxLetters);
        checkBoxLetters.setOnCheckedChangeListener(settingCheckboxChange);
        CheckBox checkBoxDigits =
                (CheckBox) findViewById(R.id.checkBoxDigits);
        checkBoxDigits.setOnCheckedChangeListener(settingCheckboxChange);
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
