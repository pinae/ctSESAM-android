package de.pinyto.ctSESAM;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.ParseException;

/**
 * Handles the responses from the sync service if the sync app is installed.
 */
class SyncResponseHandler extends Handler {
    public static final String syncAppName = "de.pinyto.ctSESAMsync";
    public static final String syncServiceName = "SyncService";
    static final int REQUEST_SYNC = 1;
    private static final int SEND_UPDATE = 2;
    private static final int SYNC_RESPONSE = 1;
    private static final int SEND_UPDATE_RESPONSE = 2;
    private WeakReference<OnSyncFinishedListener> syncFinishedListenerWeakRef;
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    private Messenger mService;
    private boolean mBound;
    private boolean updateKgk;
    private byte[] masterpassword;

    SyncResponseHandler(OnSyncFinishedListener listener,
                        KgkManager kgkManager,
                        PasswordSettingsManager settingsManager,
                        Messenger mService,
                        boolean mBound,
                        boolean updateKgk,
                        byte[] masterpassword) {
        super();
        this.syncFinishedListenerWeakRef = new WeakReference<>(listener);
        this.kgkManager = kgkManager;
        this.settingsManager = settingsManager;
        this.mService = mService;
        this.mBound = mBound;
        this.updateKgk = updateKgk;
        this.masterpassword = masterpassword;
    }

    @Override
    public void handleMessage(Message msg) {
        OnSyncFinishedListener syncFinishedListener = syncFinishedListenerWeakRef.get();
        if (syncFinishedListener != null) {
            int respCode = msg.what;

            switch (respCode) {
                case SYNC_RESPONSE: {
                    String syncData = msg.getData().getString("respData");
                    try {
                        JSONObject syncDataObject = new JSONObject(syncData);
                        if (!syncDataObject.getString("status").equals("ok")) {
                            Log.e("Sync error", "The server did not respond with an 'OK'.");
                            syncFinishedListener.onSyncFinished(false);
                            break;
                        }
                        if (kgkManager == null || settingsManager == null) {
                            Log.e("Sync error", "receiving data structures are missing.");
                            syncFinishedListener.onSyncFinished(false);
                            break;
                        }
                        boolean updateRemote = true;
                        if (syncDataObject.has("result")) {
                            byte[] blob = Base64.decode(syncDataObject.getString("result"),
                                    Base64.DEFAULT);
                            if (updateKgk) {
                                byte[] password = this.masterpassword;
                                kgkManager.updateFromBlob(password, blob);
                            } else {
                                kgkManager.updateIv2Salt2(blob);
                            }
                            try {
                                updateRemote = settingsManager.updateFromExportData(
                                        kgkManager, blob);
                            } catch (JSONException e) {
                                Log.e("Update settings error", "Unable to read JSON data.");
                                e.printStackTrace();
                                syncFinishedListener.onSyncFinished(false);
                                break;
                            } catch (ParseException e) {
                                Log.e("Update settings error", "Unable to parse the date.");
                                e.printStackTrace();
                                syncFinishedListener.onSyncFinished(false);
                                break;
                            } catch (SyncDataFormatException e) {
                                Log.e("Version error", "Wrong data format. Could not import anything.");
                                e.printStackTrace();
                                syncFinishedListener.onSyncFinished(false);
                                break;
                            }
                        }
                        if (updateRemote) {
                            byte[] encryptedBlob = settingsManager.getExportData(kgkManager);
                            if (mService != null && mBound) {
                                Message updateMsg = Message.obtain(null, SEND_UPDATE, 0, 0);
                                updateMsg.replyTo = new Messenger(new SyncResponseHandler(
                                        syncFinishedListener,
                                        kgkManager,
                                        settingsManager,
                                        mService,
                                        mBound, false, new byte[]{}));
                                Bundle bUpdateMsg = new Bundle();
                                bUpdateMsg.putString("updatedData",
                                        Base64.encodeToString(encryptedBlob, Base64.DEFAULT));
                                updateMsg.setData(bUpdateMsg);
                                try {
                                    mService.send(updateMsg);
                                } catch (RemoteException e) {
                                    Log.e("Sync error",
                                            "Could not send update message to sync service.");
                                    e.printStackTrace();
                                    syncFinishedListener.onSyncFinished(false);
                                }
                            } else {
                                Log.e("Sync error", "No sync service connected.");
                                syncFinishedListener.onSyncFinished(false);
                            }
                        } else {
                            syncFinishedListener.onSyncFinished(true);
                        }
                    } catch (JSONException e) {
                        Log.e("Sync error", "The response is not valid JSON.");
                        e.printStackTrace();
                        syncFinishedListener.onSyncFinished(false);
                    }
                    break;
                }
                case SEND_UPDATE_RESPONSE: {
                    String updateRequestAnswer = msg.getData().getString("respData");
                    try {
                        JSONObject syncDataObject = new JSONObject(updateRequestAnswer);
                        if (syncDataObject.getString("status").equals("ok")) {
                            if (settingsManager != null) settingsManager.setAllSettingsToSynced();
                            syncFinishedListener.onSyncFinished(true);
                        } else {
                            syncFinishedListener.onSyncFinished(false);
                        }
                    } catch (JSONException e) {
                        Log.e("update Settings error", "Server response is not JSON.");
                        e.printStackTrace();
                        syncFinishedListener.onSyncFinished(false);
                    }
                    break;
                }
            }
        } else {
            Log.e("Sync notification error", "No success listener.");
        }
    }

    public interface OnSyncFinishedListener {
        void onSyncFinished(boolean success);
    }
}
