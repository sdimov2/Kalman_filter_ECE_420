package com.ece420.lab1;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class GPSManager {
    private TextView textLongitude;
    private TextView textLatitude;

    public GPSManager(Context context, TextView textLatitude, TextView textLongitude, GPSGraphManager gpsGraphManager) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.textLatitude = textLatitude;
        this.textLongitude = textLongitude;
        this.gpsGraphManager = gpsGraphManager;
        currentLocation = null;
    }

    private void updateUI(Location location) {
        if (location != null) {
            ((Activity) context).runOnUiThread(() -> {
                textLatitude.setText("Latitude: " + location.getLatitude());
                textLongitude.setText("Longitude: " + location.getLongitude());
            });
        } else {
            ((Activity) context).runOnUiThread(() -> {
                textLatitude.setText("Latitude: N/A");
                textLongitude.setText("Longitude: N/A");
            });
        }
    }


    public static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "GPSManager";

    private final Context context;
    private final LocationManager locationManager;
    private final GPSGraphManager gpsGraphManager;
    private LocationListener locationListener;
    private Location currentLocation;

    public interface LocationUpdateListener {
        void onLocationUpdated(Location location);
    }

    private LocationUpdateListener locationUpdateListener;

    public void setLocationUpdateListener(LocationUpdateListener listener) {
        this.locationUpdateListener = listener;
    }

//    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentLocation = location;
        Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());

        // Notify the listener
        if (locationUpdateListener != null) {
            locationUpdateListener.onLocationUpdated(location);
        }
    }


    public GPSManager(Context context, GPSGraphManager gpsGraphManager) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.gpsGraphManager = gpsGraphManager;
        currentLocation = null;
    }

    // Start GPS location updates
    public void startGPSUpdates() {
        // Check if permissions are granted
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted");
            return; // Exit if permissions are not granted
        }

        // Initialize the location listener
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                currentLocation = location;
                Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
                // Update the UI
                updateUI(location);
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Log.d(TAG, "Location updated: " + latitude + ", " + longitude);

                // Update the graph with the new data
                gpsGraphManager.updateGraph(latitude, longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(@NonNull String provider) {
                Log.d(TAG, "Provider enabled: " + provider);
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Log.e(TAG, "Provider disabled: " + provider);
            }
        };

        // Request updates from GPS_PROVIDER
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000, // 1-second interval
                1,    // 1-meter distance
                locationListener
        );

        // Request updates from NETWORK_PROVIDER
        locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000, // 1-second interval
                1,    // 1-meter distance
                locationListener
        );

        Log.d(TAG, "GPS updates started");
    }

    // Stop GPS location updates
    public void stopGPSUpdates() {
        if (locationListener != null) {
            locationManager.removeUpdates(locationListener);
            locationListener = null;
            Log.d(TAG, "GPS updates stopped");
        }
    }

    public Location getCurrentLocation() {
        // Check if location permissions are granted
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions are not granted. Cannot get last known location.");
            return null; // Return null if permissions are missing
        }

        // Try to get the last known location from GPS_PROVIDER
        Location lastKnownGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownGPS != null) {
            Log.d(TAG, "Using last known GPS location: " + lastKnownGPS.getLatitude() + ", " + lastKnownGPS.getLongitude());
            return lastKnownGPS;
        }

        // If GPS fails, try to get the last known location from NETWORK_PROVIDER
        Location lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (lastKnownNetwork != null) {
            Log.d(TAG, "Using last known Network location: " + lastKnownNetwork.getLatitude() + ", " + lastKnownNetwork.getLongitude());
            return lastKnownNetwork;
        }

        // If no location is available, log a message
        Log.d(TAG, "No last known location available from GPS or Network providers.");
        return null; // No location data available
    }



    // Check if permissions are granted
    public boolean hasLocationPermissions() {
        boolean permissionsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "Location permissions granted: " + permissionsGranted);
        return permissionsGranted;
    }

    // Request permissions from the user
    public void requestLocationPermissions(Activity activity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(context, "GPS access is required to determine your location.", Toast.LENGTH_LONG).show();
        }

        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }
}
