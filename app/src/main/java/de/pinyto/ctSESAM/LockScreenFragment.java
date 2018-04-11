package de.pinyto.ctSESAM;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;


/**
 * A {@link Fragment} for the lock screen. Enter the
 * masterpassword here. This fragment will create a
 * {@link KgkManager} if the masterpassword is correct.
 * Activities that contain this fragment must implement the
 * {@link LockScreenFragment.OnUnlockSuccessfulListener} interface
 * to handle interaction events.
 * Use the {@link LockScreenFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LockScreenFragment extends Fragment {
    private OnUnlockSuccessfulListener unlockSuccessfulListener;
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    private EditText editTextMasterPassword;
    private TextView textViewDecryptionMessage;
    private Button unlockButton;
    private Button deleteSettingsButton;
    private LoadLocalSettingsTask.OnKgkDecryptionFinishedListener kgkDecryptionFinishedListener;
    private CreateNewKgkTask.OnNewKgkFinishedListener newKgkFinishedListener;

    public LockScreenFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LockScreenFragment.
     */
    public static LockScreenFragment newInstance() {
        LockScreenFragment fragment = new LockScreenFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fLayout = inflater.inflate(R.layout.fragment_lock_screen, container, false);
        editTextMasterPassword = (EditText) fLayout.findViewById(R.id.editTextMasterPassword);
        textViewDecryptionMessage = (TextView) fLayout.findViewById(R.id.textViewDecryptionMessage);
        unlockButton = (Button) fLayout.findViewById(R.id.unlockButton);
        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tryToUnlockKgk();
            }
        });
        deleteSettingsButton = (Button) fLayout.findViewById(R.id.deleteKgkButton);
        deleteSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsManager.deleteAllSettings();
                kgkManager.deleteKgkAndSettings();
                kgkManager.reset();
                unlockButton.setText(R.string.create_new_kgk);
            }
        });
        Switch expertOptionsSwitch = (Switch) fLayout.findViewById(R.id.showExpertOptinsSwitch);
        expertOptionsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                deleteSettingsButton.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
            }
        });
        return fLayout;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (kgkManager.getLocalKgkBlock().length == 112) {
            unlockButton.setText(R.string.unlock);
        } else {
            unlockButton.setText(R.string.create_new_kgk);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
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

    public void setUnlockSuccessfulListener(OnUnlockSuccessfulListener listener) {
        this.unlockSuccessfulListener = listener;
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
            textViewDecryptionMessage.setTextColor(0xffbc0000);
            textViewDecryptionMessage.setTextSize(30);
        } else {
            textViewDecryptionMessage.setTextColor(0xff000000);
            textViewDecryptionMessage.setTextSize(20);
        }
    }

    private void tryToUnlockKgk() {
        byte[] password = UTF8.encode(editTextMasterPassword.getText());
        if (kgkManager.getLocalKgkBlock().length == 112) {
            setMessageTextStyle(false);
            textViewDecryptionMessage.setText(getString(R.string.loading));
            kgkDecryptionFinishedListener =
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
            };
            LoadLocalSettingsTask loadLocalSettingsTask = new LoadLocalSettingsTask(
                    kgkDecryptionFinishedListener,
                    kgkManager,
                    settingsManager);
            loadLocalSettingsTask.execute(password, kgkManager.getKgkCrypterSalt());
        } else {
            kgkManager.deleteKgkAndSettings();
            kgkManager.reset();
            settingsManager.deleteAllSettings();
            kgkManager.storeSalt(Crypter.createSalt());
            setMessageTextStyle(false);
            textViewDecryptionMessage.setText(getString(R.string.creatingKgk));
            newKgkFinishedListener = new CreateNewKgkTask.OnNewKgkFinishedListener() {
                @Override
                public void onFinished(boolean success) {
                    if (success) {
                        setMessageTextStyle(false);
                        textViewDecryptionMessage.setText(
                                getString(R.string.KgkCreationFinished));
                        settingsManager.storeLocalSettings(kgkManager);
                        unlockSuccessfulListener.onUnlock(kgkManager);
                    } else {
                        setMessageTextStyle(true);
                        textViewDecryptionMessage.setText(
                                getString(R.string.KgkCreationError));
                    }
                }
            };
            CreateNewKgkTask createNewKgkTask = new CreateNewKgkTask(
                    newKgkFinishedListener,
                    kgkManager,
                    settingsManager);
            createNewKgkTask.execute(password, kgkManager.getKgkCrypterSalt());
        }
    }
}
