package de.pinyto.passwordgenerator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;


public class MainActivity extends AppCompatActivity {

    static final String syncAppName = "de.pinyto.passwordsettingssync";
    static final String syncServiceName = "SyncService";
    Messenger mService = null;
    boolean mBound;
    static final int REQUEST_SYNC = 1;
    static final int SEND_UPDATE = 2;
    static final int SYNC_RESPONSE = 1;
    static final int SEND_UPDATE_RESPONSE = 2;

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
                        byte[] password;
                        try {
                            password = editTextMasterPassword.getText().toString().getBytes(
                                    "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            Log.d("Key generation error",
                                    "UTF-8 is not supported. Using default encoding.");
                            password = editTextMasterPassword.getText().toString().getBytes();
                        }
                        Crypter crypter = new Crypter(password);
                        SettingsPacker packer = new SettingsPacker(getBaseContext());
                        boolean changed = false;
                        if (syncDataObject.has("result")) {
                            byte[] decrypted = crypter.decrypt(Base64.decode(
                                    syncDataObject.getString("result"),
                                    Base64.DEFAULT));
                            if (decrypted.length > 0) {
                                changed = packer.updateFromBlob(decrypted);
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        R.string.wrong_password, Toast.LENGTH_SHORT).show();
                            }
                        }
                        if (changed) {
                            byte[] blob = packer.getBlob();
                            byte[] encryptedBlob = crypter.encrypt(blob);
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
        SharedPreferences savedDomains = getSharedPreferences("savedDomains", MODE_PRIVATE);
        Set<String> domainSet = savedDomains.getStringSet("domainSet", new HashSet<String>());
        if (domainSet != null) {
            String[] domainList = new String[domainSet.size()];
            Iterator it = domainSet.iterator();
            int i = 0;
            while (it.hasNext()) {
                domainList[i] = (String) it.next();
                i++;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, domainList);
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
            autoCompleteTextViewDomain.setAdapter(adapter);
        }
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
        String domain = autoCompleteTextViewDomain.getText().toString();
        EditText editTextMasterPassword =
                (EditText) findViewById(R.id.editTextMasterPassword);
        PasswordGenerator generator = new PasswordGenerator(
                domain,
                editTextMasterPassword.getText().toString());
        generator.hash(iterations);
        CheckBox checkBoxSpecialCharacters =
                (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
        CheckBox checkBoxLetters =
                (CheckBox) findViewById(R.id.checkBoxLetters);
        CheckBox checkBoxNumbers =
                (CheckBox) findViewById(R.id.checkBoxNumbers);
        SeekBar seekBarLength =
                (SeekBar) findViewById(R.id.seekBarLength);
        return generator.getPassword(
                checkBoxSpecialCharacters.isChecked(),
                checkBoxLetters.isChecked(),
                checkBoxNumbers.isChecked(),
                seekBarLength.getProgress() + 4);
    }

    private void saveSettings(int iterations) {
        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        String domain = autoCompleteTextViewDomain.getText().toString();
        CheckBox checkBoxSpecialCharacters =
                (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
        CheckBox checkBoxLetters =
                (CheckBox) findViewById(R.id.checkBoxLetters);
        CheckBox checkBoxNumbers =
                (CheckBox) findViewById(R.id.checkBoxNumbers);
        SeekBar seekBarLength =
                (SeekBar) findViewById(R.id.seekBarLength);
        SharedPreferences savedDomains = getSharedPreferences("savedDomains", MODE_PRIVATE);
        Set<String> domainSet = savedDomains.getStringSet(
                "domainSet",
                new HashSet<String>()
        );
        if (domainSet != null) {
            domainSet.add(domain);
        }
        SharedPreferences.Editor savedDomainsEditor = savedDomains.edit();
        savedDomainsEditor.putStringSet("domainSet", domainSet);
        savedDomainsEditor.putBoolean(
                domain + "_letters",
                checkBoxLetters.isChecked()
        );
        savedDomainsEditor.putBoolean(
                domain + "_numbers",
                checkBoxNumbers.isChecked()
        );
        savedDomainsEditor.putBoolean(
                domain + "_special_characters",
                checkBoxSpecialCharacters.isChecked()
        );
        savedDomainsEditor.putInt(
                domain + "_length",
                seekBarLength.getProgress() + 4
        );
        savedDomainsEditor.putInt(
                domain + "_iterations",
                iterations
        );
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        String cDate = savedDomains.getString(domain + "_cDate", "");
        int cDateLength = 0;
        try {
            assert cDate != null;
            cDateLength = cDate.length();
        } catch (NullPointerException | AssertionError e) {
            e.printStackTrace();
        }
        if (cDateLength < 1) {
            savedDomainsEditor.putString(
                    domain + "_cDate",
                    df.format(Calendar.getInstance().getTime())
            );
        }
        savedDomainsEditor.putString(
                domain + "_mDate",
                df.format(Calendar.getInstance().getTime())
        );
        savedDomainsEditor.apply();
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
        SharedPreferences savedDomains = getSharedPreferences("savedDomains", MODE_PRIVATE);
        Set<String> domainSet = savedDomains.getStringSet(
                "domainSet",
                new HashSet<String>()
        );
        if (domainSet != null) {
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
            String domain = autoCompleteTextViewDomain.getText().toString();
            if (domainSet.contains(domain)) {
                CheckBox checkBoxSpecialCharacters =
                        (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
                CheckBox checkBoxLetters =
                        (CheckBox) findViewById(R.id.checkBoxLetters);
                CheckBox checkBoxNumbers =
                        (CheckBox) findViewById(R.id.checkBoxNumbers);
                SeekBar seekBarLength =
                        (SeekBar) findViewById(R.id.seekBarLength);
                checkBoxLetters.setChecked(
                        savedDomains.getBoolean(domain + "_letters", true)
                );
                checkBoxNumbers.setChecked(
                        savedDomains.getBoolean(domain + "_numbers", true)
                );
                checkBoxSpecialCharacters.setChecked(
                        savedDomains.getBoolean(domain + "_special_characters", true)
                );
                seekBarLength.setProgress(
                        savedDomains.getInt(domain + "_length", 10) - 4
                );
            }
        }
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
                SharedPreferences savedDomains = getSharedPreferences("savedDomains", MODE_PRIVATE);
                int iterations = savedDomains.getInt(
                        domain + "_iterations",
                        4096
                );
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
        CheckBox checkBoxNumbers =
                (CheckBox) findViewById(R.id.checkBoxNumbers);
        checkBoxNumbers.setOnCheckedChangeListener(settingCheckboxChange);
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
