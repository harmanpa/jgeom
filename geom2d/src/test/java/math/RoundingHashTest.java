/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import math.geom2d.Tolerance2D;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author peter
 */
@RunWith(Parameterized.class)
public class RoundingHashTest {

    private final double[] pair;

    public RoundingHashTest(double[] pair) {
        this.pair = pair;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        Collection<Object[]> out = new ArrayList<>();
        Arrays.asList(
                new double[]{119288.74119836985, 119288.74116397164},
                new double[]{102758.17348906874, 102758.17345084697},
                new double[]{83.19521814310731, 83.19526733421816},
                new double[]{119288.56255260987, 119288.56254365374},
                new double[]{102759.51979503884, 102759.51978452061},
                new double[]{83.41076978241333, 83.41078260476893}).forEach(pair -> out.add(new Object[]{pair}));
        return out;
    }

    @Test
    public void test() {
        Tolerance2D.set(1e-4);
        System.out.println(Tolerance2D.round(pair[0]));
        System.out.println(Tolerance2D.round(pair[1]));
        System.out.println(Tolerance2D.compare(pair[0], pair[1]));
        System.out.println(Double.compare(Tolerance2D.round(pair[0]), Tolerance2D.round(pair[1])));
    }

}
