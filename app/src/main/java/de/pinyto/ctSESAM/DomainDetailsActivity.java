package de.pinyto.ctSESAM;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class DomainDetailsActivity extends SyncServiceEnabledActivity
        implements DomainDetailsFragment.OnPasswordGeneratedListener {
    private PasswordSetting setting;
    private DomainDetailsFragment domainDetailsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_domain_details);
        Toolbar upAndCopyToolbar = findViewById(R.id.up_and_copy_toolbar);
        setSupportActionBar(upAndCopyToolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        try {
            setting = (PasswordSetting) this.settingsManager.getSetting(
                    intent.getStringExtra(PasswordSettingsListActivity.DOMAIN)).clone();
        } catch (CloneNotSupportedException e) {
            Log.e("Unable to clone setting", e.toString());
        }
        boolean isNewSetting = intent.getBooleanExtra(
                PasswordSettingsListActivity.ISNEWSETTING, false);
        domainDetailsFragment = (DomainDetailsFragment) getFragmentManager().findFragmentById(
                R.id.domainDetailsFragment);
        domainDetailsFragment.setPasswordGeneratedListener(this);
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
    public void onPasswordGenerated() {
        invalidateOptionsMenu();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.domain_details_actions, menu);
        MenuItem copyItem = menu.findItem(R.id.action_copy);
        copyItem.setVisible(domainDetailsFragment.hasPassword());
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_copy:
                TextView textViewPassword = findViewById(R.id.editTextPassword);
                ClipData clipDataPassword = ClipData.newPlainText(
                        "password",
                        textViewPassword.getText()
                );
                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clipDataPassword);
                }
                return true;
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                upIntent.putExtra(UnlockActivity.KGKMANAGER, kgkManager);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
