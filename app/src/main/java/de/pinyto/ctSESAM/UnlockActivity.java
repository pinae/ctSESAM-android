package de.pinyto.ctSESAM;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class UnlockActivity extends AppCompatActivity
        implements LockScreenFragment.OnUnlockSuccessfulListener,
        SyncResponseHandler.OnSyncFinishedListener {
    public static final String KEYIVKEY = "de.pinyto.ctsesam.KEYIV";
    public static final String FRESHLYUNLOCKED = "de.pinyto.ctsesam.FRESHLYUNLOCKED";
    protected Messenger syncServiceMessenger = null;
    protected boolean syncServiceBound;
    private byte[] masterpassword;

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

    @Override
    protected void onStart() {
        super.onStart();
        if (isAppInstalled(SyncResponseHandler.syncAppName)) {
            // Bind to the service
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    SyncResponseHandler.syncAppName,
                    SyncResponseHandler.syncAppName + "." +
                            SyncResponseHandler.syncServiceName));
            bindService(intent, syncServiceConnection, Context.BIND_AUTO_CREATE);
        }
        invalidateOptionsMenu();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.unlock_actions, menu);
        MenuItem initialSyncItem = menu.findItem(R.id.action_initial_sync);
        initialSyncItem.setVisible(isAppInstalled(SyncResponseHandler.syncAppName));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onUnlock(KgkManager kgkManager) {
        Intent intent = new Intent(this, PasswordSettingsListActivity.class);
        intent.putExtra(KEYIVKEY, kgkManager.exportKeyIv());
        intent.putExtra(FRESHLYUNLOCKED, true);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LockScreenFragment lockScreenFragment = (LockScreenFragment)
                getFragmentManager().findFragmentById(R.id.lockScreenFragment);
        switch (item.getItemId()) {
            case R.id.action_initial_sync:
                KgkManager kgkManager = lockScreenFragment.getKgkManager();
                PasswordSettingsManager settingsManager = lockScreenFragment.getSettingsManager();
                masterpassword = lockScreenFragment.getMasterpassword();
                kgkManager.deleteKgkAndSettings();
                kgkManager.reset();
                settingsManager.deleteAllSettings();
                if (!syncServiceBound) {
                    Log.e("Sync error", "Sync service is not bound. This button should not be visible.");
                    return true;
                }
                Message msg = Message.obtain(null, SyncResponseHandler.REQUEST_SYNC, 0, 0);
                msg.replyTo = new Messenger(new SyncResponseHandler(
                        this,
                        kgkManager,
                        settingsManager,
                        syncServiceMessenger,
                        syncServiceBound, true, masterpassword));
                try {
                    syncServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    Log.e("Sync error", "Could not send message to sync service.");
                    e.printStackTrace();
                }
                return true;
            case R.id.action_delete_settings:
                lockScreenFragment.deleteAllSettings();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSyncFinished(boolean success) {
        Clearer.zero(masterpassword);
        masterpassword = null;
        if (success) {
            Toast.makeText(getBaseContext(), R.string.sync_successful, Toast.LENGTH_SHORT).show();
            LockScreenFragment lockScreenFragment = (LockScreenFragment)
                    getFragmentManager().findFragmentById(R.id.lockScreenFragment);
            this.onUnlock(lockScreenFragment.getKgkManager());
        } else {
            Toast.makeText(getBaseContext(), R.string.sync_error, Toast.LENGTH_SHORT).show();
        }
    }

    private ServiceConnection syncServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            syncServiceMessenger = new Messenger(service);
            syncServiceBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            syncServiceMessenger = null;
            syncServiceBound = false;
        }
    };

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
}
