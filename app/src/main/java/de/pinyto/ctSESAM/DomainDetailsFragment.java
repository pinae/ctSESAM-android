package de.pinyto.ctSESAM;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
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


/**
 * A {@link Fragment} for account details.
 * Activities that contain this fragment must implement the
 * {@link DomainDetailsFragment.OnPasswordGeneratedListener} interface
 * to handle interaction events.
 */
public class DomainDetailsFragment extends Fragment implements SmartSelector.OnStrengthSelectedEventListener {
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
        domainView = (TextView) fLayout.findViewById(R.id.textViewDomain);
        urlView = (EditText) fLayout.findViewById(R.id.editTextUrl);
        usernameView = (EditText) fLayout.findViewById(R.id.editTextUsername);
        editTextPassword = (EditText) fLayout.findViewById(R.id.editTextPassword);
        legacyPasswordSwitch = (Switch) fLayout.findViewById(R.id.switchLegacyPassword);
        iterationCountLayout = (RelativeLayout) fLayout.findViewById(R.id.iterationCountLayout);
        editTextIterationCount = (EditText) fLayout.findViewById(R.id.iterationCount);
        smartSelector = (SmartSelector) fLayout.findViewById(R.id.smartSelector);
        smartSelector.setOnStrengthSelectedEventListener(this);
        lengthComplexityLayout = (LinearLayout) fLayout.findViewById(R.id.lengthComplexityLayout);
        textViewLength = (TextView) fLayout.findViewById(R.id.textViewLength);
        saveButton = (Button) fLayout.findViewById(R.id.saveButton);
        dismissChangesButton = (Button) fLayout.findViewById(R.id.dismissChangesButton);
        return fLayout;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
    }

    @Override
    public void onPause() {
        editTextPassword.setText("");
        passwordGenerator = null;
        super.onPause();
    }

    @Override
    public void onStop() {
        if (isNewSetting) {
            applyChanges();
        } else {
            dismissChanges();
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
        String shuffledTemplate = shuffleString(template.toString());
        setting.setTemplate(shuffledTemplate);
        updateView();
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
                    GeneratePasswordTask generatePasswordTask = new GeneratePasswordTask(
                            new GeneratePasswordTask.OnPasswordGeneratedListener() {
                        @Override
                        public void onFinished(PasswordGenerator generator) {
                            passwordGenerator = generator;
                            generatePassword();
                        }
                    });
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
                    this.settingsManager.setSetting(setting);
                    this.settingsManager.storeLocalSettings(this.kgkManager);
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
        generatePassword();
    }

    private static String shuffleString(String string)
    {
        List<String> letters = Arrays.asList(string.split(""));
        Collections.shuffle(letters);
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
                        setting.setLegacyPassword(editTextPassword.getText().toString());
                        updateView();
                    } else {
                        setting.setLegacyPassword(null);
                        generatePassword();
                        updateView();
                    }
                }
            });
        this.updateView();
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
            }
        });
        dismissChangesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissChanges();
            }
        });
    }

    private void applyChanges() {
        settingsManager.setSetting(setting);
        settingsManager.storeLocalSettings(kgkManager);
    }

    private void dismissChanges() {
        if (isNewSetting) {
            settingsManager.deleteSetting(setting.getDomain());
            settingsManager.storeLocalSettings(kgkManager);
            getActivity().finish();
        } else {
            setting = settingsManager.getSetting(setting.getDomain());
            updateView();
        }
    }

    public boolean hasPassword() {
        return this.passwordGenerator != null;
    }
}
