package com.example.app_ans.core.network;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.app_ans.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class PersistentCookieJar implements CookieJar {
    private static final String PREF_NAME = "cookie_store";
    private static final String KEY_COOKIES = "cookies";
    
    // In-memory cache
    private final Map<String, List<Cookie>> cookieStore = new HashMap<>();
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public PersistentCookieJar(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        load();
    }

    @Override
    public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        String host = url.host();
        List<Cookie> existing = cookieStore.get(host);
        
        List<Cookie> targetList = existing != null ? new ArrayList<>(existing) : new ArrayList<>();
        
        for (Cookie newCookie : cookies) {
            // Remove cookies with same name
            Iterator<Cookie> it = targetList.iterator();
            while (it.hasNext()) {
                Cookie current = it.next();
                if (current.name().equals(newCookie.name())) {
                    it.remove();
                }
            }
            targetList.add(newCookie);
        }
        
        cookieStore.put(host, targetList);
        persist();
    }

    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = cookieStore.get(url.host());
        List<Cookie> validCookies = new ArrayList<>();
        if (cookies != null) {
            long now = System.currentTimeMillis();
            for (Cookie c : cookies) {
                if (c.expiresAt() > now) {
                    validCookies.add(c);
                }
            }
        }
        return validCookies;
    }
    
    private void persist() {
        // Convert internal map to Serializable map
        Map<String, List<SerializableCookie>> serializableMap = new HashMap<>();
        for (Map.Entry<String, List<Cookie>> entry : cookieStore.entrySet()) {
            List<SerializableCookie> list = new ArrayList<>();
            for (Cookie c : entry.getValue()) {
                list.add(new SerializableCookie(c));
            }
            serializableMap.put(entry.getKey(), list);
        }
        
        String json = gson.toJson(serializableMap);
        prefs.edit().putString(KEY_COOKIES, json).apply();
    }
    
    private void load() {
        String json = prefs.getString(KEY_COOKIES, null);
        if (json != null) {
            Type type = new TypeToken<Map<String, List<SerializableCookie>>>(){}.getType();
            Map<String, List<SerializableCookie>> serializableMap = gson.fromJson(json, type);
            
            if (serializableMap != null) {
                for (Map.Entry<String, List<SerializableCookie>> entry : serializableMap.entrySet()) {
                    List<Cookie> cookies = new ArrayList<>();
                    for (SerializableCookie sc : entry.getValue()) {
                        cookies.add(sc.toCookie());
                    }
                    cookieStore.put(entry.getKey(), cookies);
                }
            }
        }
    }

    // DTO for serialization
    private static class SerializableCookie {
        String name;
        String value;
        long expiresAt;
        String domain;
        String path;
        boolean secure;
        boolean httpOnly;
        boolean hostOnly;
        boolean persistent;

        SerializableCookie(Cookie cookie) {
            this.name = cookie.name();
            this.value = cookie.value();
            this.expiresAt = cookie.expiresAt();
            this.domain = cookie.domain();
            this.path = cookie.path();
            this.secure = cookie.secure();
            this.httpOnly = cookie.httpOnly();
            this.hostOnly = cookie.hostOnly();
            this.persistent = cookie.persistent();
        }
        
        Cookie toCookie() {
            Cookie.Builder builder = new Cookie.Builder()
                    .name(name)
                    .value(value)
                    .expiresAt(expiresAt)
                    .path(path);
            
            if (hostOnly) {
                builder.hostOnlyDomain(domain);
            } else {
                builder.domain(domain);
            }
            
            if (secure) builder.secure();
            if (httpOnly) builder.httpOnly();
            
            return builder.build();
        }
    }
}
