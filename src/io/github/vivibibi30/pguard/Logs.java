package io.github.vivibibi30.pguard;

import android.util.Log;

final class Logs {

    private static final String TAG = "PrivacyGuard";

    private Logs() {
    }

    static void i(String msg) {
        try {
            Log.i(TAG, msg);
        } catch (Throwable ignored) {
        }
    }
}
