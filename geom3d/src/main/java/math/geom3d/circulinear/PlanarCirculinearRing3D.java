/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package math.geom3d.circulinear;

import math.geom2d.circulinear.GenericCirculinearRing2D;
import math.geom3d.plane.PlanarShape3D;
import math.geom3d.plane.Plane3D;

/**
 *
 * @author peter
 */
public class PlanarCirculinearRing3D extends PlanarShape3D<GenericCirculinearRing2D> {

    public PlanarCirculinearRing3D(Plane3D plane, GenericCirculinearRing2D shape) {
        super(plane, shape);
    }
    
}
