package de.pinyto.passwordgenerator;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * This class packs and unpacks the settings into an compressed and encrypted byte[].
 */
public class SettingsPacker {
    private JSONArray settings;
    private SharedPreferences savedDomains;

    public SettingsPacker(Context context) {
        settings = new JSONArray();
        savedDomains = context.getSharedPreferences(
                "savedDomains", Context.MODE_PRIVATE);
        Set<String> domainSet = savedDomains.getStringSet(
                "domainSet",
                new HashSet<String>()
        );
        if (domainSet != null) {
            for (String domain : domainSet) {
                JSONObject domainObject = new JSONObject();
                try {
                    domainObject.put("domain", domain);
                    domainObject.put("useLowerCase",
                            savedDomains.getBoolean(domain + "_letters", true));
                    domainObject.put("useUpperCase",
                            savedDomains.getBoolean(domain + "_letters", true));
                    domainObject.put("useDigits",
                            savedDomains.getBoolean(domain + "_numbers", true));
                    domainObject.put("useExtra",
                            savedDomains.getBoolean(domain + "_special_characters", true));
                    domainObject.put("iterations",
                            savedDomains.getInt(domain + "_iterations", 4096));
                    domainObject.put("length",
                            savedDomains.getInt(domain + "_length", 10));
                    domainObject.put("cDate",
                            savedDomains.getString(domain + "_cDate", "1970-01-01T00:00:00"));
                    domainObject.put("mDate",
                            savedDomains.getString(domain + "_mDate", "1970-01-01T00:00:00"));
                } catch (JSONException e) {
                    Log.d("Settings packing error", "Unable to pack the JSON data.");
                }
                settings.put(domainObject);
            }
        }
    }

    private String stringify() {
        return settings.toString();
    }

    private byte[] compress(String data) {
        byte[] encodedData;
        try {
            encodedData = data.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d("Compression error", "UTF-8 is not supported. Using default encoding.");
            encodedData = data.getBytes();
        }

        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(encodedData);
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        while (!deflater.finished()) {
            int byteCount = deflater.deflate(buf);
            baos.write(buf, 0, byteCount);
        }
        deflater.end();

        byte[] compressedData = baos.toByteArray();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        try {
            outputStream.write(ByteBuffer.allocate(4).putInt(encodedData.length).array());
        } catch (IOException e) {
            Log.d("Compression error", "Unable to write compressed data length.");
            e.printStackTrace();
        }
        try {
            outputStream.write(compressedData);
        } catch (IOException e) {
            Log.d("Compression error", "Unable to write compressed data.");
            e.printStackTrace();
        }

        return outputStream.toByteArray();
    }

    private String uncompress(byte[] data) {
        int length = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).getInt();
        if (length > 100000) {
            // This is a sanity check. More than 100kb of password settings make no sense.
            Log.d("Decompression error", "The trasferred length is too big.");
            return "";
        }
        Inflater inflater = new Inflater();
        inflater.setInput(data, 4, data.length-4);
        byte[] decompressedBytes = new byte[length];
        try {
            if (inflater.inflate(decompressedBytes) != length) {
                throw new AssertionError();
            }
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        inflater.end();
        try {
            return new String(decompressedBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d("Decompression error", "UTF-8 is not supported. Using default encoding.");
            return new String(decompressedBytes);
        }
    }

    public byte[] getBlob() {
        return compress(stringify());
    }

    private void removeDeletedSettings(Set<String> domainSet) {
        for (String domain : domainSet) {
            boolean found = false;
            for (int i = 0; i < settings.length(); i++) {
                try {
                    if (settings.getJSONObject(i).getString("domain").equals(domain)) {
                        found = true;
                        break;
                    }
                } catch (JSONException e) {
                    Log.d("update Settings error", "Unable to get JSON data for setting.");
                    e.printStackTrace();
                }
            }
            if (!found) {
                Log.d("removing", domain);
                SharedPreferences.Editor savedDomainsEditor = savedDomains.edit();
                savedDomainsEditor.remove(domain + "_letters");
                savedDomainsEditor.remove(domain + "_numbers");
                savedDomainsEditor.remove(domain + "_special_characters");
                savedDomainsEditor.remove(domain + "_length");
                savedDomainsEditor.remove(domain + "_iterations");
                savedDomainsEditor.remove(domain + "_cDate");
                savedDomainsEditor.remove(domain + "_mDate");
                savedDomainsEditor.apply();
            }
        }
    }

    private void updateSettings() {
        Set<String> domainSet = savedDomains.getStringSet(
                "domainSet",
                new HashSet<String>()
        );
        if (domainSet != null) {
            removeDeletedSettings(domainSet);
            domainSet.clear();
        } else {
            domainSet = new HashSet<>();
        }
        for (int i = 0; i < settings.length(); i++) {
            try {
                domainSet.add(settings.getJSONObject(i).getString("domain"));
            } catch (JSONException e) {
                Log.d("update Settings error", "Unable to get JSON data for setting.");
                e.printStackTrace();
            }
        }
        SharedPreferences.Editor savedDomainsEditor = savedDomains.edit();
        savedDomainsEditor.putStringSet("domainSet", domainSet);
        for (int i = 0; i < settings.length(); i++) {
            try {
                JSONObject setting = settings.getJSONObject(i);
                String domain = setting.getString("domain");
                savedDomainsEditor.putBoolean(
                        domain + "_letters",
                        setting.getBoolean("useLowerCase") && setting.getBoolean("useUpperCase")
                );
                savedDomainsEditor.putBoolean(
                        domain + "_numbers",
                        setting.getBoolean("useDigits")
                );
                savedDomainsEditor.putBoolean(
                        domain + "_special_characters",
                        setting.getBoolean("useExtra")
                );
                savedDomainsEditor.putInt(
                        domain + "_length",
                        setting.getInt("length")
                );
                savedDomainsEditor.putInt(
                        domain + "_iterations",
                        setting.getInt("iterations")
                );
                savedDomainsEditor.putString(
                        domain + "_mDate",
                        setting.getString("mDate")
                );
            } catch (JSONException e) {
                Log.d("update Settings error", "Unable to get JSON data for setting.");
                e.printStackTrace();
            }
        }
        savedDomainsEditor.apply();
    }

    public boolean updateFromBlob(byte[] blob) {
        String jsonString = uncompress(blob);
        try {
            JSONArray loadedSettings = new JSONArray(jsonString);
            boolean updateRemote = false;
            boolean modified = false;
            for (int i = 0; i < loadedSettings.length(); i++) {
                JSONObject loadedSetting = (JSONObject) loadedSettings.get(i);
                boolean found = false;
                for (int j = 0; j < settings.length(); j++) {
                    JSONObject setting = settings.getJSONObject(j);
                    if (setting.get("domain").equals(loadedSetting.get("domain"))) {
                        found = true;
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
                        Date modifiedRemote = df.parse(loadedSetting.getString("mDate"));
                        Date modifiedLocal = df.parse(setting.getString("mDate"));
                        if (modifiedLocal.after(modifiedRemote)) {
                            updateRemote = true;
                        } else {
                            settings.put(j, loadedSetting);
                            modified = true;
                        }
                        break;
                    }
                }
                if (!found) {
                    settings.put(loadedSetting);
                }
            }
            for (int i = 0; i < settings.length(); i++) {
                JSONObject setting = settings.getJSONObject(i);
                boolean found = false;
                for (int j = 0; j < loadedSettings.length(); j++) {
                    JSONObject loadedSetting = loadedSettings.getJSONObject(j);
                    if (setting.get("domain").equals(loadedSetting.get("domain"))) {
                        found = true;
                    }
                }
                if (!found) {
                    updateRemote = true;
                }
            }
            if (modified) {
                updateSettings();
            }
            return updateRemote;
        } catch (JSONException e) {
            Log.d("Update settings error", "Unable to read JSON data.");
            e.printStackTrace();
            return false;
        } catch (ParseException e) {
            Log.d("Update settings error", "Unable to parse the date.");
            e.printStackTrace();
            return false;
        }
    }
}
