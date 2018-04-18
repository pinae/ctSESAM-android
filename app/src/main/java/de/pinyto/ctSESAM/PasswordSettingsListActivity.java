package de.pinyto.ctSESAM;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;

public class PasswordSettingsListActivity extends SyncServiceEnabledActivity
        implements PasswordSettingsListFragment.OnSettingSelected,
        PasswordSettingsListFragment.OnNewSetting {
    public static final String DOMAIN = "de.pinyto.ctsesam.DOMAIN";
    public static final String ISNEWSETTING = "de.pinyto.ctsesam.ISNEWSETTING";
    private PasswordSettingsListFragment listScreen;
    private boolean freshlyUnlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent.hasExtra(UnlockActivity.FRESHLYUNLOCKED)) {
            freshlyUnlocked = intent.getBooleanExtra(UnlockActivity.FRESHLYUNLOCKED, false);
        }
        setContentView(R.layout.activity_password_settings_list);
        listScreen = (PasswordSettingsListFragment) getFragmentManager().findFragmentById(
                R.id.passwordSettingsListFragment);
        Toolbar listToolbar = findViewById(R.id.list_toolbar);
        setSupportActionBar(listToolbar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (freshlyUnlocked) setDomainFieldFromClipboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listScreen.setKgkAndSettingsManager(kgkManager, settingsManager);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings_list_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onSettingSelected(PasswordSetting setting) {
        Intent intent = new Intent(this, DomainDetailsActivity.class);
        intent.putExtra(UnlockActivity.KGKMANAGER, kgkManager);
        intent.putExtra(DOMAIN, setting.getDomain());
        startActivity(intent);
    }

    @Override
    public void onNewSetting(PasswordSetting setting) {
        Intent intent = new Intent(this, DomainDetailsActivity.class);
        intent.putExtra(UnlockActivity.KGKMANAGER, kgkManager);
        intent.putExtra(DOMAIN, setting.getDomain());
        intent.putExtra(ISNEWSETTING, true);
        startActivity(intent);
    }

    private void setDomainFieldFromClipboard() {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clipDataCurrent = clipboard.getPrimaryClip();
            CharSequence pasteData = clipDataCurrent.getItemAt(0).getText();
            if (pasteData != null) {
                listScreen.setDomainFilter(pasteData);
            }
        }
    }
}
