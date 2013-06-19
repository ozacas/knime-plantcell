package au.edu.unimelb.plantcell.views.bar3d;

import java.util.ArrayList;

import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;


/**
 * Dont want the silly anchors to show on the canvas so override
 * @author andrew.cassin
 *
 */
public class MySelectableSphere extends org.jzy3d.plot3d.primitives.selectable.SelectableSphere {
	public MySelectableSphere(Coord3d coord3d, float bar_radius, int slices, Color color) {
		super(coord3d, bar_radius, slices, color);
	}

	@Override
	protected void buildAnchors() {
		// anchors cannot be null unless we override draw() too... so...
		anchors = new ArrayList<Coord3d>();
	}
}
