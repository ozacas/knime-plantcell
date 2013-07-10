package au.edu.unimelb.plantcell.views.plot3d;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.jzy3d.colors.Color;
import org.jzy3d.maths.BoundingBox3d;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.HistogramBar;
import org.jzy3d.plot3d.primitives.selectable.Selectable;
import org.jzy3d.plot3d.rendering.view.Camera;

/**
 * A bar between 0..z which is centred at location (x,y) of the given radius and shape. This bar is
 * selectable.
 * 
 * @author andrew.cassin
 *
 */
public class MyHistogramBar extends HistogramBar implements Selectable {
	private int idx;
	private List<Coord3d> projection;
	
	public MyHistogramBar(float x, float y, float z, float bar_radius,
			Color color, boolean wireframe, int idx) {
		super();
		setData(new Coord3d(x, y, 0.0f), z, bar_radius, color);
		setWireframeDisplayed(wireframe);
		this.idx = idx;
	}

	/**
	 * returns the index of the bar from the node model. Not alterable once constructed
	 * @return
	 */
	public int getIdx() {
		return idx;
	}

	@Override
	public Polygon getHull2d() {
		if (projection == null || projection.size() < 1)
			return new Polygon();
		// TODO...
		return null;
	}

	@Override
	public List<Coord3d> getLastProjection() {
		return projection;
	}

	@Override
	public void project(GL2 gl, GLU glu, Camera cam) {
		BoundingBox3d bb = this.getBounds();
		projection = new ArrayList<Coord3d>();
		for (Coord3d pt : bb.getVertices()) {
			projection.add(cam.modelToScreen(gl, glu, pt));
		}
	}
	
	
}
