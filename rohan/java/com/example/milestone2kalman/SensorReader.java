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
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import com.jjoe64.graphview.GraphView;
import org.ejml.simple.SimpleMatrix;
public class SensorReader extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor mAccel;
    //initialize graphManager
    private GraphManager graphManager;
    private static final String TAG = "SensorReader";
    private double delta_t;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;

    //Member variable for Kalman Filter
    private KalmanFilter kalmanFilter;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        String data = "X: " + x + ", Y: " + y + ", Z: " + z + "\n";
        try {
            // File path for saving data
            File file = new File(Environment.getExternalStorageDirectory(), "accelerometer_data.txt");
            FileWriter writer = new FileWriter(file, true); // Append mode
            writer.append(data);
            writer.flush();
            writer.close();
            Log.d(TAG, "Data saved to file: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "File write failed: " + e.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the sensor listener with desired delay
        sensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the sensor listener to save power
        sensorManager.unregisterListener(this);
    }
}
