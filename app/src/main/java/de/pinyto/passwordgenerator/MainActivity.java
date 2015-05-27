package de.pinyto.passwordgenerator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;


public class MainActivity extends ActionBarActivity {

    private boolean isGenerated = false;

    private void setIterationCountVisibility(int visible) {
        TextView textViewIterationCountBeginning =
                (TextView) findViewById(R.id.iterationCountBeginning);
        textViewIterationCountBeginning.setVisibility(visible);
        TextView textViewIterationCount =
                (TextView) findViewById(R.id.iterationCount);
        textViewIterationCount.setVisibility(visible);
        TextView textViewIterationCountEnd =
                (TextView) findViewById(R.id.iterationCountEnd);
        textViewIterationCountEnd.setVisibility(visible);
    }

    private void loadAutoCompleteFromSettings() {
        SharedPreferences savedDomains = getSharedPreferences("savedDomains", MODE_PRIVATE);
        Set<String> domainSet = savedDomains.getStringSet("domainSet", new HashSet<String>());
        if (domainSet != null) {
            String[] domainList = new String[domainSet.size()];
            Iterator it = domainSet.iterator();
            int i = 0;
            while (it.hasNext()) {
                domainList[i] = (String) it.next();
                i++;
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, domainList);
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
            autoCompleteTextViewDomain.setAdapter(adapter);
        }
    }

    private void setDomainFieldFromClipboard() {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            ClipData clipDataCurrent = clipboard.getPrimaryClip();
            CharSequence pasteData = clipDataCurrent.getItemAt(0).getText();
            if (pasteData != null) {
                DomainExtractor extractor = new DomainExtractor();
                AutoCompleteTextView autoCompleteTextViewDomain =
                        (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
                autoCompleteTextViewDomain.setText(extractor.extract(pasteData.toString()));
            }
        }
    }

    private String generatePassword(int iterations) {
        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        String domain = autoCompleteTextViewDomain.getText().toString();
        EditText editTextMasterPassword =
                (EditText) findViewById(R.id.editTextMasterPassword);
        PasswordGenerator generator = new PasswordGenerator();
        generator.initialize(
                domain,
                editTextMasterPassword.getText().toString());
        generator.hash(iterations);
        CheckBox checkBoxSpecialCharacters =
                (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
        CheckBox checkBoxLetters =
                (CheckBox) findViewById(R.id.checkBoxLetters);
        CheckBox checkBoxNumbers =
                (CheckBox) findViewById(R.id.checkBoxNumbers);
        SeekBar seekBarLength =
                (SeekBar) findViewById(R.id.seekBarLength);
        return generator.getPassword(
                checkBoxSpecialCharacters.isChecked(),
                checkBoxLetters.isChecked(),
                checkBoxNumbers.isChecked(),
                seekBarLength.getProgress() + 4);
    }

    private void saveSettings(int iterations) {
        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        String domain = autoCompleteTextViewDomain.getText().toString();
        CheckBox checkBoxSpecialCharacters =
                (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
        CheckBox checkBoxLetters =
                (CheckBox) findViewById(R.id.checkBoxLetters);
        CheckBox checkBoxNumbers =
                (CheckBox) findViewById(R.id.checkBoxNumbers);
        SeekBar seekBarLength =
                (SeekBar) findViewById(R.id.seekBarLength);
        SharedPreferences savedDomains = getSharedPreferences("savedDomains", MODE_PRIVATE);
        Set<String> domainSet = savedDomains.getStringSet(
                "domainSet",
                new HashSet<String>()
        );
        if (domainSet != null) {
            domainSet.add(domain);
        }
        SharedPreferences.Editor savedDomainsEditor = savedDomains.edit();
        savedDomainsEditor.putStringSet("domainSet", domainSet);
        savedDomainsEditor.putBoolean(
                domain + "_letters",
                checkBoxLetters.isChecked()
        );
        savedDomainsEditor.putBoolean(
                domain + "_numbers",
                checkBoxNumbers.isChecked()
        );
        savedDomainsEditor.putBoolean(
                domain + "_special_characters",
                checkBoxSpecialCharacters.isChecked()
        );
        savedDomainsEditor.putInt(
                domain + "_length",
                seekBarLength.getProgress() + 4
        );
        savedDomainsEditor.putInt(
                domain + "_iterations",
                iterations
        );
        savedDomainsEditor.apply();
    }

    private void clearMasterPassword() {
        EditText editTextMasterPassword = (EditText) findViewById(R.id.editTextMasterPassword);
        Editable password = editTextMasterPassword.getText();
        CharSequence zero = "0";
        for (int i = 0; i < password.length(); i++) {
            password.replace(i, i+1, zero);
        }
        editTextMasterPassword.setText("", TextView.BufferType.EDITABLE);
    }

    private void setToNotGenerated() {
        isGenerated = false;
        Button generateButton = (Button) findViewById(R.id.generatorButton);
        generateButton.setText(getResources().getString(R.string.generator_button));
        setIterationCountVisibility(View.INVISIBLE);
        invalidateOptionsMenu();
        TextView textViewPassword = (TextView) findViewById(R.id.textViewPassword);
        textViewPassword.setText("");
    }

    private void loadSettings() {
        SharedPreferences savedDomains = getSharedPreferences("savedDomains", MODE_PRIVATE);
        Set<String> domainSet = savedDomains.getStringSet(
                "domainSet",
                new HashSet<String>()
        );
        if (domainSet != null) {
            AutoCompleteTextView autoCompleteTextViewDomain =
                    (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
            String domain = autoCompleteTextViewDomain.getText().toString();
            if (domainSet.contains(domain)) {
                CheckBox checkBoxSpecialCharacters =
                        (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
                CheckBox checkBoxLetters =
                        (CheckBox) findViewById(R.id.checkBoxLetters);
                CheckBox checkBoxNumbers =
                        (CheckBox) findViewById(R.id.checkBoxNumbers);
                SeekBar seekBarLength =
                        (SeekBar) findViewById(R.id.seekBarLength);
                checkBoxLetters.setChecked(
                        savedDomains.getBoolean(domain + "_letters", true)
                );
                checkBoxNumbers.setChecked(
                        savedDomains.getBoolean(domain + "_numbers", true)
                );
                checkBoxSpecialCharacters.setChecked(
                        savedDomains.getBoolean(domain + "_special_characters", true)
                );
                seekBarLength.setProgress(
                        savedDomains.getInt(domain + "_length", 10) - 4
                );
            }
        }
    }

    private void setButtonEnabledByDomainLegth() {
        Button generateButton = (Button) findViewById(R.id.generatorButton);
        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        generateButton.setEnabled(autoCompleteTextViewDomain.getText().length() >= 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setIterationCountVisibility(View.INVISIBLE);
        loadAutoCompleteFromSettings();
        setDomainFieldFromClipboard();
        loadSettings();
        setButtonEnabledByDomainLegth();
        EditText editTextMasterPassword = (EditText) findViewById(R.id.editTextMasterPassword);
        editTextMasterPassword.setText("", TextView.BufferType.EDITABLE);
        setToNotGenerated();
        clearMasterPassword();

        SeekBar seekBarLength = (SeekBar) findViewById(R.id.seekBarLength);
        seekBarLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView textViewLengthDisplay =
                        (TextView) findViewById(R.id.textViewLengthDisplay);
                textViewLengthDisplay.setText(Integer.toString(progress + 4));
                setToNotGenerated();
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Button generateButton = (Button) findViewById(R.id.generatorButton);
        generateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Load fields
                AutoCompleteTextView autoCompleteTextViewDomain =
                        (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
                String domain = autoCompleteTextViewDomain.getText().toString();
                // Load iteration count from settings
                SharedPreferences savedDomains = getSharedPreferences("savedDomains", MODE_PRIVATE);
                int iterations = savedDomains.getInt(
                        domain + "_iterations",
                        4096
                );
                if (isGenerated) {
                    iterations++;
                }
                // Generate password
                TextView textViewPassword = (TextView) findViewById(R.id.textViewPassword);
                textViewPassword.setText(generatePassword(iterations));
                isGenerated = true;
                invalidateOptionsMenu();
                Button generateButton = (Button) findViewById(R.id.generatorButton);
                generateButton.setText(getResources().getString(R.string.re_generator_button));
                TextView textViewIterationCount =
                        (TextView) findViewById(R.id.iterationCount);
                textViewIterationCount.setText(Integer.toString(iterations));
                setIterationCountVisibility(View.VISIBLE);
                // Save domain and settings
                saveSettings(iterations);
                loadAutoCompleteFromSettings();
            }
        });

        AutoCompleteTextView autoCompleteTextViewDomain =
                (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewDomain);
        autoCompleteTextViewDomain.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable editable) {
                loadSettings();
                setButtonEnabledByDomainLegth();
                setToNotGenerated();
            }
        });

        editTextMasterPassword.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable editable) {
                setToNotGenerated();
            }
        });

        CheckBox.OnCheckedChangeListener settingCheckboxChange =
                new CheckBox.OnCheckedChangeListener()
                {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton compoundButton,
                            boolean isChecked) {
                        setToNotGenerated();
                    }
                };
        CheckBox checkBoxSpecialCharacters =
                (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
        checkBoxSpecialCharacters.setOnCheckedChangeListener(settingCheckboxChange);
        CheckBox checkBoxLetters =
                (CheckBox) findViewById(R.id.checkBoxLetters);
        checkBoxLetters.setOnCheckedChangeListener(settingCheckboxChange);
        CheckBox checkBoxNumbers =
                (CheckBox) findViewById(R.id.checkBoxNumbers);
        checkBoxNumbers.setOnCheckedChangeListener(settingCheckboxChange);
    }

    @Override
    protected void onPause() {
        setToNotGenerated();
        clearMasterPassword();
        super.onPause();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main_actions, menu);
        MenuItem copyItem = menu.findItem(R.id.action_copy);
        copyItem.setVisible(isGenerated);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_copy) {
            TextView textViewPassword = (TextView) findViewById(R.id.textViewPassword);
            ClipData clipDataPassword = ClipData.newPlainText(
                    "password",
                    textViewPassword.getText()
            );
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(clipDataPassword);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
