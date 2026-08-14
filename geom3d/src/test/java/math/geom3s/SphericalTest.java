/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package math.geom3s;

import java.util.Random;
import math.geom3d.Vector3D;
import math.geom3d.transform.AffineTransform3D;
import org.junit.Test;
import org.junit.Assert;

/**
 *
 * @author peter
 */
public class SphericalTest {

    @Test
    public void test() {
        Random r = new Random();
        for (int i = 0; i < 100; i++) {
            Vector3D v = new Vector3D(r.nextDouble(), r.nextDouble(), r.nextDouble());
            Assert.assertTrue(Vector3S.fromCartesian(v).toCartesian().minus(v).norm() < 1e-12);
        }
    }

    @Test
    public void testNorm() {
        Random r = new Random();
        for (int i = 0; i < 100; i++) {
            Vector3S v = new Vector3S(r.nextDouble(), r.nextDouble());
            Assert.assertTrue(Math.abs(v.toCartesian().norm() - 1.0) < 1e-12);
        }
    }

    /**
     * The transform has to carry v1 onto v2 and its inverse carry v2 back.
     * <p>
     * Compared as directions rather than as spherical components: theta and phi
     * are singular at the poles, where a vector a whisker away from the Z axis
     * has an all but arbitrary phi, and comparing them component by component
     * fails on a pair that is in fact the same direction. The tolerance is
     * loosened to match, since composing four rotations through that
     * singularity costs about 1e-11 - the seed is fixed so that a genuine
     * failure can be reproduced.
     */
    @Test
    public void testTransform() {
        Random r = new Random(20260814L);
        for (int i = 0; i < 1000; i++) {
            Vector3S v1 = new Vector3S(r.nextDouble(), r.nextDouble());
            Vector3S v2 = new Vector3S(r.nextDouble(), r.nextDouble());
            AffineTransform3D t = v1.transformTo(v2);
            Vector3S v3 = v1.transform(t);
            Vector3S v4 = v2.transform(t.inverse());
            Assert.assertEquals("case " + i + " did not reach " + v2,
                    0.0, v3.toCartesian().minus(v2.toCartesian()).norm(), 1e-9);
            Assert.assertEquals("case " + i + " did not return to " + v1,
                    0.0, v4.toCartesian().minus(v1.toCartesian()).norm(), 1e-9);
        }
    }
}
