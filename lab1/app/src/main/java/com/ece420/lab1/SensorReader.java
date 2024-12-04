package com.ece420.lab1;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

import com.jjoe64.graphview.GraphView;
import org.ejml.simple.SimpleMatrix;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SensorReader implements SensorEventListener {
    private static final String TAG = "SensorReader";

    // Sensor and Graph variables
    private final SensorManager sensorManager;
    private final Sensor mAccel;
    private final GraphManager graphManager;

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
        double delta_t = 1.0 / 50.0; // Sampling period

        // State transition matrix (F)
        F = new SimpleMatrix(new double[][] {
                {1, 0, delta_t, 0, 0.5 * delta_t * delta_t, 0},
                {0, 1, 0, delta_t, 0, 0.5 * delta_t * delta_t},
                {0, 0, 1, 0, delta_t, 0},
                {0, 0, 0, 1, 0, delta_t},
                {0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 1}
        });

        // Measurement matrix (H)
        H = new SimpleMatrix(new double[][] {
                {0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 1}
        });

        // Process noise covariance matrix (Q)
        Q = new SimpleMatrix(new double[][] {
                {0.001, 0, 0, 0, 0, 0},
                {0, 0.001, 0, 0, 0, 0},
                {0, 0, 0.01, 0, 0, 0},
                {0, 0, 0, 0.01, 0, 0},
                {0, 0, 0, 0, 0.1, 0},
                {0, 0, 0, 0, 0, 0.1}
        });

        // Measurement noise covariance matrix
        residualMatrix = new SimpleMatrix(new double[][] {
                {0.04, 0},
                {0, 0.04}
        });

        // Initial state estimate
        x0 = new SimpleMatrix(6, 1); // 6x1 state vector, initialized to zero

        // Initialize Kalman Filter
        kalmanFilter = new KalmanFilter(F, H, null, Q, residualMatrix, null, x0);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Sensor accuracy changed: " + accuracy);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Retrieve accelerometer data
            float accelX = event.values[0];
            float accelY = event.values[1];

            // Measurement matrix (z)
            SimpleMatrix z = new SimpleMatrix(new double[][] {
                    {accelX},
                    {accelY}
            });

            // Control input matrix (u), assuming no control input
            SimpleMatrix u = new SimpleMatrix(2, 1);

            // Perform Kalman Filter prediction and update
            SimpleMatrix predictedState = kalmanFilter.prediction(u);
            kalmanFilter.update(z);

            // Extract predicted accelerations
            double predictedAccelX = predictedState.get(4, 0);
            double predictedAccelY = predictedState.get(5, 0);

            // Update the graph with predicted values
            graphManager.updateGraph((float) predictedAccelX, (float) predictedAccelY, 0);
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
