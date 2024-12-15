package com.example.milestone2kalman;

import android.content.Context;
import android.util.Log;


import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class GraphManager {
    private LineGraphSeries<DataPoint> seriesX, seriesY, seriesvelox, seriesveloy;
    private int timeIndex = 0;

    public GraphManager(Context context, GraphView graph) {
        // Initialize the graph and series
        seriesX = new LineGraphSeries<>();
        seriesY = new LineGraphSeries<>();
        seriesvelox = new LineGraphSeries<>();  // GPS X velo
        seriesveloy = new LineGraphSeries<>();  // GPS Y Acceleration

        graph.addSeries(seriesX);
        graph.addSeries(seriesY);
        graph.addSeries(seriesvelox);
        graph.addSeries(seriesveloy);

        // Style series
        seriesX.setTitle("X-Axis (Kalman)");
        seriesX.setColor(android.graphics.Color.RED);

        seriesY.setTitle("Y-Axis (Kalman)");
        seriesY.setColor(android.graphics.Color.BLUE);

        seriesvelox.setTitle("X-Axis (GPS)");
        seriesvelox.setColor(android.graphics.Color.GREEN);

        seriesveloy.setTitle("Y-Axis (GPS)");
        seriesveloy.setColor(android.graphics.Color.MAGENTA);

        //seriesZ.setTitle("Z-Axis");
        //seriesZ.setColor(android.graphics.Color.GREEN);

        // Optional: enable scaling and scrolling
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);
    }


    public void updateGraph(float accelX, float accelY, float velox, float veloy) {
        // Log the values being appended
//        Log.d("myTag", "AccelX: " + accelX + ", AccelY: " + accelY);
//        Log.d("myTag", "GPS AccelX: " + gpsAccelX + ", GPS AccelY: " + gpsAccelY);

        // Add data points to the series
        seriesX.appendData(new DataPoint(timeIndex, accelX), true, 50);
        seriesY.appendData(new DataPoint(timeIndex, accelY), true, 50);
        seriesvelox.appendData(new DataPoint(timeIndex, velox), true, 50);
        seriesveloy.appendData(new DataPoint(timeIndex, veloy), true, 50);

        timeIndex++;
    }
}

