/* File AbstractLine2D.java 
 *
 * Project : Java Geometry Library
 *
 * ===========================================
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY, without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. if not, write to :
 * The Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */
// package
package math.geom2d.line;

//Imports
import math.geom2d.exceptions.DegeneratedLine2DException;
import java.util.ArrayList;
import java.util.Collection;

import math.geom2d.*;
import math.geom2d.circulinear.CirculinearDomain2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.buffer.BufferCalculator;
import math.geom2d.conic.Circle2D;
import math.geom2d.conic.CircleArc2D;
import math.geom2d.curve.*;
import math.geom2d.domain.SmoothOrientedCurve2D;
import math.geom2d.transform.CircleInversion2D;
import org.apache.commons.math3.util.FastMath;

/**
 * <p>
 * Base class for straight curves, such as straight lines, rays, or edges.
 * </p>
 * <p>
 * Internal representation of straight objects is parametric: (x0, y0) is a
 * point in the object, and (dx, dy) is a direction vector of the line.
 * </p>
 * <p>
 * If the line is defined by two point, we can set (x0,y0) to the first point,
 * and (dx,dy) to the vector (p1, p2).
 * </p>
 * <p>
 * Then, coordinates for a point (x,y) such as x=x0+t*dx and y=y0+t=dy, t
 * between 0 and 1 give a point inside p1 and p2, t<0 give a point 'before' p1,
 * and t>1 give a point 'after' p2, so it is convenient to easily manage edges,
 * rays and straight lines.
 * <p>
 */
public abstract class AbstractLine2D extends AbstractSmoothCurve2D
        implements SmoothOrientedCurve2D, LinearElement2D {

    // ===================================================================
    // static methods
    /**
     * Returns the unique intersection of two straight objects.If the
     * intersection doesn't exist (parallel lines, short edge), return null.
     *
     * @param line1
     * @param line2
     * @return
     */
    public static Point2D getIntersection(AbstractLine2D line1,
            AbstractLine2D line2) {
        // Compute denominator, and tests its validity
        double denom = line1.dx * line2.dy - line1.dy * line2.dx;
        if (Math.abs(denom) < Tolerance2D.get()) {
            return null;
        }

        // Compute position of intersection point
        double t = ((line1.y0 - line2.y0) * line2.dx
                - (line1.x0 - line2.x0) * line2.dy) / denom;
        return new Point2D(line1.x0 + t * line1.dx, line1.y0 + t * line1.dy);
    }

    /**
     * Tests if the two linear objects are located on the same straight line.
     *
     * @param line1
     * @param line2
     * @return
     */
    public static boolean isColinear(AbstractLine2D line1, AbstractLine2D line2) {
        // test if the two lines are parallel
        if (Math.abs(line1.dx * line2.dy - line1.dy * line2.dx) > Tolerance2D.get()) {
            return false;
        }

        // test if the two lines share at least one point (see the contains()
        // method for details on tests)
        return (Math.abs((line2.y0 - line1.y0) * line2.dx
                - (line2.x0 - line1.x0) * line2.dy)
                / FastMath.hypot(line2.dx, line2.dy) < Tolerance2D.get());
    }

    /**
     * Tests if the two linear objects are parallel.
     *
     * @param line1
     * @param line2
     * @return
     */
    public static boolean isParallel(AbstractLine2D line1, AbstractLine2D line2) {
        return (Math.abs(line1.dx * line2.dy - line1.dy * line2.dx) < Tolerance2D.get());
    }

    // ===================================================================
    // class variables
    /**
     * Coordinates of starting point of the line
     */
    protected double x0, y0;

    /**
     * Direction vector of the line. dx and dy should not be both zero.
     */
    protected double dx, dy;

    // ===================================================================
    // Protected constructors
    protected AbstractLine2D(double x0, double y0, double dx, double dy) {
        this.x0 = x0;
        this.y0 = y0;
        this.dx = dx;
        this.dy = dy;
    }

    protected AbstractLine2D(Point2D point, Vector2D vector) {
        this.x0 = point.x();
        this.y0 = point.y();
        this.dx = vector.x();
        this.dy = vector.y();
    }

    // ===================================================================
    // Methods specific to Line shapes
    /**
     * Tests if the given linear shape is parallel to this shape.
     *
     * @param linear
     * @return
     */
    public boolean isColinear(LinearShape2D linear) {
        // test if the two lines are parallel
        if (!isParallel(linear)) {
            return false;
        }

        // test if the two lines share at least one point (see the contains()
        // method for details on tests)
        StraightLine2D line = linear.supportingLine();
        if (Math.abs(dx) > Math.abs(dy)) {
            return Math.abs((line.x0 - x0) * dy / dx + y0 - line.y0) <= Tolerance2D.get();
        } else {
            return Math.abs((line.y0 - y0) * dx / dy + x0 - line.x0) <= Tolerance2D.get();
        }
    }

    /**
     * Tests if the this object is parallel to the given one.
     *
     * @param line
     * @return
     */
    public boolean isParallel(LinearShape2D line) {
        return Vector2D.isColinear(this.direction(), line.direction());
    }

    /**
     * Returns true if the point (x, y) lies on the line covering the object,
     * with precision given by Tolerance2D.get().
     *
     * @param x
     * @param y
     * @return
     */
    protected boolean supportContains(double x, double y) {
        double denom = FastMath.hypot(dx, dy);
        if (denom < Tolerance2D.get()) {
            return new Point2D(x0, y0).distance(x, y) < Tolerance2D.get();
        }
        return (Math.abs((x - x0) * dy - (y - y0) * dx) / (denom * denom) < Tolerance2D.get());
    }

    /**
     * Returns the matrix of parametric representation of the line. Result has
     * the form:
     * <p>
     * [ x0 dx ]
     * <p>
     * [ y0 dy ]
     * <p>
     * It can be easily extended to higher dimensions and/or higher polynomial
     * forms.
     *
     * @return
     */
    public double[][] parametric() {
        double tab[][] = new double[2][2];
        tab[0][0] = x0;
        tab[0][1] = dx;
        tab[1][0] = y0;
        tab[1][1] = dy;
        return tab;
    }

    /**
     * Returns the coefficient of the Cartesian representation of the line.
     * Cartesian equation has the form: <code>ax+by+c=0</code>
     *
     * @return the array {a, b, c}.
     */
    public double[] cartesianEquation() {
        double tab[] = new double[3];
        tab[0] = dy;
        tab[1] = -dx;
        tab[2] = dx * y0 - dy * x0;
        return tab;
    }

    /**
     * Returns polar coefficients of this linear shape.
     *
     * @return an array of 2 elements, the first one is the distance to the
     * origin, the second one is the angle with horizontal, between 0 and 2*PI.
     */
    public double[] polarCoefficients() {
        double tab[] = new double[2];
        double d = signedDistance(0, 0);
        tab[0] = Math.abs(d);
        if (d > 0) {
            tab[1] = (horizontalAngle() + Math.PI) % (2 * Math.PI);
        } else {
            tab[1] = horizontalAngle();
        }
        return tab;
    }

    /**
     * Returns the signed polar coefficients. Distance to origin can be
     * negative: this allows representation of directed lines.
     *
     * @return an array of 2 elements, the first one is the signed distance to
     * the origin, the second one is the angle with horizontal, between 0 and
     * 2*PI.
     */
    public double[] polarCoefficientsSigned() {
        double tab[] = new double[2];
        tab[0] = signedDistance(0, 0);
        tab[1] = horizontalAngle();
        return tab;
    }

    public double positionOnLine(Point2D point) {
        return positionOnLine(point.x(), point.y());
    }

    /**
     * Computes position on the line of the point given by (x,y). The position
     * is the number t such that if the point belong to the line, it location is
     * given by x=x0+t*dx and y=y0+t*dy.
     * <p>
     * If the point does not belong to the line, the method returns the position
     * of its projection on the line.
     *
     * @param x
     * @param y
     * @return
     */
    public double positionOnLine(double x, double y) {
        double denom = dx * dx + dy * dy;
        if (Math.abs(denom) < FastMath.pow(Tolerance2D.get(), 2)) {
            throw new DegeneratedLine2DException(this);
        }
        return ((y - y0) * dy + (x - x0) * dx) / denom;
    }

    /**
     * Returns the projection of point p on the line. The returned point can be
     * used to compute distance from point to line.
     *
     * @param p a point outside the line (if point p lies on the line, it is
     * returned)
     * @return the projection of the point p on the line
     */
    public Point2D projectedPoint(Point2D p) {
        return projectedPoint(p.x(), p.y());
    }

    /**
     * Returns the projection of point p on the line. The returned point can be
     * used to compute distance from point to line.
     *
     * @param x coordinate x of point to be projected
     * @param y coordinate y of point to be projected
     * @return the projection of the point p on the line
     */
    public Point2D projectedPoint(double x, double y) {
        if (contains(x, y)) {
            return new Point2D(x, y);
        }

        // compute position on the line
        double t;
        try {
            t = positionOnLine(x, y);
        } catch (DegeneratedLine2DException ex) {
            t = 0;
        }

        // compute position of intersection point
        return new Point2D(x0 + t * dx, y0 + t * dy);
    }

    /**
     * Return the symmetric of point p relative to this straight line.
     *
     * @param p a point outside the line (if point p lies on the line, it is
     * returned)
     * @return the projection of the point p on the line
     */
    public Point2D getSymmetric(Point2D p) {
        return getSymmetric(p.x(), p.y());
    }

    /**
     * Return the symmetric of point with coordinate (x, y) relative to this
     * straight line.
     *
     * @param x x-coordinate of point to be projected
     * @param y y-coordinate of point to be projected
     * @return the projection of the point (x,y) on the line
     */
    public Point2D getSymmetric(double x, double y) {
        // compute position on the line
        double t = 2 * positionOnLine(x, y);

        // compute position of intersection point
        return new Point2D(2 * x0 + t * dx - x, 2 * y0 + t * dy - y);
    }

    /**
     * Creates a straight line parallel to this object, and going through the
     * given point.
     *
     * @param point the point to go through
     * @return the parallel through the point
     */
    public StraightLine2D parallel(Point2D point) {
        return new StraightLine2D(point, this.dx, this.dy);
    }

    /**
     * Creates a straight line perpendicular to this object, and going through
     * the given point.
     *
     * @param point the point to go through
     * @return the perpendicular through the point
     */
    public StraightLine2D perpendicular(Point2D point) {
        return new StraightLine2D(point, -this.dy, this.dx);
    }

    // ===================================================================
    // Methods implementing the LinearShape2D interface
    /**
     * Returns the origin point of this linear shape.
     */
    @Override
    public Point2D origin() {
        return new Point2D(x0, y0);
    }

    /**
     * Returns the direction vector of this linear shape.
     */
    @Override
    public Vector2D direction() {
        return new Vector2D(dx, dy);
    }

    /**
     * Gets Angle with axis (O,i), counted counter-clockwise.Result is given
     * between 0 and 2*pi.
     *
     * @return
     */
    @Override
    public double horizontalAngle() {
        return (FastMath.atan2(dy, dx) + 2 * Math.PI) % (2 * Math.PI);
    }

    /**
     * Returns the unique intersection with a linear shape.If the intersection
     * doesn't exist (parallel lines, short edges), return null.
     *
     * @param line
     * @return
     */
    @Override
    public Point2D intersection(LinearShape2D line) {
        Vector2D vect = line.direction();
        double dx2 = vect.x();
        double dy2 = vect.y();

        // test if two lines are parallel
        double denom = this.dx * dy2 - this.dy * dx2;
        if (Math.abs(denom) < Tolerance2D.get()) {
            return null;
        }

        // compute position on the line
        Point2D origin = line.origin();
        double x2 = origin.x();
        double y2 = origin.y();
        double t = ((y0 - y2) * dx2 - (x0 - x2) * dy2) / denom;

        // compute position of intersection point
        Point2D point = new Point2D(x0 + t * dx, y0 + t * dy);

        // check if point is inside the bounds of the object. This test
        // is left to derived classes.
        if (containsProjection(point) && line.containsProjection(point)) {
            return point;
        }
        return null;
    }

    /* (non-Javadoc)
	 * @see math.geom2d.line.AbstractLine2D#supportingLine()
     */
    @Override
    public StraightLine2D supportingLine() {
        return new StraightLine2D(this);
    }

    @Override
    public boolean containsProjection(Point2D point) {
        try {
            double pos = this.positionOnLine(point);
            double tol = Tolerance2D.getRelative(this);
            return pos > (this.t0() - tol) && pos < (this.t1() + tol);
        } catch (DegeneratedLine2DException ex) {
            return false;
        }
    }

    // ===================================================================
    // methods implementing the CirculinearCurve2D interface
    /* (non-Javadoc)
	 * @see math.geom2d.circulinear.CirculinearCurve2D#length()
     */
    @Override
    public double length() {
        if (!this.isBounded()) {
            return Double.POSITIVE_INFINITY;
        }
        return (t1() - t0()) * FastMath.hypot(dx, dy);
    }

    /* (non-Javadoc)
	 * @see math.geom2d.circulinear.CirculinearCurve2D#length(double)
     */
    @Override
    public double length(double pos) {
        return pos * FastMath.hypot(dx, dy);
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see math.geom2d.circulinear.CirculinearCurve2D#position(double)
     */
    @Override
    public double position(double distance) {
        double delta = FastMath.hypot(dx, dy);
        if (delta < Tolerance2D.get()) {
            return t0();
        }
        return distance / delta;
    }

    // ===================================================================
    // methods implementing the CirculinearShape2D interface

    /* (non-Javadoc)
	 * @see math.geom2d.circulinear.CirculinearCurve2D#transform(math.geom2d.transform.CircleInversion2D)
     */
    @Override
    public CirculinearElement2D transform(CircleInversion2D inv) {
        // Extract inversion parameters
        Point2D center = inv.center();
        double r = inv.radius();

        // compute distance of line to inversion center
        StraightLine2D line = this.supportingLine();
        Point2D po = line.projectedPoint(center);
        double d = line.distance(center);

        // flag for indicating if line extremities are finite
        boolean inf0 = Double.isInfinite(this.t0());
        boolean inf1 = Double.isInfinite(this.t1());

        // Degenerate case of a line passing through the center.
        // returns the line itself.
        if (Math.abs(d) < Tolerance2D.get()) {
            if (inf0) {
                if (inf1) {
                    // case of a straight line, which transform into itself
                    return new StraightLine2D(this);
                } else {
                    // case of an inverted ray, which transform into another
                    // inverted ray
                    Point2D p2 = this.lastPoint().transform(inv);
                    return new InvertedRay2D(p2, this.direction());
                }
            } else {
                Point2D p1 = this.firstPoint().transform(inv);
                if (inf1) {
                    // case of a ray, which transform into another ray
                    return new Ray2D(p1, this.direction());
                } else {
                    // case of an line segment
                    Point2D p2 = this.lastPoint().transform(inv);
                    return new LineSegment2D(p1, p2);
                }
            }
        }

        // angle from center to line
        double angle = Angle2D.horizontalAngle(center, po);

        // center of transformed circle
        double r2 = r * r / d / 2;
        Point2D c2 = Point2D.createPolar(center, r2, angle);

        // choose direction of circle arc
        boolean direct = this.isInside(center);

        // case of a straight line
        if (inf0 && inf1) {
            return new Circle2D(c2, r2, direct);
        }

        // Compute the transform of the end points, which can be the center of
        // the inversion circle in the case of an infinite line.
        Point2D p1 = inf0 ? center : this.firstPoint().transform(inv);
        Point2D p2 = inf1 ? center : this.lastPoint().transform(inv);

        // compute angle between center of transformed circle and end points
        double theta1 = Angle2D.horizontalAngle(c2, p1);
        double theta2 = Angle2D.horizontalAngle(c2, p2);

        // create the new circle arc
        return new CircleArc2D(c2, r2, theta1, theta2, direct);
    }

    /* (non-Javadoc)
	 * @see math.geom2d.circulinear.CirculinearShape2D#buffer(double)
     */
    @Override
    public CirculinearDomain2D buffer(double dist) {
        BufferCalculator bc = BufferCalculator.getDefaultInstance();
        return bc.computeBuffer(this, dist);
    }

    // ===================================================================
    // methods of OrientedCurve2D interface

    /* (non-Javadoc)
	 * @see math.geom2d.domain.OrientedCurve2D#windingAngle(Point2D)
     */
    @Override
    public double windingAngle(Point2D point) {

        double t0 = this.t0();
        double t1 = this.t1();

        double angle1, angle2;
        if (t0 == Double.NEGATIVE_INFINITY) {
            angle1 = Angle2D.horizontalAngle(-dx, -dy);
        } else {
            angle1 = Angle2D.horizontalAngle(point.x(), point.y(),
                    x0 + t0 * dx, y0 + t0 * dy);
        }

        if (t1 == Double.POSITIVE_INFINITY) {
            angle2 = Angle2D.horizontalAngle(dx, dy);
        } else {
            angle2 = Angle2D.horizontalAngle(point.x(), point.y(),
                    x0 + t1 * dx, y0 + t1 * dy);
        }

        if (this.isInside(point)) {
            if (angle2 > angle1) {
                return angle2 - angle1;
            } else {
                return 2 * Math.PI - angle1 + angle2;
            }
        } else {
            if (angle2 > angle1) {
                return angle2 - angle1 - 2 * Math.PI;
            } else {
                return angle2 - angle1;
            }
        }
    }

    /**
     * Returns the signed distance of the StraightObject2d to the given
     * point.The signed distance is positive if point lies 'to the right' of the
     * line, when moving in the direction given by direction vector. This method
     * is not designed to be used directly, because AbstractLine2D is an
     * abstract class, but it can be used by subclasses to help computations.
     *
     * @param p
     */
    @Override
    public double signedDistance(Point2D p) {
        return signedDistance(p.x(), p.y());
    }

    /**
     * Returns the signed distance of the StraightObject2d to the given point.
     * The signed distance is positive if point lies 'to the right' of the line,
     * when moving in the direction given by direction vector. This method is
     * not designed to be used directly, because AbstractLine2D is an abstract
     * class, but it can be used by subclasses to help computations.
     */
    @Override
    public double signedDistance(double x, double y) {
        double delta = FastMath.hypot(dx, dy);
        if (delta < Tolerance2D.get()) {
            return 0.0;
        }
        return ((x - x0) * dy - (y - y0) * dx) / delta;
    }

    /**
     * Returns true if the given point lies to the left of the line when
     * traveling along the line in the direction given by its direction vector.
     *
     * @param p the point to test
     * @return true if point p lies on the 'left' of the line.
     */
    @Override
    public boolean isInside(Point2D p) {
        return ((p.x() - x0) * dy - (p.y() - y0) * dx < 0);
    }

    // ===================================================================
    // methods implementing the SmoothCurve2D interface

    /* (non-Javadoc)
	 * @see math.geom2d.curve.SmoothCurve2D#tangent(double)
     */
    @Override
    public Vector2D tangent(double t) {
        return new Vector2D(dx, dy);
    }

    /**
     * Returns 0 as for every straight object.
     */
    @Override
    public double curvature(double t) {
        return 0.0;
    }

    // ===================================================================
    // methods implementing the ContinuousCurve2D interface
    /**
     * Always returns false, because we can not come back to starting point if
     * we always go straight...
     */
    @Override
    public boolean isClosed() {
        return false;
    }

    // ===================================================================
    // methods implementing the Curve2D interface
    /**
     * Returns a collection of lines containing only this line.
     */
    @Override
    public Collection<? extends AbstractLine2D> smoothPieces() {
        return wrapCurve(this);
    }

    /**
     * Returns the intersection points of the curve with the specified line. The
     * length of the result array is the number of intersection points.
     *
     * @param line
     * @return
     */
    @Override
    public Collection<Point2D> intersections(LinearShape2D line) {
        if (this.isParallel(line)) {
            return new ArrayList<>(0);
        }

        ArrayList<Point2D> points = new ArrayList<>(1);
        Point2D point = intersection(line);
        if (point != null) {
            points.add(point);
        }

        // return array with the intersection point.
        return points;
    }

    /**
     * Returns the position of the point on the line arc. If the point belongs
     * to the line, this position is defined by the ratio:
     * <p>
     * <code> t = (xp - x0)/dx <\code>, or equivalently:
     * <p>
     * <code> t = (yp - y0)/dy <\code>.<p>
     * If point does not belong to line, returns Double.NaN.
     */
    @Override
    public double position(Point2D point) {
        try {
            double pos = this.positionOnLine(point);

            // compute a threshold depending on line slope
            double eps = FastMath.hypot(dx, dy) * Tolerance2D.get();

            // return either pos or NaN
            if (pos < this.t0() - eps) {
                return Double.NaN;
            }
            if (pos > this.t1() + eps) {
                return Double.NaN;
            }
            return pos;
        } catch (DegeneratedLine2DException ex) {
            if (point.distance(x0, y0) < Tolerance2D.get()) {
                return t0();
            }
            return Double.NaN;
        }
    }

    /**
     * Returns the position of the closest point on the line arc. If the point
     * belongs to the line, this position is defined by the ratio:
     * <p>
     * <code> t = (xp - x0)/dx <\code>, or equivalently:
     * <p>
     * <code> t = (yp - y0)/dy <\code>.<p>
     * If point does not belong to line, returns t0, or t1, depending on which
     * one is the closest.
     */
    @Override
    public double project(Point2D point) {
        try {
            double pos = this.positionOnLine(point);
            // Bounds between t0 and t1
            return Math.min(Math.max(pos, this.t0()), this.t1());
        } catch (DegeneratedLine2DException ex) {
            return this.t0();
        }
    }

    /**
     * Returns a new AbstractLine2D, which is the portion of this AbstractLine2D
     * delimited by parameters t0 and t1.Casts the result to StraightLine2D,
     * Ray2D or LineSegment2D when appropriate.
     *
     * @param t0
     * @param t1
     * @return
     */
    @Override
    public AbstractLine2D subCurve(double t0, double t1) {
        // keep min and max of bounds
        t0 = Math.max(t0, this.t0());
        t1 = Math.min(t1, this.t1());

        // check for special cases
        if (Double.isInfinite(t1)) {
            if (Double.isInfinite(t0)) {
                return new StraightLine2D(this);
            } else {
                return new Ray2D(this.point(t0), this.direction());
            }
        }

        if (Double.isInfinite(t0)) {
            return new InvertedRay2D(this.point(t1), this.direction());
        } else {
            return new LineSegment2D(this.point(t0), this.point(t1));
        }

    }

    /**
     * Returns a collection of lines containing only this line.
     */
    @Override
    public Collection<? extends AbstractLine2D> continuousCurves() {
        return wrapCurve(this);
    }

    // ===================================================================
    // methods implementing the Shape2D
    /**
     * Returns the distance of the StraightObject2d to the given point.This
     * method is not designed to be used directly, because AbstractLine2D is an
     * abstract class, but it can be called by subclasses to help computations.
     *
     * @param p
     * @return
     */
    @Override
    public double distance(Point2D p) {
        return distance(p.x(), p.y());
    }

    /**
     * Returns the distance of the StraightObject2d to the given point. This
     * method is not designed to be used directly, because AbstractLine2D is an
     * abstract class, but it can be called by subclasses to help computations.
     *
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return distance between this object and the point (x,y)
     */
    @Override
    public double distance(double x, double y) {
        // first project on the line
        Point2D proj = projectedPoint(x, y);

        // if the line contains the projection, returns the distance
        if (contains(proj)) {
            return proj.distance(x, y);
        }

        // otherwise, returns the distance to the closest singular point
        double dist = Double.POSITIVE_INFINITY;
        if (!Double.isInfinite(t0())) {
            dist = firstPoint().distance(x, y);
        }
        if (!Double.isInfinite(t1())) {
            dist = Math.min(dist, lastPoint().distance(x, y));
        }
        return dist;
    }

    /* (non-Javadoc)
	 * @see math.geom2d.Shape2D#contains(Point2D)
     */
    @Override
    public boolean contains(Point2D p) {
        return this.contains(p.x(), p.y());
    }

    /**
     * Returns false, unless both dx and dy equal 0.
     */
    @Override
    public boolean isEmpty() {
        return FastMath.hypot(dx, dy) < Tolerance2D.get();
    }

    /* (non-Javadoc)
	 * @see math.geom2d.Shape2D#transform(AffineTransform2D)
     */
    @Override
    public abstract AbstractLine2D transform(AffineTransform2D transform);

    @Override
    public CurveSet2D<? extends AbstractLine2D> clip(Box2D box) {
        // Clip the curve
        CurveSet2D<ContinuousCurve2D> set = Curves2D.clipContinuousCurve(
                this, box);

        // Stores the result in appropriate structure
        CurveArray2D<AbstractLine2D> result
                = new CurveArray2D<>(set.size());

        // convert the result
        set.curves().stream().filter((curve) -> (curve instanceof AbstractLine2D)).forEachOrdered((curve) -> {
            result.add((AbstractLine2D) curve);
        });
        return result;
    }

}
