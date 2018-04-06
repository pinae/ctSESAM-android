package de.pinyto.ctSESAM;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A fragment representing a list of PasswordSettings.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnSettingSelected}
 * interface.
 */
public class PasswordSettingListFragment extends ListFragment {
    private OnSettingSelected settingSelectedListener;
    private PasswordSettingsManager settingsManager;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PasswordSettingListFragment() {
    }

    public static PasswordSettingListFragment newInstance(int columnCount) {
        PasswordSettingListFragment fragment = new PasswordSettingListFragment();
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
        View view = inflater.inflate(R.layout.fragment_passwordsetting_list, container, false);
        return view;
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
        settingSelectedListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnSettingSelected {
        void onSettingSelected(PasswordSetting setting);
    }

    public void setSettingSelectedListener(OnSettingSelected listener) {
        this.settingSelectedListener = listener;
    }

    public void setSettingsManager(PasswordSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        updateList();
    }

    private void updateList() {
        if (settingsManager != null) {
            ArrayList<HashMap<String,String>> settingsList=new ArrayList<>();
            String[] allDomains = settingsManager.getDomainList();
            for (String domain : allDomains) {
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
            this.setListAdapter(adapter);
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
}
