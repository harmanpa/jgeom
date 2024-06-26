package math.geom3d.transform;

import math.geom3d.Point3D;
import math.geom3d.Vector3D;
import junit.framework.TestCase;

public class AffineTransform3DTest extends TestCase {

	public void testGetInverseTransform() {
		double tx = 2;
		double ty = 3;
		double tz = 4;
		AffineTransform3D T234 = new AffineTransform3D(1, 0, 0, tx, 0, 1, 0, ty, 0, 0, 1, tz);
		AffineTransform3D inv = T234.inverse();
		AffineTransform3D T234m = new AffineTransform3D(1, 0, 0, -tx, 0, 1, 0, -ty, 0, 0, 1, -tz);
		System.out.println(inv.determinant());
		System.out.println(T234m.determinant());
		assertTrue(inv.equals(T234m));
	}

	public void testCreateTranslation_Vector3D() {
		double tx = 2;
		double ty = 3;
		double tz = 4;
		AffineTransform3D T1 = new AffineTransform3D(1, 0, 0, tx, 0, 1, 0, ty, 0, 0, 1, tz);
		
		Vector3D vec = new Vector3D(tx, ty, tz);
		AffineTransform3D T2 = AffineTransform3D.createTranslation(vec);
		System.out.println(T1.determinant());
		System.out.println(T2.determinant());
		assertTrue(T1.equals(T2));
	}

	public void testCreateRotationOx_Double() {
		double theta = Math.PI/3;
		double cot = Math.cos(theta);
		double sit = Math.sin(theta);

		AffineTransform3D T1 = new AffineTransform3D(
				1, 0, 0, 0, 
				0, cot, -sit, 0,
				0, sit, cot, 0);
		
		AffineTransform3D T2 = AffineTransform3D.createRotationOx(theta);
		System.out.println(T1.determinant());
		System.out.println(T2.determinant());
		assertTrue(T1.equals(T2));
	}

	public void testCreateRotationOy_Double() {
		double theta = Math.PI/3;
		double cot = Math.cos(theta);
		double sit = Math.sin(theta);

		AffineTransform3D T1 = new AffineTransform3D(
				cot, 0, sit, 0,
				0, 1, 0, 0, 
				-sit, 0, cot, 0);
		
		AffineTransform3D T2 = AffineTransform3D.createRotationOy(theta);
		System.out.println(T1.determinant());
		System.out.println(T2.determinant());
		assertTrue(T1.equals(T2));
	}

	public void testCreateRotationOz_Double() {
		double theta = Math.PI/3;
		double cot = Math.cos(theta);
		double sit = Math.sin(theta);

		AffineTransform3D T1 = new AffineTransform3D(
				cot, -sit, 0, 0, 
				sit, cot, 0, 0, 
				0, 0, 1, 0);
		
		AffineTransform3D T2 = AffineTransform3D.createRotationOz(theta);
		System.out.println(T1.determinant());
		System.out.println(T2.determinant());
		assertTrue(T1.equals(T2));
	}

	public void testTransformPoint() {
		Vector3D vec = new Vector3D(3, 4, 5);
		AffineTransform3D trans = AffineTransform3D.createTranslation(vec);
		System.out.println(trans.determinant());
		Point3D p1 = new Point3D(10, 10, 10); 
		Point3D p2 = new Point3D(10, 20, 30);
		
		Point3D res1 = trans.transformPoint(p1);
		Point3D res2 = trans.transformPoint(p2);
		
		assertTrue(res1.equals(new Point3D(13, 14, 15)));
		assertTrue(res2.equals(new Point3D(13, 24, 35)));	
	}
	
	public void testTransformPoints_Array() {
		Vector3D vec = new Vector3D(3, 4, 5);
		AffineTransform3D trans = AffineTransform3D.createTranslation(vec);
		System.out.println(trans.determinant());
		Point3D[] pts = new Point3D[]{
				new Point3D(10, 10, 10), 
				new Point3D(10, 20, 30) };
		Point3D[] res = new Point3D[pts.length];
		
		trans.transformPoints(pts, res);
		assertTrue(res[0].equals(new Point3D(13, 14, 15)));
		assertTrue(res[1].equals(new Point3D(13, 24, 35)));	
	}

	public void testMirror() {
		System.out.println("-");
		System.out.println(AffineTransform3D.createReflectionX().determinant());
		System.out.println(AffineTransform3D.createReflectionY().determinant());
		System.out.println(AffineTransform3D.createReflectionZ().determinant());
		System.out.println(AffineTransform3D.createReflectionX().preConcatenate(AffineTransform3D.createReflectionY()).determinant());
	}
}
