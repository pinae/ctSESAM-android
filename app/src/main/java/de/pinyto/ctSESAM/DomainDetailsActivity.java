package de.pinyto.ctSESAM;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class DomainDetailsActivity extends SyncServiceEnabledFragmentActivity {
    private PasswordSetting setting;
    private DomainDetailsFragment domainDetailsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("details create: KGK mng", kgkManager.toString());
        setContentView(R.layout.activity_domain_details);
        Intent intent = getIntent();
        setting = this.settingsManager.getSetting(
                intent.getStringExtra(PasswordSettingsListActivity.DOMAIN));
        boolean isNewSetting = intent.getBooleanExtra(
                PasswordSettingsListActivity.ISNEWSETTING, false);
        domainDetailsFragment = (DomainDetailsFragment) getFragmentManager().findFragmentById(
                R.id.domainDetailsFragment);
        domainDetailsFragment.setSettingsManagerAndKgkManager(settingsManager, kgkManager);
        domainDetailsFragment.setSetting(setting, isNewSetting);
        setToNotGenerated();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("details resume: KGK mng", kgkManager.toString());
        domainDetailsFragment.setSettingsManagerAndKgkManager(settingsManager, kgkManager);
    }

    protected void setToNotGenerated() {
        super.setToNotGenerated();
        if (domainDetailsFragment != null) {
            domainDetailsFragment.clearPassword();
        }
    }
}
