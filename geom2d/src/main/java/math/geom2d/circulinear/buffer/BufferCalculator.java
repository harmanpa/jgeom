/**
 * File: 	BufferCalculator.java
 * Project: javageom-buffer
 *
 * Distributed under the LGPL License.
 *
 * Created: 4 janv. 2011
 */
package math.geom2d.circulinear.buffer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import math.geom2d.Point2D;
import math.geom2d.Tolerance2D;
import math.geom2d.circulinear.*;
import math.geom2d.conic.Circle2D;
import math.geom2d.curve.Curves2D;
import math.geom2d.line.StraightLine2D;
import math.geom2d.point.PointSet2D;

/**
 * Compute the buffer of a circulinear curve or domain, and gather some methods
 * for computing parallel curves.<p>
 * This class can be instantiated, but also contains a lot of static methods.
 * The default instance of BufferCalculator is accessible through the static
 * method 'getDefaultInstance'. The public constructor can be called if
 * different cap or join need to be specified.
 *
 * @author dlegland
 *
 */
public class BufferCalculator {

    // ===================================================================
    // static methods and variables
    private static BufferCalculator defaultInstance = null;

    /**
     * Returns the default instance of bufferCalculator.
     *
     * @return
     */
    public static BufferCalculator getDefaultInstance() {
        if (defaultInstance == null) {
            defaultInstance = new BufferCalculator();
        }
        return defaultInstance;
    }

    // ===================================================================
    // Class variables
    private final JoinFactory joinFactory;
    private final CapFactory capFactory;
    private final InternalCornerFactory internalCornerFactory;

    // ===================================================================
    // Constructors
    /**
     * Creates a new buffer calculator with default join and cap factories.
     */
    public BufferCalculator() {
        this(new RoundJoinFactory(), new RoundCapFactory(), new NullInternalCornerFactory());
    }

    /**
     * Creates a new buffer calculator with specific join and cap factories.
     *
     * @param joinFactory
     * @param capFactory
     */
    public BufferCalculator(JoinFactory joinFactory, CapFactory capFactory) {
        this(joinFactory, capFactory, new NullInternalCornerFactory());
    }

    /**
     * Creates a new buffer calculator with specific join, cap and internal
     * corner factories.
     *
     * @param joinFactory
     * @param capFactory
     * @param internalCornerFactory
     */
    public BufferCalculator(JoinFactory joinFactory, CapFactory capFactory, InternalCornerFactory internalCornerFactory) {
        this.joinFactory = joinFactory;
        this.capFactory = capFactory;
        this.internalCornerFactory = internalCornerFactory;
    }

    // ===================================================================
    // General methods
    /**
     * Computes the parallel curve of a circulinear curve (composed only of
     * pieces of lines and circles). The result is itself a circulinear curve.
     *
     * @param curve
     * @param dist
     * @return
     */
    public CirculinearCurve2D createParallel(
            CirculinearCurve2D curve, double dist) {

        // case of a continuous curve -> call specialized method
        if (curve instanceof CirculinearContinuousCurve2D) {
            return createContinuousParallel(
                    (CirculinearContinuousCurve2D) curve, dist);
        }

        // Create array for storing result
        CirculinearCurveArray2D<CirculinearContinuousCurve2D> parallels
                = new CirculinearCurveArray2D<>();

        // compute parallel of each continuous part, and add it to the result
        curve.continuousCurves().stream().map((continuous) -> createContinuousParallel(continuous, dist)).filter((contParallel) -> (contParallel != null)).forEachOrdered((contParallel) -> {
            parallels.add(contParallel);
        });

        // return the set of parallel curves
        return parallels;
    }

    public CirculinearBoundary2D createParallelBoundary(
            CirculinearBoundary2D boundary, double dist) {

        // in the case of a single contour, return the parallel of the contour
        if (boundary instanceof CirculinearContour2D) {
            return createParallelContour((CirculinearContour2D) boundary, dist);
        }

        // get the set of individual contours
        Collection<? extends CirculinearContour2D> contours
                = boundary.continuousCurves();

        // allocate the array of parallel contours
        Collection<CirculinearContour2D> parallelContours
                = new ArrayList<>(contours.size());

        // compute the parallel of each contour
        contours.forEach((contour) -> {
            parallelContours.add(contour.parallel(dist));
        });

        // Create an agglomeration of the curves
        return CirculinearContourArray2D.create(parallelContours.toArray(new CirculinearContour2D[0]));
    }

    public CirculinearContour2D createParallelContour(
            CirculinearContour2D contour, double dist) {

        // straight line is already a circulinear contour
        if (contour instanceof StraightLine2D) {
            return ((StraightLine2D) contour).parallel(dist);
        }
        // The circle is already a circulinear contour
        if (contour instanceof Circle2D) {
            return ((Circle2D) contour).parallel(dist);
        }

        // extract collection of parallel curves, that connect each other
        Collection<CirculinearElement2D> parallelCurves
                = getParallelElements(contour, dist);

        // Create a new boundary with the set of parallel curves
        return BoundaryPolyCirculinearCurve2D.create(parallelCurves.toArray(new CirculinearElement2D[0]),
                contour.isClosed());
    }

    /**
     * Compute the parallel curve of a Circulinear and continuous curve. The
     * result is itself an instance of CirculinearContinuousCurve2D.
     *
     * @param curve
     * @param dist
     * @return
     */
    public CirculinearContinuousCurve2D createContinuousParallel(
            CirculinearContinuousCurve2D curve, double dist) {

        // For circulinear elements, getParallel() is already implemented
        if (curve instanceof CirculinearElement2D) {
            return ((CirculinearElement2D) curve).parallel(dist);
        }

        // extract collection of parallel curves, that connect each other
        Collection<CirculinearElement2D> parallelCurves = getParallelElements(curve, dist);

        return PolyCirculinearCurve2D.create(parallelCurves.toArray(new CirculinearElement2D[0]), curve.isClosed());
    }

    protected Collection<CirculinearElement2D> getParallelElements(
            CirculinearContinuousCurve2D curve, double dist) {

        // extract collection of circulinear elements
        Iterator<? extends CirculinearElement2D> iterator = curve
                .smoothPieces().stream()
                .filter(sp -> !(sp instanceof PointElement2D))
                .iterator();

        // previous curve
        CirculinearElement2D previous;
        CirculinearElement2D current;

        // create array for storing result
        Deque<CirculinearElement2D> parallelCurves
                = new ArrayDeque<>();

        // check if curve is empty
        if (!iterator.hasNext()) {
            return parallelCurves;
        }

        // add parallel to the first curve
        current = iterator.next();
        CirculinearElement2D currentParallel = current.parallel(dist);
//        while (Math.pow(parallel.length(), 2) < Tolerance2D.get()) {
//            current = iterator.next();
//            parallel = current.parallel(dist);
//        }
        if (!(currentParallel instanceof PointElement2D)) {
            parallelCurves.add(currentParallel);
        }
        CirculinearElement2D first = current;

        // iterate on circulinear element couples
        CirculinearContinuousCurve2D join;
        CirculinearElement2D previousParallel;
        while (iterator.hasNext()) {
            // update the couple of circulinear elements
            previous = current;
            previousParallel = currentParallel;
            current = iterator.next();

            // create the parallel curve for the current curve
            currentParallel = current.parallel(dist);
//            while (currentParallel.length() < Tolerance2D.get() && iterator.hasNext()) {
//                current = iterator.next();
//                currentParallel = current.parallel(dist);
//            }

            // check if this is an internal corner
            boolean internalCorner = dist < 0 ? previous.isInside(current.point(0.01)) : !previous.isInside(current.point(0.01));

            // if it is an internal corner, check if the internalCornerFactory wishes to handle it. if not handle as normal.
            if (!internalCorner || !internalCornerFactory.createInternalCorner(parallelCurves, currentParallel)) {
                // add circle arc between the two curve elements
                // This is bollocks
                // Need the parallel lines. They might have a gap, they might intersect, or one might be the wrong side of the other.

                join = joinFactory.createJoin(previous, current, dist, previousParallel.lastPoint(), currentParallel.firstPoint());

                if (join.length() > 0) {
                    parallelCurves.addAll(join.smoothPieces());
                }

                // add parallel to set of parallels
                if (!(currentParallel instanceof PointElement2D)) {
                    parallelCurves.add(currentParallel);
                }
            }
        }

        // Add eventually a circle arc to close the parallel curve
        if (curve.isClosed() && !parallelCurves.isEmpty()) {
            double distance = current.lastPoint().distance(first.firstPoint());
            boolean reallyClosed = distance <= Tolerance2D.get();
            if (!reallyClosed) {
                System.out.println("Closing a buffer when the original curve wasn't closed");
            }
            
            previous = current;
            previousParallel = currentParallel;
            current = first;
            currentParallel = parallelCurves.getFirst();

            // check if this is an internal corner
            boolean internalCorner = dist < 0 ? previous.isInside(current.point(0.01)) : !previous.isInside(current.point(0.01));

            // if it is an internal corner, check if the internalCornerFactory wishes to handle it. if not handle as normal.
            if (internalCorner) {
                if (internalCornerFactory.createInternalCorner(parallelCurves, currentParallel)) {
                    parallelCurves.removeFirst();
                } else {
                    // add circle arc between the two curve elements
                    join = joinFactory.createJoin(previous, current, dist, previousParallel.lastPoint(), currentParallel.firstPoint());

                    if (join.length() > 0) {
                        parallelCurves.addAll(join.smoothPieces());
                    }
                }
            } else {
                // add circle arc between the two curve elements
                join = joinFactory.createJoin(previous, current, dist, previousParallel.lastPoint(), currentParallel.firstPoint());

                if (join.length() > 0) {
                    parallelCurves.addAll(join.smoothPieces());
                }
            }
        }

        return parallelCurves;
    }

    /**
     * Compute the buffer of a circulinear curve.<p>
     * The algorithm is as follow:
     * <ol>
     * <li> split the curve into a set of curves without self-intersections
     * <li> for each split curve, compute the contour of its buffer
     * <li> split self-intersecting contours into set of disjoint contours
     * <li> split all contour which intersect each other to disjoint contours
     * <li> remove contours which are too close from the original curve
     * <li> create a new domain with the final set of contours
     * </ol>
     *
     * @param curve
     * @param dist
     * @return
     */
    public CirculinearDomain2D computeBuffer(
            CirculinearCurve2D curve, double dist) {

        List<CirculinearContour2D> contours
                = new ArrayList<>();

        // iterate on all continuous curves
        for (CirculinearContinuousCurve2D cont : curve.continuousCurves()) {
            // split the curve into a set of non self-intersecting curves
            for (CirculinearContinuousCurve2D splitted
                    : CirculinearCurves2D.splitContinuousCurve(cont)) {
                // compute the rings composing the simple curve buffer
                contours.addAll(computeBufferSimpleCurve(splitted, dist));
            }
        }

        // split contours which intersect each others
        contours = new ArrayList<>(
                CirculinearCurves2D.splitIntersectingContours(contours));

        // Remove contours that cross or that are too close from base curve
        ArrayList<CirculinearContour2D> contours2
                = new ArrayList<>(contours.size());
        Collection<Point2D> intersects;
        Collection<Point2D> vertices;

        for (CirculinearContour2D contour : contours) {

            // do not keep contours which cross original curve
            intersects = CirculinearCurves2D.findIntersections(curve, contour);

            // remove intersection points that are vertices of the reference curve
            //vertices = curve.singularPoints();
            vertices = curve.vertices();
            intersects.removeAll(vertices);

            if (intersects.size() > 0 || contour.isEmpty()) {
                continue;
            }

            // check that vertices of contour are not too close from original
            // curve
            double distCurves
                    = getDistanceCurveSingularPoints(curve, contour);
            if (distCurves < dist - Tolerance2D.get()) {
                continue;
            }

            // keep the contours that meet the above conditions
            contours2.add(contour);
        }

        // All the rings are created, we can now create a new domain with the
        // set of rings
        return new GenericCirculinearDomain2D(
                CirculinearContourArray2D.create(contours2.toArray(new CirculinearContour2D[0])));
    }

    /**
     * Compute the buffer of a circulinear continuous non self-intersecting
     * curve.<p>
     * The algorithm is as follow:
     * <ol>
     * <li> compute the contour of the curve buffer
     * <li> split self-intersecting contours into set of disjoint contours
     * <li> split all contour which intersect each other to disjoint contours
     * <li> remove contours which are too close from the original curve
     * <li> create a new domain with the final set of contours
     * </ol>
     *
     * @param curve
     * @param dist
     * @return
     */
    public CirculinearDomain2D computeBufferNonIntersecting(
            CirculinearCurve2D curve, double dist) {

        List<CirculinearContour2D> contours
                = new ArrayList<>();

        // iterate on all continuous curves
        for (CirculinearContinuousCurve2D cont : curve.continuousCurves()) {
            // compute the rings composing the simple curve buffer
            contours.addAll(computeBufferSimpleCurve(cont, dist));
        }

        // split contours which intersect each others
        contours = new ArrayList<>(
                CirculinearCurves2D.splitIntersectingContours(contours));

        // Remove contours that cross or that are too close from base curve
        ArrayList<CirculinearContour2D> contours2
                = new ArrayList<>(contours.size());
        Collection<Point2D> intersects;
        Collection<Point2D> vertices;

        for (CirculinearContour2D contour : contours) {

            // do not keep contours which cross original curve
            intersects = CirculinearCurves2D.findIntersections(curve, contour);

            // remove intersection points that are vertices of the reference curve
            //vertices = curve.singularPoints();
            vertices = curve.vertices();
            intersects.removeAll(vertices);

            if (intersects.size() > 0 || contour.isEmpty()) {
                continue;
            }

            // check that vertices of contour are not too close from original
            // curve
            double distCurves
                    = getDistanceCurveSingularPoints(curve, contour);
            if (distCurves < dist - Tolerance2D.get()) {
                continue;
            }

            // keep the contours that meet the above conditions
            contours2.add(contour);
        }

        // All the rings are created, we can now create a new domain with the
        // set of rings
        return new GenericCirculinearDomain2D(
                CirculinearContourArray2D.create(contours2.toArray(new CirculinearContour2D[0])));
    }

    /**
     * Compute buffer of a point set.
     *
     * @param set
     * @param dist
     * @return
     */
    public CirculinearDomain2D computeBuffer(PointSet2D set,
            double dist) {
        // create array for storing result
        Collection<CirculinearContour2D> contours
                = new ArrayList<>(set.size());

        // for each point, add a new circle
        for (Point2D point : set) {
            contours.add(new Circle2D(point, Math.abs(dist), dist > 0));
        }

        // process circles to remove intersections
        contours = CirculinearCurves2D.splitIntersectingContours(contours);

        // Remove contours that cross or that are too close from base curve
        List<CirculinearContour2D> contours2
                = new ArrayList<>(contours.size());
        for (CirculinearContour2D ring : contours) {

            // check that vertices of contour are not too close from original
            // curve
            double minDist = CirculinearCurves2D.getDistanceCurvePoints(
                    ring, set.points());
            if (minDist < dist - Tolerance2D.get()) {
                continue;
            }

            // keep the contours that meet the above conditions
            contours2.add(ring);
        }

        return new GenericCirculinearDomain2D(
                CirculinearContourArray2D.create(contours2.toArray(new CirculinearContour2D[0])));
    }

    /**
     * Computes the buffer of a simple curve. This method should replace the
     * method 'computeBufferSimpleContour'.
     */
    private Collection<? extends CirculinearContour2D>
            computeBufferSimpleCurve(CirculinearContinuousCurve2D curve, double d) {

        Collection<CirculinearContour2D> contours
                = new ArrayList<>(2);

        // the parallel in each side
        CirculinearContinuousCurve2D parallel1, parallel2;
        parallel1 = createContinuousParallel(curve, d);
        parallel2 = createContinuousParallel(curve, -d).reverse();

        if (curve.isClosed()) {
            // each parallel is itself a contour
            contours.add(convertCurveToBoundary(parallel1));
            contours.add(convertCurveToBoundary(parallel2));
        } else {
            // create a new contour from the two parallels and 2 caps
            contours.addAll(createSingleContourFromTwoParallels(parallel1, parallel2));
        }

        // some contours may intersect, so we split them
        Collection<CirculinearContour2D> contours2
                = removeIntersectingContours(contours, curve, d);

        // Remove empty contours
        contours2.removeIf(c -> c.smoothPieces().isEmpty());

        // return the set of created contours
        return contours2;
    }

    /**
     * Creates the unique contour based on two parallels of the base curve, by
     * adding appropriate circle arcs at extremities of the base curve.
     */
    private Collection<CirculinearContour2D>
            createSingleContourFromTwoParallels(
                    CirculinearContinuousCurve2D curve1,
                    CirculinearContinuousCurve2D curve2) {

        // create array for storing result
        List<CirculinearContour2D> contours
                = new ArrayList<>();

        CirculinearContinuousCurve2D cap;

        // create new ring using two open curves and two circle arcs
        if (curve1 != null && curve2 != null) {
            // array of elements for creating new ring.
            List<CirculinearElement2D> elements
                    = new ArrayList<>();

            // some shortcuts for computing infinity of curve
            boolean b0 = !Curves2D.isLeftInfinite(curve1);
            boolean b1 = !Curves2D.isRightInfinite(curve1);

            if (b0 && b1) {
                // case of a curve finite at each extremity

                // extremity points
                Point2D p11 = curve1.firstPoint();
                Point2D p12 = curve1.lastPoint();
                Point2D p21 = curve2.firstPoint();
                Point2D p22 = curve2.lastPoint();

                // Check how to associate open curves and circle arcs
                elements.addAll(curve1.smoothPieces());
                cap = capFactory.createCap(p12, p21);
                elements.addAll(cap.smoothPieces());
                elements.addAll(curve2.smoothPieces());
                cap = capFactory.createCap(p22, p11);
                elements.addAll(cap.smoothPieces());

                // create the last ring
                contours.add(new GenericCirculinearRing2D(elements));

            } else if (!b0 && !b1) {
                // case of an infinite curve at both extremities
                // In this case, the two parallel curves do not join,
                // and are added as contours individually					
                contours.add(convertCurveToBoundary(curve1));
                contours.add(convertCurveToBoundary(curve2));

            } else if (b0 && !b1) {
                // case of a curve starting from infinity, and finishing
                // on a given point

                // extremity points
                Point2D p11 = curve1.firstPoint();
                Point2D p22 = curve2.lastPoint();

                // add elements of the new contour
                elements.addAll(curve2.smoothPieces());
                cap = capFactory.createCap(p22, p11);
                elements.addAll(cap.smoothPieces());
                elements.addAll(curve1.smoothPieces());

                // create the last ring
                contours.add(new GenericCirculinearRing2D(elements));

            } else if (b1 && !b0) {
                // case of a curve starting at a point and finishing at
                // the infinity

                // extremity points
                Point2D p12 = curve1.lastPoint();
                Point2D p21 = curve2.firstPoint();

                // add elements of the new contour
                elements.addAll(curve1.smoothPieces());
                cap = capFactory.createCap(p12, p21);
                elements.addAll(cap.smoothPieces());
                elements.addAll(curve2.smoothPieces());

                // create the last contour
                contours.add(new GenericCirculinearRing2D(elements));

            }
        }

        return contours;
    }

    private Collection<CirculinearContour2D> removeIntersectingContours(
            Collection<CirculinearContour2D> contours,
            CirculinearCurve2D curve, double d) {
        // prepare an array to store the set of rings
        List<CirculinearContour2D> contours2
                = new ArrayList<>();

        // iterate on the set of rings
        contours.forEach((contour) -> {
            CirculinearCurves2D.splitContinuousCurve(contour).forEach((splitted) -> {
                // compute distance to original curve
                // (assuming it is sufficient to compute distance to vertices
                // of the reference curve).
                double dist = CirculinearCurves2D.getDistanceCurvePoints(
                        curve, splitted.singularPoints());
                // check if distance condition is verified
                if (!(dist - d < -Tolerance2D.get())) {
                    // convert the set of elements to a Circulinear ring
                    contours2.add(convertCurveToBoundary(splitted));
                }
            });
        }); // split rings into curves which do not self-intersect

        // return the set of created rings
        return contours2;
    }

    /**
     * Converts the given continuous curve to an instance of
     * CirculinearContour2D. This can be the curve itself, a new instance of
     * GenericCirculinearRing2D if the curve is bounded, or a new instance of
     * BoundaryPolyCirculinearCurve2D if the curve is unbounded.
     */
    private CirculinearContour2D convertCurveToBoundary(
            CirculinearContinuousCurve2D curve) {
        // basic case: curve is already a contour
        if (curve instanceof CirculinearContour2D) {
            return (CirculinearContour2D) curve;
        }

        // if the curve is closed, return an instance of GenericCirculinearRing2D
        if (curve.isClosed()) {
            return GenericCirculinearRing2D.create(curve.smoothPieces().toArray(new CirculinearElement2D[0]));
        }

        return BoundaryPolyCirculinearCurve2D.create(curve.smoothPieces().toArray(new CirculinearContinuousCurve2D[0]));
    }

    private double getDistanceCurveSingularPoints(
            CirculinearCurve2D ref, CirculinearCurve2D curve) {
        // extract singular points
        Collection<Point2D> points = curve.singularPoints();

        // If no singular point, choose an arbitrary point on the curve
        if (points.isEmpty()) {
            points = new ArrayList<>();
            double t = Curves2D.choosePosition(curve.t0(), curve.t1());
            points.add(curve.point(t));
        }

        // Iterate on points to get minimal distance
        double minDist = Double.MAX_VALUE;
        for (Point2D point : points) {
            minDist = Math.min(minDist, ref.distance(point));
        }
        return minDist;
    }
}
