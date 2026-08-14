package math.geom3d.io;

import math.geom3d.Point3D;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Tests for {@link Triangle3D#distance(math.geom3d.io.Triangle3D, double)} and
 * for the point to triangle distance it builds on.
 *
 * @author peter
 */
public class Triangle3DDistanceTest {

    private static final double EPS = 1e-9;

    private static Triangle3D triangle(double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3) {
        return new Triangle3D(new Point3D(x1, y1, z1), new Point3D(x2, y2, z2), new Point3D(x3, y3, z3));
    }

    /**
     * Asserts the distance in both directions, which must agree.
     */
    private static void assertDistance(double expected, Triangle3D a, Triangle3D b) {
        assertEquals(expected, a.distance(b), EPS);
        assertEquals("distance is not symmetric", expected, b.distance(a), EPS);
    }

    /**
     * The smallest of the six vertex to triangle distances, which is what the
     * distance used to be taken to be.
     */
    private static double vertexOnlyDistance(Triangle3D a, Triangle3D b) {
        double min = Double.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            min = Math.min(min, b.distance(a.getVertices()[i]));
            min = Math.min(min, a.distance(b.getVertices()[i]));
        }
        return min;
    }

    @Test
    public void crossingBarsAreEdgeToEdge() {
        // Two long thin triangles crossing at right angles 3 apart in Z. They
        // are placed so that no vertex of either lies over the face of the
        // other, which leaves the closest approach entirely to the edges: the
        // long edge of one passes directly over the long edge of the other.
        Triangle3D alongX = triangle(
                -10, 0, 0,
                10, 0, 0,
                -1, -8, 0);
        Triangle3D alongY = triangle(
                0, -10, 3,
                0, 10, 3,
                6, -9, 3);

        assertDistance(3.0, alongX, alongY);
        assertTrue("the fixture is pointless unless the vertex cases overstate it",
                vertexOnlyDistance(alongX, alongY) > 3.0 + EPS);
    }

    @Test
    public void parallelTrianglesAreVertexToFace() {
        Triangle3D lower = triangle(0, 0, 0, 4, 0, 0, 0, 4, 0);
        Triangle3D upper = triangle(0, 0, 2, 4, 0, 2, 0, 4, 2);
        assertDistance(2.0, lower, upper);
    }

    @Test
    public void coplanarTrianglesAreEdgeToEdge() {
        Triangle3D left = triangle(0, 0, 0, 4, 0, 0, 0, 4, 0);
        // Shifted along X so the nearest features are two parallel edges
        Triangle3D right = triangle(7, 0, 0, 11, 0, 0, 7, 4, 0);
        assertDistance(3.0, left, right);
    }

    @Test
    public void vertexToVertex() {
        Triangle3D a = triangle(0, 0, 0, 4, 0, 0, 0, 4, 0);
        Triangle3D b = triangle(9, 0, 0, 13, 0, 0, 13, 4, 0);
        // Nearest features are the vertex (4,0,0) and the vertex (9,0,0)
        assertDistance(5.0, a, b);
    }

    @Test
    public void intersectingTrianglesAreZeroApart() {
        Triangle3D base = triangle(0, 0, 0, 4, 0, 0, 0, 4, 0);
        assertDistance(0.0, base, triangle(1, 1, -1, 1, 1, 1, 2, 1, 0));
        assertDistance(0.0, base, triangle(1, 1, 0, 5, 1, 0, 1, 5, 0));
    }

    @Test
    public void touchingTrianglesAreZeroApart() {
        Triangle3D base = triangle(0, 0, 0, 4, 0, 0, 0, 4, 0);
        // Sharing an edge, and meeting at a single vertex
        assertDistance(0.0, base, triangle(4, 0, 0, 0, 4, 0, 4, 4, 0));
        assertDistance(0.0, base, triangle(4, 0, 0, 6, 0, 2, 6, 2, 2));
    }

    @Test
    public void edgeCrossingAFaceIsZeroApart() {
        // The case the feature minimum alone gets wrong: an edge passes through
        // the middle of the face, far from every edge and vertex of it
        Triangle3D wide = triangle(-10, -10, 0, 10, -10, 0, 0, 10, 0);
        Triangle3D through = triangle(0, 0, -1, 0, 0, 1, 1, 0, 0);
        assertDistance(0.0, wide, through);
    }

    @Test
    public void degenerateTriangleIsMeasuredAsASegment() {
        Triangle3D base = triangle(0, 0, 0, 4, 0, 0, 0, 4, 0);
        // Collinear vertices forming a segment held 2 above the face
        assertDistance(2.0, base, triangle(1, 1, 2, 3, 1, 2, 2, 1, 2));
    }

    @Test
    public void pointOverTheFace() {
        Triangle3D t = triangle(0, 0, 0, 10, 0, 0, 0, 10, 0);
        assertEquals(3.0, t.distance(new Point3D(2, 2, 3)), EPS);
        assertEquals(0.0, t.distance(new Point3D(2, 2, 0)), EPS);
    }

    @Test
    public void pointBesideAnEdge() {
        Triangle3D t = triangle(0, 0, 0, 10, 0, 0, 0, 10, 0);
        // Beside the middle of an edge, where the nearest vertex is much
        // further away than the edge itself
        assertEquals(1.0, t.distance(new Point3D(5, -1, 0)), EPS);
        assertTrue(t.getVertices()[0].distance(new Point3D(5, -1, 0)) > 5.0);
        // Out of plane as well as beyond the edge
        assertEquals(Math.sqrt(2.0), t.distance(new Point3D(5, -1, 1)), EPS);
    }

    @Test
    public void pointBeyondAVertex() {
        Triangle3D t = triangle(0, 0, 0, 10, 0, 0, 0, 10, 0);
        assertEquals(5.0, t.distance(new Point3D(15, 0, 0)), EPS);
        assertEquals(3.0, t.distance(new Point3D(0, 0, -3)), EPS);
    }
}
