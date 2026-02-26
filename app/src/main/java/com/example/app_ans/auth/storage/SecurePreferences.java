package com.example.app_ans.auth.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/** Simple wrapper around EncryptedSharedPreferences for storing tokens safely. */
public class SecurePreferences {
    private static final String PREF_NAME = "ans_secure_prefs";
    private final SharedPreferences prefs;

    public SecurePreferences(Context context) {
        try {
            MasterKey key = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    key,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to init secure storage", e);
        }
    }

    public void saveToken(String key, String value) {
        prefs.edit().putString(key, value).apply();
    }

    public String getToken(String key) {
        return prefs.getString(key, null);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}

