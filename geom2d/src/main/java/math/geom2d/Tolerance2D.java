/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package math.geom2d;

import java.math.BigDecimal;
import math.geom2d.circulinear.CirculinearCurve2D;
import org.apache.commons.math3.util.FastMath;

/**
 *
 * @author peter
 */
public class Tolerance2D {

    private static Double ACCURACY = 1e-12;
    private static Double TENPOWSCALE = 1000000000000.0;
    private static final ThreadLocal<Double> THREAD_TENPOWSCALE = new ThreadLocal<Double>() {
        @Override
        protected Double initialValue() {
            return TENPOWSCALE;
        }
    };

    private static final ThreadLocal<Double> THREAD_ACCURACY = new ThreadLocal<Double>() {
        @Override
        protected Double initialValue() {
            return ACCURACY;
        }
    };

    public static void setGlobal(Double accuracy) {
        ACCURACY = accuracy;
        TENPOWSCALE = FastMath.pow(10, new BigDecimal(String.format("%.0e", accuracy)).scale());
        reset();
    }

    public static void set(Double accuracy) {
        THREAD_ACCURACY.set(accuracy);
        THREAD_TENPOWSCALE.set(FastMath.pow(10, new BigDecimal(String.format("%.0e", accuracy)).scale()));
    }

    public static Double get() {
        double result = THREAD_ACCURACY.get();
        return result;
    }

    public static Double getRelative(CirculinearCurve2D curve) {
        return getRelative(curve.length());
    }

    public static Double getRelative(double length) {
        return Math.max(1e-12, get() / length);
    }

    public static void reset() {
        THREAD_ACCURACY.remove();
        THREAD_TENPOWSCALE.remove();
    }

    public static double round(double d) {
        return castRound(d, THREAD_TENPOWSCALE.get());
    }

    static double castRound(double d, double tenpowscale) {
        return (long) (d * tenpowscale + 0.5) / tenpowscale;
    }

    public static int hash(double d) {
        return Double.valueOf(Double.isNaN(d) ? d : round(d)).hashCode();
    }

    public static int compare(double a, double b) {
        return Double.compare(round(a), round(b));
//        return compare(a, b, get());
    }

    public static int compare(double a, double b, double eps) {
        int res = Double.compare(a, b);
        return res == 0 ? 0 : (Math.abs(a - b) <= eps ? 0 : res);
    }
}
