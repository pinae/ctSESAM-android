package de.pinyto.ctSESAM;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

public class UnlockActivity extends FragmentActivity implements LockScreen.OnUnlockSuccessfulListener {
    public static final String KEYIVKEY = "de.pinyto.ctsesam.KEYIV";
    private LockScreen lockScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock);
        lockScreen = (LockScreen) getFragmentManager().findFragmentById(R.id.lockScreenFragment);
        lockScreen.setUnlockSuccessfulListener(this);
    }

    @Override
    public void onUnlock(KgkManager kgkManager) {
        Intent intent = new Intent(this, PasswordSettingsListActivity.class);
        intent.putExtra(KEYIVKEY, kgkManager.exportKeyIvAndReset());
        startActivity(intent);
    }
}
