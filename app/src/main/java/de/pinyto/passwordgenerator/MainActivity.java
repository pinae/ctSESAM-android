package de.pinyto.passwordgenerator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {

    private boolean isGenerated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SeekBar seekBarLength = (SeekBar) findViewById(R.id.seekBarLength);
        seekBarLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                TextView textViewLengthDisplay =
                        (TextView) findViewById(R.id.textViewLengthDisplay);
                textViewLengthDisplay.setText(Integer.toString(progress + 4));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        Button generateButton = (Button) findViewById(R.id.generatorButton);
        generateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText editTextDomain =
                        (EditText) findViewById(R.id.editTextDomain);
                EditText editTextMasterPassword =
                        (EditText) findViewById(R.id.editTextMasterPassword);
                PasswordGenerator generator = new PasswordGenerator();
                generator.initialize(
                        editTextDomain.getText().toString(),
                        editTextMasterPassword.getText().toString());
                generator.hash(1);
                CheckBox checkBoxSpecialCharacters =
                        (CheckBox) findViewById(R.id.checkBoxSpecialCharacter);
                CheckBox checkBoxLetters =
                        (CheckBox) findViewById(R.id.checkBoxLetters);
                CheckBox checkBoxNumbers =
                        (CheckBox) findViewById(R.id.checkBoxNumbers);
                SeekBar seekBarLength =
                        (SeekBar) findViewById(R.id.seekBarLength);
                String password = generator.getPassword(
                        checkBoxSpecialCharacters.isChecked(),
                        checkBoxLetters.isChecked(),
                        checkBoxNumbers.isChecked(),
                        seekBarLength.getProgress() + 4);
                TextView textViewPassword = (TextView) findViewById(R.id.textViewPassword);
                textViewPassword.setText(password);
                isGenerated = true;
                invalidateOptionsMenu();
            }
        });
        TextWatcher changeEditTextListener = new TextWatcher(){
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
            public void afterTextChanged(Editable editable) {
                isGenerated = false;
                invalidateOptionsMenu();
                TextView textViewPassword = (TextView) findViewById(R.id.textViewPassword);
                textViewPassword.setText("");
            }
        };
        EditText editTextDomain =
                (EditText) findViewById(R.id.editTextDomain);
        editTextDomain.addTextChangedListener(changeEditTextListener);
        EditText editTextMasterPassword =
                (EditText) findViewById(R.id.editTextMasterPassword);
        editTextMasterPassword.addTextChangedListener(changeEditTextListener);
    }

    @Override
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
