/**
 * 
 */
package math.geom2d.circulinear;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JFrame;
import javax.swing.JPanel;

import math.geom2d.Box2D;
import math.geom2d.Point2D;
import math.geom2d.Vector2D;
import math.geom2d.line.StraightLine2D;

/**
 * Compute buffer of a simple polyline.
 * @author dlegland
 *
 */
public class CheckGetBufferTwoLines extends JPanel{
	private static final long serialVersionUID = 1L;

	CirculinearCurve2D curve;
	CirculinearDomain2D domain;
	Box2D box = Box2D.create(50, 350, 50, 350);
	
	public CheckGetBufferTwoLines(){
		Point2D p0 = new Point2D(200, 200);
		Vector2D v1 = Vector2D.create(1, 0);
		Vector2D v2 = Vector2D.create(0, 1);
		StraightLine2D line1 = StraightLine2D.create(p0, v1);
		StraightLine2D line2 = StraightLine2D.create(p0, v2);
		
		curve = 
			CirculinearCurveArray2D.create(new StraightLine2D[]{line1, line2});
		domain = curve.getBuffer(30);
	}
	
	public void paintComponent(Graphics g){
		Graphics2D g2 = (Graphics2D) g;
		
		g2.setColor(Color.CYAN);
		domain.clip(box).fill(g2);
		g2.setColor(Color.BLUE);
		domain.getBoundary().clip(box).draw(g2);
		
		g2.setColor(Color.BLACK);
		curve.clip(box).draw(g2);
	}
	
	public final static void main(String[] args){
		JPanel panel = new CheckGetBufferTwoLines();
		panel.setPreferredSize(new Dimension(500, 400));
		JFrame frame = new JFrame("Compute buffer of two intersecting lines");
		frame.setContentPane(panel);
		frame.pack();
		frame.setVisible(true);		
	}
}