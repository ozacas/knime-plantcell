package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.LogOutputStream;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackCreator;
import au.edu.unimelb.plantcore.core.regions.RegionsAnnotation;
import au.edu.unimelb.plantcore.core.regions.ScoredRegion;

public class GFFReader extends LogOutputStream {
	private boolean is_cancelled = false;
	private ExecutionContext m_exec;
	private MyDataContainer m_c;
	private final HashMap<UniqueID,List<ScoredRegion>> annotations = new HashMap<UniqueID,List<ScoredRegion>>();
	private Map<UniqueID,SequenceValue> m_batch;
	
	public GFFReader(ExecutionContext exec, MyDataContainer c, final Map<UniqueID, SequenceValue> batch_map) {
		assert(m_exec != null && m_c != null && m_batch != null);
		m_exec = exec;
		m_c = c;
		m_batch = batch_map;
	}
	
	@Override
	protected void processLine(String line, int lvl) {
		if (line.startsWith("#"))
			return;
		
		String[] fields = line.split("\\t+");
		if (fields.length == 9) {
			DataCell[] cells = new DataCell[9];		// must match EmbossPredictorNodeModel.make_output_spec()
			
			ScoredRegion sr = getScoredRegion(fields);
			UniqueID uid = new UniqueID(fields[0]);
			SequenceValue sv = m_batch.get(uid);
			if (sv == null)
				return;
			
			cells[0] = new StringCell(sv.getID());
			cells[1] = new StringCell(fields[1]);
			cells[2] = new StringCell(fields[2]);
			cells[3] = new IntCell(sr.getStart());
			cells[4] = new IntCell(sr.getEnd());
			cells[5] = new DoubleCell(sr.getScore());
			cells[6] = new StringCell(fields[6]);
			cells[7] = DataType.getMissingCell();
			cells[8] = new StringCell(fields[8]);
			m_c.addRow(cells);
			
			if (!annotations.containsKey(uid)) {
				annotations.put(uid, new ArrayList<ScoredRegion>());
			}
			List<ScoredRegion> l = annotations.get(uid);
			l.add(sr);
		}
		
		try {
			m_exec.checkCanceled();
		} catch (Exception e) {
			is_cancelled = true;
		}
	}
	
	private ScoredRegion getScoredRegion(String[] fields) {
		int start = 0;
		int end   = 0;
		double score = 0.0;
		try { 
			start = Integer.parseInt(fields[3]);
			end   = Integer.parseInt(fields[4]);
			score = Double.parseDouble(fields[5]);
		} catch (NumberFormatException e) {
		}
		ScoredRegion sr = new ScoredRegion(1, start, end, fields[2]+" "+fields[8], score);
		sr.setOffset(1);		// Q: all emboss programs start at 1... true?
		return sr;
	}

	@Override
	public void write(byte[] b, int a, int c) throws IOException {
		if (is_cancelled) {
			throw new IOException("EMBOSS cancelled.");
		}
		super.write(b, a, c);
	}
	
	@Override
	public void write(int cc) throws IOException {
		if (is_cancelled) {
			throw new IOException("EMBOSS cancelled.");
		}
		super.write(cc);
	}
	
	/**
	 * 
	 * @param prog
	 * @param c
	 * @param tc must create a track with a {@link RegionsAnnotation}
	 * @throws InvalidSettingsException
	 */
	public void save_annotations(NodeLogger l, String prog, MyDataContainer c, TrackCreator tc) 
					throws InvalidSettingsException {
		int total_features = 0;
		for (UniqueID id : m_batch.keySet()) {
			SequenceCell      sc = new SequenceCell(m_batch.get(id));
			Track              t = sc.getTrackByName(Track.EMBOSS_TRACKS+prog, tc);
			RegionsAnnotation ra = (RegionsAnnotation) t.getAnnotation();
			List<ScoredRegion> annots = annotations.get(id);
			if (annots != null) {
				ra.addAll(annots, false);
				total_features += annots.size();
			}
			c.addRow(new DataCell[] { sc });
		}
		annotations.clear();	// ready for next batch
		if (total_features < 1) {
			l.warn("No features detected in batch of "+m_batch.size()+" sequences. Are settings correct?");
		}
	}
}
