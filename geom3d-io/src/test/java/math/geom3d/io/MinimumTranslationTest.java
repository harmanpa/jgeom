package math.geom3d.io;

import java.util.List;
import java.util.stream.Collectors;
import math.geom3d.Vector3D;
import math.geom3d.csg.primitives.Cube;
import math.geom3d.quickhull.QuickHullException;
import math.geom3d.transform.AffineTransform3D;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Tests for {@link MinimumTranslation}.
 *
 * @author peter
 */
public class MinimumTranslationTest {

    private static final double EPS = 1e-9;

    /**
     * A cube of side 2 centred on the origin, so spanning -1 to 1 on each axis.
     */
    private static List<Triangle3D> cube() throws QuickHullException {
        return Triangle3D.fromCSG(new Cube(2.0).toCSG());
    }

    private static List<Triangle3D> cube(double w, double h, double d) throws QuickHullException {
        return Triangle3D.fromCSG(new Cube(w, h, d).toCSG());
    }

    private static List<Triangle3D> moved(List<Triangle3D> triangles, AffineTransform3D transform) {
        return triangles.stream().map(t -> t.transform(transform)).collect(Collectors.toList());
    }

    private static List<Triangle3D> moved(List<Triangle3D> triangles, double x, double y, double z) {
        return moved(triangles, AffineTransform3D.createTranslation(x, y, z));
    }

    @Test
    public void overlapAlongOneAxisIsTheOverlap() throws QuickHullException {
        // Two cubes of side 2 offset by 1.5 along X overlap by 0.5
        MinimumTranslation mt = MinimumTranslation.between(cube(), moved(cube(), 1.5, 0, 0));
        assertTrue(mt.isOverlapping());
        assertEquals(0.5, mt.getDepth(), EPS);
        // Separating means pushing the second one further along +X
        assertEquals(1.0, Math.abs(mt.getDirection().getX()), EPS);
        assertTrue(mt.getDirection().getX() > 0);
    }

    @Test
    public void depthIsTheShallowestAxis() throws QuickHullException {
        // Overlaps of 0.7, 1.3 and 1.5 on the three axes; the shortest way out
        // is the 0.7 along X
        MinimumTranslation mt = MinimumTranslation.between(cube(), moved(cube(), 1.3, 0.7, 0.5));
        assertEquals(0.7, mt.getDepth(), EPS);
        assertTrue(mt.getDirection().getX() > 0);
    }

    @Test
    public void depthGrowsAsTheyArePushedTogether() throws QuickHullException {
        List<Triangle3D> fixed = cube();
        double previous = -1.0;
        for (double offset = 2.0; offset >= 0.0; offset -= 0.1) {
            double depth = MinimumTranslation.between(fixed, moved(cube(), offset, 0, 0)).getDepth();
            assertEquals("offset " + offset, Math.max(0.0, 2.0 - offset), depth, 1e-9);
            assertTrue("depth went backwards at offset " + offset, depth >= previous - EPS);
            previous = depth;
        }
    }

    @Test
    public void touchingIsZeroDepth() throws QuickHullException {
        // Face to face with no overlap at all
        MinimumTranslation mt = MinimumTranslation.between(cube(), moved(cube(), 2.0, 0, 0));
        assertFalse(mt.isOverlapping());
        assertEquals(0.0, mt.getDepth(), EPS);
    }

    @Test
    public void separatedIsZeroDepth() throws QuickHullException {
        assertEquals(0.0, MinimumTranslation.between(cube(), moved(cube(), 5, 0, 0)).getDepth(), EPS);
        assertEquals(0.0, MinimumTranslation.between(cube(), moved(cube(), 0, 0, 2.001)).getDepth(), EPS);
    }

    @Test
    public void identicalSetsOverlapByTheirFullExtent() throws QuickHullException {
        assertEquals(2.0, MinimumTranslation.between(cube(), cube()).getDepth(), EPS);
    }

    @Test
    public void rotatedCubeUsesItsOwnFaceNormals() throws QuickHullException {
        // Turned 45 degrees about Z the cube reaches out to sqrt(2) in X, so at
        // a centre separation of 2.2 its corner is 1 - (2.2 - sqrt(2)) inside
        AffineTransform3D turned = AffineTransform3D.createRotationOz(Math.PI / 4)
                .preConcatenate(AffineTransform3D.createTranslation(2.2, 0, 0));
        MinimumTranslation mt = MinimumTranslation.between(cube(), moved(cube(), turned));
        assertEquals(1.0 - (2.2 - Math.sqrt(2.0)), mt.getDepth(), EPS);
    }

    @Test
    public void barThroughACubeMustClearIt() throws QuickHullException {
        // A bar 1 deep in Y and Z driven through a cube of side 2. The bar's
        // extent sits wholly inside the cube's on those axes, so it has to
        // cross the whole cube to get out: 1.5, not the 1 of the overlap
        MinimumTranslation mt = MinimumTranslation.between(cube(), cube(20.0, 1.0, 1.0));
        assertEquals(1.5, mt.getDepth(), EPS);
        assertEquals(0.0, mt.getDirection().getX(), EPS);
        assertSeparatedByItsOwnAnswer(cube(), cube(20.0, 1.0, 1.0));
    }

    @Test
    public void containedSetHasTheDepthToGetOut() throws QuickHullException {
        // A cube of side 2 at the centre of one of side 10 has to travel 6 to
        // clear it - the half width of each, added. Nothing about this is
        // visible to a test that only looks for crossing surfaces.
        MinimumTranslation mt = MinimumTranslation.between(cube(10.0, 10.0, 10.0), cube());
        assertTrue(mt.isOverlapping());
        assertEquals(6.0, mt.getDepth(), EPS);
        assertSeparatedByItsOwnAnswer(cube(10.0, 10.0, 10.0), cube());
    }

    @Test
    public void containedOffCentreLeavesByTheNearestFace() throws QuickHullException {
        // Pushed towards +X inside the big cube, so the short way out is +X
        MinimumTranslation mt = MinimumTranslation.between(
                cube(10.0, 10.0, 10.0), moved(cube(), 3, 0, 0));
        assertEquals(3.0, mt.getDepth(), EPS);
        assertTrue(mt.getDirection().getX() > 0);
    }

    /**
     * Applying the answer has to actually part them, which is the property that
     * the length of the overlap between two extents does not have.
     */
    private static void assertSeparatedByItsOwnAnswer(List<Triangle3D> a, List<Triangle3D> b) {
        MinimumTranslation mt = MinimumTranslation.between(a, b);
        Vector3D push = mt.getDirection().times(mt.getDepth());
        List<Triangle3D> pushed = moved(b, push.getX(), push.getY(), push.getZ());
        assertEquals(0.0, MinimumTranslation.between(a, pushed).getDepth(), 1e-9);
    }

    @Test
    public void directionSeparatesWhenApplied() throws QuickHullException {
        assertSeparatedByItsOwnAnswer(cube(), moved(cube(), 1.3, 0.7, 0.5));
        assertSeparatedByItsOwnAnswer(cube(), moved(cube(), 0.2, 0, 0));
        assertSeparatedByItsOwnAnswer(cube(10.0, 10.0, 10.0), moved(cube(), 1, 2, -3));
    }

    @Test
    public void emptySetsDoNotOverlap() throws QuickHullException {
        assertFalse(MinimumTranslation.between(List.of(), cube()).isOverlapping());
        assertFalse(MinimumTranslation.between(cube(), List.of()).isOverlapping());
    }
}
