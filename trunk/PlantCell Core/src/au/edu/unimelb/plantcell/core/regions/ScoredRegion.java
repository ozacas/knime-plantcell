package au.edu.unimelb.plantcell.core.regions;

import java.io.IOException;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;

/**
 * Represents a region with a numeric value (range unspecified) that represents a score of that region.
 * This class is used directly, but also subclassed too.
 * 
 * @author andrew.cassin
 *
 */
public class ScoredRegion extends Region {
	private double m_score;
	private int    m_offset;
	
	public ScoredRegion() {
		this(0, 0, "");
	}
	
	public ScoredRegion(int start, String label, double score) {
		this(0, start, start+1, label, score);
	}
	
	public ScoredRegion(int start, int end, String label)  {
		this(0, start, end, label, 0.0);
	}
	
	public ScoredRegion(int offset, int start, int end, String label, double score) {
		super(start, end, label);
		setOffset(offset);
		setScore(score);
	}
	
	public void setOffset(int new_offset) {
		m_offset = new_offset;
	}
	
	public void setScore(double score) {
		m_score = score;
	}
	
	@Override
	public int getOffset() {
		return m_offset;
	}
	
	public double getScore() {
		return m_score;
	}
	
	@Override 
	public String getIDPrefix() {
		return "sreg";
	}
	
	@Override
	public void serialize(DataCellDataOutput output) throws IOException {
		super.serialize(output);
		output.writeDouble(m_score);
		output.writeInt(m_offset);
	}

	@Override
	public RegionInterface deserialize(DataCellDataInput input) throws IOException {
		super.deserialize(input);
		m_score = input.readDouble();
		m_offset= input.readInt();
		return this;
	}
	
	@Override
	public Map<String,DataCell> asCells(String prefix) {
		Map<String,DataCell> ret = super.asCells(prefix);
		double    d = getScore();
		DataCell dc = Double.isNaN(d) ? DataType.getMissingCell() : new DoubleCell(d);
		ret.put(prefix+": Score", dc);
		return ret;
	}

	@Override
	public String asGFF(SequenceValue sv, Track t) {
		String[] fields = super.asGFF(sv, t).split("\\t+");
		
		StringBuilder sb = new StringBuilder(1024);
		double d = getScore();
		if (Double.isNaN(d)) {
			fields[5] = ".";
		} else {
			fields[5] = Double.toString(d);
		}
		for (String f : fields) {
			sb.append(f);
			sb.append("\t");
		}
		return sb.toString().trim();
	}
}
