package com.ece420.lab1;
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
                        SimpleMatrix x0) {
        if (F == null || H == null) {
            throw new IllegalArgumentException("State transition matrix F and observation matrix H cannot be null.");
        }

        this.F = F;
        this.H = H;

        // Initialize B
        if (B != null) {
            if (B.numRows() != F.numRows()) {
                throw new IllegalArgumentException("Control matrix B must have the same number of rows as F.");
            }
            this.B = B;
        } else {
            this.B = null; // No control input
        }

        // Initialize Q
        this.Q = (Q != null) ? Q : SimpleMatrix.identity(F.numRows());

        // Initialize residualMatrix
        this.residualMatrix = (residualMatrix != null) ? residualMatrix : SimpleMatrix.identity(H.numRows());

        // Initialize P
        if (P != null) {
            if (P.numRows() != F.numRows() || P.numCols() != F.numCols()) {
                throw new IllegalArgumentException("Matrix P dimensions must match F.");
            }
            this.P = P;
        } else {
            this.P = SimpleMatrix.identity(F.numRows());
        }

        // Initialize x
        this.x = (x0 != null) ? x0 : new SimpleMatrix(F.numRows(), 1);
    }

    public SimpleMatrix prediction(SimpleMatrix u) {
        if (B == null || u == null) {
            x = F.mult(x); // Predict without control input
        } else {
            if (u.numRows() != B.numCols()) {
                throw new IllegalArgumentException("Control input vector u dimensions must match B.");
            }
            x = F.mult(x).plus(B.mult(u));
        }

        // Predict next state error covariance
        P = F.mult(P).mult(F.transpose()).plus(Q);
        return x;
    }

    public void update(SimpleMatrix z) {
        if (z == null || z.numRows() != H.numRows()) {
            throw new IllegalArgumentException("Measurement vector z dimensions must match H.");
        }

        // Measurement update
        SimpleMatrix y = z.minus(H.mult(x)); // Innovation
        SimpleMatrix S = residualMatrix.plus(H.mult(P).mult(H.transpose())); // Innovation covariance
        SimpleMatrix K = P.mult(H.transpose()).mult(S.invert()); // Kalman gain

        // Identity matrix
        SimpleMatrix I = SimpleMatrix.identity(F.numRows());

        // Update state estimate
        x = x.plus(K.mult(y));

        // Update estimate covariance
        P = (I.minus(K.mult(H))).mult(P).mult(I.minus(K.mult(H)).transpose()).plus(K.mult(residualMatrix).mult(K.transpose()));
    }
}
