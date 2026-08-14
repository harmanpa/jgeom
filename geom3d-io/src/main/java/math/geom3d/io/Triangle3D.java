/*
The MIT License (MIT)

Copyright (c) 2014 CCHall (aka Cyanobacterium aka cyanobacteruim)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package math.geom3d.io;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import math.geom2d.Tolerance2D;
import math.geom2d.Point2D;
import math.geom2d.polygon.Polygons2D;
import math.geom2d.polygon.SimplePolygon2D;
import math.geom3d.Point3D;
import math.geom3d.GeometricObject3D;
import math.geom3d.Shape3D;
import math.geom3d.Box3D;
import math.geom3d.transform.AffineTransform3D;
import math.geom3d.Vector3D;
import math.geom3d.csg.CSG;
import math.geom3d.csg.Polygon;
import math.geom3d.line.StraightLine3D;
import math.geom3d.plane.Plane3D;
import math.geom3d.quickhull.QuickHull3D;
import math.geom3d.quickhull.QuickHullException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.util.FastMath;

/**
 * This object represents a triangle in 3D space.
 *
 * @author CCHall
 */
public class Triangle3D implements Shape3D {

    private final Point3D[] vertices;
    private final Vector3D normal;

    Triangle3D(List<Point3D> points, Vector3D normal) {
        this(points.get(0), points.get(1), points.get(2), normal);
    }

    /**
     * Creates a triangle with the given vertices at its corners. The normal is
     * calculated by assuming that the vertices were provided in right-handed
     * coordinate space (counter-clockwise)
     *
     * @param v1 A corner vertex
     * @param v2 A corner vertex
     * @param v3 A corner vertex
     */
    public Triangle3D(Point3D v1, Point3D v2, Point3D v3) {
        this.vertices = new Point3D[3];
        this.vertices[0] = v1;
        this.vertices[1] = v2;
        this.vertices[2] = v3;
        Vector3D edge1 = new Vector3D(v1, v2);
        Vector3D edge2 = new Vector3D(v1, v3);
        this.normal = Vector3D.crossProduct(edge1, edge2).normalize();
    }

    /**
     * Creates a triangle with the given vertices at its corners and a given
     * normal.
     *
     * @param v1 A corner vertex
     * @param v2 A corner vertex
     * @param v3 A corner vertex
     * @param normal The normal
     */
    public Triangle3D(Point3D v1, Point3D v2, Point3D v3, Vector3D normal) {
        this.vertices = new Point3D[3];
        this.vertices[0] = v1;
        this.vertices[1] = v2;
        this.vertices[2] = v3;
        this.normal = normal;
    }

    public static List<Triangle3D> hull(List<Triangle3D> tris) throws QuickHullException {
        QuickHull3D hull = new QuickHull3D();
        Point3D[] hullPoints = tris.stream().flatMap(tri -> Stream.of(tri.getVertices())).toArray(Point3D[]::new);
        hull.build(hullPoints);
        hull.triangulate();
        int[][] faces = hull.getFaces();
        return Stream.of(faces)
                .map(verts -> IntStream.of(verts).mapToObj(vert -> hullPoints[hull.getVertexPointIndices()[vert]]).toArray(Point3D[]::new))
                .flatMap(points -> Polygon.fromPoints(points).toTriangles().stream())
                .map(triangularPolygon -> new Triangle3D(triangularPolygon.getPoints(), triangularPolygon.getNormal()))
                .collect(Collectors.toList());
    }

    public static List<Triangle3D> fromCSG(CSG csg) {
        return csg.getPolygons().stream()
                .flatMap(polygon -> polygon.toTriangles().stream())
                .map(triangularPolygon -> new Triangle3D(triangularPolygon.getPoints(), triangularPolygon.getNormal()))
                .collect(Collectors.toList());
    }

    public static CSG toCSG(List<Triangle3D> triangles) {
        return CSG.fromPolygons(triangles.stream()
                .map(tri -> Polygon.fromPoints(tri.getVertices()))
                .collect(Collectors.toList()));
    }

    public static double windingNumber(List<Triangle3D> triangles, Point3D p) {
        return triangles.stream().parallel().mapToDouble(tri -> tri.windingNumber(p)).sum();
    }

    /**
     * Whether the point is enclosed by the triangles, which are taken to form a
     * closed surface.
     * <p>
     * The winding number is half the solid angle the surface subtends, so it is
     * 2 pi seen from within and zero from without - or minus 2 pi if the
     * surface is wound the other way, which is why the magnitude is what
     * counts. The two are so far apart that anything past halfway settles it.
     *
     * @param triangles A closed surface
     * @param p The point to test
     * @return True if the point is enclosed
     */
    public static boolean isInside(List<Triangle3D> triangles, Point3D p) {
        return Math.abs(windingNumber(triangles, p)) > Math.PI;
    }

    public static double distance(List<Triangle3D> triangles, Point3D p) {
        return triangles.stream().parallel().mapToDouble(tri -> tri.distance(p)).min().orElse(Double.MAX_VALUE);
    }

    public static double signedDistance(List<Triangle3D> triangles, Point3D p) {
        return (isInside(triangles, p) ? -1.0 : 1.0) * distance(triangles, p);
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isEmpty() {
        return vertices[0].distance(vertices[1]) < Tolerance2D.get()
                || vertices[1].distance(vertices[2]) < Tolerance2D.get()
                || vertices[2].distance(vertices[0]) < Tolerance2D.get();
    }

    @Override
    public boolean isBounded() {
        return true;
    }

    @Override
    public Box3D boundingBox() {
        return Box3D.fromPoints(this.vertices);
    }

    @Override
    public Triangle3D transform(AffineTransform3D trans) {
        return new Triangle3D(vertices[0].transform(trans), vertices[1].transform(trans), vertices[2].transform(trans));
    }

    /**
     * The minimum distance from the point to this triangle: the perpendicular
     * distance where the point lies over the face, and the distance to the
     * nearest edge otherwise. Degenerate triangles fall back to their edges.
     *
     * @param p
     * @return
     */
    @Override
    public double distance(Point3D p) {
        Vector3D planeNormal = planeNormal(vertices, 0.0);
        if (planeNormal != null && containsInPlane(vertices, planeNormal, p, 0.0)) {
            return Math.abs(planeNormal.dot(new Vector3D(vertices[0], p)));
        }
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            // A zero length segment through p gives the point to edge distance
            minDistance = Math.min(minDistance, segmentDistance(p, p, vertices[i], vertices[(i + 1) % 3]));
        }
        return minDistance;
    }

    /**
     * The minimum distance between this triangle and another, using the default
     * tolerance.
     *
     * @param other The triangle to measure to
     * @return The distance, or zero if the two triangles intersect
     * @see #distance(math.geom3d.io.Triangle3D, double)
     */
    public double distance(Triangle3D other) {
        return distance(other, Tolerance2D.get());
    }

    /**
     * The minimum distance between this triangle and another, which is zero if
     * they intersect.
     * <p>
     * For two triangles that miss each other the closest approach is between an
     * edge of one and an edge of the other, or between a vertex of one and the
     * face of the other. Taking only the vertex cases - as a point to triangle
     * distance in each direction does - overstates the distance whenever the
     * closest approach is edge to edge, which is the usual case for crossing
     * bars and for any pair of faces meeting at an angle.
     *
     * @param other The triangle to measure to
     * @param tolerance Distance within which the triangles count as touching,
     * and so at zero distance
     * @return The distance, or zero if the two triangles intersect
     */
    public double distance(Triangle3D other, double tolerance) {
        // The feature minimum below is only the true distance for triangles
        // that miss each other: an edge crossing the far side of a face is
        // nearer to it than any of these pairs
        if (intersects(other, tolerance)) {
            return 0.0;
        }
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                minDistance = Math.min(minDistance, segmentDistance(
                        this.vertices[i], this.vertices[(i + 1) % 3],
                        other.vertices[j], other.vertices[(j + 1) % 3]));
            }
        }
        for (int i = 0; i < 3; i++) {
            minDistance = Math.min(minDistance, other.distance(this.vertices[i]));
            minDistance = Math.min(minDistance, this.distance(other.vertices[i]));
        }
        return minDistance;
    }

    public Point3D intersection(StraightLine3D ray) {
        Plane3D plane = getPlane();
        Point3D p = plane.lineIntersection(ray);
        Point2D p2 = plane.pointPosition(plane.projectPoint(p));
        if (triangleContains(plane, p2)) {
            return p;
        }
        return null;
    }

    boolean triangleContains(Plane3D plane, Point2D point2) {
        List<Point2D> ps = Arrays.asList(
                plane.pointPosition(plane.projectPoint(this.vertices[0])),
                plane.pointPosition(plane.projectPoint(this.vertices[1])),
                plane.pointPosition(plane.projectPoint(this.vertices[2])));
        return ps.contains(point2) || Polygons2D.rayTestInside(new SimplePolygon2D(ps), point2);
    }

    public double area() {
        Plane3D plane = getPlane();
        return Math.abs(new SimplePolygon2D(
                plane.pointPosition(plane.projectPoint(this.vertices[0])),
                plane.pointPosition(plane.projectPoint(this.vertices[1])),
                plane.pointPosition(plane.projectPoint(this.vertices[2]))).area());
    }

    public Plane3D getPlane() {
        return Plane3D.fromNormal(this.vertices[0], this.normal);
    }

    @Override
    public boolean contains(Point3D point) {
        return distance(point) < Tolerance2D.get();
    }

    /**
     * Based on
     * https://github.com/marmakoide/inside-3d-mesh/blob/master/is_inside_mesh.py
     *
     * @param point
     * @return
     */
    public double windingNumber(Point3D point) {
        Point3D pa = vertices[0].minus(point);
        Point3D pb = vertices[1].minus(point);
        Point3D pc = vertices[2].minus(point);
        double det = new LUDecomposition(new Array2DRowRealMatrix(new double[][]{
            new double[]{pa.getX(), pa.getY(), pa.getZ()},
            new double[]{pb.getX(), pb.getY(), pb.getZ()},
            new double[]{pc.getX(), pc.getY(), pc.getZ()}
        })).getDeterminant();
        double a = vertices[0].distance(point);
        double b = vertices[1].distance(point);
        double c = vertices[2].distance(point);
        double dab = pa.asVector().dot(pb.asVector());
        double dbc = pb.asVector().dot(pc.asVector());
        double dca = pc.asVector().dot(pa.asVector());
        return FastMath.atan2(det, (a * b * c) + c * dab + a * dbc + b * dca);
    }

    /**
     * Tests whether this triangle overlaps another one, using the default
     * tolerance.
     *
     * @param other The triangle to test against
     * @return True if the two triangles share at least one point
     * @see #intersects(math.geom3d.io.Triangle3D, double)
     */
    public boolean intersects(Triangle3D other) {
        return intersects(other, Tolerance2D.get());
    }

    /**
     * Tests whether this triangle overlaps another one, using the Möller
     * interval-overlap test with the coplanar case handled separately.
     * <p>
     * Triangles are treated as closed sets, so contacts along an edge or at a
     * single vertex count as an intersection. The tolerance is a fuzz factor
     * for those touching contacts and for classifying vertices as lying on a
     * plane: triangles that genuinely overlap always report true, and triangles
     * separated by more than the tolerance always report false, but a pair
     * separated by less than the tolerance may report either. This is not a
     * proximity query - use it to decide whether two triangles clash, not how
     * far apart they are.
     * <p>
     * Triangles whose height over their longest edge is within the tolerance
     * are degenerate (a sliver, segment or point) and are tested as segments.
     *
     * @param other The triangle to test against
     * @param tolerance Distance within which touching triangles are considered
     * to intersect; negative values are treated as zero
     * @return True if the two triangles share at least one point
     */
    public boolean intersects(Triangle3D other, double tolerance) {
        double tol = Math.max(tolerance, 0.0);
        Point3D[] a = this.vertices;
        Point3D[] b = other.vertices;
        // The stored normal may have been supplied by the caller and need not
        // agree with the vertices, so derive the plane normals from scratch.
        Vector3D na = planeNormal(a, tol);
        Vector3D nb = planeNormal(b, tol);
        if (na == null || nb == null) {
            return degenerateIntersects(a, na, b, nb, tol);
        }
        // Reject if either triangle lies wholly on one side of the other's
        // plane; distances within the tolerance are snapped onto the plane.
        double[] da = planeDistances(nb, b[0], a, tol);
        if (allPositive(da) || allNegative(da)) {
            return false;
        }
        double[] db = planeDistances(na, a[0], b, tol);
        if (allPositive(db) || allNegative(db)) {
            return false;
        }
        Vector3D direction = Vector3D.crossProduct(na, nb);
        double directionNorm = direction.norm();
        if (allZero(da) || allZero(db) || directionNorm < PARALLEL_EPSILON) {
            return coplanarIntersects(a, b, na, tol);
        }
        // Both triangles cross the line where the two planes meet: they
        // intersect exactly when the two crossed intervals overlap.
        direction = direction.times(1.0 / directionNorm);
        double[] intervalA = interval(a, da, direction, a[0]);
        double[] intervalB = interval(b, db, direction, a[0]);
        if (intervalA == null || intervalB == null) {
            return false;
        }
        return Math.max(intervalA[0], intervalB[0]) <= Math.min(intervalA[1], intervalB[1]) + tol;
    }

    /**
     * Threshold below which two plane normals are treated as parallel, and the
     * intersection line of their planes as undefined. Both normals are unit
     * length, so the norm of their cross product is the sine of the angle
     * between them.
     */
    private static final double PARALLEL_EPSILON = 1e-12;

    /**
     * The unit normal of the plane through the three vertices, or null if the
     * triangle is degenerate - that is, if its height over its longest edge is
     * within the tolerance, so that it is indistinguishable from a segment.
     */
    static Vector3D planeNormal(Point3D[] v, double tol) {
        Vector3D cross = Vector3D.crossProduct(new Vector3D(v[0], v[1]), new Vector3D(v[0], v[2]));
        double norm = cross.norm();
        double longestEdge = Math.max(v[0].distance(v[1]),
                Math.max(v[1].distance(v[2]), v[2].distance(v[0])));
        // norm is twice the area, ie longestEdge multiplied by the height over it
        if (norm <= tol * longestEdge) {
            return null;
        }
        return cross.times(1.0 / norm);
    }

    /**
     * Signed distances of each vertex from the plane through origin with the
     * given unit normal, with distances within the tolerance snapped to zero.
     */
    private static double[] planeDistances(Vector3D normal, Point3D origin, Point3D[] v, double tol) {
        double[] distances = new double[3];
        for (int i = 0; i < 3; i++) {
            double d = normal.dot(new Vector3D(origin, v[i]));
            distances[i] = Math.abs(d) <= tol ? 0.0 : d;
        }
        return distances;
    }

    private static boolean allPositive(double[] d) {
        return d[0] > 0 && d[1] > 0 && d[2] > 0;
    }

    private static boolean allNegative(double[] d) {
        return d[0] < 0 && d[1] < 0 && d[2] < 0;
    }

    private static boolean allZero(double[] d) {
        return d[0] == 0 && d[1] == 0 && d[2] == 0;
    }

    /**
     * The range covered by the triangle along the given unit direction, over
     * the part of it lying on the plane the distances were measured against.
     * That is the set of vertices on the plane together with the points where
     * edges cross it. Returns null if the triangle does not reach the plane.
     */
    private static double[] interval(Point3D[] v, double[] d, Vector3D direction, Point3D origin) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 3; i++) {
            int j = (i + 1) % 3;
            if (d[i] == 0.0) {
                double p = direction.dot(new Vector3D(origin, v[i]));
                min = Math.min(min, p);
                max = Math.max(max, p);
            }
            if (d[i] * d[j] < 0) {
                double t = d[i] / (d[i] - d[j]);
                Point3D crossing = v[i].plus(new Vector3D(v[i], v[j]).times(t));
                double p = direction.dot(new Vector3D(origin, crossing));
                min = Math.min(min, p);
                max = Math.max(max, p);
            }
        }
        return min > max ? null : new double[]{min, max};
    }

    /**
     * Overlap test for two triangles sharing a plane, whose unit normal is
     * given. Either one may contain the other, or their outlines may cross.
     */
    private static boolean coplanarIntersects(Point3D[] a, Point3D[] b, Vector3D normal, double tol) {
        for (int i = 0; i < 3; i++) {
            if (containsInPlane(b, normal, a[i], tol) || containsInPlane(a, normal, b[i], tol)) {
                return true;
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (segmentDistance(a[i], a[(i + 1) % 3], b[j], b[(j + 1) % 3]) <= tol) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether the point, projected onto the plane with the given unit normal,
     * lies inside the triangle or within the tolerance of its outline. The
     * winding of the triangle relative to the normal does not matter.
     */
    private static boolean containsInPlane(Point3D[] v, Vector3D normal, Point3D point, double tol) {
        boolean allLeft = true;
        boolean allRight = true;
        for (int i = 0; i < 3; i++) {
            Vector3D edge = new Vector3D(v[i], v[(i + 1) % 3]);
            double length = edge.norm();
            if (length == 0) {
                continue;
            }
            // In-plane distance from the point to the edge, signed by the side
            double side = Vector3D.crossProduct(edge, new Vector3D(v[i], point)).dot(normal) / length;
            if (side < -tol) {
                allLeft = false;
            }
            if (side > tol) {
                allRight = false;
            }
        }
        return allLeft || allRight;
    }

    /**
     * Whether the segment touches the triangle, whose unit normal is given.
     * Used for triangles too degenerate to have a plane of their own.
     */
    private static boolean segmentIntersects(Point3D[] v, Vector3D normal, Point3D p, Point3D q, double tol) {
        double dp = normal.dot(new Vector3D(v[0], p));
        double dq = normal.dot(new Vector3D(v[0], q));
        dp = Math.abs(dp) <= tol ? 0.0 : dp;
        dq = Math.abs(dq) <= tol ? 0.0 : dq;
        if ((dp > 0 && dq > 0) || (dp < 0 && dq < 0)) {
            return false;
        }
        if (dp == 0 && containsInPlane(v, normal, p, tol)) {
            return true;
        }
        if (dq == 0 && containsInPlane(v, normal, q, tol)) {
            return true;
        }
        if (dp * dq < 0) {
            Point3D crossing = p.plus(new Vector3D(p, q).times(dp / (dp - dq)));
            if (containsInPlane(v, normal, crossing, tol)) {
                return true;
            }
        }
        for (int i = 0; i < 3; i++) {
            if (segmentDistance(p, q, v[i], v[(i + 1) % 3]) <= tol) {
                return true;
            }
        }
        return false;
    }

    /**
     * Overlap test where at least one triangle is degenerate, and so is treated
     * as the segments of its edges. A null normal marks the degenerate one.
     */
    private static boolean degenerateIntersects(Point3D[] a, Vector3D na, Point3D[] b, Vector3D nb, double tol) {
        for (int i = 0; i < 3; i++) {
            if (na != null && segmentIntersects(a, na, b[i], b[(i + 1) % 3], tol)) {
                return true;
            }
            if (nb != null && segmentIntersects(b, nb, a[i], a[(i + 1) % 3], tol)) {
                return true;
            }
        }
        if (na != null || nb != null) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (segmentDistance(a[i], a[(i + 1) % 3], b[j], b[(j + 1) % 3]) <= tol) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Shortest distance between two segments, after Ericson, Real-Time
     * Collision Detection, section 5.1.9.
     */
    private static double segmentDistance(Point3D p1, Point3D q1, Point3D p2, Point3D q2) {
        Vector3D d1 = new Vector3D(p1, q1);
        Vector3D d2 = new Vector3D(p2, q2);
        Vector3D r = new Vector3D(p2, p1);
        double a = d1.normSq();
        double e = d2.normSq();
        double f = d2.dot(r);
        double s;
        double t;
        if (a <= 0 && e <= 0) {
            s = 0;
            t = 0;
        } else if (a <= 0) {
            s = 0;
            t = clamp(f / e);
        } else {
            double c = d1.dot(r);
            if (e <= 0) {
                t = 0;
                s = clamp(-c / a);
            } else {
                double b = d1.dot(d2);
                double denom = a * e - b * b;
                s = denom != 0 ? clamp((b * f - c * e) / denom) : 0;
                t = (b * s + f) / e;
                if (t < 0) {
                    t = 0;
                    s = clamp(-c / a);
                } else if (t > 1) {
                    t = 1;
                    s = clamp((b - c) / a);
                }
            }
        }
        return p1.plus(d1.times(s)).distance(p2.plus(d2.times(t)));
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    /**
     * Moves the triangle in the X,Y,Z direction
     *
     * @param translation A vector of the delta for each coordinate.
     */
    public void translate(Vector3D translation) {
        for (int i = 0; i < vertices.length; i++) {
            vertices[i] = vertices[i].plus(translation);
        }
    }

    /**
     * @see java.lang.Object#toString()
     * @return A string that provides some information about this triangle
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Triangle[");
        for (Point3D v : vertices) {
            sb.append(v.toString());
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Gets the vertices at the corners of this triangle
     *
     * @return An array of vertices
     */
    public Point3D[] getVertices() {
        return vertices;
    }

    /**
     * Gets the normal vector
     *
     * @return A vector pointing in a direction perpendicular to the surface of
     * the triangle.
     */
    public Vector3D getNormal() {
        return normal;
    }

    /**
     * Changes the scale, e.g. for unit translation
     *
     * @param scale
     * @return
     */
    public Triangle3D scale(double scale) {
        return new Triangle3D(vertices[0].times(scale), vertices[1].times(scale), vertices[2].times(scale), normal);
    }

    @Override
    public boolean almostEquals(GeometricObject3D obj, double eps) {
        if (obj instanceof Triangle3D) {
            Triangle3D other = (Triangle3D) obj;
            return (this.vertices[0].almostEquals(other.vertices[0], eps)
                    && this.vertices[1].almostEquals(other.vertices[1], eps)
                    && this.vertices[2].almostEquals(other.vertices[2], eps))
                    || (this.vertices[0].almostEquals(other.vertices[1], eps)
                    && this.vertices[1].almostEquals(other.vertices[2], eps)
                    && this.vertices[2].almostEquals(other.vertices[0], eps))
                    || (this.vertices[0].almostEquals(other.vertices[2], eps)
                    && this.vertices[1].almostEquals(other.vertices[0], eps)
                    && this.vertices[2].almostEquals(other.vertices[1], eps));
        }
        return false;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     * @param obj Object to test equality
     * @return True if the other object is a triangle whose verticese are the
     * same as this one.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Triangle3D other = (Triangle3D) obj;
        if (!Arrays.deepEquals(this.vertices, other.vertices)) {
            return false;
        }
        return true;
    }

    /**
     * @see java.lang.Object#hashCode()
     * @return A hashCode for this triangle
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Arrays.deepHashCode(this.vertices);
        return hash;
    }
}
