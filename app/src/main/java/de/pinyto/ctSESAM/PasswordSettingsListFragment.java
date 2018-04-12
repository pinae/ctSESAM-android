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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * A fragment representing a list of PasswordSettings.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnSettingSelected}
 * interface.
 */
public class PasswordSettingsListFragment extends Fragment
        implements AdapterView.OnItemClickListener {
    public static final String KEYIVKEY = "de.pinyto.ctsesam.KEYIV";
    private OnSettingSelected settingSelectedListener;
    private OnNewSetting newSettingListener;
    private KgkManager kgkManager;
    private PasswordSettingsManager settingsManager;
    private ListView listView;
    private TextView emptyView;
    private ImageButton addNewDomainButton;
    private EditText domainEntry;
    private LinkedList<String> filteredDomains = new LinkedList<>();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PasswordSettingsListFragment() {
    }

    public static PasswordSettingsListFragment newInstance(int columnCount) {
        PasswordSettingsListFragment fragment = new PasswordSettingsListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            kgkManager = new KgkManager(getActivity(),
                    savedInstanceState.getByteArray(KEYIVKEY));
            settingsManager = new PasswordSettingsManager(getActivity());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_passwordsetting_list, container, false);
        listView = (ListView) layout.findViewById(R.id.domainList);
        listView.setOnItemClickListener(this);
        emptyView = (TextView) layout.findViewById(R.id.domainListEmpty);
        addNewDomainButton = (ImageButton) layout.findViewById(R.id.addNewDomainButton);
        domainEntry = (EditText) layout.findViewById(R.id.domainEntry);
        return layout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnSettingSelected) {
            settingSelectedListener = (OnSettingSelected) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnSettingSelected");
        }
        if (activity instanceof OnNewSetting) {
            newSettingListener = (OnNewSetting) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnNewSetting");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putByteArray(KEYIVKEY, kgkManager.exportKeyIv());
    }

    @Override
    public void onPause() {
        if (settingsManager.getDomainList().length > 0) {
            Log.d("List Fragment", "Storing Settings...");
            for (String domain: settingsManager.getDomainList()) {
                Log.d("List Fragment List", domain);
            }
            if (kgkManager.hasKgk()) settingsManager.storeLocalSettings(kgkManager);
        } else {
            settingsManager.deleteAllSettings();
            kgkManager.deleteKgkAndSettings();
        }
        super.onPause();
    }

    @Override
    public void onDetach() {
        kgkManager.reset();
        super.onDetach();
        settingSelectedListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        settingsManager.storeLocalSettings(kgkManager);
        this.settingSelectedListener.onSettingSelected(
                settingsManager.getSetting(filteredDomains.get(i)));
    }

    /**
     * This interfaces must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnSettingSelected {
        void onSettingSelected(PasswordSetting setting);
    }

    public interface OnNewSetting {
        void onNewSetting(PasswordSetting setting);
    }

    public void setKgkAndSettingsManager(KgkManager newKgkManager,
                                         PasswordSettingsManager newSettingsManager) {
        this.kgkManager = newKgkManager;
        this.settingsManager = newSettingsManager;
        setListeners();
        updateList();
    }

    private void setListeners() {
        if (settingsManager != null)
        domainEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() > 0) {
                    boolean domainFound = false;
                    for (String domain : settingsManager.getDomainList()) {
                        if (domain.contentEquals(charSequence)) {
                            domainFound = true;
                        }
                    }
                    if (domainFound) {
                        addNewDomainButton.setVisibility(View.INVISIBLE);
                    } else {
                        addNewDomainButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    addNewDomainButton.setVisibility(View.INVISIBLE);
                }
                updateList();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        if (kgkManager != null && kgkManager.hasKgk() && settingsManager != null)
        addNewDomainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PasswordSetting newSetting = new PasswordSetting(domainEntry.getText().toString());
                settingsManager.setSetting(newSetting);
                settingsManager.storeLocalSettings(kgkManager);
                view.setVisibility(View.INVISIBLE);
                updateList();
                if (newSettingListener != null) newSettingListener.onNewSetting(newSetting);
            }
        });
    }

    private void updateList() {
        if (settingsManager != null) {
            ArrayList<HashMap<String,String>> settingsList=new ArrayList<>();
            String[] allDomains = settingsManager.getDomainList();
            String domainEntryString = domainEntry.getText().toString();
            String filter = domainEntryString.toLowerCase();
            filteredDomains.clear();
            boolean addButtonVisible = true;
            for (String domain : allDomains) {
                if (domain.toLowerCase().contains(filter)) {
                    filteredDomains.add(domain);
                }
                if (domain.equals(domainEntryString)) {
                    addButtonVisible = false;
                }
            }
            addNewDomainButton.setVisibility(addButtonVisible ? View.VISIBLE : View.INVISIBLE);
            for (String domain : filteredDomains) {
                HashMap<String, String> entry = new HashMap<>();
                entry.put("domainName", domain);
                String settingHints = "";
                if (settingsManager.getSetting(domain).hasLegacyPassword()) {
                    settingHints += "legacy Password";
                }
                entry.put("settingHints", settingHints);
                entry.put("icon", getIcon(domain.toLowerCase())+"");
                settingsList.add(entry);
            }
            String[] fromArray = {"domainName", "settingHints", "icon"};
            int[] viewIndexes = {R.id.listDomainName, R.id.listSettingHints, R.id.listDomainIcon};
            SimpleAdapter adapter = new SimpleAdapter(getActivity(), settingsList,
                    R.layout.settings_list_item_layout, fromArray, viewIndexes);
            listView.setAdapter(adapter);
            if (filteredDomains.size() > 0) {
                listView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.INVISIBLE);
            } else {
                listView.setVisibility(View.INVISIBLE);
                emptyView.setVisibility(View.VISIBLE);
            }
        }
    }

    private int getIcon(String domain) {
        if (domain.toLowerCase().contains("google")) {
            return R.drawable.ic_google__g__logo;
        }
        if (domain.toLowerCase().contains("amazon")) {
            return R.drawable.ic_amazon_favicon;
        }
        return R.mipmap.ic_launcher;
    }

    public void setDomainFilter(CharSequence sequence) {
        if (domainEntry != null) {
            domainEntry.setText(sequence);
            updateList();
        } else {
            Log.e("Paste error", "domainEntry object does not exist.");
        }
    }
}
