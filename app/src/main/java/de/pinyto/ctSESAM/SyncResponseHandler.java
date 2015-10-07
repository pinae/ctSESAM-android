package de.pinyto.ctSESAM;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Handles the responses from the sync service if the sync app is installed.
 */
class SyncResponseHandler extends Handler {
    static final int REQUEST_SYNC = 1;
    static final int SEND_UPDATE = 2;
    static final int SYNC_RESPONSE = 1;
    static final int SEND_UPDATE_RESPONSE = 2;
    private WeakReference<MainActivity> mainActivityWeakRef;
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    Messenger mService = null;
    boolean mBound;

    SyncResponseHandler(MainActivity mainActivity,
                        KgkManager kgkManager,
                        PasswordSettingsManager settingsManager,
                        Messenger mService,
                        boolean mBound) {
        super();
        this.mainActivityWeakRef = new WeakReference<>(mainActivity);
        this.kgkManager = kgkManager;
        this.settingsManager = settingsManager;
        this.mService = mService;
        this.mBound = mBound;
    }

    @Override
    public void handleMessage(Message msg) {
        MainActivity activity = mainActivityWeakRef.get();
        if (activity != null && !activity.isFinishing()) {
            int respCode = msg.what;

            switch (respCode) {
                case SYNC_RESPONSE: {
                    String syncData = msg.getData().getString("respData");
                    try {
                        JSONObject syncDataObject = new JSONObject(syncData);
                        if (!syncDataObject.getString("status").equals("ok")) break;
                        boolean updateRemote = true;
                        EditText editTextMasterPassword = (EditText) activity.findViewById(
                                R.id.editTextMasterPassword);
                        if (syncDataObject.has("result")) {
                            byte[] password = UTF8.encode(editTextMasterPassword.getText());
                            byte[] blob = Base64.decode(syncDataObject.getString("result"),
                                    Base64.DEFAULT);
                            kgkManager.updateFromBlob(password, blob);
                            updateRemote = settingsManager.updateFromExportData(
                                    kgkManager, blob);
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    activity.getBaseContext(),
                                    android.R.layout.simple_dropdown_item_1line,
                                    settingsManager.getDomainList());
                            AutoCompleteTextView autoCompleteTextViewDomain =
                                    (AutoCompleteTextView) activity.findViewById(
                                            R.id.autoCompleteTextViewDomain);
                            autoCompleteTextViewDomain.setAdapter(adapter);
                            Toast.makeText(activity.getBaseContext(),
                                    R.string.sync_loaded, Toast.LENGTH_SHORT).show();
                            Clearer.zero(password);
                        }
                        if (updateRemote) {
                            byte[] encryptedBlob = settingsManager.getExportData(kgkManager);
                            if (mService != null && mBound) {
                                Message updateMsg = Message.obtain(null, SEND_UPDATE, 0, 0);
                                updateMsg.replyTo = new Messenger(new SyncResponseHandler(
                                        activity,
                                        kgkManager,
                                        settingsManager,
                                        mService,
                                        mBound));
                                Bundle bUpdateMsg = new Bundle();
                                bUpdateMsg.putString("updatedData",
                                        Base64.encodeToString(encryptedBlob, Base64.DEFAULT));
                                updateMsg.setData(bUpdateMsg);
                                try {
                                    mService.send(updateMsg);
                                } catch (RemoteException e) {
                                    Log.d("Sync error",
                                            "Could not send update message to sync service.");
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.d("Sync error", "The response is not valid JSON.");
                        e.printStackTrace();
                    }
                    break;
                }
                case SEND_UPDATE_RESPONSE: {
                    String updateRequestAnswer = msg.getData().getString("respData");
                    try {
                        JSONObject syncDataObject = new JSONObject(updateRequestAnswer);
                        if (syncDataObject.getString("status").equals("ok")) {
                            settingsManager.setAllSettingsToSynced();
                            Toast.makeText(activity.getBaseContext(),
                                    R.string.sync_successful, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(activity.getBaseContext(),
                                    R.string.sync_error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.d("update Settings error", "Server response is not JSON.");
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }
}
