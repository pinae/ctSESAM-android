package de.pinyto.ctSESAM;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;


/**
 * A {@link Fragment} for account details.
 * Activities that contain this fragment must implement the
 * {@link DomainDetailsFragment.OnPasswordGeneratedListener} interface
 * to handle interaction events.
 */
public class DomainDetailsFragment extends Fragment
        implements SmartSelector.OnStrengthSelectedEventListener,
        GeneratePasswordTask.OnPasswordGeneratedListener {
    public static final String KGKMANAGER = "de.pinyto.ctsesam.KGKMANAGER";
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    private PasswordSetting setting;
    private boolean isNewSetting;
    private PasswordGenerator passwordGenerator;
    private OnPasswordGeneratedListener passwordGeneratedListener;
    private TextView domainView;
    private EditText editTextPassword;
    private EditText urlView;
    private EditText usernameView;
    private EditText notesView;
    private Switch legacyPasswordSwitch;
    private RelativeLayout iterationCountLayout;
    private EditText editTextIterationCount;
    private LinearLayout lengthComplexityLayout;
    private TextView textViewLength;
    private SmartSelector smartSelector;
    private Button saveButton;
    private Button dismissChangesButton;

    public DomainDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fLayout = inflater.inflate(R.layout.fragment_domain_details, container, false);
        domainView = fLayout.findViewById(R.id.textViewDomain);
        urlView = fLayout.findViewById(R.id.editTextUrl);
        usernameView = fLayout.findViewById(R.id.editTextUsername);
        notesView = fLayout.findViewById(R.id.editTextNotes);
        editTextPassword = fLayout.findViewById(R.id.editTextPassword);
        legacyPasswordSwitch = fLayout.findViewById(R.id.switchLegacyPassword);
        iterationCountLayout = fLayout.findViewById(R.id.iterationCountLayout);
        editTextIterationCount = fLayout.findViewById(R.id.iterationCount);
        smartSelector = fLayout.findViewById(R.id.smartSelector);
        smartSelector.setOnStrengthSelectedEventListener(this);
        lengthComplexityLayout = fLayout.findViewById(R.id.lengthComplexityLayout);
        textViewLength = fLayout.findViewById(R.id.textViewLength);
        saveButton = fLayout.findViewById(R.id.saveButton);
        dismissChangesButton = fLayout.findViewById(R.id.dismissChangesButton);
        // Restore managers if needed
        if (savedInstanceState != null) {
            kgkManager = savedInstanceState.getParcelable(KGKMANAGER);
            kgkManager.loadSharedPreferences(getActivity());
            settingsManager = new PasswordSettingsManager(getActivity());
            this.setSettingsManagerAndKgkManager(settingsManager, kgkManager);
        }
        return fLayout;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
        generatePassword();
    }

    @Override
    public void onPause() {
        Clearer.zero(editTextPassword.getText());
        editTextPassword.setText("");
        passwordGenerator = null;
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(KGKMANAGER, kgkManager);
    }

    @Override
    public void onStop() {
        if (isNewSetting) {
            applyChanges();
        } else {
            dismissChanges(false);
        }
        super.onStop();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        passwordGeneratedListener = null;
    }

    @Override
    public void onStrengthSelected(int length, int complexity) {
        StringBuilder template = new StringBuilder();
        if (complexity % 3 == 0 || complexity >= 5) {
            template.append("n");
        }
        if (complexity == 1 || complexity >= 3) {
            template.append("a");
        }
        if (complexity == 2 || complexity >= 4) {
            template.append("A");
        }
        if (complexity >= 6) {
            template.append("o");
        }
        while (template.length() < length) {
            template.append("x");
        }
        String shuffledTemplate = shuffleString(template.toString(), setting.getSalt());
        setting.setTemplate(shuffledTemplate);
        passwordGenerator = null;
        updateView();
        generatePassword();
    }

    @Override
    public void onPasswordGenerationFinished(PasswordGenerator generator) {
        passwordGenerator = generator;
        generatePassword();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnPasswordGeneratedListener {
        void onPasswordGenerated();
    }

    public void setPasswordGeneratedListener(OnPasswordGeneratedListener listener) {
        this.passwordGeneratedListener = listener;
    }

    public void generatePassword() {
        if (this.kgkManager.hasKgk()) {
            if (!setting.hasLegacyPassword()) {
                if (this.passwordGenerator == null) {
                    GeneratePasswordTask generatePasswordTask = new GeneratePasswordTask(this);
                    if (setting.getIterations() <= 0) {
                        Log.e("Password Setting Error",
                                "Iterations too small: " +
                                        Integer.toString(setting.getIterations()) +
                                        ". Setting to 4096.");
                        setting.setIterations(4096);
                    }
                    generatePasswordTask.execute(
                            UTF8.encode(setting.getDomain()),
                            UTF8.encode(setting.getUsername()),
                            kgkManager.getKgk(),
                            setting.getSalt(),
                            ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
                                    .putInt(setting.getIterations()).array());
                } else {
                    editTextPassword.setText(this.passwordGenerator.getPassword(setting));
                    if (passwordGeneratedListener != null)
                        passwordGeneratedListener.onPasswordGenerated();
                }
            }
        } else {
            Log.e("Important Bug!",
                    "There should never be a kgkManager without a kgk at this point.");
        }
        if (setting.hasLegacyPassword()) {
            editTextPassword.setText(setting.getLegacyPassword());
            if (passwordGeneratedListener != null)
                passwordGeneratedListener.onPasswordGenerated();
        }
    }

    private void updateView() {
        domainView.setText(setting.getDomain());
        urlView.setText(setting.getUrl());
        usernameView.setText(setting.getUsername());
        notesView.setText(setting.getNotes());
        legacyPasswordSwitch.setChecked(setting.hasLegacyPassword());
        if (setting.hasLegacyPassword()) {
            editTextPassword.setEnabled(true);
            iterationCountLayout.setVisibility(View.INVISIBLE);
            smartSelector.setVisibility(View.INVISIBLE);
            lengthComplexityLayout.setVisibility(View.INVISIBLE);
        } else {
            editTextPassword.setEnabled(false);
            iterationCountLayout.setVisibility(View.VISIBLE);
            smartSelector.setVisibility(View.VISIBLE);
            lengthComplexityLayout.setVisibility(View.VISIBLE);
        }
        editTextIterationCount.setText(String.format(Locale.GERMANY, "%d",
                setting.getIterations()));
        smartSelector.setSelectedLength(setting.getLength()-4);
        smartSelector.setSelectedComplexity(setting.getComplexity());
        textViewLength.setText(String.format(Locale.GERMANY, "%d", setting.getLength()));
    }

    private static String shuffleString(String string, byte[] salt)
    {
        List<String> letters = Arrays.asList(string.split(""));
        long seed = 0;
        for (int i=0; i < 48/8; i++) {
            seed += (long) salt[i] << i*8;
        }
        Random rng = new Random(seed);
        Collections.shuffle(letters, rng);
        StringBuilder shuffled = new StringBuilder();
        for (String letter : letters) {
            shuffled.append(letter);
        }
        return shuffled.toString();
    }

    public void clearPassword() {
        if (editTextPassword != null) {
            Clearer.zero(editTextPassword.getText());
            editTextPassword.setText("");
        }
    }

    public void setSetting(PasswordSetting newSetting, boolean isNewSetting) {
        this.setting = newSetting;
        this.isNewSetting = isNewSetting;
        smartSelector.setCharacterCounts(
                setting.getDigitsCharacterSet().size(),
                setting.getLowerCaseLettersCharacterSet().size(),
                setting.getUpperCaseLettersCharacterSet().size(),
                setting.getExtraCharacterSet().size());
        legacyPasswordSwitch.setOnCheckedChangeListener(
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        if (!setting.hasLegacyPassword()) {
                            setting.setLegacyPassword(editTextPassword.getText().toString());
                        }
                    } else {
                        setting.setLegacyPassword(null);
                    }
                    updateView();
                    generatePassword();
                }
            });
        editTextIterationCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    setting.setIterations(Integer.parseInt(charSequence.toString()));
                    passwordGenerator = null;
                    generatePassword();
                } catch (NumberFormatException e) {
                    Log.e("#iterations not an int", e.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        usernameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                setting.setUsername(charSequence.toString());
                passwordGenerator = null;
                generatePassword();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        urlView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                setting.setUrl(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        notesView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                setting.setNotes(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        this.updateView();
        passwordGenerator = null;
        this.generatePassword();
    }

    public void setSettingsManagerAndKgkManager(PasswordSettingsManager newSettingsManager,
                                                KgkManager newKgkManager) {
        this.kgkManager = newKgkManager;
        this.settingsManager = newSettingsManager;
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applyChanges();
                updateView();
                isNewSetting = false;
            }
        });
        dismissChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissChanges(true);
            }
        });
    }

    private void applyChanges() {
        settingsManager.setSetting(setting);
        try {
            setting = (PasswordSetting) setting.clone();
        } catch (CloneNotSupportedException e) {
            Log.e("Unable to clone setting", e.toString());
        }
        settingsManager.storeLocalSettings(kgkManager);
    }

    private void dismissChanges(boolean doGeneratePassword) {
        if (isNewSetting) {
            settingsManager.deleteSetting(setting.getDomain());
            settingsManager.storeLocalSettings(kgkManager);
            getActivity().finish();
        } else {
            try {
                setting = (PasswordSetting) settingsManager.getSetting(setting.getDomain()).clone();
                updateView();
                if (doGeneratePassword) generatePassword();
            } catch (CloneNotSupportedException e) {
                Log.e("Unable to clone setting", e.toString());
            }
        }
    }

    public boolean hasPassword() {
        return this.passwordGenerator != null || this.setting.hasLegacyPassword();
    }
}
