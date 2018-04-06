package de.pinyto.ctSESAM;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.AutoCompleteTextView;

public class DomainDetailsActivity extends SyncServiceEnabledFragmentActivity {
    private PasswordSetting setting;
    private DomainDetails domainDetailsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_domain_details);
        Intent intent = getIntent();
        setting = this.settingsManager.getSetting(
                intent.getStringExtra(PasswordSettingsListActivity.DOMAIN));
        domainDetailsFragment = (DomainDetails) getFragmentManager().findFragmentById(
                R.id.domainDetailsFragment);
    }

    protected void setToNotGenerated() {
        super.setToNotGenerated();
        domainDetailsFragment.clearPassword();
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
}
