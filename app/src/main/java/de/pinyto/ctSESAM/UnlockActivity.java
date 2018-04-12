package de.pinyto.ctSESAM;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

public class UnlockActivity extends FragmentActivity
        implements LockScreenFragment.OnUnlockSuccessfulListener {
    public static final String KEYIVKEY = "de.pinyto.ctsesam.KEYIV";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);
        LockScreenFragment lockScreenFragment = (LockScreenFragment)
                getFragmentManager().findFragmentById(R.id.lockScreenFragment);
        lockScreenFragment.setUnlockSuccessfulListener(this);
    }

    @Override
    public void onUnlock(KgkManager kgkManager) {
        Log.d("initial KGK manager", kgkManager.toString());
        Intent intent = new Intent(this, PasswordSettingsListActivity.class);
        intent.putExtra(KEYIVKEY, kgkManager.exportKeyIv());
        startActivity(intent);
    }
}
