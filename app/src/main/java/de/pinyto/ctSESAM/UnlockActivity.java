package de.pinyto.ctSESAM;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class UnlockActivity extends AppCompatActivity
        implements LockScreenFragment.OnUnlockSuccessfulListener {
    public static final String KEYIVKEY = "de.pinyto.ctsesam.KEYIV";
    public static final String FRESHLYUNLOCKED = "de.pinyto.ctsesam.FRESHLYUNLOCKED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);
        LockScreenFragment lockScreenFragment = (LockScreenFragment)
                getFragmentManager().findFragmentById(R.id.lockScreenFragment);
        lockScreenFragment.setUnlockSuccessfulListener(this);
        Toolbar syncToolbar = findViewById(R.id.sync_toolbar);
        setSupportActionBar(syncToolbar);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.unlock_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onUnlock(KgkManager kgkManager) {
        Log.d("initial KGK manager", kgkManager.toString());
        Intent intent = new Intent(this, PasswordSettingsListActivity.class);
        intent.putExtra(KEYIVKEY, kgkManager.exportKeyIv());
        intent.putExtra(FRESHLYUNLOCKED, true);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete_settings:
                LockScreenFragment lockScreenFragment = (LockScreenFragment)
                        getFragmentManager().findFragmentById(R.id.lockScreenFragment);
                lockScreenFragment.deleteAllSettings();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
