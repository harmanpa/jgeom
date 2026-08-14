package math.geom3d.io;

import math.geom3d.Point3D;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Tests for {@link Triangle3D#intersects(math.geom3d.io.Triangle3D, double)}.
 *
 * @author peter
 */
public class Triangle3DIntersectsTest {

    private static final double TOL = 1e-9;

    /**
     * A right triangle in the z=0 plane, with the right angle at the origin and
     * legs of length 4 along X and Y. Most of the other triangles are placed
     * relative to this one.
     */
    private static final Triangle3D BASE = triangle(
            0, 0, 0,
            4, 0, 0,
            0, 4, 0);

    private static Triangle3D triangle(double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3) {
        return new Triangle3D(new Point3D(x1, y1, z1), new Point3D(x2, y2, z2), new Point3D(x3, y3, z3));
    }

    /**
     * Asserts that the pair intersects, and that the test is symmetric.
     */
    private static void assertIntersects(Triangle3D a, Triangle3D b) {
        assertTrue("expected " + a + " to intersect " + b, a.intersects(b, TOL));
        assertTrue("expected " + b + " to intersect " + a, b.intersects(a, TOL));
    }

    /**
     * Asserts that the pair does not intersect, and that the test is symmetric.
     */
    private static void assertSeparated(Triangle3D a, Triangle3D b) {
        assertFalse("expected " + a + " to be clear of " + b, a.intersects(b, TOL));
        assertFalse("expected " + b + " to be clear of " + a, b.intersects(a, TOL));
    }

    @Test
    public void crossingTrianglesIntersect() {
        // Vertical triangle in the y=1 plane, cutting straight through BASE
        assertIntersects(BASE, triangle(
                1, 1, -1,
                1, 1, 1,
                2, 1, 0));
    }

    @Test
    public void piercingVertexIntersects() {
        // A single vertex pokes below the z=0 plane, inside BASE
        assertIntersects(BASE, triangle(
                1, 1, -2,
                2, 1, 1,
                1, 2, 1));
    }

    @Test
    public void coplanarOverlappingTrianglesIntersect() {
        assertIntersects(BASE, triangle(
                1, 1, 0,
                5, 1, 0,
                1, 5, 0));
    }

    @Test
    public void coplanarContainedTriangleIntersects() {
        assertIntersects(BASE, triangle(
                1, 1, 0,
                2, 1, 0,
                1, 2, 0));
    }

    @Test
    public void identicalTrianglesIntersect() {
        assertIntersects(BASE, triangle(
                0, 0, 0,
                4, 0, 0,
                0, 4, 0));
    }

    @Test
    public void coplanarSeparatedTrianglesDoNotIntersect() {
        assertSeparated(BASE, triangle(
                10, 10, 0,
                12, 10, 0,
                10, 12, 0));
    }

    @Test
    public void coplanarEdgeTouchingIntersects() {
        // Shares the hypotenuse of BASE, and has the opposite winding
        assertIntersects(BASE, triangle(
                4, 0, 0,
                0, 4, 0,
                4, 4, 0));
    }

    @Test
    public void perpendicularEdgeTouchingIntersects() {
        // Stands upright on the X leg of BASE
        assertIntersects(BASE, triangle(
                0, 0, 0,
                4, 0, 0,
                0, 0, 3));
    }

    @Test
    public void edgeRestingOnFaceIntersects() {
        // Upright triangle whose lower edge lies across the middle of BASE
        assertIntersects(BASE, triangle(
                1, 1, 0,
                2, 1, 0,
                1.5, 1, 2));
    }

    @Test
    public void vertexTouchingIntersects() {
        // Meets BASE only at the vertex (4,0,0), and only at that single point
        assertIntersects(BASE, triangle(
                4, 0, 0,
                6, 0, 2,
                6, 2, 2));
    }

    @Test
    public void vertexTouchingFaceIntersects() {
        // A vertex rests on the interior of BASE, the rest is clear of it
        assertIntersects(BASE, triangle(
                1, 1, 0,
                3, 1, 2,
                1, 3, 2));
    }

    @Test
    public void parallelPlanesDoNotIntersect() {
        assertSeparated(BASE, triangle(
                0, 0, 1,
                4, 0, 1,
                0, 4, 1));
    }

    @Test
    public void crossingPlanesWithDisjointIntervalsDoNotIntersect() {
        // Shares the y=1 plane with the crossing case above, but sits beyond
        // the far edge of BASE, so the two crossed intervals do not overlap
        assertSeparated(BASE, triangle(
                5, 1, -1,
                5, 1, 1,
                7, 1, 0));
    }

    @Test
    public void nearMissDoesNotIntersect() {
        // Vertical triangle stopping just short of the z=0 plane
        assertSeparated(BASE, triangle(
                1, 1, 1e-3,
                2, 1, 1e-3,
                1.5, 1, 2));
    }

    @Test
    public void gapWithinToleranceIntersects() {
        Triangle3D lifted = triangle(
                0, 0, 1e-6,
                4, 0, 1e-6,
                0, 4, 1e-6);
        assertFalse(BASE.intersects(lifted, 1e-9));
        assertTrue(BASE.intersects(lifted, 1e-3));
    }

    @Test
    public void negativeToleranceIsTreatedAsZero() {
        Triangle3D touching = triangle(
                4, 0, 0,
                0, 4, 0,
                4, 4, 0);
        assertTrue(BASE.intersects(touching, -1));
        assertFalse(BASE.intersects(triangle(
                0, 0, 1,
                4, 0, 1,
                0, 4, 1), -1));
    }

    @Test
    public void degenerateTriangleInPlaneIntersects() {
        // Collinear vertices lying across the middle of BASE
        assertIntersects(BASE, triangle(
                1, 1, 0,
                3, 1, 0,
                2, 1, 0));
    }

    @Test
    public void degenerateTriangleCrossingPlaneIntersects() {
        // Collinear vertices spanning the z=0 plane over the middle of BASE
        assertIntersects(BASE, triangle(
                2, 1, -1,
                2, 1, 1,
                2, 1, 0));
    }

    @Test
    public void degenerateTriangleClearOfPlaneDoesNotIntersect() {
        assertSeparated(BASE, triangle(
                10, 10, 0,
                12, 10, 0,
                11, 10, 0));
    }

    @Test
    public void twoDegenerateTrianglesCrossing() {
        Triangle3D alongX = triangle(0, 0, 0, 4, 0, 0, 2, 0, 0);
        assertIntersects(alongX, triangle(2, -2, 0, 2, 2, 0, 2, 0, 0));
        assertSeparated(alongX, triangle(2, 1, 0, 2, 2, 0, 2, 1.5, 0));
    }

    @Test
    public void overlappingCubeFacesIntersect() {
        // The kind of pair a mesh collider sees: a face of one box and a face
        // of another box that has been pushed into it
        Triangle3D face = triangle(
                0, 0, 0,
                2, 0, 0,
                2, 2, 0);
        Triangle3D intruder = triangle(
                1, 1, -1,
                1, 1, 1,
                1.5, 0.5, 0);
        assertIntersects(face, intruder);
    }
}
