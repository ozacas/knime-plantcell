package au.edu.unimelb.plantcore.core.regions;

import java.io.IOException;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

/**
 * Records a single blast hit record associated with a region of a sequence. The fields
 * tracked are similar to "-outfmt 7" ie. tab separated output values from BLAST.
 * 
 * @author andrew.cassin
 *
 */
public class BlastHitRegion extends ScoredRegion {
	private String m_q_id;
	private double m_evalue;
	private String m_frame;
	private int    m_alignment_length;
	private double m_identity;
	// NB: bitscore is kept by the superclass it is Score member
	
	/**
	 * Default constructor, used only during deserialisation
	 */
	public BlastHitRegion() {
		this(-1, -1, "", "");
	}
	
	public BlastHitRegion(int qstart, int qend, String query_id, String subject_id) {
		super(qstart, qend, subject_id);
		setQuery(query_id);
		setFrame("");
		setEvalue(-1d);
		setScore(-1d);
		setAlignmentLength(-1);
		setIdentity(-1d);
	}
	
	public BlastHitRegion(Map<String,String> fields) {
		for (String key : fields.keySet()) {
			String val = fields.get(key);
			if (key.equals("q. start")) {
				this.setStart(Integer.parseInt(val));
			} else if (key.equals("q. end")) {
				this.setEnd(Integer.parseInt(val));
			} else if (key.equals("evalue")) {
				setEvalue(Double.parseDouble(val));
			} else if (key.equals("bit score")) {
				setScore(Double.parseDouble(val));
			} else if (key.equals("% identity")) {
				setIdentity(Double.parseDouble(val));
			} else if (key.equals("alignment length")) {
				setAlignmentLength(Integer.parseInt(val));
			} else if (key.equals("frame")) {
				setFrame(val);
			} else if (key.equals("query id")) {
				setQuery(val);
			} else if (key.equals("subject id")) {
				setSubject(val);
			}
		}
	}
	
	public String getQuery() {
		return m_q_id;
	}
	
	public String getSubject() {
		return getLabel();
	}
	
	public double getEvalue() {
		return m_evalue;
	}
	
	public double getIdentity() {
		return m_identity;
	}
	
	public String getFrame() {
		return m_frame;
	}
	
	public int getAlignmentLength() {
		return m_alignment_length;
	}
	
	private void setQuery(String val) {
		m_q_id = val;
	}

	private void setSubject(String val) {
		this.setLabel(val);
	}
	
	public void setEvalue(double ev) {
		m_evalue = ev;
	}
	
	public void setFrame(String frame) {
		m_frame = frame;
	}
	
	public void setAlignmentLength(int len) {
		m_alignment_length = len;
	}
	
	public void setIdentity(double id) {
		m_identity = id;
	}
	
	@Override
	public String getIDPrefix() {
		return "bhreg";		// must be unique to this Region subclass
	}
	
	@Override
	public void serialize(DataCellDataOutput output) throws IOException {
		super.serialize(output);
		
		output.writeUTF(m_q_id);
		output.writeDouble(m_evalue);
		output.writeDouble(m_identity);
		output.writeInt(m_alignment_length);
		if (m_frame == null)
			output.writeUTF("");
		else 
			output.writeUTF(m_frame);
	}
	
	@Override 
	public RegionInterface deserialize(DataCellDataInput input) throws IOException {
		super.deserialize(input);
		m_q_id             = input.readUTF();
		m_evalue           = input.readDouble();
		m_identity         = input.readDouble();
		m_alignment_length = input.readInt();
		m_frame            = input.readUTF();
		return this;
	}
	
	public Map<String,DataCell> asCells(String prefix) {
		Map<String,DataCell> ret = super.asCells(prefix);
		ret.put(prefix+": Query ID", new StringCell(getQuery()));
		ret.put(prefix+": E-Value", new DoubleCell(getEvalue()));
		ret.put(prefix+": %Identity", new DoubleCell(getIdentity()));
		ret.put(prefix+": Alignment length", new IntCell(getAlignmentLength()));
		String frame = getFrame();
		ret.put(prefix+": Frame", (frame == null) ? DataType.getMissingCell() : new StringCell(frame));
		return ret;
	}
}
