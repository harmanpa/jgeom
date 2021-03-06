package tech.cae.binpacking;

import java.util.ArrayList;
import tech.cae.binpacking.exceptions.ConvexHullException;

//Hao Hua, Southeast University, whitegreen@163.com
public class Convex implements Cloneable {

    public ArrayList<double[]> convex; // convex is clockwise
    // TODO: Can't have this unit-specific:
    private static final double area_zero = 1E-9; // for m

    @Override
    public Convex clone() throws CloneNotSupportedException {
        super.clone();
        ArrayList<double[]> list = new ArrayList<>();
        list.addAll(convex);
        Convex con = new Convex();
        con.convex = list;
        return con;
    }

    public Convex() {
    }

    public Convex(double[][] points) throws ConvexHullException { // always ready for new points
        double[][] tri = {points[0], points[1], points[2]};
        double area = M.area(tri);
//        if (Math.abs(area) < area_zero) {
//            throw new ConvexHullException("Convex hull of zero area shape");
//        }
        convex = new ArrayList<>();
        if (area > 0) {
            convex.add(points[0]);
            convex.add(points[1]);
            convex.add(points[2]);
        } else {
            convex.add(points[1]);
            convex.add(points[0]);
            convex.add(points[2]);
        }
        for (int i = 3; i < points.length; i++) {
            increment_hull(points[i]);
        }
    }

    public final void increment_hull(double[] np) throws ConvexHullException { // convex is clockwise
        if (M.inside(np, convex)) {
            return;
        }
        int size = convex.size();
        boolean[] visible = new boolean[size];
        boolean hasTure = false;
        for (int i = 0; i < size; i++) {
            double[] q0 = convex.get(i);
            double[] q1 = convex.get((i + 1) % size);
            double[] d = M.sub(q1, q0);
            double[] a = M.sub(np, q0);
            visible[i] = -d[1] * a[0] + d[0] * a[1] > 0; // n={-d[1], d[0]} on the left
            if (visible[i]) {
                hasTure = true;
            }
        }
        if (!hasTure) {
            return;
        }

        int T1st = -1;
        for (int i = 0; i < size; i++) { // find 1st visible
            int j = (i + 1) % size;
            if (!visible[i] && visible[j]) {
                T1st = j;
                break;
            }
        }
        if (0 > T1st) {
//			System.out.println(np[0] + "*" + np[1]);
//			for (double[] p : convex) {
//				System.out.println( "new double[]{"+p[0] + "," + p[1]+"}");
//                        }
//			for (boolean b:visible) {
//				System.out.println(b);
//                        }
            throw new ConvexHullException("Internal error");
        }
        ArrayList<double[]> list = new ArrayList<>(size);
        list.add(convex.get(T1st));
        list.add(np);
        for (int i = 0; i < size; i++) {
            int j = (T1st + i + 1) % size;
            if (!visible[j]) {
                list.add(convex.get(j));
            }
        }
        convex = list;
    }

}
