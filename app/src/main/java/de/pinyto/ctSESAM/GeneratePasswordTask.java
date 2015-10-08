package de.pinyto.ctSESAM;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Calculate a password and display it.
 */
class GeneratePasswordTask extends AsyncTask<byte[], Void, PasswordGenerator> {
    private WeakReference<MainActivity> mainActivityWeakRef;

    GeneratePasswordTask(MainActivity mainActivity) {
        super();
        this.mainActivityWeakRef = new WeakReference<>(mainActivity);
    }

    @Override
    protected PasswordGenerator doInBackground(byte[]... params) {
        byte[] domain = params[0];
        byte[] username = params[1];
        byte[] kgk = params[2];
        byte[] salt = params[3];
        int iterations = ByteBuffer.wrap(Arrays.copyOfRange(params[4], 0, 4)).getInt();
        try {
            return new PasswordGenerator(
                    domain,
                    username,
                    kgk,
                    salt,
                    iterations);
        } catch (NotHashedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(PasswordGenerator generator) {
        MainActivity activity = mainActivityWeakRef.get();
        if (activity != null && !activity.isFinishing()) {
            activity.setPasswordGenerator(generator);
            activity.generatePassword();
        }
    }
}
