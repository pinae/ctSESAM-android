package de.pinyto.ctSESAM;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Calculate a password and display it.
 */
class GeneratePasswordTask extends AsyncTask<byte[], Void, PasswordGenerator> {
    private WeakReference<OnPasswordGeneratedListener> passwordGeneratedListenerWeakRef;

    GeneratePasswordTask(OnPasswordGeneratedListener passwordGeneratedListener) {
        super();
        this.passwordGeneratedListenerWeakRef = new WeakReference<>(passwordGeneratedListener);
    }

    @Override
    protected PasswordGenerator doInBackground(byte[]... params) {
        byte[] domain = params[0];
        byte[] username = params[1];
        byte[] kgk = params[2];
        byte[] salt = params[3];
        int iterations = ByteBuffer.wrap(
                Arrays.copyOfRange(params[4], 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt();
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
        OnPasswordGeneratedListener passwordGeneratedListener =
                passwordGeneratedListenerWeakRef.get();
        if (passwordGeneratedListener != null) {
            passwordGeneratedListener.onFinished(generator);
        }
    }

    public interface OnPasswordGeneratedListener {
        void onFinished(PasswordGenerator generator);
    }
}
