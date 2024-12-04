package com.example.milestone2kalman;




import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;

import static org.ejml.dense.row.CommonOps_DDRM.*;


public class KalmanFilter {
    // kinematics description
    private DMatrixRMaj F, Q, H;

    // system state estimate
    private DMatrixRMaj x, P;

    // these are predeclared for efficiency reasons
    private DMatrixRMaj a, b;
    private DMatrixRMaj y, S, S_inv, c, d;
    private DMatrixRMaj K;

    private LinearSolverDense<DMatrixRMaj> solver;

     public void configure( DMatrixRMaj F, DMatrixRMaj Q, DMatrixRMaj H ) {
        this.F = F;
        this.Q = Q;
        this.H = H;

        int dimenX = F.numCols;
        int dimenZ = H.numRows;

        a = new DMatrixRMaj(dimenX, 1);
        b = new DMatrixRMaj(dimenX, dimenX);
        y = new DMatrixRMaj(dimenZ, 1);
        S = new DMatrixRMaj(dimenZ, dimenZ);
        S_inv = new DMatrixRMaj(dimenZ, dimenZ);
        c = new DMatrixRMaj(dimenZ, dimenX);
        d = new DMatrixRMaj(dimenX, dimenZ);
        K = new DMatrixRMaj(dimenX, dimenZ);

        x = new DMatrixRMaj(dimenX, 1);
        P = new DMatrixRMaj(dimenX, dimenX);

        // covariance matrices are symmetric positive semi-definite
        solver = LinearSolverFactory_DDRM.symmPosDef(dimenX);
    }

    public void setState( DMatrixRMaj x, DMatrixRMaj P ) {
        this.x.setTo(x);
        this.P.setTo(P);
    }

    public void predict() {
        // x = F x
        mult(F, x, a);
        x.setTo(a);

        // P = F P F' + Q
        mult(F, P, b);
        multTransB(b, F, P);
        addEquals(P, Q);
    }

   public void update( DMatrixRMaj z, DMatrixRMaj R ) {
        // y = z - H x
        mult(H, x, y);
        subtract(z, y, y);

        // S = H P H' + R
        mult(H, P, c);
        multTransB(c, H, S);
        addEquals(S, R);

        // K = PH'S^(-1)
        if (!solver.setA(S)) throw new RuntimeException("Invert failed");
        solver.invert(S_inv);
        multTransA(H, S_inv, d);
        mult(P, d, K);

        // x = x + Ky
        mult(K, y, a);
        addEquals(x, a);

        // P = (I-kH)P = P - (KH)P = P-K(HP)
        mult(H, P, c);
        mult(K, c, b);
        subtractEquals(P, b);
    }

     public DMatrixRMaj getState() {
         return x;
     }

   public DMatrixRMaj getCovariance() {
         return P;
     }
}