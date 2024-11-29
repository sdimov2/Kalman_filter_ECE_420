package com.example.milestone2kalman;

import android.content.Context;


import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class GraphManager {
    private LineGraphSeries<DataPoint> seriesX, seriesY, seriesZ;
    private int timeIndex = 0;

    public GraphManager(Context context, GraphView graph) {
        // Initialize the graph and series
        seriesX = new LineGraphSeries<>();
        seriesY = new LineGraphSeries<>();
        //seriesZ = new LineGraphSeries<>();

        graph.addSeries(seriesX);
        graph.addSeries(seriesY);
        //graph.addSeries(seriesZ);

        // Style series
        seriesX.setTitle("X-Axis");
        seriesX.setColor(android.graphics.Color.RED);

        seriesY.setTitle("Y-Axis");
        seriesY.setColor(android.graphics.Color.BLUE);

        //seriesZ.setTitle("Z-Axis");
        //seriesZ.setColor(android.graphics.Color.GREEN);

        // Optional: enable scaling and scrolling
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);
    }

    public void updateGraph(float accelX, float accelY, float accelZ) {
        // Add data points to the series
        seriesX.appendData(new DataPoint(timeIndex, accelX), true, 50);
        seriesY.appendData(new DataPoint(timeIndex, accelY), true, 50);
        //seriesZ.appendData(new DataPoint(timeIndex, accelZ), true, 50);

        timeIndex++;
    }
}

