package com.example.milestone2kalman;

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

    //Kalman Filter parameters:
    //the timestep used for the delta t

    private KalmanFilter kalmanFilter;
    private SimpleMatrix F, H, Q, residualMatrix, x0;

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

        GraphView graph = findViewById(R.id.graph);
        graphManager = new GraphManager(this, graph);

        //initialize KF:
        initializeKalmanFilter();
    }

    private void initializeKalmanFilter() {
        // Initialize F, H, Q, R matrices based on your system dynamics
        double delta_t = 1.0 / 50.0; // You can initialize delta_t here (e.g., with an arbitrary value)

        // Create your SimpleMatrix instances for Kalman Filter
        SimpleMatrix F = new SimpleMatrix(new double[][] {
                {1, 0, delta_t, 0, 0.5 * delta_t * delta_t, 0},
                {0, 1, 0, delta_t, 0, 0.5 * delta_t * delta_t},
                {0, 0, 1, 0, delta_t, 0},
                {0, 0, 0, 1, 0, delta_t},
                {0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 1}
        });

        SimpleMatrix H = new SimpleMatrix(new double[][] {
                {0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 1}
        });

        SimpleMatrix Q = new SimpleMatrix(new double[][] {
                {0.001, 0, 0, 0, 0, 0},
                {0, 0.001, 0, 0, 0, 0},
                {0, 0, 0.01, 0, 0, 0},
                {0, 0, 0, 0.01, 0, 0},
                {0, 0, 0, 0, 0.1, 0},
                {0, 0, 0, 0, 0, 0.1}
        });

        SimpleMatrix residualMatrix = new SimpleMatrix(new double[][] {
                {0.04, 0},
                {0, 0.04}
        });

        SimpleMatrix x0 = SimpleMatrix.identity(6); // Initial state estimate

        kalmanFilter = new KalmanFilter(F, H, null, Q, residualMatrix, null, x0);
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

            // Log data for debugging
            //Log.d(TAG, "Accel X: " + accelX + ", Y: " + accelY + ", Z: " + accelZ);

            // Optionally save data to a file
            //saveDataToFile(accelX, accelY, accelZ);
            //graphManager.updateGraph(accelX, accelY, accelZ);
            SimpleMatrix z = new SimpleMatrix(new double[][] {
                    {accelX}, // Measurement for accelX
                    {accelY}  // Measurement for accelY
            });
            SimpleMatrix u = new SimpleMatrix(2, 1); // Control input (assuming no control input for simplicity)
            //Perform predict
            SimpleMatrix predictedState = kalmanFilter.prediction(u);
            // Perform Kalman filter update step
            kalmanFilter.update(z);

            // Retrieve the estimated values for accelX and accelY from the state estimate (x)
            double predictedAccelX = predictedState.get(4, 0); // 5th entry in the vector (accelX)
            double predictedAccelY = predictedState.get(5, 0); // 6th entry in the vector (accelY)

            // Update graph with the predicted accelX and accelY values
            graphManager.updateGraph((float)predictedAccelX, (float)predictedAccelY, 0); // Plot predicted accelX and accelY

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
