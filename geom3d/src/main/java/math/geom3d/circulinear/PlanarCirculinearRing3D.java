/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package math.geom3d.circulinear;

import math.geom2d.circulinear.CirculinearRing2D;
import math.geom3d.plane.Plane3D;

/**
 *
 * @author peter
 */
public class PlanarCirculinearRing3D<T extends CirculinearRing2D> extends PlanarCirculinearContinuousCurve3D<T> implements CirculinearRing3D {

    public PlanarCirculinearRing3D(Plane3D plane, T shape) {
        super(plane, shape);
    }

}
