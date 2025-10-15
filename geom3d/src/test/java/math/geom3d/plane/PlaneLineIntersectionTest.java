/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package math.geom3d.plane;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import math.geom3d.Point3D;
import math.geom3d.line.LineSegment3D;
import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Plane;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author peter
 */
@RunWith(Parameterized.class)
public class PlaneLineIntersectionTest {

    private final Plane3D p;
    private final LineSegment3D l;

    public PlaneLineIntersectionTest(Plane3D p, LineSegment3D l) {
        this.p = p;
        this.l = l;
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters() {
        int n = 100;
        Random r = new Random();
        List<Object[]> out = new ArrayList<>(n);
//        for (int i = 0; i < n; i++) {
//            out.add(new Object[]{
//                Plane3D.fromNormal(new Vector3S(r.nextDouble(), r.nextDouble()).toCartesian(), r.nextDouble()),
//                new LineSegment3D(new Vector3S(r.nextDouble(), r.nextDouble()).toCartesian(), r.nextDouble())
//            });
//        }
        out.add(new Object[]{
            Plane3D.createXYPlane(),
            new LineSegment3D(new Point3D(1, 2, -1), new Point3D(2, 1.8373, 1))
        });
        return out;
    }

    @Test
    public void test() {
        Point3D pi = p.lineIntersection(l.supportingLine());
        Point3D pi2 = unconvert(convert(p).intersection(convert(l)));
        System.out.println(pi.distance(pi2));
    }

    @Test
    public void testDistances() {
//        p.signedDistance(l.firstPoint());
//        p.signedDistance(l.lastPoint());
    }

    public Point3D unconvert(org.apache.commons.math3.geometry.euclidean.threed.Vector3D v) {
        return v == null ? null : new Point3D(v.getX(), v.getY(), v.getZ());
    }

    public Line convert(LineSegment3D l3d) {
        return new Line(
                new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                        l3d.firstPoint().getX(),
                        l3d.firstPoint().getY(),
                        l3d.firstPoint().getZ()
                ),
                new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                        l3d.lastPoint().getX(),
                        l3d.lastPoint().getY(),
                        l3d.lastPoint().getZ()
                ),
                0.0
        );
    }

    public Plane convert(Plane3D p3d) {
        return new Plane(
                new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                        p3d.origin().getX(),
                        p3d.origin().getY(),
                        p3d.origin().getZ()
                ),
                new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                        p3d.normal().getX(),
                        p3d.normal().getY(),
                        p3d.normal().getZ()
                ),
                0.0
        );
    }
}
