/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package math.geom2d.math;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import math.geom2d.Angle2D;
import math.geom2d.Box2D;
import math.geom2d.Point2D;
import math.geom2d.Tolerance2D;
import math.geom2d.Vector2D;
import math.geom2d.circulinear.CirculinearContinuousCurve2D;
import math.geom2d.circulinear.CirculinearCurve2D;
import math.geom2d.circulinear.CirculinearCurves2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.CirculinearRing2D;
import math.geom2d.circulinear.GenericCirculinearRing2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.conic.CircleArc2D;
import math.geom2d.conic.CircularShape2D;
import math.geom2d.line.LineSegment2D;
import math.geom2d.line.LinearShape2D;
import math.geom2d.line.Ray2D;
import math.geom2d.line.StraightLine2D;
import math.geom2d.polygon.MultiPolygon2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.Polygons2D;
import math.geom2d.polygon.SimplePolygon2D;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.events.EventHandler;
import org.apache.commons.math3.ode.nonstiff.DormandPrince54Integrator;
import org.jgrapht.Graph;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.alg.cycle.DirectedSimpleCycles;
import org.jgrapht.alg.cycle.JohnsonSimpleCycles;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphBuilder;
import org.jgrapht.graph.builder.GraphTypeBuilder;

/**
 *
 * @author peter
 */
public class Rings2D {

    public static <T extends Supplier<CirculinearCurve2D>> Graph<T, DefaultEdge> getFaceHierarchy(Class<T> type, List<T> faces, double tolerance) {
        // Build graph
        GraphBuilder<T, DefaultEdge, Graph<T, DefaultEdge>> builder = GraphTypeBuilder.directed()
                .vertexClass(type)
                .edgeClass(DefaultEdge.class)
                .allowingMultipleEdges(false)
                .allowingSelfLoops(false)
                .buildGraphBuilder();
        Set<T> faceSet = Sets.newHashSet(faces);
        faceSet.forEach(builder::addVertex);
        if (faceSet.size() >= 2) {
            Sets.combinations(faceSet, 2).forEach(facePairSet -> {
                T[] pair = Iterables.toArray(facePairSet, type);
                CirculinearCurve2D outer = pair[0].get();
                CirculinearCurve2D inner = pair[1].get();
                ContainedState state = getState(outer, inner, tolerance);
                switch (state) {
                    case Contained:
                        builder.addEdge(pair[0], pair[1], new DefaultEdge());
                        break;
                    case Inverted:
                        builder.addEdge(pair[1], pair[0], new DefaultEdge());
                        break;
                    case Overlapping:
                        double area1 = area(outer);
                        double area2 = area(inner);
                        double unionArea = union(outer, inner, tolerance).stream().mapToDouble(u -> area(u)).sum();
                        // If the areas summed is closer then next to each other, else contained
                        if (Math.abs(area1 + area2 - unionArea) > Math.abs(Math.max(area1, area2) - unionArea)) {
                            if (area1 > area2) {
                                builder.addEdge(pair[0], pair[1], new DefaultEdge());
                            } else {
                                builder.addEdge(pair[1], pair[0], new DefaultEdge());
                            }
                        }
                        break;
                    case Equal:
                    case None:
                }
            });
        }
        Graph<T, DefaultEdge> g = builder.build();
        // Remove transitive edges
        TransitiveReduction.INSTANCE.reduce(g);
        return g;
    }

    public static boolean isPolygon(CirculinearCurve2D curve) {
        return curve.continuousCurves().stream().map(cc -> cc.smoothPieces().stream().filter(sp -> !(sp instanceof LineSegment2D)).count()).reduce((a, b) -> a + b).get() == 0;
    }

    public static CirculinearCurve2D rotate(CirculinearCurve2D curve, double t) {
        if (t < curve.t0() + Tolerance2D.get() || t > curve.t1() - Tolerance2D.get()) {
            return curve;
        }
        List<CirculinearContinuousCurve2D> curves = new ArrayList<>();
        curves.addAll(curve.subCurve(t, curve.t1()).continuousCurves());
        curves.addAll(curve.subCurve(curve.t0(), t).continuousCurves());
        return makeRing(curves);
    }

    public static CirculinearCurve2D rotate(CirculinearCurve2D curve, Point2D closeTo) {
        return rotate(curve, curve.project(closeTo));
    }

    public static Polygon2D toPolygon(CirculinearCurve2D curve) {
        Deque<Point2D> points = new ArrayDeque<>();
        new LineArcIterator(curve) {
            @Override
            public void handleLine(LineSegment2D line) {
                if (!line.firstPoint().almostEquals(points.peekLast(), Tolerance2D.get())) {
                    points.add(line.firstPoint());
                }
                points.add(line.lastPoint());
            }

            @Override
            public void handleArc(CircleArc2D arc) {
                arc.asPolyline(2).edges().forEach(edge -> handleLine(edge));
            }
        }.iterate();
        return new SimplePolygon2D(points);
    }

    public static List<CirculinearCurve2D> fromPolygon(Polygon2D polygon) {
        List<CirculinearCurve2D> out = new ArrayList<>();
        if (polygon instanceof MultiPolygon2D) {
            ((MultiPolygon2D) polygon).contours().forEach(p -> out.addAll(fromPolygon(new SimplePolygon2D(p))));
        } else if (polygon != null) {
            if (polygon.area() > 1e-3) {
                Deque<CirculinearElement2D> elements = new ArrayDeque<>();
                polygon.edges().forEach(e -> elements.add(new LineSegment2D(e.firstPoint(), e.lastPoint())));
                out.add(new GenericCirculinearRing2D(elements));
            }
        }
        return out;
    }

    public static double area(CirculinearCurve2D curve) {
        return new GenericCirculinearRing2D(curve).area();
//        if (isPolygon(curve)) {
//            return Math.abs(toPolygon(curve).area());
//        }
//        return integrateArea(curve);
    }

    static double integrateArea(CirculinearCurve2D curve) {
        Box2D bounds = curve.boundingBox();
        FirstOrderIntegrator integrator = new DormandPrince54Integrator(1e-7, 1e-3, 1e-6, 1e-5);
        double[] area = new double[]{0};
        integrator.integrate(new FirstOrderDifferentialEquations() {
            @Override
            public int getDimension() {
                return 1;
            }

            @Override
            public void computeDerivatives(double t, double[] y, double[] yDot) throws MaxCountExceededException, DimensionMismatchException {
                yDot[0] = intersectionHeight(curve, t, bounds.getMinY());
            }
        }, bounds.getMinX(), area, bounds.getMaxX(), area);
        return area[0];
    }

    static double intersectionHeight(CirculinearCurve2D curve, double x, double yMin) {
        Collection<Point2D> points = curve.intersections(new Ray2D(x, yMin, 0, 1));
        double height = 0;
        if (points.isEmpty() || points.size() == 1) {
            return height;
        }
        if (points.size() % 2 == 0) {
            Iterator<Point2D> it = points.iterator();
            while (it.hasNext()) {
                Point2D a = it.next();
                Point2D b = it.next();
                height += Math.abs(a.getY() - b.getY());
            }
        }
        // Special case - if odd number but more than 1 it is an internal feature, just get the overall height
        double maxY = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        for (Point2D p : points) {
            maxY = Math.max(maxY, p.getY());
            minY = Math.min(minY, p.getY());
        }
        return maxY - minY;
    }

    public static Point2D findPointInside(CirculinearCurve2D curve) {
        Box2D bounds = curve.boundingBox();
        if (bounds.getMaxX() <= bounds.getMinX()) {
            throw new RuntimeException("Can't find internal point");
        }
        PrimitiveIterator.OfDouble randoms = new Random().doubles(bounds.getMinX(), bounds.getMaxX()).iterator();
        Collection<Point2D> points = curve.intersections(new Ray2D(randoms.nextDouble(), bounds.getMinY(), 0, 1));
        int n = 0;
        while (points.isEmpty() || points.size() % 2 != 0) {
            points = curve.intersections(new Ray2D(randoms.nextDouble(), bounds.getMinY(), 0, 1));
            n++;
            if (n > 100) {
                throw new RuntimeException("Can't find internal point");
            }
        }
        Iterator<Point2D> it = points.stream().sorted((p1, p2) -> (int) Math.signum(p2.getY() - p1.getY())).iterator();
        Point2D a = it.next();
        Point2D b = it.next();
        return new Point2D(a.getX(), b.getY() + (a.getY() - b.getY()) / 2);
    }

    public static boolean isClosed(CirculinearCurve2D curve) {
        return curve.lastPoint().distance(curve.firstPoint()) <= Tolerance2D.get();
    }

    public static CirculinearRing2D makeRing(CirculinearCurve2D curve) {
        return makeRing(curve.continuousCurves());
    }

    public static CirculinearRing2D makeRing(Collection<? extends CirculinearContinuousCurve2D> curve) {
        List<CirculinearElement2D> elements = new ArrayList<>();
        curve.forEach(cc -> elements.addAll(cc.smoothPieces()));
        elements.removeIf(e -> {
            if (e instanceof CircleArc2D) {
                return (elements.size() > 1 && e.firstPoint().distance(e.lastPoint()) < Tolerance2D.get())
                        || ((CircleArc2D) e).supportingCircle().radius() < Tolerance2D.get();
            }
            return e.length() < Tolerance2D.get();
        });
        return new GenericCirculinearRing2D(elements);
    }

    public static boolean isClockwise(CirculinearCurve2D curve) {
        return toPolygon(curve).area() < 0;
    }

    public static boolean isPointInside(CirculinearCurve2D curve, Point2D point) {
        return Polygons2D.windingNumber(toPolygon(curve).vertices(), point) != 0;
    }

    public static CirculinearCurve2D ensureClockwise(CirculinearCurve2D curve) {
        if (isClockwise(curve)) {
            return curve;
        }
        return curve.reverse();
    }

    public static CirculinearCurve2D ensureCounterClockwise(CirculinearCurve2D curve) {
        if (!isClockwise(curve)) {
            return curve;
        }
        return curve.reverse();
    }

    public static List<CirculinearCurve2D> union(List<CirculinearCurve2D> curves, double tolerance) {
        //System.out.println("Union of " + curves.size() + " curves");
        switch (curves.size()) {
            case 0:
            case 1:
                return curves;
            case 2:
                return union(curves.get(0), curves.get(1), tolerance);
            default:
                // Chessboard
                List<CirculinearCurve2D> out = new ArrayList<>();
                for (int column = 0; column < curves.size(); column++) {
                    for (int row = column + 1; row < curves.size(); row++) {
                        List<CirculinearCurve2D> result = union(curves.get(column), curves.get(row), tolerance);
                        if (result.size() <= 1) {
                            List<CirculinearCurve2D> newCurves = new ArrayList<>();
                            // If a single result returned, it was a valid union
                            // If nothing returned then the curves were probably empty
                            if (result.size() == 1) {
                                newCurves.add(result.get(0));
                            }
                            // Add any curves that didn't form part of the new union
                            for (int other = 0; other < curves.size(); other++) {
                                if (other != column && other != row) {
                                    newCurves.add(curves.get(other));
                                }
                            }
                            // Ensure that those marked as not-intersecting are not included
                            newCurves.removeAll(out);
                            // Union the remaining curves
                            out.addAll(union(newCurves, tolerance));
                            return out;
                        }
                    }
                    // This means that this curve does not intersect with any others of the list and will be retained
                    out.add(curves.get(column));
                }
                return out;
        }
    }

    public static List<CirculinearCurve2D> difference(CirculinearCurve2D curve1, List<CirculinearCurve2D> curves, double tolerance) {
        CirculinearCurve2D target = curve1;
        for (int i = 0; i < curves.size(); i++) {
            List<CirculinearCurve2D> result = difference(target, curves.get(i), tolerance);
            switch (result.size()) {
                case 0:
                    return Collections.emptyList();
                case 1:
                    target = result.get(0);
                    break;
                default:
                    List<CirculinearCurve2D> out = new ArrayList<>();
                    for (CirculinearCurve2D subresult : result) {
                        out.addAll(difference(subresult, curves.subList(i + 1, curves.size()), tolerance));
                    }
                    return out;
            }
        }
        return Collections.singletonList(target);
    }

    public static List<CirculinearCurve2D> union(CirculinearCurve2D curve1, CirculinearCurve2D curve2, double tolerance) {
        return combine(curve1, curve2, true, tolerance);
    }

    public static List<CirculinearCurve2D> difference(CirculinearCurve2D curve1, CirculinearCurve2D curve2, double tolerance) {
        return combine(curve1, curve2, false, tolerance);
    }

    static boolean isEmpty(CirculinearCurve2D curve) {
        if (curve.isEmpty()) {
            return true;
        }
        Box2D bounds = curve.boundingBox();
        return bounds.getWidth() < 0 || bounds.getHeight() < 0;
    }

    static List<CirculinearCurve2D> combine(CirculinearCurve2D curve1, CirculinearCurve2D curve2, boolean union, double tolerance) {
        if (isEmpty(curve1)) {
            if (union) {
                if (isEmpty(curve2)) {
                    return Collections.emptyList();
                } else {
                    return Collections.singletonList(curve2);
                }
            } else {
                return Collections.emptyList();
            }
        }
        if (isEmpty(curve2)) {
            return Collections.singletonList(curve1);
        }
        if (curve1.almostEquals(curve2, tolerance)) {
            if (union) {
                return Collections.singletonList(curve1);
            } else {
                return Collections.emptyList();
            }
        }
        List<Point2D> points = haveParallelElements(curve1, curve2, tolerance)
                ? iterativeIntersections(curve2, curve1, tolerance)
                : new ArrayList<>(CirculinearCurves2D.findIntersections(curve1, curve2));
        if (isContained(curve1, curve2, points)) {
            if (union) {
                return Collections.singletonList(curve1);
            } else {
                return Collections.singletonList(curve1);
            }
        }
        if (isContained(curve2, curve1, points)) {
            if (union) {
                return Collections.singletonList(curve2);
            } else {
                return Collections.emptyList();
            }
        }
        if (isNonOverlapping(curve1, curve2, points)) {
            if (union) {
                return Arrays.asList(curve1, curve2);
            } else {
                return Collections.singletonList(curve1);
            }
        }
        if (isPolygon(curve1) && isPolygon(curve2)) {
            if (union) {
                Polygon2D result = Polygons2D.union(toPolygon(curve1), toPolygon(curve2));
                return fromPolygon(result);
            } else {
                Polygon2D result = Polygons2D.difference(toPolygon(curve1), toPolygon(curve2));
                return fromPolygon(result);
            }
        }
        if (union) {
            List<CirculinearCurve2D> loops = loopCurves(ensureClockwise(curve1), ensureClockwise(curve2), points, tolerance);
            return Collections.singletonList(loops.stream().filter(c -> !isEmpty(c)
                            && isClockwise(c))
                    .sorted((c1, c2) -> (int) Math.signum(c2.length() - c1.length())).findFirst().get());
        } else {
            List<CirculinearCurve2D> loops = loopCurves(ensureClockwise(curve1), ensureCounterClockwise(curve2), points, tolerance);
            return loops.stream().filter(c -> !isEmpty(c)
                    && !hasParallelElements(c, tolerance)
                    && isClockwise(c)
                    && !c.almostEquals(curve1, tolerance)).collect(Collectors.toList());
        }
    }

    static List<List<IntersectionOrCurve>> loops(CirculinearCurve2D curve1, CirculinearCurve2D curve2, Collection<Point2D> intersections, double tolerance) {
        if (intersections.size() < 2) {
            return Collections.emptyList();
        }
        DefaultDirectedGraph<IntersectionOrCurve, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        // Add each intersection
        intersections.forEach(i -> graph.addVertex(new IntersectionOrCurve(i)));
        // Add curves
        buildGraph(graph, curve1, intersections, true, tolerance);
        buildGraph(graph, curve2, intersections, false, tolerance);
        // Build and analyse graph        
        DirectedSimpleCycles<IntersectionOrCurve, DefaultEdge> simpleCycles = new JohnsonSimpleCycles<>(graph);
        return simpleCycles.findSimpleCycles();
    }

    static double position(CirculinearCurve2D curve, Point2D point) {
        double res = curve.position(point);
        if (Double.isNaN(res)) {
            res = curve.project(point);
        }
        return res;
    }

    static void buildGraph(DefaultDirectedGraph<IntersectionOrCurve, DefaultEdge> graph, CirculinearCurve2D curve, Collection<Point2D> intersections, boolean firstCurve, double tolerance) {
        // Sort the intersections in rising order for curve
        List<Point2D> intersections1 = new ArrayList<>(intersections);
        Collections.sort(intersections1, (p1, p2) -> (int) Math.signum(position(curve, p1) - position(curve, p2)));

        // Add curve segments
        for (int i = 0; i < intersections1.size() - 1; i++) {
            Point2D p1 = intersections1.get(i);
            Point2D p2 = intersections1.get(i + 1);

            //System.out.println("Adding from " + position(curve, p1) + " to " + position(curve, p2));
            List<CirculinearContinuousCurve2D> segments = new ArrayList<>(curve.subCurve(position(curve, p1), position(curve, p2)).continuousCurves());
            IntersectionOrCurve v = new IntersectionOrCurve(segments, firstCurve);
            if (graph.addVertex(v)) {
                graph.addEdge(new IntersectionOrCurve(p1), v, new DefaultEdge());
                graph.addEdge(v, new IntersectionOrCurve(p2), new DefaultEdge());
            }
        }
        List<CirculinearContinuousCurve2D> segments = new ArrayList<>();
        Point2D p1 = intersections1.get(intersections1.size() - 1);
        Point2D p2 = intersections1.get(0);
        if (curve.length(curve.t1()) - curve.length(position(curve, p1)) > tolerance) {
            //System.out.println("Adding from " + position(curve, p1) + " to " + curve.t1());
            segments.addAll(curve.subCurve(position(curve, p1), curve.t1()).continuousCurves());
        }
        if (curve.length(position(curve, p2)) - curve.length(curve.t0()) > tolerance) {
            //System.out.println("Adding from " + curve.t0() + " to " + position(curve, p2));
            segments.addAll(curve.subCurve(curve.t0(), position(curve, p2)).continuousCurves());
        }
        IntersectionOrCurve v = new IntersectionOrCurve(segments, firstCurve);
        if (graph.addVertex(v)) {
            graph.addEdge(new IntersectionOrCurve(p1), v, new DefaultEdge());
            graph.addEdge(v, new IntersectionOrCurve(p2), new DefaultEdge());
        }
    }

    static List<CirculinearCurve2D> loopCurves(CirculinearCurve2D curve1, CirculinearCurve2D curve2, Collection<Point2D> intersections, double tolerance) {
        List<CirculinearCurve2D> out = new ArrayList<>();
        for (List<IntersectionOrCurve> loop : loops(curve1, curve2, intersections, tolerance)) {
            List<CirculinearContinuousCurve2D> segments = new ArrayList<>();
            boolean isAlternating = true;
            boolean isValid = true;
            IntersectionOrCurve previous = null;
            for (IntersectionOrCurve vertex : loop) {
                if (vertex.getCurves() != null && !vertex.getCurves().isEmpty()) {
                    if (previous != null) {
                        isAlternating = isAlternating && (vertex.isFirstCurve() != previous.isFirstCurve());
                        if (isValid) {
                            CirculinearContinuousCurve2D lastSegment = previous.curves.get(previous.curves.size() - 1);
                            double dot = lastSegment.leftTangent(lastSegment.t1()).dot(vertex.curves.get(0).rightTangent(vertex.curves.get(0).t0()));
                            //System.out.println("Dot product " + dot);
                            isValid = Math.abs(dot + 1) >= 6e-4; // This equates to 2 degrees
                        }
                    }
                    segments.addAll(vertex.getCurves());
                    previous = vertex;
                }
            }
            if (isAlternating && isValid) {
                CirculinearRing2D ring = makeRing(segments);
                out.add(ring);
            }
        }
        return out;
    }

    static class IntersectionOrCurve {

        private final Point2D intersection;
        private final List<CirculinearContinuousCurve2D> curves;
        private final boolean firstCurve;

        IntersectionOrCurve(Point2D intersection) {
            this.intersection = intersection;
            this.curves = null;
            this.firstCurve = false;
        }

        IntersectionOrCurve(List<CirculinearContinuousCurve2D> curves, boolean firstCurve) {
            this.intersection = null;
            this.curves = curves;
            this.firstCurve = firstCurve;
        }

        public Point2D getIntersection() {
            return intersection;
        }

        public List<CirculinearContinuousCurve2D> getCurves() {
            return curves;
        }

        public boolean isFirstCurve() {
            return firstCurve;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.intersection);
            hash = 97 * hash + Objects.hashCode(this.curves);
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
            final IntersectionOrCurve other = (IntersectionOrCurve) obj;
            if (!Objects.equals(this.intersection, other.intersection)) {
                return false;
            }
            return Objects.equals(this.curves, other.curves);
        }

    }

    public enum ContainedState {
        Contained, Inverted, Overlapping, Equal, None
    }

    public static ContainedState getState(CirculinearCurve2D outer, CirculinearCurve2D inner, double tolerance) {
        if (outer.almostEquals(inner, tolerance)) {
            return ContainedState.Equal;
        } else if (outer.boundingBox().containsBounds(inner)) {
            List<Point2D> points = haveParallelElements(outer, inner, tolerance)
                    ? iterativeIntersections(inner, outer, tolerance)
                    : new ArrayList<>(CirculinearCurves2D.findIntersections(inner, outer));
            return points.isEmpty()
                    ? (verticesContained(outer, inner) ? ContainedState.Contained : ContainedState.None)
                    : ContainedState.Overlapping;
        } else if (inner.boundingBox().containsBounds(outer)) {
            List<Point2D> points = haveParallelElements(outer, inner, tolerance)
                    ? iterativeIntersections(inner, outer, tolerance)
                    : new ArrayList<>(CirculinearCurves2D.findIntersections(inner, outer));
            return points.isEmpty()
                    ? (verticesContained(inner, outer) ? ContainedState.Inverted : ContainedState.None)
                    : ContainedState.Overlapping;
        } else {
            Box2D intersection = outer.boundingBox().intersection(inner.boundingBox());
            if (intersection.getHeight() > 0 && intersection.getWidth() > 0) {
                List<Point2D> points = haveParallelElements(outer, inner, tolerance)
                        ? iterativeIntersections(inner, outer, tolerance)
                        : new ArrayList<>(CirculinearCurves2D.findIntersections(inner, outer));
                return points.isEmpty() ? ContainedState.None : ContainedState.Overlapping;
            }
            return ContainedState.None;
        }
    }

    static boolean verticesContained(CirculinearCurve2D outer, CirculinearCurve2D inner) {
        CirculinearCurve2D ccw = ensureCounterClockwise(outer);
        return inner.vertices().stream().allMatch(v -> isPointInside(ccw, v));
    }

    public static boolean isContained(CirculinearCurve2D outer, CirculinearCurve2D inner, Collection<Point2D> points) {
        return outer.boundingBox().containsBounds(inner) && points.isEmpty() && verticesContained(outer, inner);
    }

    public static boolean isNonOverlapping(CirculinearCurve2D outer, CirculinearCurve2D inner, Collection<Point2D> points) {
        Box2D bounds = outer.boundingBox().intersection(inner.boundingBox());
        return bounds.getWidth() < 0 || bounds.getHeight() < 0 || points.isEmpty();
    }

    public static List<CirculinearElement2D> getElements(CirculinearCurve2D curve) {
        List<CirculinearElement2D> elements = new ArrayList<>();
        curve.continuousCurves().forEach((cont) -> {
            elements.addAll(cont.smoothPieces());
        });
        return elements;
    }

    public static List<Point2D> iterativeIntersections(CirculinearCurve2D curve1, CirculinearCurve2D curve2, double tolerance) {
        List<Point2D> points = new ArrayList<>();
        FirstOrderIntegrator integrator = new DormandPrince54Integrator((curve1.t1() - curve1.t0()) / 1000, (curve1.t1() - curve1.t0()) / 10, 1e-4, 1e-4);
        integrator.addEventHandler(new EventHandler() {
            double dLast = Double.NEGATIVE_INFINITY;

            @Override
            public void init(double t0, double[] y0, double t) {
            }

            @Override
            public double g(double t, double[] y) {
                double g = Math.abs(curve2.point(curve2.project(curve1.point(t))).distance(curve1.point(t))) - tolerance;
                return g;
            }

            @Override
            public EventHandler.Action eventOccurred(double t, double[] y, boolean increasing) {
                double d = curve1.length(t);
                if (!points.isEmpty() && d - dLast < 10 * tolerance) {
                    points.remove(points.size() - 1);
                    points.add(curve1.point(curve1.position((d + dLast) / 2)));
                } else {
                    points.add(curve1.point(t));
                }
                dLast = d;
                return EventHandler.Action.CONTINUE;
            }

            @Override
            public void resetState(double t, double[] y) {

            }
        }, (curve1.t1() - curve1.t0()) / 100, 1e-3, 100);
        try {
            integrator.integrate(new FirstOrderDifferentialEquations() {
                @Override
                public int getDimension() {
                    return 1;
                }

                @Override
                public void computeDerivatives(double t, double[] y, double[] yDot) throws MaxCountExceededException, DimensionMismatchException {
                    yDot[0] = 1;
                }
            }, curve1.t0(), new double[]{0}, curve1.t1(), new double[1]);
        } catch (DimensionMismatchException | NumberIsTooSmallException | MaxCountExceededException |
                 NoBracketingException ex) {
        }
        return points;
    }

    public static boolean hasParallelElements(CirculinearCurve2D curve, double tolerance) {
        List<CirculinearElement2D> elements = getElements(curve);
        for (int i = 0; i < elements.size(); i++) {
            for (int j = i + 1; j < elements.size(); j++) {
                if (elementsParallel(elements.get(i), elements.get(j), tolerance)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean haveParallelElements(CirculinearCurve2D curve1, CirculinearCurve2D curve2, double tolerance) {
        return getElements(curve1).stream()
                .filter(elem -> elem.length() > Tolerance2D.get())
                .anyMatch((elem1) -> (getElements(curve2).stream()
                        .filter(elem -> elem.length() > Tolerance2D.get())
                        .anyMatch((elem2) -> elementsParallel(elem1, elem2, tolerance))));
    }

    public static boolean elementsParallel(
            CirculinearElement2D elem1, CirculinearElement2D elem2, double tolerance) {
        if (elem1 == null || elem2 == null) {
            return false;
        }
        if (elem1.length() < tolerance || elem2.length() < tolerance) {
            return false;
        }

        // find which shapes are linear
        boolean b1 = elem1 instanceof LinearShape2D;
        boolean b2 = elem2 instanceof LinearShape2D;

        // if both elements are linear, check parallism to avoid computing
        // intersection of parallel lines
        if (b1 && b2) {
            LinearShape2D line1 = (LinearShape2D) elem1;
            LinearShape2D line2 = (LinearShape2D) elem2;

            // test parallel elements
            Vector2D v1 = line1.direction();
            Vector2D v2 = line2.direction();
            if (Vector2D.isColinear(v1, v2)) {
                // Test how far apart they are
                double distance = line1.supportingLine().distance(line2.firstPoint());
                if (distance < tolerance) {
                    double t12 = line1.project(line2.lastPoint());
                    double t11 = line1.project(line2.firstPoint());
                    return Math.abs(t12 - t11) >= Tolerance2D.get();
                }
            }
            return false;
        }
        if (!b1 && !b2) {
            // From now, both elem1 and elem2 are instances of CircleShape2D
            // It is therefore possible to extract support circles
            Circle2D circ1 = ((CircularShape2D) elem1).supportingCircle();
            Circle2D circ2 = ((CircularShape2D) elem2).supportingCircle();
            if (circ1.center().distance(circ2.center()) < tolerance && Math.abs(circ1.radius() - circ2.radius()) < tolerance) {
                if (elem1 instanceof CircleArc2D && elem2 instanceof CircleArc2D) {
                    return ((CircleArc2D) elem1).containsAngle(((CircleArc2D) elem2).getAngle(elem2.t0()))
                            || ((CircleArc2D) elem1).containsAngle(((CircleArc2D) elem2).getAngle(elem2.t1()));
                }
                return true;
            }
            return false;
        }
        Box2D intersection = elem1.boundingBox().intersection(elem2.boundingBox());
        if (intersection.getWidth() > -tolerance && intersection.getHeight() > -tolerance) {
            LinearShape2D line = b1 ? (LinearShape2D) elem1 : (LinearShape2D) elem2;
            CircularShape2D circle = b1 ? (CircularShape2D) elem2 : (CircularShape2D) elem1;
            // Check line is valid
            if (line.length() < Tolerance2D.get()) {
                return false;
            }

            // extract parameters of the circle
            Circle2D parent = circle.supportingCircle();
            Point2D center = parent.center();
            double radius = parent.radius();

            // Compute line perpendicular to the test line, and going through the
            // circle center
            StraightLine2D perp = StraightLine2D.createPerpendicular(line, center);

            // Compute distance between line and circle center
            Point2D inter = perp.intersection(new StraightLine2D(line));
            if (inter == null) {
                return false;
            }
            double dist = inter.distance(center);

            if (Math.abs(dist - radius) < tolerance) {
                double t12 = elem1.project(elem2.lastPoint());
                double t11 = elem1.project(elem2.firstPoint());
                return Math.abs(t12 - t11) >= Tolerance2D.get();
            }
        }
        return false;
    }
}
