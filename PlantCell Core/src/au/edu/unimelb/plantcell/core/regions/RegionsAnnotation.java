package au.edu.unimelb.plantcell.core.regions;

import java.awt.Dimension;
import java.awt.Graphics;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;

import au.edu.unimelb.plantcell.core.cells.AnnotationType;
import au.edu.unimelb.plantcell.core.cells.SequenceAnnotation;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.StandardTrackRenderer;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackRendererInterface;

/**
 * To annotate a series of distinct regions (possibly overlapping) with labels. This 
 * implementation is not efficient if the number of regions is large.
 * 
 * @author andrew.cassin
 *
 */
public class RegionsAnnotation extends SequenceAnnotation implements TrackRendererInterface {
	// regions are NOT ordered
	private final List<RegionInterface> m_regions = new ArrayList<RegionInterface>();
	// for rendering only (not persisted)
	private final StandardTrackRenderer m_rndr = new StandardTrackRenderer();
	
	public List<RegionInterface> getRegions() {
		return m_regions;
	}
	
	@Override
	public void serialize(DataCellDataOutput output) throws IOException {
		output.writeUTF(getAnnotationType().name());
		output.writeInt(m_regions.size());
		for (RegionInterface r: m_regions) {
			r.serialize(output);
		}
	}

	@Override
	public SequenceAnnotation deserialize(DataCellDataInput input) throws IOException {
		// NBL: annotation type eg. LABELLED_REGIONS has already been read from input
		int len = input.readInt();
		empty();
		for (int i=0; i<len; i++) {
			String id = input.readUTF();
			Region r = makeRegion(id);
			r.deserialize(input);
			addRegion(r, false);
		}
		
		return this;
	}
	
	/**
	 * Be sure to add new region types to this method or serialisation will not be correct!
	 * @param id
	 * @return
	 */
	private final Region makeRegion(String id) {
		Region r = null;
	
		if (id.startsWith("bh")) {
			r = new BlastHitRegion();
		} else if (id.startsWith("ipro")) {
			r = new InterProRegion("", "", 0, 0);
		} else if (id.startsWith("sreg")) {
			r = new ScoredRegion();
		} else if (id.startsWith("fs")) {
			r = new FramedScoredRegion();
		} else if (id.startsWith("pfamreg")) {
			r = new PFAMHitRegion();
		} else {
			r = new Region();
		}
		
		r.setID(id);
		return r;
	}
	
	@Override
	public int countAnnotations() {
		return m_regions.size();
	}

	@Override
	public AnnotationType getAnnotationType() {
		return AnnotationType.LABELLED_REGIONS;
	}

	/**
	 * Start offsets at zero?
	 * @return
	 */
	public int getOffset() {
		return 0;
	}
	
	
	/**
	 * Removes all regions
	 */
	public void empty() {
		if (m_regions != null)
			m_regions.clear();
	}
	
	/**
	 * Convenience method for <code>addRegion(r, true)</code>
	 * @param a
	 */
	public final void addRegion(RegionInterface a) {
		addRegion(a, true);
	}
	
	/**
	 * Alters the offset for the specified region to be the same as <code>this</code> and then adds the specified
	 * region to the list of regions present in the instance. Instances must not be shared
	 * between multiple annotation objects
	 * 
	 * @param r
	 */
	public void addRegion(RegionInterface r, boolean do_not_add_duplicates) {
		assert(r != null);
		
		if (do_not_add_duplicates && m_regions.indexOf(r) >= 0) 
			return;
		
		m_regions.add(r);
	}

	public void addAll(List<? extends RegionInterface> regions, boolean do_not_add_duplicates) {
		assert(regions != null);
		for (RegionInterface r : regions) {
			addRegion(r, do_not_add_duplicates);
		}
	}
	
	@Override 
	public String toString() {
		StringBuilder sb = new StringBuilder(1024);
		for (RegionInterface r : m_regions) {
			sb.append(r.toString());
			sb.append(", ");
		}
		return sb.toString();
	}

	public DenseBitVector asBitVector(int max_len) {
		DenseBitVector bv = new DenseBitVector(max_len);
		for (RegionInterface r: m_regions) {
			bv.set(r.getZStart(), r.getZEnd());
		}
		return bv;
	}

	/**
	 * This implementation must support a polymorphic list of Region and ScoredRegion's, so we provide the columns for that
	 */
	@Override
	public List<DataColumnSpec> asColumnSpec(String prefix) {
		ArrayList<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
		cols.add(new DataColumnSpecCreator(prefix+": Label",          StringCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator(prefix+": Start (from 1)", IntCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator(prefix+": End (from 1)",   IntCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator(prefix+": Score",          DoubleCell.TYPE).createSpec());
		
		return cols;
	}

	@Override
	public TrackRendererInterface getRenderer() {
		return this;
	}

	@Override
	public void paintLabel(final Graphics g, final String l, int offset) {
		m_rndr.paintLabel(g, l, offset);
	}
	
	@Override
	public Dimension paintTrack(Map<String, Integer> props, final Graphics g,
			SequenceValue sv, Track t) {
		return m_rndr.paintTrack(props, g, sv, t);
	}
	
}
