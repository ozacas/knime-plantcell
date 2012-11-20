package au.edu.unimelb.plantcore.core.regions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;

import au.edu.unimelb.plantcell.core.cells.SequenceImpl;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.SerializableInterface;
import au.edu.unimelb.plantcell.core.cells.Track;

/**
 * Represents a labelled region of sequence data. Co-ordinate system
 * is established by the track containing the annotation, but this class assumes
 * Coordinates.OFFSET_FROM_START with similar semantics as per <code>String.indexOf()</code>.
 * See {@link java.lang.String} for more details.
 * 
 * @author andrew.cassin
 *
 */
public class Region implements SerializableInterface<RegionInterface>, Comparable<RegionInterface>, RegionInterface {
	private String m_label;
	private int    m_start;
	private int    m_end;
	private String m_unique_id;
	private static int m_id = 1;	// also never to be persisted
	
	public Region() {
		this(0, 0, "");
	}

	public Region(int start, String label) {
		this(start, start+1, label);
	}
	
	public Region(int start, int end, String label) {
		assert(start >= 0 && end >= 0 && label != null);
		setLabel(label);
		setStart(start);
		setEnd(end);
		setID(null);		// compute a unique ID
	}
	
	public String getLabel() {
		return m_label;
	}
	
	public void setLabel(String l) {
		m_label = l;
	}
	
	public final String getID() {
		return m_unique_id;	// guaranteed non-null
	}
	
	public String getIDPrefix() {
		return "reg";
	}
	
	/**
	 * Note: the prefix for the ID specifies the type of region eg. ScoredRegion, Region etc. so make
	 * sure you supply the correct prefix for the class of region
	 * 
	 * @param new_id
	 */
	public void setID(String new_id) {
		if (new_id == null) {	
			new_id = getIDPrefix() + m_id++;
		}
		m_unique_id = new_id;
	}
	
	/**
	 * Returns true if the region corresponds to a single residue, false otherwise.
	 * Renderers may use this to render a site differently to a longer region
	 * 
	 * @return
	 */
	public boolean isSingleSite() {
		return (m_end - 1 <= m_start);
	}
	
	/**
	 * Returns the current start value
	 */
	public int getStart() {
		return m_start;
	}

	public int getEnd() {
		return m_end;
	}
	
	public int getOffset() {
		return 0;
	}
	
	/**
	 * Returns the start relative to zero (ie. taking the current offset into account)
	 * @return
	 */
	@Override
	public int getZStart() {
		return m_start - getOffset();
	}
	
	@Override
	public int getZEnd() {
		return m_end - getOffset();
	}
	
	public void setStart(int start) {
		m_start = start;
	}
	
	public void setEnd(int end) {
		m_end = end;
	}
	
	@Override
	public void serialize(DataCellDataOutput output) throws IOException {
		assert(output != null);
		output.writeUTF(m_unique_id);
		output.writeInt(getStart());	// write out zero-relative offsets to avoid have to persist offset too
		output.writeInt(getEnd());
		if (m_label == null) 			// avoid KNIME serialization exceptions at all costs!
			output.writeUTF("");
		else
			output.writeUTF(m_label);
	}

	@Override
	public RegionInterface deserialize(DataCellDataInput input) throws IOException {
		assert(input != null);
		// NB: unique id has already been read and set in the instance to construct this
		m_start = input.readInt();
		m_end   = input.readInt();
		m_label = input.readUTF();
		return this;
	}

	@Override
	public int compareTo(RegionInterface o) {
		int a_start = this.getZStart();
		int b_start = o.getZStart();
		if (a_start < b_start)
			return -1;
		else if (a_start > b_start) {
			return 1;
		} else {
			int a_end = getZEnd();
			int b_end = o.getZEnd();
			if (a_end < b_end)
				return -1;
			else if (a_end > b_end)
				return 1;
			else
				return 0;
		}
	}
	
	@Override
	public String toString() {
		return ""+ getZStart() +"-"+ getZEnd() +" "+m_label;
	}
	
	/**
	 * The returned map column names and types must correspond to the subclass of <code>RegionsAnnotation</code>
	 * which they reside in.
	 * 
	 * @param prefix
	 * @return
	 */
	public Map<String,DataCell> asCells(String prefix) {
		Map<String,DataCell> ret = new HashMap<String,DataCell>();
		ret.put(prefix+": Label", new StringCell(getLabel()));
		String from = " (from 1)";
		ret.put(prefix+": Start"+from, new IntCell(1+getZStart()));
		ret.put(prefix+": End"+from, new IntCell(1+getZEnd()));
		return ret;
	}

	@Override
	public void setRegionOfInterest(DenseBitVector bv) {
		// NO-OP
	}

	@Override
	public SequenceValue getFeatureSequence(SequenceValue sv) {
		int end = getZEnd();
		int start = getZStart();
		if (end >= start) {
			try {
				if (end > sv.getLength())
					end = sv.getLength();
				String feature = sv.getStringValue().substring(start, end);
				String accsn = sv.getID()+"_"+start+"-"+end;
				return new SequenceImpl(sv.getSequenceType(), accsn, feature);
			} catch (Exception e) {
				return null;
			}
		} 
		// else... fail since an OrientedRegion should have been used (eg. reverse frame)
		return null;
	}

	@Override
	public String asGFF(SequenceValue sv, Track t) {
		StringBuilder sb = new StringBuilder(1024);
		sb.append(sv.getID());
		sb.append("\t");
		sb.append(t.getName());
		sb.append("\t");
		sb.append(getLabel());
		sb.append("\t");
		sb.append(1+getZStart());
		sb.append("\t");
		sb.append(1+getZEnd());
		sb.append("\t0.0\t.\t.\t");
		return sb.toString();
	}
}
