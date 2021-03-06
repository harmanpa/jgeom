/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package math.geom2d.math;

import de.lighti.clipper.Clipper;
import de.lighti.clipper.ClipperOffset;
import de.lighti.clipper.Path;
import de.lighti.clipper.Paths;
import de.lighti.clipper.Point;
import java.awt.Graphics2D;
import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import math.geom2d.AffineTransform2D;
import math.geom2d.Box2D;
import math.geom2d.GeometricObject2D;
import math.geom2d.Point2D;
import math.geom2d.Shape2D;
import math.geom2d.Tolerance2D;
import math.geom2d.Vector2D;
import math.geom2d.circulinear.CirculinearContourArray2D;
import math.geom2d.circulinear.CirculinearCurve2D;
import math.geom2d.circulinear.CirculinearCurves2D;
import math.geom2d.circulinear.CirculinearDomain2D;
import math.geom2d.conic.Circle2D;
import math.geom2d.conic.CircleArc2D;
import math.geom2d.conic.Ellipse2D;
import math.geom2d.conic.EllipseArc2D;
import math.geom2d.curve.Curve2D;
import math.geom2d.curve.SmoothCurve2D;
import math.geom2d.line.AbstractLine2D;
import math.geom2d.line.LineSegment2D;
import math.geom2d.line.LinearElement2D;
import math.geom2d.line.StraightLine2D;
import math.geom2d.polygon.LinearCurve2D;
import math.geom2d.polygon.LinearRing2D;
import math.geom2d.polygon.MultiPolygon2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.polygon.Polyline2D;
import math.geom2d.polygon.SimplePolygon2D;
import math.geom2d.polygon.convhull.JarvisMarch2D;
import math.geom2d.transform.CircleInversion2D;

/**
 *
 * @author peter
 */
public class Polygonizer {

    public static Polygon2D toPolygon(Shape2D shape, double maxError, boolean inside) {
        return toPolygon(toCirculinear(shape, maxError, inside), maxError, inside);
    }

    public static Polygon2D toPolygon(CirculinearCurve2D curve, double maxError, boolean inside) {
        boolean curveCW = Rings2D.isClockwise(curve);
        Collection<Point2D> points = curve.continuousCurves().stream()
                .flatMap(subcurve -> subcurve.smoothPieces().stream())
                .map(sp -> toPolyline(sp, maxError, curveCW == isClockwise(sp) ? inside : !inside).vertices())
                .reduce(new ArrayList<>(), (c1, c2) -> {
                    c1.addAll(c2);
                    return c1;
                });
        return toPolygon(removeColinearEdges(removeCoincidentPoints(points)));
    }

    static boolean isClockwise(SmoothCurve2D segment) {
        return segment.curvature(segment.t0() + (segment.t1() - segment.t0()) / 2) < 0;
    }

    public static CirculinearCurve2D toCirculinear(Shape2D shape, double maxError, boolean inside) {
        return CirculinearCurves2D.convert(shape, c -> toPolyline(c, maxError, inside));
    }

    public static Polygon2D toPolygon(Shape2D shape, double maxError, boolean inside, double jumpDistance) {
        return toPolygon(toCirculinear(shape, maxError, inside), maxError, inside, jumpDistance);
    }

    public static Polygon2D toPolygon(CirculinearCurve2D curve, double maxError, boolean inside, double jumpDistance) {
        boolean curveCW = Rings2D.isClockwise(curve);
        Collection<Point2D> points = curve.continuousCurves().stream()
                .flatMap(subcurve -> subcurve.smoothPieces().stream())
                .map(sp -> toPolyline(sp, maxError, curveCW == isClockwise(sp) ? inside : !inside).vertices())
                .reduce(new ArrayList<>(), (c1, c2) -> {
                    c1.addAll(c2);
                    return c1;
                });
        return toPolygon(removeColinearEdges(wrapWithJumps(removeCoincidentPoints(points), jumpDistance)));
    }

    public final static int windingNumber(List<Point2D> vertices, Point2D point) {
        double y = point.getY();
        return sequentials(Point2D.class, vertices, 2)
                .filter(pair -> (pair[0].getY() <= y && pair[1].getY() > y)
                || (pair[0].getY() > y && pair[1].getY() <= y))
                .mapToInt(pair -> isLeft(pair[0], pair[1], point)).sum();
    }

    public static List<Polygon2D> offset(Polygon2D polygon, double distance) {
        ClipperOffset offset = new ClipperOffset();
        offset.addPath(
                convertToClipperPath(polygon, 8),
                Clipper.JoinType.ROUND,
                Clipper.EndType.OPEN_BUTT);
        Paths paths = new Paths();
        offset.execute(paths, distance * Math.pow(10, 8));
        Polygon2D out = convertFromClipperPaths(paths, 8);
        if (out instanceof MultiPolygon2D) {
            return ((MultiPolygon2D) out).contours().stream()
                    .map(ring -> toPolygon(
                    removeColinearEdges(
                            removeCoincidentPoints(ring.vertices()))))
                    .collect(Collectors.toList());
        }
        if (out.vertexNumber() <= 1) {
            return Arrays.asList();
        }
        return Arrays.asList(
                toPolygon(
                        removeColinearEdges(
                                removeCoincidentPoints(out.vertices()))));
    }

    /**
     * Tests if a point is Left|On|Right of an infinite line. Input: three
     * points P0, P1, and P2 Return: >0 for P2 left of the line through P0 and
     * P1 =0 for P2 on the line <0 for P2 right of the line See: the January
     * 2001 Algorithm "Area of 2D and 3D Triangles and Polygons"
     */
    private static int isLeft(Point2D p1, Point2D p2, Point2D pt) {
        double x = p1.x();
        double y = p1.y();
        return (int) Math.signum(
                (p2.x() - x) * (pt.y() - y) - (pt.x() - x) * (p2.y() - y));
    }

    static Polygon2D toPolygon(Collection<Point2D> points) {
        if (points.isEmpty()) {
            return new EmptyPolygon2D();
        }
        return new SimplePolygon2D(points);
    }

    public static LinearCurve2D toPolyline(Curve2D segment, double maxError, boolean inside) {
        if (segment instanceof LinearElement2D) {
            return ((LinearElement2D) segment).asPolyline(1);
        }
        if (segment instanceof Circle2D) {
            double maxAngle = inside
                    ? Math.acos(1 - maxError / ((Circle2D) segment).supportingCircle().radius())
                    : Math.acos(((Circle2D) segment).supportingCircle().radius() / (maxError + ((Circle2D) segment).supportingCircle().radius()));
            int n = (int) Math.ceil(Math.PI * 2 / maxAngle);
            return toPolylineN((SmoothCurve2D) segment, n, inside);
        }
        if (segment instanceof CircleArc2D) {
            double maxAngle = inside
                    ? Math.acos(1 - maxError / ((CircleArc2D) segment).supportingCircle().radius())
                    : Math.acos(((CircleArc2D) segment).supportingCircle().radius() / (maxError + ((CircleArc2D) segment).supportingCircle().radius()));
            int n = (int) Math.ceil(Math.abs(((CircleArc2D) segment).getAngleExtent()) / maxAngle);
            return toPolylineN((SmoothCurve2D) segment, n, inside);
        }
        if (segment instanceof Ellipse2D) {
            double rMax = Math.max(((Ellipse2D) segment).semiMajorAxisLength(), ((Ellipse2D) segment).semiMinorAxisLength()) / 2;
            double rMin = Math.min(((Ellipse2D) segment).semiMajorAxisLength(), ((Ellipse2D) segment).semiMinorAxisLength()) / 2;
            double maxAngle = inside
                    ? Math.acos(1 - maxError / rMax)
                    : Math.acos(rMin / (maxError + rMax));
            int n = (int) Math.ceil(Math.PI * 2 / maxAngle);
            return toPolylineN((SmoothCurve2D) segment, n, inside);
        }
        if (segment instanceof EllipseArc2D) {
            double rMax = Math.max(((EllipseArc2D) segment).getSupportingEllipse().semiMajorAxisLength(), 
                    ((EllipseArc2D) segment).getSupportingEllipse().semiMinorAxisLength()) / 2;
            double rMin = Math.min(((EllipseArc2D) segment).getSupportingEllipse().semiMajorAxisLength(), 
                    ((EllipseArc2D) segment).getSupportingEllipse().semiMinorAxisLength()) / 2;
            double maxAngle = inside
                    ? Math.acos(1 - maxError / rMax)
                    : Math.acos(rMin / (maxError + rMax));
            int n = (int) Math.ceil(Math.abs(((EllipseArc2D) segment).getAngleExtent()) / maxAngle);
            return toPolylineN((SmoothCurve2D) segment, n, inside);
        }
        int n = inside ? 1 : 2;
        double error;
        LinearCurve2D out;
        do {
            if (segment instanceof SmoothCurve2D) {
                out = toPolyline((SmoothCurve2D) segment, n, inside);
                error = calculateError(segment, out, inside);
            } else {
                // NB: Can only be inside
                out = toPolyline(segment, n);
                error = calculateError(segment, out, true);
            }
            n++;
        } while (error > maxError);
        return out;
    }

    private static LinearCurve2D toPolylineN(SmoothCurve2D segment, int nSubSegments, boolean inside) {
        switch (nSubSegments) {
            case 1:
                return new Polyline2D(segment.firstPoint(), segment.lastPoint());
            case 2:
                if (!inside) {
                    if (segment.tangent(segment.t0()).isColinear(segment.tangent(segment.t1()))) {
                        return toPolylineN(segment, 3, inside);
                    }
                    List<AbstractLine2D> lines = new ArrayList<>();
                    lines.add(new StraightLine2D(segment.firstPoint(), segment.tangent(segment.t0())));
                    lines.add(new StraightLine2D(segment.lastPoint(), segment.tangent(segment.t1())));
                    return toPolyline(segment.firstPoint(), lines, segment.lastPoint());
                }
            default:
                if (inside) {
                    return segment.asPolyline(nSubSegments);
                } else {
                    double[] splits = splitPoints(segment, nSubSegments - 2);
                    List<AbstractLine2D> lines = new ArrayList<>();
                    lines.add(new StraightLine2D(segment.firstPoint(), segment.tangent(segment.t0())));
                    for (double split : splits) {
                        lines.add(new StraightLine2D(segment.point(split), segment.tangent(split)));
                    }
                    lines.add(new StraightLine2D(segment.lastPoint(), segment.tangent(segment.t1())));
                    return toPolyline(segment.firstPoint(), lines, segment.lastPoint());
                }
        }
    }

    private static LinearCurve2D toPolyline(Curve2D segment, int nSubSegments) {
        switch (nSubSegments) {
            case 1:
                return new Polyline2D(segment.firstPoint(), segment.lastPoint());
            default:
                double spacing = (segment.t1() - segment.t0()) / nSubSegments;
                return new Polyline2D(Stream.iterate(segment.t0(), t -> t + spacing)
                        .limit(nSubSegments + 1)
                        .map(t -> segment.point(t))
                        .collect(Collectors.toList()));
        }
    }

    private static double[] splitPoints(SmoothCurve2D segment, int nPoints) {
        double tEach = (segment.t1() - segment.t0()) / (nPoints + 1);
        double[] out = new double[nPoints];
        for (int i = 0; i < nPoints; i++) {
            out[i] = segment.t0() + (i + 1) * tEach;
        }
        return out;
    }

    private static Polyline2D toPolyline(Point2D start, List<AbstractLine2D> lines, Point2D end) {
        List<Point2D> points = new ArrayList<>();
        points.add(start);
        for (int i = 0; i < lines.size() - 1; i++) {
            Point2D intersection = lines.get(i).intersection(lines.get(i + 1));
            if (intersection != null) {
                points.add(intersection);
            }
        }
        points.add(end);
        return new Polyline2D(points);
    }

    private static double calculateError(Curve2D segment, LinearCurve2D polyline, boolean inside) {
        return (inside
                ? polyline.edges().stream().map(line -> line.point(line.t0() + (line.t1() - line.t0()) / 2))
                : polyline.vertices().stream())
                .mapToDouble(p -> segment.distance(p)).max().getAsDouble();
    }

    public static LinearCurve2D reduce(LinearCurve2D polyline) {
        return new Polyline2D(removeColinearEdges(removeCoincidentPoints(polyline.vertices())));
    }

    private static List<Point2D> removeCoincidentPoints(Collection<Point2D> vertices) {
        if (vertices.isEmpty()) {
            return new ArrayList<>(vertices);
        }
        return sequentials(Point2D.class, new ArrayList<>(vertices), 2)
                .map(pair -> pair[0].almostEquals(pair[1], Tolerance2D.get()) ? null : pair[0])
                .filter(point -> point != null)
                .collect(Collectors.toList());
    }

    private static boolean colinear(Point2D[] points) {
        return new Vector2D(points[0], points[1]).normalize()
                .almostEquals(new Vector2D(points[1], points[2]).normalize(), Math.pow(Tolerance2D.get(), 2));
    }

    private static List<Point2D> removeColinearEdges(List<Point2D> vertices) {
        if (vertices.size() < 3) {
            return Arrays.asList();
        }
        List<Point2D> newVertices = new ArrayList<>(vertices);
        int n;
        do {
            n = newVertices.size();
            newVertices = sequentials(Point2D.class, newVertices, 3)
                    .map(triple -> colinear(triple) ? null : triple[1])
                    .filter(p -> p != null)
                    .collect(Collectors.toList());
        } while (newVertices.size() < n);
        return newVertices;
    }

    public static List<Point2D> wrapWithJumps(Collection<Point2D> points, double jumpDistance) {
        List<Point2D> pointList = new ArrayList<>(points);
        // Ensure points are ccw
        if (new SimplePolygon2D(points).area() < 0) {
            Collections.reverse(pointList);
        }
        Polygon2D hull = new JarvisMarch2D().convexHull(pointList);
        // Limit jump distance to 20% of smallest dimension so we don't eliminate key features
        Box2D bounds = hull.boundingBox();
        jumpDistance = Math.min(jumpDistance, 0.2 * Math.min(bounds.getHeight(), bounds.getWidth()));

        // Position the list to start with the first point in the hull
        Collections.rotate(pointList, -pointList.indexOf(hull.vertex(0)));
//        List<Point2D> pointList = shiftToLowestPoint(points);
        Deque<Point2D> pointQueue = new ArrayDeque<>(pointList.size());
        pointQueue.addLast(pointList.remove(0));
        while (!pointList.isEmpty()) {
            addNextPoint(pointList, pointQueue, jumpDistance, hull);
        }
        return new ArrayList<>(pointQueue);
    }

    static List<Point2D> shiftToLowestPoint(Collection<? extends Point2D> points) {
        List<Point2D> pointList = new ArrayList<>(points);
        // Ensure points are ccw
        if (new SimplePolygon2D(points).area() < 0) {
            Collections.reverse(pointList);
        }
        // Find index of lowest point
        double y;
        double ymin = Double.MAX_VALUE;
        int nPoint = 0;

        // Iteration on the set of points to find point with lowest y-coord
        for (int i = 0; i < pointList.size(); i++) {
            y = pointList.get(i).getY();
            if (y < ymin) {
                ymin = y;
                nPoint = i;
            }
        }

        // Shuffle the 
        Collections.rotate(pointList, -nPoint);

        return pointList;
    }

    static void addNextPoint(List<Point2D> available, Deque<Point2D> out, double jumpDistance, Polygon2D hull) {
        Point2D previous = out.peekLast();
        int nPoint = findNextIndex(previous, available, jumpDistance, hull);
        // Shift list nPoint to the left
        Point2D point = available.remove(0);
        for (int i = 1; i < nPoint; i++) {
            point = available.remove(0);
        }
        out.addLast(point);
    }

    static int findNextIndex(Point2D previous, List<Point2D> available, double jumpDistance, Polygon2D hull) {
        // Find next member of the hull, we must include all hull members so mustn't skip them
        int iFirstHull;
        for (iFirstHull = 0; iFirstHull < available.size(); iFirstHull++) {
            if (Math.abs(hull.boundary().signedDistance(available.get(iFirstHull))) < Tolerance2D.get()) {
                break;
            }
        }
        // If it is within jumping range, jump to it
        if (iFirstHull < available.size() && available.get(iFirstHull).distance(previous) < jumpDistance) {
            return iFirstHull;
        }
        // Else just continue
        return 0;
    }

    public static <T> Stream<T[]> sequentials(Class<T> type, List<T> list, int each) {
        return indexStream(list.size(), each).map(indices -> {
            T[] arr = (T[]) Array.newInstance(type, each);
            for (int i = 0; i < indices.length; i++) {
                arr[i] = list.get(indices[i]);
            }
            return arr;
        });
    }

    public static Stream<int[]> indexStream(final int n, final int each) {
        if (n == 0) {
            return Stream.of();
        }
        int[] seed = new int[each];
        for (int i = 0; i < each; i++) {
            seed[i] = i % n;
        }
        return Stream.iterate(seed, indices -> {
            int[] out = new int[indices.length];
            for (int i = 0; i < indices.length; i++) {
                out[i] = indices[i] == n - 1 ? 0 : indices[i] + 1;
            }
            return out;
        }).limit((long) n);
    }

    static class EmptyPolygon2D implements Polygon2D {

        @Override
        public Collection<Point2D> vertices() {
            return Arrays.asList();
        }

        @Override
        public Point2D vertex(int i) {
            return null;
        }

        @Override
        public void setVertex(int i, Point2D point) {
        }

        @Override
        public void addVertex(Point2D point) {
        }

        @Override
        public void insertVertex(int index, Point2D point) {
        }

        @Override
        public void removeVertex(int index) {
        }

        @Override
        public int vertexNumber() {
            return 0;
        }

        @Override
        public int closestVertexIndex(Point2D point) {
            return -1;
        }

        @Override
        public Collection<? extends LineSegment2D> edges() {
            return Arrays.asList();
        }

        @Override
        public int edgeNumber() {
            return 0;
        }

        @Override
        public Point2D centroid() {
            return null;
        }

        @Override
        public double area() {
            return 0;
        }

        @Override
        public CirculinearContourArray2D<? extends LinearRing2D> boundary() {
            return new CirculinearContourArray2D<>();
        }

        @Override
        public Collection<? extends LinearRing2D> contours() {
            return Arrays.asList();
        }

        @Override
        public Polygon2D complement() {
            return this;
        }

        @Override
        public Polygon2D transform(AffineTransform2D trans) {
            return this;
        }

        @Override
        public Polygon2D clip(Box2D box) {
            return this;
        }

        @Override
        public CirculinearDomain2D transform(CircleInversion2D inv) {
            return this;
        }

        @Override
        public CirculinearDomain2D buffer(double dist) {
            return this;
        }

        @Override
        public boolean contains(double x, double y) {
            return false;
        }

        @Override
        public boolean contains(Point2D p) {
            return false;
        }

        @Override
        public double distance(Point2D p) {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double distance(double x, double y) {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public boolean isBounded() {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Box2D boundingBox() {
            return null;
        }

        @Override
        public void draw(Graphics2D g2) {
        }

        @Override
        public boolean almostEquals(GeometricObject2D obj, double eps) {
            return false;
        }

        @Override
        public Polygon2D asPolygon(int n) {
            return this;
        }

        @Override
        public void fill(Graphics2D g2) {
        }

    }

    private static Path convertToClipperPath(Polygon2D polygon, int decimalPlaces) {
        if (polygon.vertexNumber() < 3) {
            return new Path();
        }
        double scaling = Math.pow(10, decimalPlaces);
        Path path = new Path(polygon.vertexNumber());
        Point2D first = polygon.vertices().iterator().next();
        polygon.vertices().forEach(v -> path.add(new Point.LongPoint((long) Math.round(v.getX() * scaling), (long) Math.round(v.getY() * scaling))));
        path.add(new Point.LongPoint((long) Math.round(first.getX() * scaling), (long) Math.round(first.getY() * scaling)));
        return path;
    }

    private static Polygon2D convertFromClipperPaths(Paths paths, int decimalPlaces) {
        int n = paths.size();

        // if the result is single, create a SimplePolygon
        if (n == 1) {
            Point2D[] points = extractPathVertices(paths.get(0), decimalPlaces);
            return SimplePolygon2D.create(points);
        }

        // extract the different rings of the resulting polygon
        LinearRing2D[] rings = new LinearRing2D[n];
        for (int i = 0; i < n; i++) {
            rings[i] = LinearRing2D.create(extractPathVertices(paths.get(i), decimalPlaces));
        }

        // create a multiple polygon
        return MultiPolygon2D.create(rings);
    }

    private static Point2D[] extractPathVertices(Path path, int decimalPlaces) {
        double scaling = Math.pow(10, decimalPlaces);
        int n = path.size();
        Point2D[] points = new Point2D[n];
        for (int i = 0; i < n; i++) {
            points[i] = new Point2D(path.get(i).getX() / scaling, path.get(i).getY() / scaling);
        }
        return points;
    }
}
