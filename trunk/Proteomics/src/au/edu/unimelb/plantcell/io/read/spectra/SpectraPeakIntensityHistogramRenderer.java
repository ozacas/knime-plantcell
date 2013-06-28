package au.edu.unimelb.plantcell.io.read.spectra;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JList;
import javax.swing.JTable;

import org.apache.commons.math.random.EmpiricalDistribution;
import org.apache.commons.math.random.EmpiricalDistributionImpl;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.commons.math.stat.descriptive.rank.Max;
import org.apache.commons.math.stat.descriptive.rank.Min;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.DataValueRenderer;

/**
 * Computes an intensity histogram useful for identifying noisy spectra and the percentile(s)
 * in which those peaks occur.
 * 
 * @author andrew.cassin
 *
 */
public class SpectraPeakIntensityHistogramRenderer implements DataValueRenderer {
	/**
	 * serial UID
	 */
	private static final long serialVersionUID = 8312187077797140178L;

	@Override
	public Component getTableCellRendererComponent(JTable arg0, Object arg1,
			boolean arg2, boolean arg3, int arg4, int arg5) {
		return getRendererComponent(arg1);
	}

	@Override
	public Component getListCellRendererComponent(JList arg0, Object arg1,
			int arg2, boolean arg3, boolean arg4) {
		return getRendererComponent(arg1);
	}

	@Override
	public boolean accepts(DataColumnSpec arg0) {
		return arg0.getType().isCompatible(SpectraValue.class);
	}

	@Override
	public String getDescription() {
		return "Spectra peak intensity histogram";
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(400, 100);
	}

	@Override
	public Component getRendererComponent(Object arg0) {
		if (arg0 == null || !(arg0 instanceof SpectraValue))
			return null;
		
		
		SpectraValue sv = (SpectraValue) arg0;
		
		double[] intensities = sv.getIntensity();
		if (intensities == null || intensities.length < 1)
			return null;
		
		final double[] freq = new double[100];
		EmpiricalDistribution eb = new EmpiricalDistributionImpl(freq.length);
		eb.load(intensities);
		int i=0;
		int up_to_q1 = 0;
		int up_to_q2 = 0;
		int up_to_q3 = 0;
		for (SummaryStatistics stats : eb.getBinStats()) {
			freq[i++] = stats.getN();
			if (i <= 25)
				up_to_q1 += stats.getN();
			if (i <= 50)
				up_to_q2 += stats.getN();
			if (i <= 75)
				up_to_q3 += stats.getN();
		}
		final int q1_final = up_to_q1;
		final int q2_final = up_to_q2;
		final int q3_final = up_to_q3; 
		
		final double max_freq = new Max().evaluate(freq);
		final double min_freq = new Min().evaluate(freq);
		final double max = new Max().evaluate(intensities);
		final double min = new Min().evaluate(intensities);
		final double range = max - min;
		final double range_freq = max_freq - min_freq;
		
		final Dimension sz = getPreferredSize();
		
		Canvas c = new Canvas() {
			
			@Override
			public void paint(Graphics g) {
				for (int i=1; i<=100; i++) {
					g.setColor(Color.BLUE);
					int height = (int) sz.getHeight() - 20;
					g.drawLine(i, height, i, (int) ((height - (height * ((freq[i-1] -min_freq )/ range_freq)))));
				}
				g.setColor(Color.BLACK);
				g.drawString(""+min, 1, 96);
				g.drawString(""+max, 100, 96);
				g.drawString("Number of peaks <= Q1: "+q1_final, 110, 20);
				g.drawString("Number of peaks <= median intensity (Q2): "+q2_final, 110, 40);
				g.drawString("Number of peaks at <= Q3 intensity: "+q3_final, 110, 60);
				g.drawString("Intensity range per bin (width): "+(range/freq.length), 110, 80);
			}
		};
		c.setPreferredSize(sz);
		
		
		return c;
	}

}
