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
import android.os.Bundle;
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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.nio.ByteBuffer;


public class MainActivity extends AppCompatActivity {

    static final String syncAppName = "de.pinyto.ctSESAMsync";
    static final String syncServiceName = "SyncService";
    Messenger mService = null;
    boolean mBound;
    private PasswordSettingsManager settingsManager;
    private KgkManager kgkManager;
    private boolean isGenerated = false;
    private boolean showSettings = false;
    private boolean showLegacyPassword = false;
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

    public MainActivity getActivity() {
        return this;
    }

    public void setIsGenerated(boolean isGenerated) {
        this.isGenerated = isGenerated;
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

    public void setIterationCountVisibility(int visible) {
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
            GeneratePasswordTask generatePasswordTask = new GeneratePasswordTask(
                    this,
                    setting,
                    kgkManager,
                    settingsManager,
                    applyCheckboxLetters,
                    applyCheckboxDigits,
                    applyCheckboxExtra);
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
                    new byte[]{(byte) (checkBoxLetters.isChecked() ? 1 : 0)},
                    new byte[]{(byte) (checkBoxDigits.isChecked() ? 1 : 0)},
                    new byte[]{(byte) (checkBoxExtra.isChecked() ? 1 : 0)},
                    new byte[]{(byte) (checkBoxLettersForce.isChecked() ? 1 : 0)},
                    new byte[]{(byte) (checkBoxDigitsForce.isChecked() ? 1 : 0)},
                    new byte[]{(byte) (checkBoxExtraForce.isChecked() ? 1 : 0)});
        } else {
            EditText editTextMasterPassword =
                    (EditText) findViewById(R.id.editTextMasterPassword);
            byte[] password = UTF8.encode(editTextMasterPassword.getText());
            CreateKgkAndPasswordTask createKgkAndPasswordTask = new CreateKgkAndPasswordTask(
                    this,
                    iterations,
                    kgkManager,
                    settingsManager,
                    applyCheckboxLetters,
                    applyCheckboxDigits,
                    applyCheckboxExtra);
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
                        TextView loadingMessage =
                                (TextView) findViewById(R.id.textViewDecryptionMessage);
                        loadingMessage.setText(getString(R.string.loading));
                        LoadLocalSettingsTask loadLocalSettingsTask = new LoadLocalSettingsTask(
                                getActivity(),
                                kgkManager,
                                settingsManager);
                        Log.d("salt", Hextools.bytesToHex(kgkManager.getKgkCrypterSalt()));
                        loadLocalSettingsTask.execute(password, kgkManager.getKgkCrypterSalt());
                    } else {
                        kgkManager.storeSalt(Crypter.createSalt());
                        Log.d("new salt", Hextools.bytesToHex(kgkManager.getKgkCrypterSalt()));
                        CreateNewKgkTask createNewKgkTask = new CreateNewKgkTask(getActivity(),
                                kgkManager, settingsManager);
                        createNewKgkTask.execute(password, kgkManager.getKgkCrypterSalt());
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
            Message msg = Message.obtain(null, SyncResponseHandler.REQUEST_SYNC, 0, 0);
            msg.replyTo = new Messenger(new SyncResponseHandler(
                    this,
                    kgkManager,
                    settingsManager,
                    mService,
                    mBound));
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
