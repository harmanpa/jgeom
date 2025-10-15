/**
 *
 */
package math.geom3d.plane;

import java.util.ArrayList;
import java.util.List;
import math.geom2d.AffineTransform2D;
import math.geom2d.Angle2D;
import math.geom2d.Point2D;
import math.geom2d.Tolerance2D;
import math.geom2d.Vector2D;
import math.geom2d.exceptions.Geom2DException;
import math.geom2d.line.LineSegment2D;
import math.geom2d.line.StraightLine2D;
import math.geom3d.Box3D;
import math.geom3d.GeometricObject3D;
import math.geom3d.Vector3D;
import math.geom3d.Point3D;
import math.geom3d.Shape3D;
import math.geom3d.fitting.Plane3DFitter;
import math.geom3d.line.LineSegment3D;
import math.geom3d.line.LinearShape3D;
import math.geom3d.line.StraightLine3D;
import math.geom3d.transform.AffineTransform3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * @author dlegland
 */
public final class Plane3D implements Shape3D {

    // ===================================================================
    // class variables
    private final double x0;// = 0;
    private final double y0;// = 0;
    private final double z0;// = 0;
    private final double dx1;// = 1;
    private final double dy1;// = 0;
    private final double dz1;// = 0;
    private final double dx2;// = 0;
    private final double dy2;// = 1;
    private final double dz2;// = 0;

    // ===================================================================
    // static methods
    public final static Plane3D createXYPlane() {
        return new Plane3D(new Point3D(0, 0, 0), new Vector3D(1, 0, 0),
                new Vector3D(0, 1, 0));
    }

    public final static Plane3D createXZPlane() {
        return new Plane3D(new Point3D(0, 0, 0), new Vector3D(1, 0, 0),
                new Vector3D(0, 0, 1));
    }

    public final static Plane3D createYZPlane() {
        return new Plane3D(new Point3D(0, 0, 0), new Vector3D(0, 1, 0),
                new Vector3D(0, 0, 1));
    }

    public final static Plane3D createXYPlane(double z) {
        return new Plane3D(new Point3D(0, 0, z), new Vector3D(1, 0, 0),
                new Vector3D(0, 1, 0));
    }

    public final static Plane3D createXZPlane(double y) {
        return new Plane3D(new Point3D(0, y, 0), new Vector3D(1, 0, 0),
                new Vector3D(0, 0, 1));
    }

    public final static Plane3D createYZPlane(double x) {
        return new Plane3D(new Point3D(x, 0, 0), new Vector3D(0, 1, 0),
                new Vector3D(0, 0, 1));
    }

    public final static Plane3D fromNormal(Point3D point, Vector3D normal) {
        Vector3D temp = normal.swapNonZero();
        Vector3D a = normal.cross(temp).normalize();
        Vector3D b = normal.cross(a);
        return new Plane3D(point, b, a);
    }

    public final static Plane3D fromNormal(Vector3D normal, double dist) {
        return fromNormal(new Point3D(0, 0, 0).plus(normal.times(dist)), normal);
    }

    public final static Plane3D fromPoints(List<Point3D> points) throws Geom2DException {
        return new Plane3DFitter().fit(points);
    }

    public final static Plane3D fromCoefficients(double[] coeffs) {
        return new Plane3D(coeffs[0], coeffs[1], coeffs[2], coeffs[3], coeffs[4], coeffs[5], coeffs[6], coeffs[7], coeffs[8]);
    }

    public Plane3D(Point3D point, Vector3D vector1, Vector3D vector2) {
        this(point.getX(), point.getY(), point.getZ(), vector1.getX(), vector1.getY(),
                vector1.getZ(), vector2.getX(), vector2.getY(), vector2.getZ());
    }

    Plane3D(double x0, double y0, double z0, double dx1, double dy1, double dz1, double dx2, double dy2, double dz2) {
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.dx1 = dx1;
        this.dy1 = dy1;
        this.dz1 = dz1;
        this.dx2 = dx2;
        this.dy2 = dy2;
        this.dz2 = dz2;
    }

    public double[] coefficients() {
        return new double[]{x0, y0, z0, dx1, dy1, dz1, dx2, dy2, dz2};
    }

    // ===================================================================
    // methods specific to Plane3D
    public Point3D origin() {
        return new Point3D(x0, y0, z0);
    }

    public Plane3D withOrigin(Point3D origin) {
        return new Plane3D(origin, new Vector3D(dx1, dy1, dz1), new Vector3D(dx2, dy2, dz2));
    }

    public boolean isParallel(Plane3D other) {
        Vector3D n1 = normal();
        Vector3D n2 = other.normal();
        return Vector3D.isCodirected(n1, n2);
    }

    public boolean isOpposing(Plane3D other) {
        Vector3D n1 = normal();
        Vector3D n2 = other.normal();
        return Vector3D.isOpposite(n1, n2);
    }

    public boolean isParallelOrOpposing(Plane3D other) {
        Vector3D n1 = normal();
        Vector3D n2 = other.normal();
        return Vector3D.isColinear(n1, n2);
    }

    public Plane3DRelationship getRelationship(Plane3D other) {
        if (isParallel(other)) {
            return Plane3DRelationship.Parallel;
        } else if (isOpposing(other)) {
            if (Vector3D.isCodirected(normal(), new Vector3D(origin(), other.projectPoint(origin())))) {
                return Plane3DRelationship.FaceToFace;
            } else {
                return Plane3DRelationship.BackToBack;
            }
        } else {
            return Plane3DRelationship.Intersecting;
        }
    }

    public double dist() {
        Point3D globalOrigin = new Point3D(0, 0, 0);
        Point3D pointOnPlane = projectPoint(globalOrigin);
        double d = globalOrigin.distance(pointOnPlane);
        return Math.signum(new Vector3D(globalOrigin, pointOnPlane).dot(normal())) * d;
    }

    public Vector3D vector1() {
        return new Vector3D(dx1, dy1, dz1);
    }

    public Vector3D vector2() {
        return new Vector3D(dx2, dy2, dz2);
    }

    /**
     * Returns a normal vector that points towards the outside part of the
     * plane.
     *
     * @return the outer normal vector.
     */
    public Vector3D normal() {
        return Vector3D.crossProduct(this.vector1(), this.vector2())
                .opposite();
    }

    public Plane3D flip() {
        return fromNormal(origin(), normal().opposite());
    }

    /**
     * Compute intersection of a line with this plane. Uses algorithm 1 given
     * in: <a href="http://local.wasp.uwa.edu.au/~pbourke/geometry/planeline/">
     * http://local.wasp.uwa.edu.au/~pbourke/geometry/planeline/</a>.
     *
     * @param line the line which intersects the plane
     * @return the intersection point
     */
    public Point3D lineIntersection(StraightLine3D line) {
        // the plane normal
        Vector3D n = this.normal();

        // the difference between origin of plane and origin of line
        Vector3D dp = new Vector3D(line.origin(), this.origin());
        
        // compute ratio of dot products,
        // see http://local.wasp.uwa.edu.au/~pbourke/geometry/planeline/
        double t = Vector3D.dotProduct(n, dp)
                / Vector3D.dotProduct(n, line.direction());

        return line.point(t);
    }

    public Point3D projectPoint(Point3D point) {
        StraightLine3D line = new StraightLine3D(point, this.normal());
        return this.lineIntersection(line);
    }

    public boolean isVectorInPlane(Vector3D vect) {
        return Vector3D.isOrthogonal(vect, normal());
    }

    public Vector3D projectVector(Vector3D vect) {
        Point3D point = new Point3D(x0 + vect.getX(), y0 + vect.getY(), z0
                + vect.getZ());
        point = this.projectPoint(point);
        return new Vector3D(point.getX() - x0, point.getY() - y0, point.getZ() - z0);
    }

    public Point3D point(Point2D p) {
        return point(p.getX(), p.getY());
    }

    public Point3D point(double u, double v) {
        return new Point3D(x0 + u * dx1 + v * dx2, y0 + u * dy1 + v * dy2, z0 + u * dz1 + v * dz2);
    }

    public Point2D pointPosition(Point3D point) {
        RealVector xy = new QRDecomposition(
                new Array2DRowRealMatrix(new double[][]{{dx1, dx2}, {dy1, dy2}, {dz1, dz2}}))
                .getSolver()
                .solve(new ArrayRealVector(new double[]{point.getX() - x0, point.getY() - y0, point.getZ() - z0}));
        return new Point2D(xy.getEntry(0), xy.getEntry(1));
    }

    public Vector2D vectorInPlane(Vector3D v) {
        RealVector xy = new QRDecomposition(
                new Array2DRowRealMatrix(new double[][]{{dx1, dx2}, {dy1, dy2}, {dz1, dz2}}))
                .getSolver()
                .solve(new ArrayRealVector(new double[]{v.getX(), v.getY(), v.getZ()}));
        return new Vector2D(xy.getEntry(0), xy.getEntry(1));
    }

    public Vector3D vector(Vector2D v) {
        return new Vector3D(v.getX() * dx1 + v.getY() * dx2, v.getX() * dy1 + v.getY() * dy2, v.getX() * dz1 + v.getY() * dz2);
    }

    public boolean isLineInPlane(LinearShape3D line) {
        return contains(line.firstPoint()) && isVectorInPlane(line.direction());
    }

    public StraightLine2D lineInPlane(StraightLine3D line) {
        return new StraightLine2D(pointPosition(line.origin()), vectorInPlane(line.direction()));
    }

    public LineSegment2D lineInPlane(LineSegment3D line) {
        return new LineSegment2D(pointPosition(line.firstPoint()), pointPosition(line.lastPoint()));
    }

    // ===================================================================
    // methods implementing Shape3D interface

    /*
     * (non-Javadoc)
     * 
     * @see math.geom3d.Shape3D#clip(math.geom3d.Box3D)
     */
//    @Override
//    public Shape3D clip(Box3D box) {
//        // TODO Auto-generated method stub
//        return this;
//    }

    /*
     * (non-Javadoc)
     * 
     * @see math.geom3d.Shape3D#contains(math.geom3d.Point3D)
     */
    @Override
    public boolean contains(Point3D point) {
        Point3D proj = this.projectPoint(point);
        return (point.distance(proj) < Tolerance2D.get());
    }

    /*
     * (non-Javadoc)
     * 
     * @see math.geom3d.Shape3D#getBoundingBox()
     */
    @Override
    public Box3D boundingBox() {
        // plane parallel to XY plane
        if (Math.abs(dz1) < Tolerance2D.get() && Math.abs(dz2) < Tolerance2D.get()) {
            return new Box3D(Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY, z0, z0);
        }

        // plane parallel to YZ plane
        if (Math.abs(dx1) < Tolerance2D.get() && Math.abs(dx2) < Tolerance2D.get()) {
            return new Box3D(x0, x0, Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY);
        }

        // plane parallel to XZ plane
        if (Math.abs(dy1) < Tolerance2D.get() && Math.abs(dy2) < Tolerance2D.get()) {
            return new Box3D(Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY, y0, y0, Double.NEGATIVE_INFINITY,
                    Double.POSITIVE_INFINITY);
        }

        return new Box3D(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /*
     * (non-Javadoc)
     * 
     * @see math.geom3d.Shape3D#getDistance(math.geom3d.Point3D)
     */
    @Override
    public double distance(Point3D point) {
        return point.distance(this.projectPoint(point));
    }

    public double signedDistance(Point3D point) {
        Point3D projected = this.projectPoint(point);
        double distance = point.distance(projected);
        return distance * (Vector3D.isCodirected(normal(), new Vector3D(point, projected)) ? 1 : -1);
    }

    public double distance(Plane3D plane) {
        return isParallelOrOpposing(plane) ? distance(plane.origin()) : 0.0;
    }

    public AffineTransform3D transform3D(AffineTransform2D transform) throws Geom2DException {
        List<Point3D> points = new ArrayList<>(3);
        List<Point3D> otherPoints = new ArrayList<>(3);
        points.add(point(new Point2D(0, 0)).plus(normal()));
        points.add(point(new Point2D(1, 0)));
        points.add(point(new Point2D(0, 1)));
        otherPoints.add(point(new Point2D(0, 0).transform(transform)).plus(normal()));
        otherPoints.add(point(new Point2D(1, 0).transform(transform)));
        otherPoints.add(point(new Point2D(0, 1).transform(transform)));
        return AffineTransform3D.calculate(points, otherPoints);
    }

    public AffineTransform3D transform3D(Plane3D other) throws Geom2DException {
        List<Point3D> points = new ArrayList<>(3);
        List<Point3D> otherPoints = new ArrayList<>(3);
        points.add(origin().plus(normal()));
        points.add(point(1, 0));
        points.add(point(0, 1));
        otherPoints.add(other.origin().plus(other.normal()));
        otherPoints.add(other.point(1, 0));
        otherPoints.add(other.point(0, 1));
        return AffineTransform3D.calculate(points, otherPoints);
    }

    public AffineTransform2D transform2D(Plane3D other) {
        Array2DRowRealMatrix m1 = new Array2DRowRealMatrix(2, 2);
        m1.setEntry(0, 0, 1);
        m1.setEntry(0, 1, 0);
        m1.setEntry(1, 0, 0);
        m1.setEntry(1, 1, 1);
        Array2DRowRealMatrix m2 = new Array2DRowRealMatrix(2, 2);
        Point2D a = other.pointPosition(point(1, 0));
        Point2D b = other.pointPosition(point(0, 1));
        m2.setEntry(0, 0, a.getX());
        m2.setEntry(1, 0, a.getY());
        m2.setEntry(0, 1, b.getX());
        m2.setEntry(1, 1, b.getY());
        RealMatrix m = new QRDecomposition(m1).getSolver().solve(m2);
        return AffineTransform2D.create(m.getEntry(0, 0), m.getEntry(0, 1), 0, m.getEntry(1, 0), m.getEntry(1, 1), 0);
    }

    public AffineTransform2D projectTransform(Plane3D other) {
        Point2D otherOrigin = other.pointPosition(other.projectPoint(point(0, 0)));
        Point2D x1 = other.pointPosition(other.projectPoint(point(1, 0)));
        Point2D y1 = other.pointPosition(other.projectPoint(point(0, 1)));
        double rotX = Angle2D.formatAngle(Angle2D.horizontalAngle(otherOrigin, x1) - Angle2D.horizontalAngle(new Point2D(0, 0), new Point2D(1, 0)));
        double rotY = Angle2D.formatAngle(Angle2D.horizontalAngle(otherOrigin, y1) - Angle2D.horizontalAngle(new Point2D(0, 0), new Point2D(0, 1)));
        AffineTransform2D transform = AffineTransform2D.createTranslation(otherOrigin.getX(), otherOrigin.getY());
        if (Angle2D.M_PI - Math.abs(rotX - rotY) <= Tolerance2D.get()) {
            // If it is flipped on the x axis
            transform = AffineTransform2D.createLineReflection(
                    new StraightLine2D(new Point2D(0, 0), new Vector2D(0, 1))).preConcatenate(transform);
        }
        return transform;
    }

    /**
     * Krumm method:
     * https://www.microsoft.com/en-us/research/publication/intersection-of-two-planes/
     *
     * @param other
     * @return
     */
    public StraightLine3D intersection(Plane3D other) {
        if (isParallelOrOpposing(other)) {
            return null;
        }
        Vector3D n1 = normal();
        Vector3D n2 = other.normal();
        Point3D p1 = origin();
        Point3D p2 = other.origin();
        Vector3D n = n1.cross(n2).normalize();
        Point3D p0 = Point3D.midpoint(p1, p2);
        Array2DRowRealMatrix m = new Array2DRowRealMatrix(5, 5);
        ArrayRealVector v = new ArrayRealVector(5);
        m.setEntry(0, 0, 2);
        m.setEntry(0, 1, 0);
        m.setEntry(0, 2, 0);
        m.setEntry(0, 3, n1.getX());
        m.setEntry(0, 4, n2.getX());
        m.setEntry(1, 0, 0);
        m.setEntry(1, 1, 2);
        m.setEntry(1, 2, 0);
        m.setEntry(1, 3, n1.getY());
        m.setEntry(1, 4, n2.getY());
        m.setEntry(2, 0, 0);
        m.setEntry(2, 1, 0);
        m.setEntry(2, 2, 2);
        m.setEntry(2, 3, n1.getZ());
        m.setEntry(2, 4, n2.getZ());
        m.setEntry(3, 0, n1.getX());
        m.setEntry(3, 1, n1.getY());
        m.setEntry(3, 2, n1.getZ());
        m.setEntry(3, 3, 0);
        m.setEntry(3, 4, 0);
        m.setEntry(4, 0, n2.getX());
        m.setEntry(4, 1, n2.getY());
        m.setEntry(4, 2, n2.getZ());
        m.setEntry(4, 3, 0);
        m.setEntry(4, 4, 0);
        v.setEntry(0, 2 * p0.getX());
        v.setEntry(1, 2 * p0.getY());
        v.setEntry(2, 2 * p0.getZ());
        v.setEntry(3, p1.asVector().dot(n1));
        v.setEntry(3, p2.asVector().dot(n2));
        RealVector res = new LUDecomposition(m).getSolver().solve(v);
        Point3D p = new Point3D(res.getEntry(0), res.getEntry(1), res.getEntry(2));
        return new StraightLine3D(p, n);
    }

    /*
     * (non-Javadoc)
     * 
     * @see math.geom3d.Shape3D#isBounded()
     */
    @Override
    public boolean isBounded() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see math.geom3d.Shape3D#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see math.geom3d.Shape3D#transform(math.geom3d.transform.AffineTransform3D)
     */
    @Override
    public Shape3D transform(AffineTransform3D trans) {
        return new Plane3D(this.origin().transform(trans), this.vector1()
                .transform(trans), this.vector2().transform(trans));
    }

    // ===================================================================
    // methods overriding Object superclass
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Plane3D && almostEquals((Plane3D) obj, Tolerance2D.get());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Tolerance2D.hash(this.x0);
        hash = 71 * hash + Tolerance2D.hash(this.y0);
        hash = 71 * hash + Tolerance2D.hash(this.z0);
        hash = 71 * hash + Tolerance2D.hash(this.dx1);
        hash = 71 * hash + Tolerance2D.hash(this.dy1);
        hash = 71 * hash + Tolerance2D.hash(this.dz1);
        hash = 71 * hash + Tolerance2D.hash(this.dx2);
        hash = 71 * hash + Tolerance2D.hash(this.dy2);
        hash = 71 * hash + Tolerance2D.hash(this.dz2);
        return hash;
    }

    @Override
    public boolean almostEquals(GeometricObject3D obj, double eps) {
        if (!(obj instanceof Plane3D)) {
            return false;
        }
        Plane3D plane = (Plane3D) obj;
        if (Tolerance2D.compare(this.x0, plane.x0) != 0) {
            return false;
        }
        if (Tolerance2D.compare(this.y0, plane.y0) != 0) {
            return false;
        }
        if (Tolerance2D.compare(this.z0, plane.z0) != 0) {
            return false;
        }
        if (Tolerance2D.compare(this.dx1, plane.dx1) != 0) {
            return false;
        }
        if (Tolerance2D.compare(this.dy1, plane.dy1) != 0) {
            return false;
        }
        if (Tolerance2D.compare(this.dz1, plane.dz1) != 0) {
            return false;
        }
        if (Tolerance2D.compare(this.dx2, plane.dx2) != 0) {
            return false;
        }
        if (Tolerance2D.compare(this.dy2, plane.dy2) != 0) {
            return false;
        }
        if (Tolerance2D.compare(this.dz2, plane.dz2) != 0) {
            return false;
        }
        return true;
    }

    public static enum Plane3DRelationship {
        Intersecting, Parallel, FaceToFace, BackToBack;
    }
}
