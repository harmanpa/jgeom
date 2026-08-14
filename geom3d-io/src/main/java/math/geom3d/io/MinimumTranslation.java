package math.geom3d.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import math.geom3d.Point3D;
import math.geom3d.Vector3D;

/**
 * The shortest translation that pulls two sets of triangles apart, found by the
 * separating axis method: for a given direction the two sets overlap by the
 * overlap of their extents along it, and translating one by that much along
 * that direction separates them. The shortest such translation over all
 * directions is the penetration depth.
 * <p>
 * Extents along a direction see only the convex hull of a set of triangles, so
 * this is the depth of the hulls. For two hollow or re-entrant parts that
 * interlock without touching it reports a depth where there is no contact at
 * all, and where they do touch it can overstate how far they must move. Use it
 * to quantify a collision that has already been established - by a triangle
 * level test - rather than to decide whether there is one.
 *
 * @author peter
 */
public final class MinimumTranslation {

    /**
     * Directions are gathered from the face normals of both sets and from the
     * cross products of their edge directions, which is the set the separating
     * axis method needs to be exact for convex bodies. The edge pairs are the
     * part that grows quadratically, so beyond this many they are dropped and
     * the face normals alone are used - a smaller set of directions can only
     * overstate the depth, never understate it.
     */
    private static final int MAX_EDGE_PAIRS = 4096;

    /**
     * Grid that directions are rounded onto to be deduplicated. Coarse enough
     * to collapse the many triangles sharing a face of a mesh onto one
     * direction; a duplicate slipping through only costs a little time.
     */
    private static final double DEDUPLICATION_GRID = 1e-9;

    private static final double DEGENERATE = 1e-12;

    private final double depth;
    private final Vector3D direction;

    private MinimumTranslation(double depth, Vector3D direction) {
        this.depth = depth;
        this.direction = direction;
    }

    /**
     * The shortest translation separating the two sets of triangles.
     *
     * @param a A set of triangles
     * @param b A set of triangles
     * @return The translation, whose depth is zero if a direction was found
     * along which the two do not overlap at all
     */
    public static MinimumTranslation between(List<Triangle3D> a, List<Triangle3D> b) {
        Point3D[] pointsA = points(a);
        Point3D[] pointsB = points(b);
        if (pointsA.length == 0 || pointsB.length == 0) {
            return new MinimumTranslation(0.0, new Vector3D(1, 0, 0));
        }
        double leastOverlap = Double.MAX_VALUE;
        Vector3D leastDirection = new Vector3D(1, 0, 0);
        for (Vector3D direction : directions(a, b)) {
            double[] rangeA = range(pointsA, direction);
            double[] rangeB = range(pointsB, direction);
            double overlap = Math.min(rangeA[1], rangeB[1]) - Math.max(rangeA[0], rangeB[0]);
            if (overlap <= 0) {
                return new MinimumTranslation(0.0, direction);
            }
            if (overlap < leastOverlap) {
                leastOverlap = overlap;
                // Point away from a, so that translating b by the depth along
                // it is what separates them
                leastDirection = rangeA[0] + rangeA[1] <= rangeB[0] + rangeB[1]
                        ? direction : direction.opposite();
            }
        }
        return leastOverlap == Double.MAX_VALUE
                ? new MinimumTranslation(0.0, leastDirection)
                : new MinimumTranslation(leastOverlap, leastDirection);
    }

    /**
     * How far the two sets have to move to come apart, zero if they do not
     * overlap.
     *
     * @return
     */
    public double getDepth() {
        return depth;
    }

    /**
     * The unit direction to translate the second set along to separate them.
     *
     * @return
     */
    public Vector3D getDirection() {
        return direction;
    }

    /**
     * Whether the two sets overlap along every direction tried.
     *
     * @return
     */
    public boolean isOverlapping() {
        return depth > 0;
    }

    /**
     * The extent of the points along a unit direction, as a minimum and a
     * maximum.
     */
    private static double[] range(Point3D[] points, Vector3D direction) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Point3D p : points) {
            double d = direction.getX() * p.getX()
                    + direction.getY() * p.getY()
                    + direction.getZ() * p.getZ();
            min = Math.min(min, d);
            max = Math.max(max, d);
        }
        return new double[]{min, max};
    }

    private static Point3D[] points(List<Triangle3D> triangles) {
        Point3D[] points = new Point3D[triangles.size() * 3];
        int i = 0;
        for (Triangle3D triangle : triangles) {
            for (Point3D vertex : triangle.getVertices()) {
                points[i++] = vertex;
            }
        }
        return points;
    }

    /**
     * The directions worth testing: the face normals of both sets, plus the
     * cross products of their edge directions where there are few enough of
     * them to afford. Duplicates are dropped, which for a mesh of any size is
     * most of them.
     */
    private static List<Vector3D> directions(List<Triangle3D> a, List<Triangle3D> b) {
        Set<List<Long>> seen = new HashSet<>();
        List<Vector3D> directions = new ArrayList<>();
        for (List<Triangle3D> triangles : Arrays.asList(a, b)) {
            for (Triangle3D triangle : triangles) {
                add(Triangle3D.planeNormal(triangle.getVertices(), 0.0), seen, directions);
            }
        }
        List<Vector3D> edgesA = edgeDirections(a);
        List<Vector3D> edgesB = edgeDirections(b);
        if (edgesA.size() * edgesB.size() <= MAX_EDGE_PAIRS) {
            for (Vector3D edgeA : edgesA) {
                for (Vector3D edgeB : edgesB) {
                    add(Vector3D.crossProduct(edgeA, edgeB), seen, directions);
                }
            }
        }
        return directions;
    }

    private static List<Vector3D> edgeDirections(List<Triangle3D> triangles) {
        Set<List<Long>> seen = new HashSet<>();
        List<Vector3D> directions = new ArrayList<>();
        for (Triangle3D triangle : triangles) {
            Point3D[] v = triangle.getVertices();
            for (int i = 0; i < 3; i++) {
                add(new Vector3D(v[i], v[(i + 1) % 3]), seen, directions);
            }
        }
        return directions;
    }

    /**
     * Normalises the vector and adds it if no direction like it is there
     * already. Opposite directions are the same axis here, so they collapse
     * onto one another.
     */
    private static void add(Vector3D v, Set<List<Long>> seen, List<Vector3D> directions) {
        if (v == null) {
            return;
        }
        double norm = v.norm();
        if (norm <= DEGENERATE) {
            return;
        }
        Vector3D unit = v.times(1.0 / norm);
        if (unit.getX() < -DEGENERATE
                || (Math.abs(unit.getX()) <= DEGENERATE && unit.getY() < -DEGENERATE)
                || (Math.abs(unit.getX()) <= DEGENERATE && Math.abs(unit.getY()) <= DEGENERATE && unit.getZ() < 0)) {
            unit = unit.opposite();
        }
        if (seen.add(Arrays.asList(
                Math.round(unit.getX() / DEDUPLICATION_GRID),
                Math.round(unit.getY() / DEDUPLICATION_GRID),
                Math.round(unit.getZ() / DEDUPLICATION_GRID)))) {
            directions.add(unit);
        }
    }

    @Override
    public String toString() {
        return "MinimumTranslation{depth=" + depth + ", direction=" + direction + '}';
    }
}
