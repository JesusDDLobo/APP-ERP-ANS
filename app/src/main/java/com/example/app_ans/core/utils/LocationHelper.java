package com.example.app_ans.core.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationHelper {

    public interface LocationCallback {
        void onSuccess(double latitude, double longitude);
        void onFailure(String message);
    }

    public static boolean hasLocationPermissions(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void getCurrentLocation(Context context, LocationCallback callback) {
        if (!hasLocationPermissions(context)) {
            callback.onFailure("Permisos de ubicación no concedidos");
            return;
        }

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            callback.onSuccess(location.getLatitude(), location.getLongitude());
                        } else {
                            callback.onFailure("No se pudo obtener la ubicación actual");
                        }
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        } catch (SecurityException e) {
            callback.onFailure("Error de seguridad al acceder a la ubicación");
        }
    }

    public static float calculateDistance(double startLat, double startLng, double endLat, double endLng) {
        float[] results = new float[1];
        Location.distanceBetween(startLat, startLng, endLat, endLng, results);
        return results[0]; // meters
    }
}

