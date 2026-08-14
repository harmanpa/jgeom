package math.geom3d.io;

import java.util.List;
import java.util.stream.Collectors;
import math.geom3d.Point3D;
import math.geom3d.csg.primitives.Cube;
import math.geom3d.csg.primitives.Sphere;
import math.geom3d.quickhull.QuickHullException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Tests for the winding number and the point-in-mesh test built on it.
 *
 * @author peter
 */
public class Triangle3DInsideTest {

    /**
     * A cube of side 2 centred on the origin, so spanning -1 to 1 on each axis.
     */
    private static List<Triangle3D> cube() throws QuickHullException {
        return Triangle3D.fromCSG(new Cube(2.0).toCSG());
    }

    /**
     * The same mesh wound the other way round, which a point-in-mesh test has
     * no business caring about.
     */
    private static List<Triangle3D> reversed(List<Triangle3D> triangles) {
        return triangles.stream()
                .map(t -> new Triangle3D(t.getVertices()[0], t.getVertices()[2], t.getVertices()[1]))
                .collect(Collectors.toList());
    }

    @Test
    public void pointsInsideACube() throws QuickHullException {
        List<Triangle3D> cube = cube();
        assertTrue(Triangle3D.isInside(cube, new Point3D(0, 0, 0)));
        assertTrue(Triangle3D.isInside(cube, new Point3D(0.9, 0.9, 0.9)));
        assertTrue(Triangle3D.isInside(cube, new Point3D(-0.5, 0.25, 0.75)));
    }

    @Test
    public void pointsOutsideACube() throws QuickHullException {
        List<Triangle3D> cube = cube();
        assertFalse(Triangle3D.isInside(cube, new Point3D(1.1, 0, 0)));
        assertFalse(Triangle3D.isInside(cube, new Point3D(5, 0, 0)));
        assertFalse(Triangle3D.isInside(cube, new Point3D(0, 0, -20)));
        // Outside but level with the box on two axes, the awkward direction
        assertFalse(Triangle3D.isInside(cube, new Point3D(3, 0.5, 0.5)));
    }

    @Test
    public void windingNumberIsTheSolidAngle() throws QuickHullException {
        List<Triangle3D> cube = cube();
        // The routine returns half the solid angle, so a closed surface gives
        // 2 pi seen from inside and nothing at all from outside
        assertEquals(2 * Math.PI, Math.abs(Triangle3D.windingNumber(cube, new Point3D(0, 0, 0))), 1e-9);
        assertEquals(2 * Math.PI, Math.abs(Triangle3D.windingNumber(cube, new Point3D(0.5, -0.25, 0.1))), 1e-9);
        assertEquals(0.0, Math.abs(Triangle3D.windingNumber(cube, new Point3D(4, 0, 0))), 1e-9);
        assertEquals(0.0, Math.abs(Triangle3D.windingNumber(cube, new Point3D(0, 0, 1.5))), 1e-9);
    }

    @Test
    public void windingDirectionDoesNotMatter() throws QuickHullException {
        List<Triangle3D> inverted = reversed(cube());
        assertTrue(Triangle3D.isInside(inverted, new Point3D(0, 0, 0)));
        assertFalse(Triangle3D.isInside(inverted, new Point3D(4, 0, 0)));
    }

    @Test
    public void pointsInsideASphere() throws QuickHullException {
        List<Triangle3D> sphere = Triangle3D.fromCSG(new Sphere(1.0).toCSG());
        assertTrue(Triangle3D.isInside(sphere, new Point3D(0, 0, 0)));
        assertTrue(Triangle3D.isInside(sphere, new Point3D(0, 0, 0.5)));
        assertFalse(Triangle3D.isInside(sphere, new Point3D(0, 0, 1.5)));
        assertFalse(Triangle3D.isInside(sphere, new Point3D(3, 3, 3)));
    }

    @Test
    public void containmentOfOneMeshInAnother() throws QuickHullException {
        // What the collider needs: is a vertex of the small mesh inside the
        // large one, when no surface of either crosses the other
        List<Triangle3D> large = Triangle3D.fromCSG(new Cube(10.0).toCSG());
        List<Triangle3D> small = cube();
        for (Triangle3D triangle : small) {
            for (Point3D vertex : triangle.getVertices()) {
                assertTrue("vertex " + vertex + " should be inside the large cube",
                        Triangle3D.isInside(large, vertex));
            }
        }
        for (Triangle3D triangle : large) {
            for (Point3D vertex : triangle.getVertices()) {
                assertFalse("vertex " + vertex + " should be outside the small cube",
                        Triangle3D.isInside(small, vertex));
            }
        }
    }
}
