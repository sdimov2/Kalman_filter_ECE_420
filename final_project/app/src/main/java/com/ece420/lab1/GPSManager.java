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
    private Location previousLocation = null;
    private double previousVelocityX = 0;
    private double previousVelocityY = 0;
    private long previousTimestamp = 0;
    private TextView textDistance ;

    public GPSManager(Context context, TextView textLatitude, TextView textLongitude, GPSGraphManager gpsGraphManager) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.textLatitude = textLatitude;
        this.textLongitude = textLongitude;
        this.gpsGraphManager = gpsGraphManager;
        this.textDistance = textDistance;
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

    private double totalDistance = 0;
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
    if (previousLocation != null) {
        // Time difference in seconds
        double deltaTime = (location.getTime() - previousTimestamp) / 1000.0;
        float distance = previousLocation.distanceTo(location); // Distance in meters
        totalDistance += distance; // Update total distance
//        textDistance.setText( totalDistance);
//        textLatitude.setText("Latitude: " + location.getLatitude());
        // Log the distance
        Log.d(TAG, "Distance traveled: " + distance + " meters, Total Distance: " + totalDistance + " meters");

        if (deltaTime > 0) {
            // Latitude/Longitude differences converted to meters
            double latitudeDifference = Math.toRadians(location.getLatitude() - previousLocation.getLatitude());
            double longitudeDifference = Math.toRadians(location.getLongitude() - previousLocation.getLongitude());
            double earthRadius = 6371000; // Earth's radius in meters

            double deltaX = earthRadius * Math.cos(Math.toRadians(previousLocation.getLatitude())) * longitudeDifference;
            double deltaY = earthRadius * latitudeDifference;

            // Calculate velocities
            double velocityX = deltaX / deltaTime;
            double velocityY = deltaY / deltaTime;

            // Calculate accelerations
            double accelerationX = (velocityX - previousVelocityX) / deltaTime;
            double accelerationY = (velocityY - previousVelocityY) / deltaTime;

            // Update the graph
            gpsGraphManager.updateGraph((float) accelerationX, (float) accelerationY);

            // Log the acceleration values
            Log.d(TAG, "Acceleration X: " + accelerationX + ", Acceleration Y: " + accelerationY);

            // Update previous values
            previousVelocityX = velocityX;
            previousVelocityY = velocityY;
        }
    }

    // Update previous location and timestamp
    previousLocation = location;
    previousTimestamp = location.getTime();

    // Update UI with current location
    updateUI(location);
}



    public GPSManager(Context context, GPSGraphManager gpsGraphManager) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.gpsGraphManager = gpsGraphManager;
        currentLocation = null;
    }

    // Start GPS location updates
    public void startGPSUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted");
            return;
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                // Delegate acceleration and graph update to the main `onLocationChanged` method
                GPSManager.this.onLocationChanged(location);
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

        // Request updates from GPS and Network providers
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, locationListener);

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
