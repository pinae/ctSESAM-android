package de.pinyto.ctSESAM;

import android.content.Intent;
import android.os.Bundle;

public class DomainDetailsActivity extends SyncServiceEnabledFragmentActivity {
    private PasswordSetting setting;
    private DomainDetailsFragment domainDetailsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_domain_details);
        Intent intent = getIntent();
        setting = this.settingsManager.getSetting(
                intent.getStringExtra(PasswordSettingsListActivity.DOMAIN));
        domainDetailsFragment = (DomainDetailsFragment) getFragmentManager().findFragmentById(
                R.id.domainDetailsFragment);
        domainDetailsFragment.setSetting(setting);
        setToNotGenerated();
    }

    protected void setToNotGenerated() {
        super.setToNotGenerated();
        if (domainDetailsFragment != null) {
            domainDetailsFragment.clearPassword();
        }
    }
}
