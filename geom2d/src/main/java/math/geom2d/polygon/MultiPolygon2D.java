package math.geom2d.polygon;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import math.geom2d.AffineTransform2D;
import math.geom2d.Box2D;
import math.geom2d.GeometricObject2D;
import math.geom2d.Point2D;
import math.geom2d.circulinear.CirculinearContourArray2D;
import math.geom2d.circulinear.CirculinearDomain2D;
import math.geom2d.circulinear.GenericCirculinearDomain2D;
import math.geom2d.domain.Boundary2D;
import math.geom2d.domain.Domain2D;
import math.geom2d.line.LineSegment2D;
import math.geom2d.transform.CircleInversion2D;

/**
 * A polygonal domain whose boundary is composed of several disjoint continuous
 * LinearRing2D.
 *
 * @author dlegland
 */
public class MultiPolygon2D implements Domain2D, Polygon2D {

    // ===================================================================
    // Static constructors
    public static MultiPolygon2D create(Collection<LinearRing2D> rings) {
        return new MultiPolygon2D(rings);
    }

    public static MultiPolygon2D create(LinearRing2D... rings) {
        return new MultiPolygon2D(rings);
    }

    // ===================================================================
    // class members
    ArrayList<LinearRing2D> rings = new ArrayList<>(1);

    // ===================================================================
    // Constructors
    public MultiPolygon2D() {
    }

    /**
     * Ensures the inner buffer has enough capacity for storing the required
     * number of rings.
     *
     * @param nRings
     */
    public MultiPolygon2D(int nRings) {
        this.rings.ensureCapacity(nRings);
    }

    public MultiPolygon2D(LinearRing2D... rings) {
        this.rings.addAll(Arrays.asList(rings));
    }

    public MultiPolygon2D(Polygon2D polygon) {
        if (polygon instanceof SimplePolygon2D) {
            rings.add(((SimplePolygon2D) polygon).getRing());
        } else {
            rings.addAll(polygon.boundary().curves());
        }
    }

    public MultiPolygon2D(Collection<LinearRing2D> lines) {
        rings.addAll(lines);
    }

    // ===================================================================
    // Management of rings
    public void addRing(LinearRing2D ring) {
        rings.add(ring);
    }

    public void insertRing(int index, LinearRing2D ring) {
        rings.add(index, ring);
    }

    public void removeRing(LinearRing2D ring) {
        rings.remove(ring);
    }

    public void clearRings() {
        rings.clear();
    }

    public LinearRing2D getRing(int index) {
        return rings.get(index);
    }

    public void setRing(int index, LinearRing2D ring) {
        rings.set(index, ring);
    }

    public int ringNumber() {
        return rings.size();
    }

    // ===================================================================
    // methods implementing the Polygon2D interface
    /**
     * Computes the signed area of the polygon.
     *
     * @return the signed area of the polygon.
     * @since 0.9.1
     */
    @Override
    public double area() {
        return Polygons2D.computeArea(this);
    }

    /**
     * Computes the centroid (center of mass) of the polygon.
     *
     * @return the centroid of the polygon
     * @since 0.9.1
     */
    @Override
    public Point2D centroid() {
        return Polygons2D.computeCentroid(this);
    }

    @Override
    public Collection<LineSegment2D> edges() {
        int nEdges = edgeNumber();
        ArrayList<LineSegment2D> edges = new ArrayList<LineSegment2D>(nEdges);
        rings.forEach((ring) -> {
            edges.addAll(ring.edges());
        });
        return edges;
    }

    @Override
    public int edgeNumber() {
        int count = 0;
        count = rings.stream().map((ring) -> ring.vertexNumber()).reduce(count, Integer::sum);
        return count;
    }

    @Override
    public Collection<Point2D> vertices() {
        int nv = vertexNumber();
        ArrayList<Point2D> points = new ArrayList<>(nv);
        rings.forEach((ring) -> {
            points.addAll(ring.vertices());
        });
        return points;
    }

    /**
     * Returns the i-th vertex of the polygon.
     *
     * @param i index of the vertex, between 0 and the number of vertices minus
     * one
     * @return
     */
    @Override
    public Point2D vertex(int i) {
        int count = 0;
        LinearRing2D boundary = null;

        for (LinearRing2D ring : rings) {
            int nv = ring.vertexNumber();
            if (count + nv > i) {
                boundary = ring;
                break;
            }
            count += nv;
        }

        if (boundary == null) {
            throw new IndexOutOfBoundsException();
        }

        return boundary.vertex(i - count);
    }

    /**
     * Sets the position of the i-th vertex of this polygon.
     *
     * @param i index of the vertex, between 0 and the number of vertices
     */
    @Override
    public void setVertex(int i, Point2D point) {
        int count = 0;
        LinearRing2D boundary = null;

        for (LinearRing2D ring : rings) {
            int nv = ring.vertexNumber();
            if (count + nv > i) {
                boundary = ring;
                break;
            }
            count += nv;
        }

        if (boundary == null) {
            throw new IndexOutOfBoundsException();
        }

        boundary.setVertex(i - count, point);
    }

    /**
     * Adds a vertex at the end of the last ring of this polygon.
     *
     * @param position
     */
    @Override
    public void addVertex(Point2D position) {
        // get the last ring
        if (rings.isEmpty()) {
            return;
        }
        LinearRing2D ring = rings.get(rings.size() - 1);
        ring.addVertex(position);
    }

    /**
     * Inserts a vertex at the given position
     *
     * @throws IllegalArgumentException if index is not smaller than vertex
     * number
     */
    @Override
    public void insertVertex(int index, Point2D point) {
        // check number of rings
        if (rings.isEmpty()) {
            return;
        }

        // Check number of vertices
        int nv = this.vertexNumber();
        if (nv <= index) {
            throw new IllegalArgumentException("Can not insert vertex at position "
                    + index + " (max is " + nv + ")");
        }

        // Find the ring that correspond to index
        int count = 0;
        LinearRing2D boundary = null;

        for (LinearRing2D ring : rings) {
            nv = ring.vertexNumber();
            if (count + nv > index) {
                boundary = ring;
                break;
            }
            count += nv;
        }

        if (boundary == null) {
            throw new IndexOutOfBoundsException();
        }

        boundary.insertVertex(index - count, point);
    }

    /**
     * Returns the i-th vertex of the polygon.
     *
     * @param i index of the vertex, between 0 and the number of vertices minus
     * one
     */
    @Override
    public void removeVertex(int i) {
        int count = 0;
        LinearRing2D boundary = null;

        for (LinearRing2D ring : rings) {
            int nv = ring.vertexNumber();
            if (count + nv > i) {
                boundary = ring;
                break;
            }
            count += nv;
        }

        if (boundary == null) {
            throw new IndexOutOfBoundsException();
        }

        boundary.removeVertex(i - count);
    }

    /**
     * Returns the total number of vertices in this polygon. The total number is
     * computed as the sum of vertex number in each ring of the polygon.
     *
     * @return
     */
    @Override
    public int vertexNumber() {
        int count = 0;
        count = rings.stream().map((ring) -> ring.vertexNumber()).reduce(count, Integer::sum);
        return count;
    }

    /**
     * Computes the index of the closest vertex to the input point.
     *
     * @param point
     * @return
     */
    @Override
    public int closestVertexIndex(Point2D point) {
        double minDist = Double.POSITIVE_INFINITY;
        int index = -1;

        int i = 0;
        for (LinearRing2D ring : this.rings) {
            for (Point2D vertex : ring.vertices()) {
                double dist = vertex.distance(point);
                if (dist < minDist) {
                    index = i;
                    minDist = dist;
                }
                i++;
            }

        }

        return index;
    }

    // ===================================================================
    // methods implementing the Domain2D interface

    /* (non-Javadoc)
	 * @see math.geom2d.circulinear.CirculinearDomain2D#transform(math.geom2d.transform.CircleInversion2D)
     */
    @Override
    public CirculinearDomain2D transform(CircleInversion2D inv) {
        return new GenericCirculinearDomain2D(
                this.boundary().transform(inv).reverse());
    }

    /* (non-Javadoc)
	 * @see math.geom2d.circulinear.CirculinearShape2D#buffer(double)
     */
    @Override
    public CirculinearDomain2D buffer(double dist) {
        return Polygons2D.createBuffer(this, dist);
    }

    /* (non-Javadoc)
	 * @see math.geom2d.domain.Domain2D#asPolygon(int)
     */
    @Override
    public Polygon2D asPolygon(int n) {
        return this;
    }

    @Override
    public CirculinearContourArray2D<LinearRing2D> boundary() {
        return CirculinearContourArray2D.create(rings.toArray(new LinearRing2D[0]));
    }

    /* (non-Javadoc)
	 * @see math.geom2d.domain.Domain2D#contours()
     */
    @Override
    public Collection<LinearRing2D> contours() {
        return Collections.unmodifiableList(rings);
    }

    @Override
    public Polygon2D complement() {
        // allocate memory for array of reversed rings
        ArrayList<LinearRing2D> reverseLines
                = new ArrayList<>(rings.size());

        // reverse each ring
        rings.forEach((ring) -> {
            reverseLines.add(ring.reverse());
        });

        // create the new MultiMpolygon2D with set of reversed rings
        return new MultiPolygon2D(reverseLines);
    }

    // ===================================================================
    // methods inherited from interface Shape2D
    @Override
    public Box2D boundingBox() {
        // start with empty bounding box
        Box2D box = new Box2D(
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);

        // compute union of all bounding boxes
        for (LinearRing2D ring : this.rings) {
            box = box.union(ring.boundingBox());
        }

        // return result
        return box;
    }

    /**
     * Clips the polygon with the specified box.
     *
     * @param box
     * @return
     */
    @Override
    public Polygon2D clip(Box2D box) {
        return Polygons2D.clipPolygon(this, box);
    }

    @Override
    public double distance(Point2D p) {
        return Math.max(this.boundary().signedDistance(p), 0);
    }

    @Override
    public double distance(double x, double y) {
        return Math.max(this.boundary().signedDistance(x, y), 0);
    }

    @Override
    public boolean isBounded() {
        // If boundary is not bounded, the polygon is not
        Boundary2D boundary = this.boundary();
        if (!boundary.isBounded()) {
            return false;
        }

        // Computes the signed area
        double area = 0;
        area = rings.stream().map((ring) -> ring.area()).reduce(area, (accumulator, _item) -> accumulator + _item);

        // bounded if positive area
        return area > 0;
    }

    /**
     * The MultiPolygon2D is empty either if it contains no ring, or if all
     * rings are empty.
     */
    @Override
    public boolean isEmpty() {
        // return true if at least one ring is not empty
        if (!rings.stream().noneMatch((ring) -> (!ring.isEmpty()))) {
            return false;
        }
        return true;
    }

    @Override
    public MultiPolygon2D transform(AffineTransform2D trans) {
        // allocate memory for transformed rings
        ArrayList<LinearRing2D> transformed
                = new ArrayList<>(rings.size());

        // transform each ring
        rings.forEach((ring) -> {
            transformed.add(ring.transform(trans));
        });

        // creates a new MultiPolygon2D with the set of trasnformed rings
        return new MultiPolygon2D(transformed);
    }

    @Override
    public boolean contains(Point2D point) {
        double angle = 0;
        angle = this.rings.stream().map((ring) -> ring.windingAngle(point)).reduce(angle, (accumulator, _item) -> accumulator + _item);

        double area = this.area();
        if (area > 0) {
            return angle > Math.PI;
        } else {
            return angle > -Math.PI;
        }
    }

    @Override
    public boolean contains(double x, double y) {
        return this.contains(new math.geom2d.Point2D(x, y));
    }

    @Override
    public void draw(Graphics2D g2) {
        g2.draw(this.boundary().getGeneralPath());
    }

    @Override
    public void fill(Graphics2D g) {
        g.fill(this.boundary().getGeneralPath());
    }

    // ===================================================================
    // methods implementing the GeometricObject2D interface

    /* (non-Javadoc)
	 * @see math.geom2d.GeometricObject2D#almostEquals(math.geom2d.GeometricObject2D, double)
     */
    @Override
    public boolean almostEquals(GeometricObject2D obj, double eps) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof MultiPolygon2D)) {
            return false;
        }
        MultiPolygon2D polygon = (MultiPolygon2D) obj;

        // check if the two objects have same number of rings
        if (polygon.rings.size() != this.rings.size()) {
            return false;
        }

        // check each couple of ring
        for (int i = 0; i < rings.size(); i++) {
            if (!this.rings.get(i).almostEquals(polygon.rings.get(i), eps)) {
                return false;
            }
        }

        return true;
    }

    // ===================================================================
    // methods overriding the Object class
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof MultiPolygon2D)) {
            return false;
        }

        // check if the two objects have same number of rings
        MultiPolygon2D polygon = (MultiPolygon2D) obj;
        if (polygon.rings.size() != this.rings.size()) {
            return false;
        }

        // check each couple of ring
        for (int i = 0; i < rings.size(); i++) {
            if (!this.rings.get(i).equals(polygon.rings.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.rings);
        return hash;
    }
}
