package com.ece420.lab1;

import android.content.Context;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class GPSGraphManager {
    private final GraphView gpsGraph;
    private final LineGraphSeries<DataPoint> latitudeSeries;
    private final LineGraphSeries<DataPoint> longitudeSeries;
    private double timeCounter = 0; // Tracks elapsed time for X-axis

    public GPSGraphManager(Context context, GraphView gpsGraph) {
        this.gpsGraph = gpsGraph;

        // Initialize series for latitude and longitude
        latitudeSeries = new LineGraphSeries<>();
        longitudeSeries = new LineGraphSeries<>();

        // Add series to the graph
        gpsGraph.addSeries(latitudeSeries);
        gpsGraph.addSeries(longitudeSeries);

        // Configure graph
        gpsGraph.getViewport().setXAxisBoundsManual(true);
        gpsGraph.getViewport().setMinX(0);
        gpsGraph.getViewport().setMaxX(30); // Show the last 30 seconds of data

        gpsGraph.getViewport().setYAxisBoundsManual(true);
        gpsGraph.getViewport().setMinY(-90); // Latitude range
        gpsGraph.getViewport().setMaxY(90); // Latitude range

        latitudeSeries.setColor(context.getResources().getColor(android.R.color.holo_red_light));
        longitudeSeries.setColor(context.getResources().getColor(android.R.color.holo_blue_light));
    }

    public void updateGraph(double latitude, double longitude) {
        // Run on the UI thread to update the graph safely
        gpsGraph.post(() -> {
            latitudeSeries.appendData(new DataPoint(timeCounter, latitude), true, 100);
            longitudeSeries.appendData(new DataPoint(timeCounter, longitude), true, 100);
            timeCounter++;
        });
    }
}
