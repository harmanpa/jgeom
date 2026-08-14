package pl.pateman.dynamicaabbtree;

import java.util.function.BiPredicate;
import org.joml.AABBf;
import math.geom3d.transform.AffineTransform3D;
import math.geom3d.Point3D;

/**
 * Created by pateman.
 */
public final class AABBUtils {

    private AABBUtils() {

    }

    public static float getMinDistance(AABBf a, AffineTransform3D transformA, AABBf b, AffineTransform3D transformB) {
        return getMinDistance(
                transformed(a, TEMP_AABB_1.get(), transformA),
                transformed(b, TEMP_AABB_2.get(), transformB)
        );
    }

    public static float getMinDistance(AABBf aabba, AABBf aabbb) {
        return (float) Math.hypot(Math.hypot(
                minDistance(aabba.minX, aabba.maxX, aabbb.minX, aabbb.maxX),
                minDistance(aabba.minY, aabba.maxY, aabbb.minY, aabbb.maxY)),
                minDistance(aabba.minZ, aabba.maxZ, aabbb.minZ, aabbb.maxZ)
        );
    }

    /**
     * The gap between two ranges on one axis, or zero if they overlap. At most
     * one of the two differences can be positive, so the gap is the larger of
     * them - taking the smaller made this always return zero.
     */
    static float minDistance(float minA, float maxA, float minB, float maxB) {
        return Math.max(Math.max(0.0f, minA - maxB), Math.max(0.0f, minB - maxA));
    }

    public static float getWidth(AABBf aabb) {
        return aabb.maxX - aabb.minX;
    }

    public static float getHeight(AABBf aabb) {
        return aabb.maxY - aabb.minY;
    }

    public static float getDepth(AABBf aabb) {
        return aabb.maxZ - aabb.minZ;
    }

    public static float getArea(AABBf aabb) {
        final float width = getWidth(aabb);
        final float height = getHeight(aabb);
        final float depth = getDepth(aabb);
        return 2.0f * (width * height + width * depth + height * depth);
    }

    /**
     * The axis-aligned bounds of the transformed box. All eight corners have to
     * be taken: under rotation the transformed min and max corners alone do not
     * bound the box, and the result comes out too small.
     *
     * @param original The box to transform
     * @param transformed The box to write the result into
     * @param transform The transform to apply
     * @return The transformed box, for chaining
     */
    public static AABBf transformed(AABBf original, AABBf transformed, AffineTransform3D transform) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            Point3D p = new Point3D(
                    (corner & 1) == 0 ? original.minX : original.maxX,
                    (corner & 2) == 0 ? original.minY : original.maxY,
                    (corner & 4) == 0 ? original.minZ : original.maxZ).transform(transform);
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
            maxZ = Math.max(maxZ, p.getZ());
        }
        return transformed
                .setMin((float) minX, (float) minY, (float) minZ)
                .setMax((float) maxX, (float) maxY, (float) maxZ);
    }

    private static final ThreadLocal<AABBf> TEMP_AABB_1 = new ThreadLocal<AABBf>() {
        @Override
        protected AABBf initialValue() {
            return new AABBf();
        }
    };
    private static final ThreadLocal<AABBf> TEMP_AABB_2 = new ThreadLocal<AABBf>() {
        @Override
        protected AABBf initialValue() {
            return new AABBf();
        }
    };

    public static boolean test(AABBf a, AffineTransform3D transformA, AABBf b, AffineTransform3D transformB, BiPredicate<AABBf, AABBf> test) {
        return test.test(
                transformed(a, TEMP_AABB_1.get(), transformA),
                transformed(b, TEMP_AABB_2.get(), transformB)
        );
    }

    public static boolean testAABB(AABBf a, AffineTransform3D transformA, AABBf b, AffineTransform3D transformB) {
        return test(a, transformA, b, transformB, (AABBf at, AABBf bt) -> at.testAABB(bt));
    }

    public static boolean test(AABBf a, AABBf b, AffineTransform3D transformB, BiPredicate<AABBf, AABBf> test) {
        return test.test(
                a,
                transformed(b, TEMP_AABB_2.get(), transformB)
        );
    }

    public static boolean testAABB(AABBf a, AABBf b, AffineTransform3D transformB) {
        return test(a, b, transformB, (AABBf at, AABBf bt) -> at.testAABB(bt));
    }
}
