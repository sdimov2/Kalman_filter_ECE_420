package com.example.milestone2kalman;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.Settings;

import android.content.Context;

import android.content.Intent;

import android.os.Environment;

import android.util.Log;
import android.widget.Toast;
import android.location.Location;
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
import android.location.LocationListener;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.jjoe64.graphview.GraphView;
import org.ejml.simple.SimpleMatrix;

public class SensorReader extends Activity implements SensorEventListener, LocationListener {
    private SensorManager sensorManager;
    private Sensor mAccel;
    //initialize graphManager
    private GraphManager graphManager;
    private static final String TAG = "SensorReader";
    private double delta_t;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private float timestampgps;

    private static final int REQUEST_PERMISSION_CODE = 100;

    //initialize previous velocity
    private Location previousLocation = null;
    private double previousVelocityX = 0;
    private double previousVelocityY = 0;
    private double accelerationX=0;
    private double accelerationY=0;




    //Member variable for Kalman Filter
    private KalmanFilter kalmanFilter;




    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_CODE);
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Check if GPS provider is enabled
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Request location updates if GPS is enabled
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this); // 1000ms (1 second) and 1 meter threshold
        } else {
            // GPS is not enabled, prompt the user to enable it
            Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show();
            Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(gpsOptionsIntent);
        }

        // Initialize SensorManager and accelerometer sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Check if the accelerometer is available
        if (mAccel == null) {
            Toast.makeText(this, "Accelerometer not available on this device", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if no accelerometer
        }

        //define sampling rate!!

        //delta_t = 1/50;
        delta_t=0;

        //initialize the initial calculations of ground truth values

        //initialize previousLocation to Null:




        //initialize Kalman Filter

        //dimensions of my state:
        int stateDim = 6; // Example state dimension (could be more based on your system)
        int measurementDim = 2; // 2D accelerometer (X, Y)

        //construct objects for F, Q, and H
        DMatrixRMaj F = new DMatrixRMaj(stateDim, stateDim); // Identity matrix or your transition model
        DMatrixRMaj Q = new DMatrixRMaj(stateDim, stateDim); // Process noise covariance matrix
        DMatrixRMaj H = new DMatrixRMaj(measurementDim, stateDim); // Measurement matrix for 2D accelerometer

        F.set(0, 0, 1); F.set(0, 2, delta_t); F.set(0, 4, 0.5 * delta_t * delta_t); // Position (X)
        F.set(1, 1, 1); F.set(1, 3, delta_t); F.set(1, 5, 0.5 * delta_t * delta_t); // Position (Y)
        F.set(2, 2, 1); F.set(2, 4, delta_t); // Velocity (X)
        F.set(3, 3, 1); F.set(3, 5, delta_t); // Velocity (Y)
        F.set(4, 4, 1);  // Acceleration (X)
        F.set(5, 5, 1);  // Acceleration (Y)

// Observation matrix (H) - accelerometer measurement
        H.set(0, 4, 1); // Measurement for X acceleration
        H.set(1, 5, 1); // Measurement for Y acceleration

// Process noise covariance matrix (Q) - adjusted based on expected process noise
        Q.set(0, 0, 0.001);
        Q.set(1, 1, 0.001);
        Q.set(2, 2, 0.01);
        Q.set(3, 3, 0.01);
        Q.set(4, 4, 0.1);
        Q.set(5, 5, 0.1);

        // Initialize the Kalman filter
        kalmanFilter = new KalmanFilter();

// Configure the filter with the state transition matrix (F), process noise covariance (Q), and measurement matrix (H)
        kalmanFilter.configure(F, Q, H);

        //Now you can initialize it!
        DMatrixRMaj initialState = new DMatrixRMaj(stateDim, 1);  // Initialize state (e.g., velocity, position)
        DMatrixRMaj initialCovariance = new DMatrixRMaj(stateDim, stateDim);
        CommonOps_DDRM.setIdentity(initialCovariance);  // High uncertainty initially
        kalmanFilter.setState(initialState, initialCovariance);

        GraphView graph = findViewById(R.id.graph);
        graphManager = new GraphManager(this, graph);

    }

    private boolean isGpsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void promptEnableGps() {
        Toast.makeText(this, "GPS is disabled, please enable it", Toast.LENGTH_SHORT).show();
        // Open the location settings screen
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    private void startLocationUpdates() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Use GPS provider if available, otherwise fall back to network provider
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, now check if GPS is enabled
                if (!isLocationEnabled()) {
                    // GPS is not enabled, prompt user to enable it
                    promptForGPS();
                } else {
                    // Start location updates if GPS is enabled
                    startLocationUpdates();
                }
            } else {
                // Permission denied, handle accordingly
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void promptForGPS() {
        Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(gpsOptionsIntent);
    }


    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle sensor accuracy changes if needed
        Log.d(TAG, "Sensor accuracy changed: " + accuracy);
    }



    @Override
    public final void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Retrieve accelerometer data
            float accelX = event.values[0]; // X-axis acceleration
            float accelY = event.values[1]; // Y-axis acceleration
            //float accelZ = event.values[2]; // Z-axis acceleration


            //Make prediction!
            kalmanFilter.predict();



            //handle update stage for Kalman!
            //Need to define R matrices and Z vector for each time step!
            DMatrixRMaj R = new DMatrixRMaj(2, 2); // 2x2 matrix for two measurements (X and Y accelerations)
            CommonOps_DDRM.setIdentity(R);

            DMatrixRMaj z = new DMatrixRMaj(2, 1); // 2x1 matrix for the measurements
            z.set(0, 0, accelX); // Set X acceleration
            z.set(1, 0, accelY); // Set Y acceleration

            //the big task! pass it into the update function and make sure it doesn't fail!
            kalmanFilter.update(z, R);

            //get the state and actually read it!
            DMatrixRMaj getState = kalmanFilter.getState();
            // Access the 5th component (index 4) and 6th component (index 5)
            double fifthComponent = getState.get(4, 0);  // 5th component of the state vector
            double sixthComponent = getState.get(5, 0);  // 6th component of the state vector
            double thirdQuestion = getState.get(2, 0);
            double fourthQuestion = getState.get(3, 0);


            // Update graph with the predicted accelX and accelY values
            graphManager.updateGraph((float) fifthComponent, (float) sixthComponent, (float) thirdQuestion, (float) fourthQuestion ); // Plot predicted accelX and accelY

            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                kalmanFilter.reconfigurefmatrix(dT);

            }
            timestamp = event.timestamp;
            //System.out.printf("timestamp is %f%n", timestamp);
        }
    }



    @Override
    public void onLocationChanged(@NonNull  Location location) {
        Log.d(TAG, "onLocationChanged triggered");
        if (previousLocation != null) {
            // Ensure timestamp is being properly updated
            if (timestampgps != 0) {
                final float deltaT = (location.getTime() - timestampgps) * 1e-3f;  // Time in seconds
                if (deltaT > 0) {
                    // Calculate differences in latitude and longitude
                    double latitudeDifference = Math.toRadians(location.getLatitude() - previousLocation.getLatitude());
                    double longitudeDifference = Math.toRadians(location.getLongitude() - previousLocation.getLongitude());
                    Log.d("LocationChanged", "Current Latitude: " + location.getLatitude());
                    Log.d("LocationChanged", "Current Longitude: " + location.getLongitude());

                    // Earth's radius in meters
                    double earthRadius = 6371000;

                    // Calculate the changes in X and Y (in meters)
                    double deltaX = earthRadius * Math.cos(Math.toRadians(previousLocation.getLatitude())) * longitudeDifference;
                    double deltaY = earthRadius * latitudeDifference;

                    // Calculate velocities (in meters per second)
                    double velocityX = deltaX / deltaT;
                    double velocityY = deltaY / deltaT;

                    // Calculate accelerations (in meters per second squared)
                    accelerationX = (float) ((velocityX - previousVelocityX) / deltaT);
                    accelerationY = (float) ((velocityY - previousVelocityY) / deltaT);

                    // Log the calculated values for debugging


                    // Update previous values
                    previousVelocityX = velocityX;
                    previousVelocityY = velocityY;
                }

                // Print the deltaT for debugging
                System.out.printf("Delta T for location: %f seconds%n", deltaT);
            }

            // Update the timestamp for the next location update
            timestampgps = location.getTime();
        }

        // Store the current location as the previous location for the next update
        previousLocation = location;

    }



    @Override

    protected void onResume() {
        super.onResume();
        // Register the sensor listener with desired delay
        sensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_GAME);

        // Initialize location manager and register location listener

    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the sensor listener to save power
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(this);


    }






}
