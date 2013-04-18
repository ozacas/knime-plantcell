package au.edu.unimelb.plantcell.blast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.LogOutputStream;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcore.core.regions.BlastHitRegion;

/**
 * Processes "-outfmt 7" blast-style output into the KNIME table. Other formats not supported at this time.
 * 
 * @author andrew.cassin
 *
 */
public class TabbedCSVLogger extends LogOutputStream {
	private NodeLogger m_logger;
	private final ExecutionContext m_exec;
	private MyDataContainer m_c_tsv = null;		// TSV output port for BLAST
	private int n_cols;
	private boolean is_cancelled;
	private final Map<String,Integer> m_col2idx = new HashMap<String,Integer>();
	private final Map<Integer,String> m_idx2col = new HashMap<Integer,String>();
	private Map<UniqueID,SequenceValue> m_batch;
	private final Map<UniqueID,List<BlastHitRegion>> m_results;
	
	/**
	 * Constructor for {@link TabbedCSVLogger}. 
	 * @param l
	 * @param exec
	 */
	public TabbedCSVLogger(final NodeLogger l, final ExecutionContext exec) {
		assert(exec != null);
		m_logger      = l;
		m_exec        = exec;
		n_cols        = 0;
		is_cancelled  = false;
		m_results     = new HashMap<UniqueID, List<BlastHitRegion>>();
	}
	
	@Override
	protected void processLine(String line, int lvl) {
		if (line.startsWith("#")) {
			// take note of query & fields lines
			if (line.startsWith("# Query: ")) {
				try {
					m_exec.checkCanceled();
				} catch (Exception e) {
					is_cancelled = true;
				}
				// FALLTHRU
			} else if (line.startsWith("# Fields: ")) {
				if (m_c_tsv == null) {
					String remaining = line.substring(10);
					String[] fields = remaining.split("\\s*,\\s+");
					n_cols = fields.length;
					m_logger.info("Found "+n_cols+" columns in results from BLAST");
					m_col2idx.clear();
					m_idx2col.clear();
					
					DataColumnSpec[] cols = new DataColumnSpec[n_cols];
					for (int i=0; i<fields.length; i++) {
						cols[i] = new DataColumnSpecCreator(fields[i], StringCell.TYPE).createSpec();
						m_col2idx.put(fields[i], new Integer(i));
						m_idx2col.put(new Integer(i), fields[i]);
					}
					m_c_tsv = new MyDataContainer(m_exec.createDataContainer(new DataTableSpec(cols)), "Hit");
				}
				// FALLTHRU
			}
			// all done with this line
			return;
		}
		
		// skip blank lines
		if (line.trim().length() < 1) {
			return;
		}
		
		// else
		String[] tabbed_fields = line.split("\t");
		
		DataCell[] cells = new DataCell[n_cols];
		Map<String,String> fields = new HashMap<String,String>();
		SequenceValue sv = null;
		Integer want_idx = m_col2idx.get("query id");
		UniqueID     uid = null;
		for (int i=0; i<n_cols; i++) {
			// map input query id to sequence batch id?
			if (want_idx != null && want_idx.intValue() == i) {
				try {
					uid = new UniqueID(tabbed_fields[i]);
					sv = m_batch.get(uid);
					if (sv != null)
						tabbed_fields[i] = sv.getID();
				} catch (InvalidSettingsException ise) {		// handle invalid sequence ID
					ise.printStackTrace();
				}
			} 
			cells[i] = new StringCell(tabbed_fields[i]);
			fields.put(m_idx2col.get(new Integer(i)), tabbed_fields[i]);
		}
		
		// add hit to list for query
		try {
			BlastHitRegion bhr = new BlastHitRegion(fields);

			bhr.setOffset(1);
			if (uid != null) {
				List<BlastHitRegion> l = m_results.get(uid);
				if (l == null) {
					l = new ArrayList<BlastHitRegion>();
					m_results.put(uid, l);
				}
				l.add(bhr);
				//Logger.getAnonymousLogger().info(sv.getID()+" has size "+l.size());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (m_c_tsv != null) {
			m_c_tsv.addRow(cells);
		}
	}

	
	/**
	 * This routine may only be called once after the BLAST has finished.
	 * 
	 * @return
	 */
	public BufferedDataTable getTSVTable() {
		if (m_c_tsv != null) { 
			BufferedDataTable ret = m_c_tsv.close();
			m_c_tsv = null;
			return ret;
		}
		
		// HACK BUG TODO: ok, so something went wrong in which case we fudge a spec to keep KNIME happy
		DataColumnSpec[] cols = new DataColumnSpec[1];
		cols[0] = new DataColumnSpecCreator("BLAST failed!", StringCell.TYPE).createSpec();
		BufferedDataContainer c = m_exec.createDataContainer(new DataTableSpec(cols));
		c.close();
		return c.getTable();
	}
	
	@Override
	public void write(byte[] b, int a, int c) throws IOException {
		if (is_cancelled) {
			throw new IOException("BLAST cancelled.");
		}
		super.write(b, a, c);
	}
	
	@Override
	public void write(int cc) throws IOException {
		if (is_cancelled) {
			throw new IOException("BLAST cancelled.");
		}
		super.write(cc);
	}

	public void setBatch(final Map<UniqueID, SequenceValue> batch_map) {
		m_batch = batch_map;
		m_results.clear();
	}

	public void walkResults(BlastResultsWalker walk) {
		assert(walk != null);
		// 1. report hits from blast
		HashSet<UniqueID> hits = new HashSet<UniqueID>();
		for (UniqueID hit : m_results.keySet()) {
			walk.hit(m_batch.get(hit), m_results.get(hit));
			hits.add(hit);
		}
		
		// 2. report non-hits if available
		if (m_batch != null) {
			for (UniqueID uid : m_batch.keySet()) {
				if (!hits.contains(uid)) {
					walk.nohit(m_batch.get(uid));
				}
			}
		}
	}
}
