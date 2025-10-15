/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package math.geom2d.svg;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import math.geom2d.Box2D;
import math.geom2d.curve.Curve2D;

/**
 *
 * @author peter
 */
public class SVGUtils {

    public static String toSVG(Curve2D... curves) {
        return toSVG((Collection<Curve2D>) Arrays.asList(curves));
    }

    public static String toSVG(Collection<Curve2D> curves) {
        Box2D bounds = new Box2D();
        StringBuilder sb = new StringBuilder();
        for (Curve2D curve : curves) {
            bounds = bounds.merge(curve.boundingBox());
            sb.append("\t<path d=\"").append(SVGPaths.toString(curve)).append("\"/>\n");
        }
        StringBuilder start = new StringBuilder("<svg viewBox=\"")
                .append(bounds.getMinX()).append(" ")
                .append(bounds.getMinY()).append(" ")
                .append(bounds.getWidth()).append(" ")
                .append(bounds.getHeight()).append("\" xmlns=\"http://www.w3.org/2000/svg\">\n");
        return start.append(sb).append("</svg>").toString();
    }

    public static void openSVG(Curve2D... curves) throws IOException {
        openSVG((Collection<Curve2D>) Arrays.asList(curves));
    }

    public static void openSVG(Collection<Curve2D> curves) throws IOException {
        String svg = toSVG(curves);
        Path path = File.createTempFile("tmp", ".svg").toPath();
        Files.write(path, Arrays.asList(svg.split("\n")));
        Desktop.getDesktop().browse(path.toUri());
    }
}
