package pl.pateman.dynamicaabbtree;

import math.geom3d.transform.AffineTransform3D;
import org.joml.AABBf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AABBUtilsTest {

   private static final float EPS = 1e-5f;

   private static AABBf unitBox() {
      return new AABBf(-1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f);
   }

   @Test
   public void shouldTranslateABox() {
      AABBf result = AABBUtils.transformed(unitBox(), new AABBf(),
            AffineTransform3D.createTranslation(2.0, 3.0, 4.0));

      assertEquals(1.0f, result.minX, EPS);
      assertEquals(3.0f, result.maxX, EPS);
      assertEquals(2.0f, result.minY, EPS);
      assertEquals(4.0f, result.maxY, EPS);
      assertEquals(3.0f, result.minZ, EPS);
      assertEquals(5.0f, result.maxZ, EPS);
   }

   @Test
   public void shouldBoundARotatedBox() {
      // Turned 45 degrees about Z, the corners of the box reach out to sqrt(2)
      // in X and Y. Taking only the min and max corners misses that entirely.
      AABBf result = AABBUtils.transformed(unitBox(), new AABBf(),
            AffineTransform3D.createRotationOz(Math.PI / 4));

      float diagonal = (float) Math.sqrt(2.0);
      assertEquals(-diagonal, result.minX, EPS);
      assertEquals(diagonal, result.maxX, EPS);
      assertEquals(-diagonal, result.minY, EPS);
      assertEquals(diagonal, result.maxY, EPS);
      assertEquals(-1.0f, result.minZ, EPS);
      assertEquals(1.0f, result.maxZ, EPS);
   }

   @Test
   public void shouldContainEveryTransformedCorner() {
      AABBf original = new AABBf(-1.0f, -2.0f, -3.0f, 4.0f, 5.0f, 6.0f);
      AffineTransform3D transform = AffineTransform3D.createRotationOz(0.7)
            .preConcatenate(AffineTransform3D.createRotationOx(0.3))
            .preConcatenate(AffineTransform3D.createTranslation(-11.0, 5.0, 2.0));

      AABBf result = AABBUtils.transformed(original, new AABBf(), transform);

      for (int corner = 0; corner < 8; corner++) {
         math.geom3d.Point3D p = new math.geom3d.Point3D(
               (corner & 1) == 0 ? original.minX : original.maxX,
               (corner & 2) == 0 ? original.minY : original.maxY,
               (corner & 4) == 0 ? original.minZ : original.maxZ).transform(transform);
         assertTrue("corner " + corner + " outside " + result,
               result.testPoint((float) p.getX(), (float) p.getY(), (float) p.getZ()));
      }
   }

   @Test
   public void shouldDetectOverlapOfRotatedBoxes() {
      // Two unit boxes 2.2 apart are clear of each other while axis aligned,
      // but turning one by 45 degrees pushes its corner into the other
      AABBf a = unitBox();
      AABBf b = unitBox();
      AffineTransform3D identity = new AffineTransform3D();
      AffineTransform3D shifted = AffineTransform3D.createTranslation(2.2, 0.0, 0.0);
      AffineTransform3D turned = AffineTransform3D.createRotationOz(Math.PI / 4)
            .preConcatenate(shifted);

      assertFalse(AABBUtils.testAABB(a, identity, b, shifted));
      assertTrue(AABBUtils.testAABB(a, identity, b, turned));
   }

   @Test
   public void shouldMeasureDistanceBetweenTransformedBoxes() {
      AABBf a = unitBox();
      AABBf b = unitBox();
      assertEquals(2.0f, AABBUtils.getMinDistance(a, new AffineTransform3D(), b,
            AffineTransform3D.createTranslation(4.0, 0.0, 0.0)), EPS);
   }
}
