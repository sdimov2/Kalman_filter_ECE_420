package com.example.milestone2kalman;
import org.ejml.simple.SimpleMatrix;


public class KalmanFilter {
    private SimpleMatrix F; // State transition matrix
    private SimpleMatrix B; // Control matrix
    private SimpleMatrix H; // Observation matrix
    private SimpleMatrix Q; // Process noise covariance
    private SimpleMatrix residualMatrix; // Measurement noise covariance
    private SimpleMatrix P; // Estimate error covariance
    private SimpleMatrix x; // State estimate

    public KalmanFilter(SimpleMatrix F, SimpleMatrix H,
                        SimpleMatrix B, SimpleMatrix Q,
                        SimpleMatrix residualMatrix, SimpleMatrix P,
                        SimpleMatrix x0){
        if (F == null || H == null) {
            throw new IllegalArgumentException("Set proper system dynamics.");
        }

        this.F = F;
        this.H = H;

        // Manually handle defaults for optional parameters
        if (B != null) {
            this.B = B;
        } else {
            this.B = new SimpleMatrix(F.numRows(), 1);
        }

        if (Q != null) {
            this.Q = Q;
        } else {
            this.Q = SimpleMatrix.identity(F.numRows());
        }

        if (residualMatrix != null) {
            this.residualMatrix = residualMatrix;
        } else {
            this.residualMatrix = SimpleMatrix.identity(H.numRows());
        }

        if (P != null) {
            this.P = P;
        } else {
            this.P = SimpleMatrix.identity(F.numRows());
        }

        if (x0 != null) {
            this.x = x0;
        } else {
            this.x = new SimpleMatrix(F.numRows(), 1);
        }
    }

    public SimpleMatrix prediction(SimpleMatrix u){
        if (u == null) {
            u = new SimpleMatrix(B.numCols(), 1);
        }
        //predict next state
        x = F.mult(x).plus(B.mult(u));
        //predict next state error covariance
        P = F.mult(P).mult(F.transpose()).plus(Q);
        return x;
    }

    public void update(SimpleMatrix z){
        //This is the measurement update
        SimpleMatrix y = z.minus(H.mult(x)); // Innovation
        SimpleMatrix S = residualMatrix.plus(H.mult(P).mult(H.transpose())); // Innovation covariance
        SimpleMatrix K = P.mult(H.transpose()).mult(S.invert()); // Kalman gain

        //identity matrix
        SimpleMatrix I = SimpleMatrix.identity(F.numRows());

        //update state estimate
        x = x.plus(K.mult(y));

        //update estimate covariance
        P = (I.minus(K.mult(H))).mult(P).mult(I.minus(K.mult(H)).transpose()).plus(K.mult(residualMatrix).mult(K.transpose()));
    }
}
