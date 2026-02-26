package com.example.app_ans.core.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class NetworkMonitor {
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> _isConnected = new MutableLiveData<>();
    public final LiveData<Boolean> isConnected = _isConnected;

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            _isConnected.postValue(true);
        }

        @Override
        public void onLost(@NonNull Network network) {
            _isConnected.postValue(false);
        }
    };

    public NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void startMonitoring() {
        // Initial check
        _isConnected.postValue(checkCurrentConnection());

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    public void stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (Exception e) {
            // Callback might not be registered
        }
    }

    private boolean checkCurrentConnection() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
