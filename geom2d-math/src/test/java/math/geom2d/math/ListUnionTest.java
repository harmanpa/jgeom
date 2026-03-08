/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package math.geom2d.math;

import java.util.ArrayList;
import java.util.List;
import math.geom2d.circulinear.CirculinearCurve2D;
import math.geom2d.conic.Circle2D;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author peter
 */
public class ListUnionTest {

//    @Test
//    public void test1() {
//        List<CirculinearCurve2D> curves = new ArrayList<>();
//        double diameter = 200;
//        for (int i = 0; i < 3; i++) {
//            curves.add(new Circle2D(2 * i * (diameter * 0.9), 0, diameter / 2));
//        }
//        for (int i = 0; i < 3; i++) {
//            curves.add(new Circle2D((1 + 2 * i) * (diameter * 0.9), 0, diameter / 2));
//        }
//        Assert.assertTrue("Should be 1 ring", Rings2D.union(curves, 1e-6).size() == 1);
//    }

    @Test
    public void test2() {
        List<CirculinearCurve2D> curves = new ArrayList<>();
        double diameter = 200;
        for (int i = 0; i < 3; i++) {
            curves.add(new Circle2D(2 * i * (diameter * 1.1), 0, diameter / 2));
        }
        for (int i = 0; i < 3; i++) {
            curves.add(new Circle2D((1 + 2 * i) * (diameter * 1.1), 0, diameter / 2));
        }
        Assert.assertTrue("Should be 6 rings", Rings2D.union(curves, 1e-6).size() == 6);
    }
}
