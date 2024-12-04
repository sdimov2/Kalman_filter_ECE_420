package com.ece420.lab1;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;

public class PedometerSimple extends Activity {

    private static final String TAG = "PedometerSimple";

    // GPS Manager and SensorReader
    private GPSManager gpsManager;
    private SensorReader sensorReader;

    // Sensor variables
//    private SensorManager mSensorManager;

    // UI Text Views
//    private TextView textStatus;
    private TextView textLatitude;
    private TextView textLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedometer_simple);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialize TextViews
//        textStatus = findViewById(R.id.textStatus);
        textLatitude = findViewById(R.id.textLatitude);
        textLongitude = findViewById(R.id.textLongitude);

//        // Initialize SensorManager
//        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Initialize GraphView
        GraphView graph = findViewById(R.id.graph);

        // Initialize SensorReader
        sensorReader = new SensorReader(this, graph);

        GraphView gpsGraph = findViewById(R.id.gpsGraph);

        GPSGraphManager gpsGraphManager = new GPSGraphManager(gpsGraph);



        // Initialize GPS Manager
        gpsManager = new GPSManager(this, textLatitude, textLongitude, gpsGraphManager);

        // Request permissions and start GPS updates
        if (gpsManager.hasLocationPermissions()) {
            gpsManager.startGPSUpdates();
        } else {
            gpsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == GPSManager.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permissions granted");
                gpsManager.startGPSUpdates();
            } else {
                Log.e(TAG, "Location permissions denied");
                Toast.makeText(this, "Location permissions are required for GPS functionality", Toast.LENGTH_SHORT).show();
                gpsManager.stopGPSUpdates();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorReader != null) {
            sensorReader.onResume();
        }
        gpsManager.startGPSUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorReader != null) {
            sensorReader.onPause();
        }
        gpsManager.stopGPSUpdates();
    }
}
