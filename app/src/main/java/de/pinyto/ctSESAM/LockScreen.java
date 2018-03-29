package de.pinyto.ctSESAM;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


/**
 * A {@link Fragment} for the lock screen. Enter the
 * masterpassword here. This fragment will create a
 * {@link KgkManager} if the masterpassword is correct.
 * Activities that contain this fragment must implement the
 * {@link LockScreen.OnUnlockSuccessfulListener} interface
 * to handle interaction events.
 * Use the {@link LockScreen#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LockScreen extends Fragment {
    private OnUnlockSuccessfulListener unlockSuccessfulListener;
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    private EditText editTextMasterPassword;
    private TextView textViewDecryptionMessage;

    public LockScreen() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LockScreen.
     */
    public static LockScreen newInstance() {
        LockScreen fragment = new LockScreen();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fLayout = inflater.inflate(R.layout.fragment_lock_screen, container, false);
        editTextMasterPassword = (EditText) fLayout.findViewById(R.id.editTextMasterPassword);
        textViewDecryptionMessage = (TextView) fLayout.findViewById(R.id.textViewDecryptionMessage);
        Button unlockButton = (Button) fLayout.findViewById(R.id.unlockButton);
        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tryToUnlockKgk();
            }
        });
        return fLayout;
    }

    @Override
    public void onPause() {
        clearMasterPassword();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            unlockSuccessfulListener = (OnUnlockSuccessfulListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnUnlockSuccessfulListener");
        }
        kgkManager = new KgkManager(activity.getBaseContext());
        settingsManager = new PasswordSettingsManager(activity.getBaseContext());
        clearMasterPassword();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        unlockSuccessfulListener = null;
    }

    /**
     * Implement this interface to register if the user successfully
     * unlocked the KGK.
     */
    public interface OnUnlockSuccessfulListener {
        void onUnlock(KgkManager kgkManager);
    }

    private void clearMasterPassword() {
        Editable password = editTextMasterPassword.getText();
        CharSequence zero = "0";
        for (int i = 0; i < password.length(); i++) {
            password.replace(i, i+1, zero);
        }
        editTextMasterPassword.setText("", TextView.BufferType.EDITABLE);
    }

    private void setMessageTextStyle(boolean error) {
        if (error) {
            textViewDecryptionMessage.setTextColor(0xffa00000);
            textViewDecryptionMessage.setTextSize(50);
        } else {
            textViewDecryptionMessage.setTextColor(0xff000000);
            textViewDecryptionMessage.setTextSize(20);
        }
    }

    private void tryToUnlockKgk() {
        byte[] password = UTF8.encode(editTextMasterPassword.getText());
        if (kgkManager.gelLocalKgkBlock().length == 112) {
            setMessageTextStyle(false);
            textViewDecryptionMessage.setText(getString(R.string.loading));
            LoadLocalSettingsTask loadLocalSettingsTask = new LoadLocalSettingsTask(
                    new LoadLocalSettingsTask.OnKgkDecryptionFinishedListener() {
                        @Override
                        public void onFinished(boolean success) {
                            if (success) {
                                textViewDecryptionMessage.setText("");
                                unlockSuccessfulListener.onUnlock(kgkManager);
                            } else {
                                setMessageTextStyle(true);
                                textViewDecryptionMessage.setText(R.string.local_wrong_password);
                            }
                        }
                    },
                    kgkManager,
                    settingsManager);
            loadLocalSettingsTask.execute(password, kgkManager.getKgkCrypterSalt());
        } else {
            kgkManager.storeSalt(Crypter.createSalt());
            setMessageTextStyle(false);
            textViewDecryptionMessage.setText(getString(R.string.creatingKgk));
            CreateNewKgkTask createNewKgkTask = new CreateNewKgkTask(
                    new CreateNewKgkTask.OnNewKgkFinishedListener() {
                        @Override
                        public void onFinished(boolean success) {
                            if (success) {
                                setMessageTextStyle(false);
                                textViewDecryptionMessage.setText(
                                        getString(R.string.KgkCreationFinished));
                            } else {
                                setMessageTextStyle(true);
                                textViewDecryptionMessage.setText(
                                        getString(R.string.KgkCreationError));
                            }
                        }
                    },
                    kgkManager,
                    settingsManager);
            createNewKgkTask.execute(password, kgkManager.getKgkCrypterSalt());
        }
    }
}
