package de.pinyto.ctSESAM;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.AutoCompleteTextView;

public class PasswordSettingsListActivity extends SyncServiceEnabledFragmentActivity
        implements PasswordSettingsListFragment.OnSettingSelected {
    public static final String DOMAIN = "de.pinyto.ctsesam.DOMAIN";
    private PasswordSettingsListFragment listScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_settings_list);
        listScreen = (PasswordSettingsListFragment) getFragmentManager().findFragmentById(
                R.id.passwordSettingsListFragment);
        listScreen.setSettingSelectedListener(this);
        listScreen.setSettingsManager(settingsManager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setDomainFieldFromClipboard();
    }

    @Override
    public void onSettingSelected(PasswordSetting setting) {
        Intent intent = new Intent(this, DomainDetailsActivity.class);
        intent.putExtra(UnlockActivity.KEYIVKEY, kgkManager.exportKeyIvAndReset());
        intent.putExtra(DOMAIN, setting.getDomain());
        startActivity(intent);
    }

    private void setDomainFieldFromClipboard() {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData clipDataCurrent = clipboard.getPrimaryClip();
            CharSequence pasteData = clipDataCurrent.getItemAt(0).getText();
            if (pasteData != null) {
                listScreen.setDomainFilter(pasteData);
            }
        }
    }
}
