package au.edu.unimelb.plantcell.core.regions;

import java.io.IOException;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.def.StringCell;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;

/**
 * 
 * @author andrew.cassin
 *
 */
public class FramedScoredRegion extends ScoredRegion {
	private boolean m_plus_strand;
	private int     m_frame;
	
	public FramedScoredRegion() {
		this(0, "");
	}

	public FramedScoredRegion(int start, String label) {
		this(0, start, start+1, label, 0.0, true);
	}
	
	public FramedScoredRegion(int offset, int start, int end, String label, double score, boolean plus_strand) {
		super(offset, start, end, label, score);
		setForwardStrand(plus_strand);
		setCodonStart(-1);
	}
	
	public boolean isForwardStrand() {
		return m_plus_strand;
	}
	
	public String getStrand() {
		String ret = "+";
		if (!isForwardStrand()) {
			ret = "-";
		}
		return ret;
	}
	
	public int getCodonStart() {
		return m_frame;
	}
	
	public String getFrame() {
		String ret = getStrand();
		int  frame = getCodonStart();
		if (frame < 0)
			return ret;
		return ret+(frame+1);
	}
	
	public void setCodonStart(int new_start) {
		assert (new_start >= -1 && new_start < 3);		// -1 = unknown, 0, 1 or 2
		m_frame = new_start;
	}
	
	@Override 
	public String getIDPrefix() {
		return "fs";
	}
	
	public void setForwardStrand(boolean plus) {
		m_plus_strand = plus;
	}
	
	@Override
	public void serialize(DataCellDataOutput output) throws IOException {
		super.serialize(output);
		output.writeBoolean(m_plus_strand);
		output.writeInt(m_frame);
	}

	@Override
	public RegionInterface deserialize(DataCellDataInput input) throws IOException {
		super.deserialize(input);
		m_plus_strand = input.readBoolean();
		m_frame = input.readInt();
		return this;
	}
	
	@Override
	public Map<String,DataCell> asCells(String prefix) {
		Map<String,DataCell> ret = super.asCells(prefix);
		DataCell fc = new StringCell(getFrame());
		ret.put(prefix+": Frame", fc);
		return ret;
	}

	@Override
	public String asGFF(SequenceValue sv, Track t) {
		String[]  fields = super.asGFF(sv, t).split("\\t+");
		
		StringBuilder sb = new StringBuilder(1024);
		fields[6] = getStrand();
		for (String f : fields) {
			sb.append(f);
			sb.append("\t");
		}
		return sb.toString().trim();
	}
}
