package com.ece420.lab1;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class GPSGraphManager {
    private final GraphView graphView;
    private final LineGraphSeries<DataPoint> accelXSeries;
    private final LineGraphSeries<DataPoint> accelYSeries;
    private int dataPointIndex = 0;

    public GPSGraphManager(GraphView graphView) {
        this.graphView = graphView;

        // Initialize the data series
        accelXSeries = new LineGraphSeries<>();
        accelYSeries = new LineGraphSeries<>();

        // Add series to the graph
        graphView.addSeries(accelXSeries);
        graphView.addSeries(accelYSeries);

        accelXSeries.setTitle("X-Axis");
        accelXSeries.setColor(android.graphics.Color.RED);

        accelYSeries.setTitle("Y-Axis");
        accelYSeries.setColor(android.graphics.Color.BLUE);

        // Customize the graph
//        graphView.getViewport().setXAxisBoundsManual(true);
//        graphView.getViewport().setYAxisBoundsManual(true);
//        graphView.getViewport().setMinX(0);
//        graphView.getViewport().setMaxX(50); // Show the last 50 samples
//        graphView.getViewport().setMinY(-10); // Adjust to match expected acceleration range
//        graphView.getViewport().setMaxY(10);
        graphView.getViewport().setScalable(true);
        graphView.getViewport().setScalableY(true);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(200);

        //labels
        graphView.getGridLabelRenderer().setHorizontalAxisTitle("Samples");
        graphView.getGridLabelRenderer().setVerticalAxisTitle("Acceleration (m/s²)");
    }

    public void updateGraph(float accelX, float accelY) {
        //add data points to the series
        accelXSeries.appendData(new DataPoint(dataPointIndex, accelX), true, 200);
        accelYSeries.appendData(new DataPoint(dataPointIndex, accelY), true, 200);

        //increment the data point index
        dataPointIndex++;
    }
}
