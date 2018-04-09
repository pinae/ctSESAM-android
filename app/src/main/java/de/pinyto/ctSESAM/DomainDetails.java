package de.pinyto.ctSESAM;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * A {@link Fragment} for account details.
 * Activities that contain this fragment must implement the
 * {@link DomainDetails.OnPasswordGeneratedListener} interface
 * to handle interaction events.
 * Use the {@link DomainDetails#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DomainDetails extends Fragment {
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    private PasswordSetting setting;
    private PasswordGenerator passwordGenerator;
    private OnPasswordGeneratedListener passwordGeneratedListener;
    private TextView textViewPassword;
    private TextView passwordHeading;
    private SmartSelector smartSelector;
    private Button generateButton;
    private boolean showSettings = false;
    private boolean showPassword = false;

    public DomainDetails() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param kgkManager No password generation without a KgkManager
     * @param settingsManager The settingsManager is used to store changes in the current setting
     * @param setting The PasswordSetting object which contains the domain name
     * @return A new instance of fragment DomainDetails.
     */
    public static DomainDetails newInstance(KgkManager kgkManager,
                                            PasswordSettingsManager settingsManager,
                                            PasswordSetting setting) {
        DomainDetails fragment = new DomainDetails();
        Bundle args = new Bundle();
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fLayout = inflater.inflate(R.layout.fragment_domain_details, container, false);
        textViewPassword = (TextView) fLayout.findViewById(R.id.textViewPassword);
        passwordHeading = (TextView) fLayout.findViewById(R.id.textViewPasswordHeading);
        smartSelector = (SmartSelector) fLayout.findViewById(R.id.smartSelector);
        smartSelector.setOnStrengthSelectedEventListener(
                new SmartSelector.OnStrengthSelectedEventListener() {
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
            }
        });
        generateButton = (Button) fLayout.findViewById(R.id.generatorButton);
        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generatePassword();
            }
        });
        return fLayout;
    }

    @Override
    public void onPause() {
        textViewPassword.setText("");
        passwordGenerator = null;
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnPasswordGeneratedListener {
        void onPasswordGenerated();
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
                                        "Setting to 4096.");
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
                    textViewPassword.setText(this.passwordGenerator.getPassword(setting));
                    this.showPassword = true;
                    this.updateView();
                    //this.invalidateOptionsMenu();
                    // load settings because the domain might be new
                    //ArrayAdapter<String> adapter = new ArrayAdapter<>(getBaseContext(),
                    //        android.R.layout.simple_dropdown_item_1line,
                    //        this.settingsManager.getDomainList());
                    //autoCompleteTextViewDomain.setAdapter(adapter);
                }
            }
        } else {
            Log.e("Important Bug!",
                    "There should never be a kgkManager without a kgk at this point.");
        }
        if (setting.hasLegacyPassword()) {
            textViewPassword.setText(setting.getLegacyPassword());
            this.showPassword = true;
            this.updateView();
            //this.invalidateOptionsMenu();
        }
    }

    private void updateView() {
        if (this.showSettings) {
            if (this.showPassword) {
                generateButton.setVisibility(View.INVISIBLE);
                passwordHeading.setVisibility(View.VISIBLE);
                textViewPassword.setVisibility(View.VISIBLE);
            } else {
                generateButton.setVisibility(View.VISIBLE);
                passwordHeading.setVisibility(View.INVISIBLE);
                textViewPassword.setVisibility(View.INVISIBLE);
            }
        } else {
            generateButton.setVisibility(View.INVISIBLE);
            passwordHeading.setVisibility(View.INVISIBLE);
            textViewPassword.setVisibility(View.INVISIBLE);
        }
    }

    private static String shuffleString(String string)
    {
        List<String> letters = Arrays.asList(string.split(""));
        Collections.shuffle(letters);
        String shuffled = "";
        for (String letter : letters) {
            shuffled += letter;
        }
        return shuffled;
    }

    public void clearPassword() {
        if (textViewPassword != null) {
            textViewPassword.setText("");
        }
    }
}
