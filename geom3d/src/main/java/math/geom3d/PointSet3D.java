/**
 *
 */
package math.geom3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import math.geom2d.Tolerance2D;

import math.geom3d.transform.AffineTransform3D;

/**
 * @author dlegland
 */
public class PointSet3D implements Shape3D, Iterable<Point3D> {

    private final List<Point3D> points;

    public PointSet3D() {
        this.points = new ArrayList<>();
    }

    /**
     * Creates a new point set and allocate memory for storing the points.
     *
     * @param n the number of points to store
     */
    public PointSet3D(int n) {
        this.points = new ArrayList<>(n);
    }

    /**
     * Instances of Point3D are directly added, other Point are converted to
     * Point3D with the same location.
     *
     * @param points
     */
    public PointSet3D(Point3D[] points) {
        this.points = Arrays.asList(points);
    }

    /**
     * Points must be a collection of java.awt.Point. Instances of Point3D are
     * directly added, other Point are converted to Point3D with the same
     * location.
     *
     * @param points
     */
    public PointSet3D(Collection<? extends Point3D> points) {
        this.points = new ArrayList<>(points);
    }

    /**
     * Adds a new point to the set of point. If point is not an instance of
     * Point3D, a Point3D with same location is added instead of point.
     *
     * @param point
     */
    public void addPoint(Point3D point) {
        this.points.add(point);
    }

    /**
     * Add a series of points
     *
     * @param points an array of points
     */
    public void addPoints(Point3D[] points) {
        for (Point3D element : points) {
            this.addPoint(element);
        }
    }

    public void addPoints(Collection<Point3D> points) {
        this.points.addAll(points);
    }

    /**
     * Returns an iterator on the internal point collection.
     *
     * @return the collection of points
     */
    public Iterator<Point3D> getPoints() {
        return points.iterator();
    }

    public Point3D getPoint(int i) {
        return points.get(i);
    }

    /**
     * Removes all points of the set.
     */
    public void clearPoints() {
        this.points.clear();
    }

    /**
     * Returns the number of points in the set.
     *
     * @return the number of points
     */
    public int pointNumber() {
        return points.size();
    }

    // ===================================================================
    // methods implementing the Shape3D interface

    /*
     * (non-Javadoc)
     * 
     * @see math.geom3d.Shape3D#clip(math.geom3d.Box3D)
     */
//    @Override
//    public Shape3D clip(Box3D box) {
//        PointSet3D res = new PointSet3D(this.points.size());
//        Shape3D clipped;
//        for (Point3D point : points) {
//            clipped = point.clip(box);
//            if (clipped != null) {
//                res.addPoint(point);
//            }
//        }
//        return res;
//    }
    @Override
    public Box3D boundingBox() {
        double xmin = Double.MAX_VALUE;
        double ymin = Double.MAX_VALUE;
        double zmin = Double.MAX_VALUE;
        double xmax = -Double.MAX_VALUE;
        double ymax = -Double.MAX_VALUE;
        double zmax = -Double.MAX_VALUE;

        for (Point3D point : points) {
            xmin = Math.min(xmin, point.getX());
            ymin = Math.min(ymin, point.getY());
            zmin = Math.min(zmin, point.getZ());
            xmax = Math.max(xmax, point.getX());
            ymax = Math.max(ymax, point.getY());
            zmax = Math.max(zmax, point.getZ());
        }
        return new Box3D(xmin, xmax, ymin, ymax, zmin, zmax);
    }

    /*
     * (non-Javadoc)
     * 
     * @see math.geom3d.Shape3D#getDistance(math.geom3d.Point3D)
     */
    @Override
    public double distance(Point3D p) {
        return points.stream().parallel().mapToDouble(pa -> pa.distance(p)).min().orElse(Double.POSITIVE_INFINITY);
    }

    @Override
    public boolean contains(Point3D point) {
        return points.stream().anyMatch((p) -> (point.distance(p) < Tolerance2D.get()));
    }

    @Override
    public boolean isEmpty() {
        return points.isEmpty();
    }

    @Override
    public boolean isBounded() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see math.geom3d.Shape3D#transform(math.geom3d.AffineTransform3D)
     */
    @Override
    public Shape3D transform(AffineTransform3D trans) {
        PointSet3D res = new PointSet3D();
        points.forEach((point) -> {
            res.addPoint(point.transform(trans));
        });
        return res;
    }

    // ===================================================================
    // methods implementing the Iterable interface

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Point3D> iterator() {
        return points.iterator();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.points);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PointSet3D other = (PointSet3D) obj;
        return Objects.equals(this.points, other.points);
    }

    @Override
    public boolean almostEquals(GeometricObject3D obj, double eps) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
