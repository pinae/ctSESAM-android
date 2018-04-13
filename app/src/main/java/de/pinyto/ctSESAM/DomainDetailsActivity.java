package de.pinyto.ctSESAM;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

public class DomainDetailsActivity extends SyncServiceEnabledActivity {
    private PasswordSetting setting;
    private DomainDetailsFragment domainDetailsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_domain_details);
        Toolbar upAndCopyToolbar = (Toolbar) findViewById(R.id.up_and_copy_toolbar);
        setSupportActionBar(upAndCopyToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);
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
        domainDetailsFragment.setSettingsManagerAndKgkManager(settingsManager, kgkManager);
    }

    protected void setToNotGenerated() {
        super.setToNotGenerated();
        if (domainDetailsFragment != null) {
            domainDetailsFragment.clearPassword();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                upIntent.putExtra(UnlockActivity.KEYIVKEY, kgkManager.exportKeyIv());
                NavUtils.navigateUpTo(this, upIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
