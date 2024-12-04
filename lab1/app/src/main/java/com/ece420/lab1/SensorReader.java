package com.ece420.lab1;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

import com.jjoe64.graphview.GraphView;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.simple.SimpleMatrix;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SensorReader implements SensorEventListener {
    private static final String TAG = "SensorReader";

    // Sensor and Graph variables
    private final SensorManager sensorManager;
    private final Sensor mAccel;
    private GraphManager graphManager;
    private float timestamp;
    private double delta_t;
    private static final float NS2S = 1.0f / 1000000000.0f;

    // Kalman filter variables
    private KalmanFilter kalmanFilter;
    private SimpleMatrix F, H, Q, residualMatrix, x0;

    public SensorReader(Context context, GraphView graph) {
        // Initialize SensorManager and Accelerometer
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Initialize GraphManager
        graphManager = new GraphManager(context, graph);

        if (mAccel == null) {
            Log.e(TAG, "No accelerometer available on this device.");
        }

        // Initialize Kalman Filter
        initializeKalmanFilter();
    }

    private void initializeKalmanFilter() {
        //define sampling rate!!

        //delta_t = 1/50;
        delta_t=0;
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

//        GraphView graph = findViewById(R.id.graph);
//        graphManager = new GraphManager(this, graph);
        // Retrieve the state vector from the Kalman filter
        DMatrixRMaj stateVector = kalmanFilter.getState();

// Extract the 5th and 6th components (indices 4 and 5) as predicted accelerations
        double predictedAccelX = stateVector.get(4, 0); // Predicted X acceleration
        double predictedAccelY = stateVector.get(5, 0); // Predicted Y acceleration

// Update the graph with the predicted accelerations
        graphManager.updateGraph((float) predictedAccelX, (float) predictedAccelY, 0); // Third parameter can represent time or other metrics

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Sensor accuracy changed: " + accuracy);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
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

            // Update graph with the predicted accelX and accelY values
            graphManager.updateGraph((float) fifthComponent, (float) sixthComponent, 0); // Plot predicted accelX and accelY

            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
            }
            timestamp = event.timestamp;
            System.out.printf("timestamp is %f%n", timestamp);

        }
    }

    private void saveDataToFile(float x, float y, float z) {
        String data = String.format("X: %f, Y: %f, Z: %f%n", x, y, z);
        try {
            File file = new File(Environment.getExternalStorageDirectory(), "accelerometer_data.txt");
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.append(data);
            }
            Log.d(TAG, "Data saved to file: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "File write failed: " + e.getMessage());
        }
    }

    public void onResume() {
        // Register the sensor listener
        sensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_GAME);
    }

    public void onPause() {
        // Unregister the sensor listener
        sensorManager.unregisterListener(this);
    }
}
