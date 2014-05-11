package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import java.awt.Color;

public class ColourGradient {
	private final Color[] gradient;
	
	public ColourGradient(final Color start, final Color end) {
		int n = 100;
		gradient = new Color[n];
		for (int i=1; i<=n; i++) {
			double pc = (i / 100.0d);
			
			double red = start.getRed() * pc + end.getRed() * (1-pc);
			double green=start.getGreen() * pc + end.getGreen() * (1-pc);
			double blue =start.getBlue() * pc + end.getBlue() * (1-pc);
			
			gradient[i-1] = new Color((int)red, (int)green, (int)blue);;
		}
	}
	
	public Color getColor(double pc) {
		int idx = (int) (pc*100);
		if (idx >= gradient.length)
			idx = gradient.length - 1;
		return gradient[idx];
	}
}
