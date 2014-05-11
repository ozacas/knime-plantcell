package au.edu.unimelb.plantcell.core.cells;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.core.regions.RegionInterface;

/**
 * A track of quantitative data for the sequence (eg. probabilities along the sequence).
 * Very simplistic implementation, needs much more...
 * 
 * @author andrew.cassin
 *
 */
public class NumericAnnotation extends SequenceAnnotation implements RegionInterface, TrackRendererInterface {
	private Vector<Double> m_values = new Vector<Double>();		// NB: list implementation must be Serializable!
	private boolean m_range_known   = false;
	private double  m_min, m_max;
	
	// not persisted state: used only by the track filter
	private int m_start, m_end;
	
	// default constructor
	public NumericAnnotation() {
		this(200);
	}
	
	public NumericAnnotation(int initial_capacity) {
		m_values           = new Vector<Double>(initial_capacity);
		m_range_known      = false;
		m_start            = -1;	// no region of interest defined ie. whole vector
		m_end			   = -1;
	}
	
	/**
	 * Number of annotations is always the length of the sequence the annotation relates to
	 */
	@Override
	public int countAnnotations() {
		return m_values.size();
	}

	/**
	 * 
	 * @param pos the annotation is grown to the necessary size as needed
	 * @param d   new value
	 */
	public void setPosition(int pos, double d) throws IndexOutOfBoundsException {
		if (pos >= m_values.size()) {
			m_values.setSize(pos+1);
		}
		m_values.setElementAt(new Double(d), pos);
		m_range_known = false;
	}
	
	@Override
	public AnnotationType getAnnotationType() {
		return AnnotationType.NUMERIC;
	}

	@Override
	public void serialize(DataCellDataOutput output) throws IOException {
		output.writeUTF(getAnnotationType().name());
		int cnt = countAnnotations();
		output.writeInt(cnt);
		for (int i=0; i<cnt; i++) {
			output.writeDouble(m_values.get(i));
		}
	}

	@Override
	public SequenceAnnotation deserialize(DataCellDataInput input)
			throws IOException {
		// NB: caller has already loaded annotation type... so skip that field here
		int  cnt = input.readInt();
		m_values = new Vector<Double>(cnt);
		m_values.setSize(cnt);
		for (int i=0; i<cnt; i++) {
			m_values.set(i, new Double(input.readDouble()));
		}
		return this;
	}

	public Iterable<Double> getValues() {
		return m_values;
	}

	public double[] getValuesAsArray() {
		double[] ret = new double[m_values.size()];
		for (int i=0; i<ret.length; i++) {
			ret[i] = m_values.get(i);
		}
		return ret;
	}
	
	private void compute_range() {
		m_min = Double.MAX_VALUE;
		m_max = Double.NEGATIVE_INFINITY;

		for (Double d : m_values) {
			double v = d.doubleValue();
			if (m_max < v) {
				m_max = v;
			} 
			if (m_min > v) {
				m_min = v;
			}
		}
		m_range_known = true;
	}
	
	public double getMinimumValue() {
		if (!m_range_known) {
			compute_range();
		}
		return m_min;
	}
	
	public double getMaximumValue() {
		if (!m_range_known) {
			compute_range();
		}
		return m_max;
	}
	
	public double range(double d) {
		if (!m_range_known) {
			compute_range();
		}
		return (d - m_min) / (m_max - m_min);
	}

	/**
	 * Needs to be overriden to provide a usable summary of the track for the summary sequence renderer
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(512);
		sb.append("" + m_values.size() + " datapoints: ");
		sb.append("[" + getMinimumValue()+ ", "+getMaximumValue()+"]");
		return sb.toString();
	}

	@Override
	public List<DataColumnSpec> asColumnSpec(String prefix) {
		ArrayList<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
		cols.add(new DataColumnSpecCreator(prefix+": Start", IntCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator(prefix+": End",   IntCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator(prefix+": N",          IntCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator(prefix+": Datapoints", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec());
		
		return cols;
	}

	/**
	 * HACK BUG TODO: this supports only extraction of contiguous regions for now... a bit silly really!
	 */
	@Override
	public void setRegionOfInterest(DenseBitVector bv) throws InvalidSettingsException {
		long start = bv.nextSetBit(0);
		if (start == -1) {
			m_start = 0;
			m_end   = m_values.size();
			return;
		}
		long pos   = start;
		long end   = start;
		while ((pos = bv.nextSetBit(pos+1)) > 0) {
			end = pos;
		}
		end++;	// include last set bit
		if (end < start)
			throw new InvalidSettingsException("End must be >= start: "+end+" - "+ start);
		if (end >= m_values.size())
			throw new InvalidSettingsException("End must < length of annotation: "+end);
		m_start = (int) start;
		m_end   = (int) end;
	}
	
	public double[] getRegionOfInterest() {
		if (m_start < 0) {
			return getValuesAsArray();
		}
			
		// else only return the region of interest
		double[] ret = new double[m_end - m_start];
		for (int i=0; i<ret.length; i++) {
			ret[i] = m_values.get(i+m_start).doubleValue();
		}
		return ret;
	}
	
	@Override
	public Map<String, DataCell> asCells(String prefix) {
		double[] roi = getRegionOfInterest();
		Map<String,DataCell> map = new HashMap<String,DataCell>();
		int start = m_start;
		if (start < 0)
			start = 0;
		int end   = m_end;
		if (end < 0 || end >= m_values.size())
			end   = m_values.size();
		map.put(prefix+": Start", new IntCell(start));
		map.put(prefix+": End",   new IntCell(end));
		map.put(prefix+": N",     new IntCell(roi.length));
		
		if (roi.length > 0) {
			List<DataCell> roi_cells = new ArrayList<DataCell>(roi.length);
			for (int i=0; i<roi.length; i++) {
				roi_cells.add(new DoubleCell(roi[i]));
			}
			map.put(prefix+": Datapoints", CollectionCellFactory.createListCell(roi_cells));
		} else {
			map.put(prefix+": Datapoints", DataType.getMissingCell());
		}
		return map;
	}

	@Override
	public int getZStart() {
		return 0;
	}

	@Override
	public int getZEnd() {
		return m_values.size();
	}

	@Override
	public boolean isSingleSite() {
		return (m_values.size() == 1);
	}

	@Override
	public String getLabel() {
		return "";
	}

	@Override
	public final String getID() {
		return "n/a";
	}
	
	/**
	 * a numeric annotation implements a single region: the entire vector
	 */
	@Override
	public List<RegionInterface> getRegions() {
		ArrayList<RegionInterface> ret = new ArrayList<RegionInterface>();
		ret.add(this);
		return ret;
	}

	@Override
	public SequenceValue getFeatureSequence(SequenceValue sv) {
		return sv; 
	}
	
	@Override
	public TrackRendererInterface getRenderer() {
		return this;
	}

	@Override
	public void paintLabel(final Graphics g, final String l, int offset) {
		g.setColor(Color.BLACK);
		g.drawString(l, 3, 12+offset);
	}
	
	@Override
	public Dimension paintTrack(Map<String, Integer> props, Graphics g,
			SequenceValue sv, Track t) {
		NumericAnnotation na = (NumericAnnotation) t.getAnnotation();
		double frac   = ((double) props.get("track.width")) / sv.getLength();
		int offset    = props.get("height");
		
		paintLabel(g, t.getName(), offset);
		
		double height = 20.0d;
		double[]    d = na.getValuesAsArray();
		int[] xPoints = new int[d.length];
		int[] yPoints = new int[d.length];
		for (int idx=0; idx<d.length; idx++) {
			xPoints[idx] = 200 + (int) (idx * frac);
			yPoints[idx] = offset - (int)(height * na.range(d[idx]));
		}
		g.setColor(Color.BLUE);
		g.drawRect(200, offset-(int)height, 400, (int)height);
		g.setColor(Color.DARK_GRAY);
		g.drawString(""+na.getMaximumValue(), 605, offset - 38);
		g.drawString(""+na.getMinimumValue(), 605, offset);
		g.setColor(Color.BLACK);
		g.drawPolyline(xPoints, yPoints, d.length);
		
		Integer width = props.get("label.width") + props.get("track.width");
		return new Dimension(width, (int)height);
	}

	@Override
	public String asGFF(SequenceValue sv, Track t) {
		// TODO: not supported (yet) for this type of annotation
		return null;
	}
}
