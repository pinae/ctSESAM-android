package de.pinyto.ctSESAM;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.widget.SeekBar;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class MainActivity extends AppCompatActivity {

    static final String syncAppName = "de.pinyto.ctSESAMsync";
    static final String syncServiceName = "SyncService";
    Messenger mService = null;
    boolean mBound;
    private PasswordSettingsManager settingsManager;
    private KgkManager kgkManager;
    private PasswordGenerator passwordGenerator;
    private boolean showSettings = false;
    private boolean showPassword = false;

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

    public void setPasswordGenerator(PasswordGenerator generator) {
        this.passwordGenerator = generator;
    }

    private void updateView() {
        Button generateButton = (Button) findViewById(R.id.generatorButton);
        TextView passwordHeading = (TextView) findViewById(R.id.textViewPasswordHeading);
        TextView password = (TextView) findViewById(R.id.textViewPassword);
        if (this.showSettings) {
            if (this.showPassword) {
                generateButton.setVisibility(View.INVISIBLE);
                passwordHeading.setVisibility(View.VISIBLE);
                password.setVisibility(View.VISIBLE);
            } else {
                generateButton.setVisibility(View.VISIBLE);
                passwordHeading.setVisibility(View.INVISIBLE);
                password.setVisibility(View.INVISIBLE);
            }
        } else {
            generateButton.setVisibility(View.INVISIBLE);
            passwordHeading.setVisibility(View.INVISIBLE);
            password.setVisibility(View.INVISIBLE);
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

    public void generatePassword() {
        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        String domainStr = autoCompleteTextViewDomain.getText().toString();
        byte[] domain = UTF8.encode(autoCompleteTextViewDomain.getText());
        EditText editTextUsername =
                (EditText) findViewById(R.id.editTextUsername);
        TextView textViewPassword = (TextView) findViewById(R.id.textViewPassword);
        byte[] username = UTF8.encode(editTextUsername.getText());
        PasswordSetting setting = this.settingsManager.getSetting(domainStr);
        if (this.kgkManager.hasKgk()) {
            if (!setting.hasLegacyPassword()) {
                if (this.passwordGenerator == null) {
                    GeneratePasswordTask generatePasswordTask = new GeneratePasswordTask(this);
                    if (setting.getIterations() <= 0) {
                        setting.setIterations(4096);
                    }
                    generatePasswordTask.execute(
                            domain,
                            username,
                            kgkManager.getKgk(),
                            setting.getSalt(),
                            ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
                                    .putInt(setting.getIterations()).array());
                } else {
                    this.settingsManager.setSetting(setting);
                    this.settingsManager.storeLocalSettings(this.kgkManager);
                    textViewPassword.setText(this.passwordGenerator.getPassword(setting));
                    this.showPassword = true;
                    this.updateView();
                    this.invalidateOptionsMenu();
                    // load settings because the domain might be new
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            this.settingsManager.getDomainList());
                    autoCompleteTextViewDomain.setAdapter(adapter);
                }
            }
        } else {
            EditText editTextMasterPassword =
                    (EditText) findViewById(R.id.editTextMasterPassword);
            byte[] password = UTF8.encode(editTextMasterPassword.getText());
            CreateKgkAndPasswordTask createKgkAndPasswordTask = new CreateKgkAndPasswordTask(
                    this,
                    setting.getIterations(),
                    this.kgkManager,
                    this.settingsManager);
            createKgkAndPasswordTask.execute(password);
        }
        if (setting.hasLegacyPassword()) {
            textViewPassword.setText(setting.getLegacyPassword());
            this.showPassword = true;
            this.updateView();
            this.invalidateOptionsMenu();
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
        this.passwordGenerator = null;
        invalidateOptionsMenu();
        TextView textViewPassword = (TextView) findViewById(R.id.textViewPassword);
        textViewPassword.setText("");
    }

    private void loadSettings() {
        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        String domain = autoCompleteTextViewDomain.getText().toString();
        EditText editTextUsername =
                (EditText) findViewById(R.id.editTextUsername);
        PasswordSetting passwordSetting = settingsManager.getSetting(domain);
        this.showSettings = domain.length() > 0;
        editTextUsername.setText(passwordSetting.getUsername());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        kgkManager = new KgkManager(getBaseContext());
        settingsManager = new PasswordSettingsManager(getBaseContext());
        setContentView(R.layout.activity_main);
        setDomainFieldFromClipboard();
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
                setToNotGenerated();
                showSettings = editable.length() > 0;
                showPassword = false;
                for (String domain : settingsManager.getDomainList()) {
                    if (domain.contentEquals(editable)) {
                        loadSettings();
                        generatePassword();
                        showPassword = true;
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
                        loadLocalSettingsTask.execute(password, kgkManager.getKgkCrypterSalt());
                    } else {
                        kgkManager.storeSalt(Crypter.createSalt());
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

        Button generateButton = (Button) findViewById(R.id.generatorButton);
        generateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                generatePassword();
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
        copyItem.setVisible(this.passwordGenerator != null);
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
