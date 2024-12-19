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

public class SensorReader extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor mAccel;
    //initialize graphManager
    private GraphManager graphManager;
    private static final String TAG = "SensorReader";
    private double delta_t;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    int timesteps;
    float totalmsesumraw;
    float currentmseraw;

    float totalmsesumfilt;
    float currentmsefilt;

    //member variable for Kalman Filter
    private KalmanFilter kalmanFilter;




    @Override
    public final void onCreate(Bundle savedInstanceState) {
        timesteps=0;
        totalmsesumraw=0;
        currentmseraw=0;

        //initialize the filtered MSE
        totalmsesumfilt=0;
        currentmsefilt=0;


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize SensorManager and accelerometer sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //check if the accelerometer is available
        if (mAccel == null) {
            Toast.makeText(this, "Accelerometer not available on this device", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if no accelerometer
        }

        //define sampling rate!!

        //delta_t = 1/50;
        delta_t=0;
        timestamp= 0.0F;


        //dimensions of my state:
        int stateDim = 6; //6 states corresponding to accelerometer in the X and Y directions
        int measurementDim = 2; // 2D accelerometer (X, Y)

        //construct objects for F, Q, and H
        DMatrixRMaj F = new DMatrixRMaj(stateDim, stateDim); // Identity matrix or your transition model
        DMatrixRMaj Q = new DMatrixRMaj(stateDim, stateDim); // Process noise covariance matrix
        DMatrixRMaj H = new DMatrixRMaj(measurementDim, stateDim); // Measurement matrix for 2D accelerometer

        F.set(0, 0, 1); F.set(0, 2, delta_t); F.set(0, 4, 0.5 * (delta_t * delta_t)); // Position (X)
        F.set(1, 1, 1); F.set(1, 3, delta_t); F.set(1, 5, 0.5 * (delta_t * delta_t)); // Position (Y)
        F.set(2, 2, 1); F.set(2, 4, delta_t); // Velocity (X)
        F.set(3, 3, 1); F.set(3, 5, delta_t); // Velocity (Y)
        F.set(4, 4, 1);  // Acceleration (X)
        F.set(5, 5, 1);  // Acceleration (Y)

//observation matrix (H) - accelerometer measurement
        H.set(0, 4, 1); // Measurement for X acceleration
        H.set(1, 5, 1); // Measurement for Y acceleration

//process noise covariance matrix (Q) - adjusted based on expected process noise
        Q.set(0, 0, 0.001);
        Q.set(1, 1, 0.001);
        Q.set(2, 2, 0.01);
        Q.set(3, 3, 0.01);
        Q.set(4, 4, 0.1);
        Q.set(5, 5, 0.1);

        //initialize the Kalman filter
        kalmanFilter = new KalmanFilter();

//configure the filter with the state transition matrix (F), process noise covariance (Q), and measurement matrix (H)
        kalmanFilter.configure(F, Q, H);

        //Now you can initialize it!
        DMatrixRMaj initialState = new DMatrixRMaj(stateDim, 1);  // Initialize state (e.g., velocity, position)
        DMatrixRMaj initialCovariance = new DMatrixRMaj(stateDim, stateDim);
        initialCovariance.set(0, 0, 0.1);
        initialCovariance.set(1, 1, 0.1);
        initialCovariance.set(2, 2, 0.1);
        initialCovariance.set(3, 3, 0.1);
        initialCovariance.set(4, 4, 0.1);
        initialCovariance.set(5, 5, 0.1);

        //replace this and change the Kalman Filter's initial covariance to have values that are minimal

        kalmanFilter.setState(initialState, initialCovariance);

        GraphView graph = findViewById(R.id.graph);
        graphManager = new GraphManager(this, graph);

    }



    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        //handle sensor accuracy changes if needed
        Log.d(TAG, "Sensor accuracy changed: " + accuracy);
    }



    @Override


            public final void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                    if (timestamp != 0) {
                        final float dT = (event.timestamp - timestamp) * NS2S;
                        kalmanFilter.reconfigurefmatrix(dT);
                        timesteps++;

                    }
                    //make prediction
                    kalmanFilter.predict();
                    //handle update stage for Kalman!
                    //Need to define R matrices and Z vector for each time step!
                    DMatrixRMaj R = new DMatrixRMaj(2, 2); // 2x2 measurement noise covariance matrix for two measurements (X and Y accelerations)
                    R.set(0, 0, 0.1);
                    R.set(1, 1, 0.1);
                    // Retrieve accelerometer data
                    float accelX = event.values[0]; // X-axis acceleration
                    float accelY = event.values[1]; // Y-axis acceleration

                    //calculate MSE error of the direct raw accelerometer data vs ground truth (zero acceleration)
                    double currenterrorX=(accelX-0)*(accelX-0);
                    totalmsesumraw+=currenterrorX;
                    currentmseraw=totalmsesumraw/timesteps;


            //float accelZ = event.values[2]; // Z-axis acceleration
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

           //calculate the MSE error of the Kalman Filtered Accelerometer Data vs the ground truth "zero accel"
           float currenterrorXfilt = (float) ((fifthComponent-0)*(fifthComponent-0));
           totalmsesumfilt+=currenterrorXfilt;
           currentmsefilt=totalmsesumfilt/timesteps;


            //Log.d("myTag", "Kalman'ed XAccel: " + fifthComponent);
            //Log.d("myTag", "Kalman'ed YAccel: " + sixthComponent);

            //update graph with the predicted accelX and accelY values
            //graphManager.updateGraph((float) fifthComponent, (float) sixthComponent); // Plot predicted accelX and accelY
            graphManager.updateGraph(currentmseraw, currentmsefilt);


            timestamp = event.timestamp;
            //System.out.printf("timestamp is %f%n", timestamp);
        }
    }







    @Override

    protected void onResume() {
        super.onResume();
        // Register the sensor listener with desired delay
        sensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_GAME);

        // initialize location manager and register location listener

    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister the sensor listener to save power



    }






}
