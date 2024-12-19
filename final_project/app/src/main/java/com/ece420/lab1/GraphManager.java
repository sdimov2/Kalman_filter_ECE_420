package com.ece420.lab1;


import android.content.Context;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class GraphManager {
    private LineGraphSeries<DataPoint> seriesX, seriesY, rseriesX, rseriesY;
    private int timeIndex = 0;

    public GraphManager(Context context, GraphView graph) {
        // Initialize the graph and series
        seriesX = new LineGraphSeries<>();
        seriesY = new LineGraphSeries<>();
        rseriesX = new LineGraphSeries<>();
        rseriesY = new LineGraphSeries<>();
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

        rseriesX.setTitle("rX-Axis");
        rseriesX.setColor(android.graphics.Color.GREEN);

        rseriesY.setTitle("rY-Axis");
        rseriesY.setColor(android.graphics.Color.YELLOW);

        //seriesZ.setTitle("Z-Axis");
        //seriesZ.setColor(android.graphics.Color.GREEN);

        graph.addSeries(rseriesX);
        graph.addSeries(rseriesY);

        // Optional: enable scaling and scrolling
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(200);
    }

    public void updateGraph(float accelX, float accelY, float accelZ, float rawaccelX, float rawaccelY) {
        // Add data points to the series
        seriesX.appendData(new DataPoint(timeIndex, accelX), true, 200);
        seriesY.appendData(new DataPoint(timeIndex, accelY), true, 200);
        rseriesX.appendData(new DataPoint(timeIndex, rawaccelX), true, 200);
        rseriesY.appendData(new DataPoint(timeIndex, rawaccelY), true, 200);

//        seriesX.appendData(new DataPoint(timeIndex, GPSaccelX), true, 50);
//        seriesY.appendData(new DataPoint(timeIndex, GPSaccelY), true, 50);


        //seriesZ.appendData(new DataPoint(timeIndex, accelZ), true, 50);

        timeIndex++;
    }
}

