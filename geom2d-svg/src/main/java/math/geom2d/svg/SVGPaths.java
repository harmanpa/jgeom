/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package math.geom2d.svg;

import math.geom2d.math.LineArcIterator;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import math.geom2d.Point2D;
import math.geom2d.Tolerance2D;
import math.geom2d.circulinear.CirculinearCurve2D;
import math.geom2d.circulinear.CirculinearElement2D;
import math.geom2d.circulinear.PolyCirculinearCurve2D;
import math.geom2d.conic.CircleArc2D;
import math.geom2d.curve.Curve2D;
import math.geom2d.line.LineSegment2D;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PathHandler;
import org.apache.batik.parser.PathParser;

/**
 *
 * @author peter
 */
public class SVGPaths {

    public static CirculinearCurve2D parse(String path) {
        try {
            CirculinearPathHandler pathHandler = new CirculinearPathHandler();
            PathParser pathParser = new PathParser();
            pathParser.setPathHandler(pathHandler);
            pathParser.parse(path);
            return pathHandler.getCurve();
        } catch (ParseException ex) {
            // Just grab it for debug
            throw ex;
        }
    }

    public static String toString(Curve2D curve) {
        DecimalFormat df = new DecimalFormat("#.#");
        df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
        df.setMaximumFractionDigits(8);
        StringBuilder sb = new StringBuilder();
        sb.append("M ")
                .append(df.format(curve.firstPoint().getX()))
                .append(" ")
                .append(df.format(curve.firstPoint().getY()))
                .append(" ");
        new LineArcIterator(curve) {
            @Override
            public void handleLine(LineSegment2D line) {
                try {
                    line(line, sb, df);
                } catch (IOException ex) {
                    Logger.getLogger(SVGPaths.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            @Override
            public void handleArc(CircleArc2D arc) {
                try {
                    arc(arc, sb, df);
                } catch (IOException ex) {
                    Logger.getLogger(SVGPaths.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.iterate();
        sb.append("Z");
        return sb.toString();
    }

    public static CirculinearElement2D line(double x1, double y1, double x2, double y2) {
        return new LineSegment2D(
                new Point2D(x1, y1),
                new Point2D(x2, y2));
    }

    public static CirculinearElement2D arc(double x1, double y1, double r, boolean large, boolean sweep, double x2, double y2) {
        Point2D startEndMid = new Point2D(x1 + (x2 - x1) / 2, y1 + (y2 - y1) / 2);
        boolean centreOnRight = sweep ? large : !large;
        double startEndLength = new Point2D(x1, y1).distance(new Point2D(x2, y2));
//        if (startEndLength > r * 2 + 1e-6) {
//            System.out.println("Warning: Invalid arc: distance between points is " + startEndLength + ", radius of " + r);
//            return new LineSegment2D(
//                    new Point2D(x1, y1),
//                    new Point2D(x2, y2));
//        }
        double offsetLength = (centreOnRight ? 1 : -1) * Math.sqrt(Math.max(0, Math.pow(r, 2.0) - Math.pow((startEndLength / 2), 2.0)));
        Point2D centre = new Point2D(startEndMid.getX() + (offsetLength / startEndLength) * (y2 - y1), startEndMid.getY() - (offsetLength / startEndLength) * (x2 - x1));
        double startAngle = angle(centre, x1, y1);
        double endAngle = angle(centre, x2, y2);
        return new CircleArc2D(centre, r, startAngle, endAngle, sweep);
    }
    static final double M_2PI = 2 * Math.PI;

    public static double angle(Point2D centre, double x, double y) {
        return (Math.atan2(y - centre.getY(), x - centre.getX()) + M_2PI) % (M_2PI);
    }

    public static void line(LineSegment2D line, Appendable out, DecimalFormat df) throws IOException {
        out.append("L ")
                .append(df.format(line.lastPoint().getX()))
                .append(" ")
                .append(df.format(line.lastPoint().getY()))
                .append(" ");
    }

    public static void arc(CircleArc2D arc, Appendable out, DecimalFormat df) throws IOException {
        out.append("A ")
                .append(df.format(arc.supportingCircle().radius()))
                .append(" ")
                .append(df.format(arc.supportingCircle().radius()))
                .append(" 0 ")
                .append(Math.abs(arc.getAngleExtent()) > Math.PI ? "1" : "0")
                .append(" ")
                .append(arc.isDirect() ? "1" : "0")
                .append(" ")
                .append(df.format(arc.point(arc.t1()).getX()))
                .append(" ")
                .append(df.format(arc.point(arc.t1()).getY()))
                .append(" ");
        if (Double.isNaN(arc.point(arc.t1()).getX()) || Double.isNaN(arc.point(arc.t1()).getY())) {
            System.out.println(arc);
        }
    }

    static class CirculinearPathHandler implements PathHandler {

        private final List<CirculinearElement2D> elements = new ArrayList<>();
        private boolean closed = false;
        private float lastX = 0;
        private float lastY = 0;

        public PolyCirculinearCurve2D getCurve() {
            return new PolyCirculinearCurve2D<>(elements, closed);
        }

        @Override
        public void startPath() throws ParseException {
        }

        @Override
        public void endPath() throws ParseException {
        }

        @Override
        public void movetoRel(float f, float f1) throws ParseException {
            movetoAbs(f + lastX, f1 + lastY);
        }

        @Override
        public void movetoAbs(float f, float f1) throws ParseException {
            lastX = f;
            lastY = f1;
        }

        @Override
        public void closePath() throws ParseException {
            closed = true;
        }

        @Override
        public void linetoRel(float f, float f1) throws ParseException {
            linetoAbs(f + lastX, f1 + lastY);
        }

        @Override
        public void linetoAbs(float f, float f1) throws ParseException {
            elements.add(line(lastX, lastY, f, f1));
            lastX = f;
            lastY = f1;
        }

        @Override
        public void linetoHorizontalRel(float f) throws ParseException {
            linetoHorizontalAbs(f + lastX);
        }

        @Override
        public void linetoHorizontalAbs(float f) throws ParseException {
            elements.add(line(lastX, lastY, f, lastY));
            lastX = f;
        }

        @Override
        public void linetoVerticalRel(float f) throws ParseException {
            linetoVerticalAbs(f + lastY);
        }

        @Override
        public void linetoVerticalAbs(float f) throws ParseException {
            elements.add(line(lastX, lastY, lastX, f));
            lastY = f;
        }

        @Override
        public void curvetoCubicRel(float f, float f1, float f2, float f3, float f4, float f5) throws ParseException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void curvetoCubicAbs(float f, float f1, float f2, float f3, float f4, float f5) throws ParseException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void curvetoCubicSmoothRel(float f, float f1, float f2, float f3) throws ParseException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void curvetoCubicSmoothAbs(float f, float f1, float f2, float f3) throws ParseException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void curvetoQuadraticRel(float f, float f1, float f2, float f3) throws ParseException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void curvetoQuadraticAbs(float f, float f1, float f2, float f3) throws ParseException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void curvetoQuadraticSmoothRel(float f, float f1) throws ParseException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void curvetoQuadraticSmoothAbs(float f, float f1) throws ParseException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void arcRel(float f, float f1, float f2, boolean bln, boolean bln1, float f3, float f4) throws ParseException {
            arcAbs(f, f1, f2, bln, bln1, f3 + lastX, f4 + lastY);
        }

        @Override
        public void arcAbs(float f, float f1, float f2, boolean bln, boolean bln1, float f3, float f4) throws ParseException {
            if (!(Math.abs(f3 - lastX) < Tolerance2D.get() && Math.abs(f4 - lastY) < Tolerance2D.get())) {
                elements.add(arc(lastX, lastY, f, bln, bln1, f3, f4));
                lastX = f3;
                lastY = f4;
            }
        }

    }
}
