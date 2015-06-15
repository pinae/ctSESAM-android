package de.pinyto.passwordgenerator;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Use this class to manage password settings. It will store them internally and it will also
 * pack them for synchronization.
 */
public class PasswordSettingsManager {
    private SharedPreferences savedDomains;

    PasswordSettingsManager(Context contentContext) {
        savedDomains = contentContext.getSharedPreferences("savedDomains", Context.MODE_PRIVATE);
    }

    public PasswordSetting getSetting(String domain) {
        Set<String> domainSet = savedDomains.getStringSet(
                "domainSet",
                new HashSet<String>()
        );
        PasswordSetting setting = new PasswordSetting(domain);
        if (domainSet != null) {
            if (domainSet.contains(domain)) {
                setting.setUseLetters(savedDomains.getBoolean(domain + "_letters", true));
                setting.setUseDigits(savedDomains.getBoolean(domain + "_digits", true));
                setting.setUseExtra(savedDomains.getBoolean(domain + "_special_characters", true));
                setting.setLength(savedDomains.getInt(domain + "_length", 10));
            }
        }
        return setting;
    }

    public void saveSetting(PasswordSetting newSetting) {
        Set<String> domainSet = savedDomains.getStringSet(
                "domainSet",
                new HashSet<String>()
        );
        if (domainSet != null) {
            domainSet.add(newSetting.getDomain());
        }
        SharedPreferences.Editor savedDomainsEditor = savedDomains.edit();
        savedDomainsEditor.putStringSet("domainSet", domainSet);
        savedDomainsEditor.putBoolean(
                newSetting.getDomain() + "_letters",
                newSetting.useLetters()
        );
        savedDomainsEditor.putBoolean(
                newSetting.getDomain() + "_digits",
                newSetting.useDigits()
        );
        savedDomainsEditor.putBoolean(
                newSetting.getDomain() + "_special_characters",
                newSetting.useExtra()
        );
        savedDomainsEditor.putInt(
                newSetting.getDomain() + "_length",
                newSetting.getLength()
        );
        savedDomainsEditor.putInt(
                newSetting.getDomain() + "_iterations",
                newSetting.getIterations()
        );
        savedDomainsEditor.putString(
                newSetting.getDomain() + "_cDate",
                newSetting.getCreationDate()
        );
        savedDomainsEditor.putString(
                newSetting.getDomain() + "_mDate",
                newSetting.getModificationDate()
        );
        savedDomainsEditor.apply();
    }

    public String[] getDomainList() {
        Set<String> domainSet = savedDomains.getStringSet("domainSet", new HashSet<String>());
        String[] domainList;
        if (domainSet != null) {
            domainList = new String[domainSet.size()];
            Iterator it = domainSet.iterator();
            int i = 0;
            while (it.hasNext()) {
                domainList[i] = (String) it.next();
                i++;
            }
        } else {
            domainList = new String[] {};
        }
        return domainList;
    }
}
