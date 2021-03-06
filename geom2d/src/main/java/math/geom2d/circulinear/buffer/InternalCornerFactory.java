/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package math.geom2d.circulinear.buffer;

import java.util.Deque;
import math.geom2d.circulinear.CirculinearContinuousCurve2D;
import math.geom2d.circulinear.CirculinearElement2D;

/**
 *
 * @author peter
 */
public interface InternalCornerFactory {
    public boolean createInternalCorner(Deque<CirculinearElement2D> parallelElementQueue, CirculinearContinuousCurve2D currentParallelElement);
}
