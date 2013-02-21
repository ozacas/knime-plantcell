package au.edu.unimelb.plantcell.gene.prediction;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.LogOutputStream;
import org.knime.core.data.DataCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcore.core.regions.FramedScoredRegion;
import au.edu.unimelb.plantcore.core.regions.RegionsAnnotation;
import au.edu.unimelb.plantcore.core.regions.ScoredRegion;

/**
 * Logs the lines from augustus (standard output only) to the KNIME table. It is assumed
 * that 8 columns exist in the table as per the standard augustus v2.5.5 output.
 * 
 * @author andrew.cassin
 *
 */
public class AugustusLogger extends LogOutputStream {
	private boolean is_cancelled = false;
	private final ExecutionContext m_exec;
	private final MyDataContainer m_proteins;
	private StringBuilder m_sb;
	private Map<UniqueID,SequenceValue> m_batch_map;
	
	// regular expressions for processing lines
	final static Pattern re_start_gene   = Pattern.compile("^#\\s+start\\s+gene\\s+(\\w+)\\s*$");
	final static Pattern re_end_gene     = Pattern.compile("^#\\s+end\\s+gene\\s+(\\w+)\\s*$");
	final static Pattern re_protein_start= Pattern.compile("^#\\s+protein\\s+sequence\\s+=\\s+\\[(.*)$");
	final static Pattern re_protein_end  = Pattern.compile("^#\\s*(.*)\\]\\s*$");
	final static Pattern re_protein_middle=Pattern.compile("^#\\s*(\\S+)\\s*$");
	final static Pattern re_feature_line = Pattern.compile("^(\\S+)\\s*AUGUSTUS\\s");
	
	// internal state as parsing of the output proceeds (not persisted)
	private boolean in_protein_sequence;
	private String m_last_seq_id;		// id from user supplied fasta file
	private String m_last_gene_id;		// last "start gene" id from augustus during processing
	private HashSet<String> m_wanted;
	private static int done = 0;
	private boolean m_report_all;
	
	public AugustusLogger(final ExecutionContext exec,
			final MyDataContainer c_proteins, Map<UniqueID, SequenceValue> batch_map, String[] wanted) {
		m_exec            = exec;
		m_proteins        = c_proteins;
		in_protein_sequence = false;
		m_last_seq_id       = "";
		m_last_gene_id      = "";
		m_batch_map         = batch_map;
		m_wanted = new HashSet<String>();
		m_report_all = false;
		for (String w : wanted) {
			String lw = w.toLowerCase().trim();
			if (lw.startsWith("all")) {
				m_report_all = true;
				continue;
			}
			m_wanted.add(lw);
		}
		done                = 0;
	}
	
	@Override
	protected void processLine(String line, int lvl) {
		try {	// NB: be careful to catch regexp expressions so as not to deadlock the caller
			Matcher m = re_start_gene.matcher(line);
			if (m.find()) {
				m_last_gene_id = m.group(1);
				return;
			}
			
			m = re_feature_line.matcher(line);
			if (m.find()) {
				String cur_id   = m.group(1);
				// be careful not to use String but UniqueID instances otherwise get() -> equals() will always be false
				try {
					SequenceValue sv= m_batch_map.get(new UniqueID(cur_id));
					if (sv == null)
						m_last_seq_id = "";
					else
						m_last_seq_id   = sv.getID();
				} catch (NumberFormatException nfe) {
					// be silent for poor regular expression matches
				}
				String[] fields = line.split("\\t+");
				if (fields.length == 9) 
					add_feature(fields);
				else {
					//if (m_logger != null && !line.startsWith("#")) {
					//	m_logger.warn("Encountered strange line: "+line);
					//}
				}
				
				if (done++ % 10 == 0) {
					m_exec.setProgress("Processed "+m_last_seq_id);
					try {
						m_exec.checkCanceled();
					} catch (CanceledExecutionException ce) {
						is_cancelled = true;
					}
				}
				return;
			}
			
			if (in_protein_sequence) {
				m = re_protein_end.matcher(line);
				if (m.find()) {
					m_sb.append(m.group(1));
					in_protein_sequence = false;
					add_protein(m_sb.toString());
				} else {
					// append the entire line (minus the leading '#' and any whitespace) to the protein sequence
					m_sb.append(line.substring(1).trim());
				}
				return;
			}
			
			if (!line.startsWith("#"))
				return;
			
			Matcher m2 = re_protein_start.matcher(line);
			if (!m2.find())
				return;
			
			in_protein_sequence = true;
			String tmp  = m2.group(1).trim();
			int end_idx = tmp.indexOf(']');		//  short one-line protein?
			if (end_idx >= 0) {
				add_protein(tmp.substring(0,end_idx));
				in_protein_sequence = false;
			} else {
				m_sb = new StringBuilder(10 * 1024);
				m_sb.append(tmp);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void add_feature(String[] fields) throws InvalidSettingsException {
		if (fields.length != 9) {
			//m_logger.warn("Bad feature line: "+fields.toString());
			return;
		}
		SequenceValue sv = m_batch_map.get(new UniqueID(fields[0]));
		if (sv == null)
			return;
		Track t = sv.getTrackByName(Track.GENE_PREDICTION_AUGUSTUS, AugustusNodeModel.getTrackCreator());
		RegionsAnnotation ra = (RegionsAnnotation) t.getAnnotation();
		int start = 0;
		int end   = 0;
		try {
			start = Integer.parseInt(fields[3]);
			end   = Integer.parseInt(fields[4]);
			end++;	// Region semantics are exclusive of end so...
		} catch (NumberFormatException nfe) {
			// no valid region defined so...
			return;
		}
		double posterior_probability = Double.NaN;
		try {
			posterior_probability = Double.parseDouble(fields[5]);
		} catch (NumberFormatException nfe) {
			// silent
		}
		
		// add only features of interest to the user
		if (!m_report_all && 
				!m_wanted.contains(fields[2].toLowerCase()))
			return;
		
		String frame = fields[6];
		if (!fields[7].equals(".")) {
			frame += String.valueOf(Integer.valueOf(fields[7]) + 1);
		} else {
			frame += "1";
		}
		String    label = fields[2] + ": " + frame + ", " + fields[8];
		ScoredRegion sr = new FramedScoredRegion(1, start, end, label, posterior_probability, frame.startsWith("+"));
		ra.addRegion(sr);
	}

	/**
	 * Called when a protein sequence has been completed read from the augustus output to add
	 * a row to the corresponding output port
	 * 
	 * @param aa_seq
	 * @throws InvalidSettingsException 
	 */
	private void add_protein(String aa_seq) throws InvalidSettingsException {
		DataCell[] cells = new DataCell[1];
		SequenceCell sc = new SequenceCell(SequenceType.AA, m_last_seq_id + "_" + m_last_gene_id, aa_seq);
		cells[0] = sc;
		m_proteins.addRow(cells);
		m_sb = new StringBuilder(10 * 1024);
	}

	@Override
	public void write(byte[] b, int a, int c) throws IOException {
		if (is_cancelled) {
			throw new IOException("Cancelled by user request.");
		}
		super.write(b, a, c);
	}
	
	@Override
	public void write(int cc) throws IOException {
		if (is_cancelled) {
			throw new IOException("Cancelled by user request.");
		}
		super.write(cc);
	}
}
