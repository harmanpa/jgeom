/* file : Point3D.java
 * 
 * Project : geometry
 *
 * ===========================================
 * 
 * This library is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY, without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. if not, write to :
 * The Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 * 
 * Created on 27 nov. 2005
 *
 */
package math.geom3d;

import math.geom2d.Tolerance2D;
import math.geom3d.transform.AffineTransform3D;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.math3.util.FastMath;

/**
 * A 3-dimensional point.
 *
 * @author dlegland
 */
@JsonClassDescription("")
@JsonIgnoreProperties(ignoreUnknown = true, allowGetters = false, allowSetters = false)
public final class Point3D implements Shape3D {

    // ===================================================================
    // Class variables
    @JsonProperty
    @JsonPropertyDescription("")
    private double x;
    @JsonProperty
    @JsonPropertyDescription("")
    private double y;
    @JsonProperty
    @JsonPropertyDescription("")
    private double z;

    // ===================================================================
    // Constructors
    /**
     * Initialize at coordinate (0,0,0).
     */
    public Point3D() {
        this(0, 0, 0);
    }

    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Point3D midpoint(Point3D... points) {
        switch (points.length) {
            case 0:
                return new Point3D();
            case 1:
                return points[0];
            case 2:
                return new Point3D(
                        (points[0].getX() + points[1].getX()) / 2,
                        (points[0].getY() + points[1].getY()) / 2,
                        (points[0].getZ() + points[1].getZ()) / 2);
            default:
                return new PointSet3D(points).boundingBox().getCenter();
        }
    }

    // ===================================================================
    // Methods specific to Point3D
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public Vector3D asVector() {
        return new Vector3D(this);
    }

    public Point3D plus(Vector3D vec) {
        return new Point3D(this.x + vec.getX(), this.y + vec.getY(), this.z + vec.getZ());
    }

    public Point3D plus(Point3D p2) {
        return new Point3D(this.x + p2.x, this.y + p2.y, this.z + p2.z);
    }

    public Point3D minus(Vector3D vec) {
        return new Point3D(this.x - vec.getX(), this.y - vec.getY(), this.z - vec.getZ());
    }

    public Point3D minus(Point3D p2) {
        return new Point3D(this.x - p2.x, this.y - p2.y, this.z - p2.z);
    }

    public Point3D times(double k) {
        return new Point3D(k * x, k * y, k * z);
    }

    public Point3D lerp(Point3D a, double t) {
        return this.plus(a.minus(this).times(t));
    }

    // ===================================================================
    // Methods implementing the Shape3D interface
    @Override
    public double distance(Point3D point) {
        double dx = point.x - x;
        double dy = point.y - y;
        double dz = point.z - z;

        return FastMath.hypot(FastMath.hypot(dx, dy), dz);
    }

    public double distanceSq(Point3D point) {
        double dx = point.x - x;
        double dy = point.y - y;
        double dz = point.z - z;

        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * A point 'contains' another point if their euclidean distance is less than
     * the accuracy.
     *
     * @param point
     * @return
     */
    @Override
    public boolean contains(Point3D point) {
        return distance(point) <= Tolerance2D.get();
    }

    /**
     * Returns false, as a point is never empty.
     *
     * @return
     */
    @JsonIgnore
    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * Returns true, as a point is always bounded.
     *
     * @return
     */
    @JsonIgnore
    @Override
    public boolean isBounded() {
        return true;
    }

    @Override
    public Box3D boundingBox() {
        return new Box3D(x, x, y, y, z, z);
    }

    /**
     * Returns the clipped point, or null if empty.
     *
     * @param box
     * @return
     */
//    @Override
//    public PointSet3D clip(Box3D box) {
//        PointSet3D set = new PointSet3D(1);
//        if (x < box.getMinX() || x > box.getMaxX()) {
//            return set;
//        }
//        if (y < box.getMinY() || y > box.getMaxY()) {
//            return set;
//        }
//        if (z < box.getMinZ() || z > box.getMaxZ()) {
//            return set;
//        }
//
//        set.addPoint(this);
//        return set;
//    }
    /**
     * Applies the given affine transform to the point, and return the
     * transformed point.
     *
     * @param trans
     * @return
     */
    @Override
    public Point3D transform(AffineTransform3D trans) {
        double coef[] = trans.coefficients();
        return new Point3D(
                x * coef[0] + y * coef[1] + z * coef[2] + coef[3],
                x * coef[4] + y * coef[5] + z * coef[6] + coef[7],
                x * coef[8] + y * coef[9] + z * coef[10] + coef[11]);

    }

    // ===================================================================
    // methods overriding Object superclass
    @Override
    public boolean equals(Object obj) {
        return obj != null && getClass().equals(obj.getClass()) && almostEquals((Point3D) obj, Tolerance2D.get());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Tolerance2D.hash(x);
        hash = 41 * hash + Tolerance2D.hash(y);
        hash = 41 * hash + Tolerance2D.hash(z);
        return hash;
    }

    @Override
    public boolean almostEquals(GeometricObject3D obj, double eps) {
        if (!(obj instanceof Point3D)) {
            return false;
        }
        Point3D point = (Point3D) obj;
        if (Tolerance2D.compare(this.x, point.x) != 0) {
            return false;
        }
        if (Tolerance2D.compare(this.y, point.y) != 0) {
            return false;
        }
        if (Tolerance2D.compare(this.z, point.z) != 0) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Point3D{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
}
