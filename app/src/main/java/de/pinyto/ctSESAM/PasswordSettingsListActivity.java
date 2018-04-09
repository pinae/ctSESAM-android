package de.pinyto.ctSESAM;

import android.content.Intent;
import android.os.Bundle;

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
    public void onSettingSelected(PasswordSetting setting) {
        Intent intent = new Intent(this, DomainDetailsActivity.class);
        intent.putExtra(UnlockActivity.KEYIVKEY, kgkManager.exportKeyIvAndReset());
        intent.putExtra(DOMAIN, setting.getDomain());
        startActivity(intent);
    }
}
