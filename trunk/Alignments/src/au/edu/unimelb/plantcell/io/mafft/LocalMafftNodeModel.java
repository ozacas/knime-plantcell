package au.edu.unimelb.plantcell.io.mafft;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.LogOutputStream;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.ExternalProgram;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.muscle.AbstractAlignerNodeModel;
import au.edu.unimelb.plantcell.io.muscle.AppendAlignmentCellFactory;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentCellFactory;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue.AlignmentType;

/**
 * Performs a local mafft alignment (only supported on windows for now)
 * 
 * @author andrew.cassin
 *
 */
public class LocalMafftNodeModel extends AbstractAlignerNodeModel {	
	public static final String CFGKEY_ROOT         = "mafft root folder";
	public static final String CFGKEY_SEQUENCES    = "sequences-column";
	public static final String CFGKEY_LOG_STDERR   = "log-stderr";
	public static final String CFGKEY_ALGO         = "algorithm";
	public static final String CFGKEY_USER_DEFINED = "user-defined-options";
	
	// order is VERY important and cannot be changed without care!
	public static final String[] TRADEOFFS = new String[] { "Auto", "User-defined", "FFT-NS-1", "FFT-NS-2", 
													"FFT-NS-i (max 1000 cycles)", "L-INS-i", "G-INS-i", "E-INS-i" };
	
	// user configured and persisted state
	private SettingsModelString m_root            = new SettingsModelString(CFGKEY_ROOT, "");
	private SettingsModelString m_input_sequences = new SettingsModelString(CFGKEY_SEQUENCES, "");
	private SettingsModelBoolean m_log            = new SettingsModelBoolean(CFGKEY_LOG_STDERR, Boolean.FALSE);
	private SettingsModelString  m_algo           = new SettingsModelString(CFGKEY_ALGO, TRADEOFFS[0]);
	private SettingsModelString m_user_defined    = new SettingsModelString(CFGKEY_USER_DEFINED, "");
	// state which is used during execute()
	private File prog;		// mafft executable path or null on error
	private final Map<String,AlignmentValue> m_view_model = new HashMap<String,AlignmentValue>();
	
	
	protected LocalMafftNodeModel() {
		super(1,1);
		m_user_defined.setEnabled(m_algo.getStringValue().equals(TRADEOFFS[1]));
		reset();
	}
	
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
		logger.info("Performing alignments on "+inData[0].getRowCount()+" input rows.");
		
		final int seqs_idx = inData[0].getDataTableSpec().findColumnIndex(m_input_sequences.getStringValue());
		if (seqs_idx < 0)
			throw new InvalidSettingsException("Cannot find input sequences - re-configure?");
		DataTableSpec outSpec = make_output_spec(inData[0].getSpec(), seqs_idx);
		
		prog = findMafft();
    	if (prog == null)
    		throw new InvalidSettingsException("Unable to locate/run "+getAlignmentLogName()+": is installation correct?");
    	logger.info("Using mafft program: "+prog.getAbsolutePath());
    	
		// if the input sequences are groupby'ed then we do the calculation this way...
    	if (isCollectionOfSequencesColumn(inData[0].getDataTableSpec().getColumnSpec(seqs_idx))) {
    		ColumnRearranger rearranger = new ColumnRearranger(inData[0].getSpec());    		
    		rearranger.append(new AppendAlignmentCellFactory(outSpec, seqs_idx, this));
    		BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], rearranger, exec.createSubProgress(1.0));
    		
            return new BufferedDataTable[]{out};
    	} else {
    		// otherwise we do the groupby and then create a single-cell table as output from the alignment
    		MyDataContainer c = new MyDataContainer(exec.createDataContainer(make_output_spec(inData[0].getSpec(), seqs_idx)), "Aln");
    		SequenceType   st = SequenceType.UNKNOWN;
			final Map<UniqueID,SequenceValue> seq_map = new HashMap<UniqueID,SequenceValue>();
			for (DataRow r : inData[0]) {
				DataCell cell = r.getCell(seqs_idx);
				if (cell instanceof SequenceValue) {
					SequenceValue sv = (SequenceValue)cell;
					if (st != SequenceType.UNKNOWN && st != sv.getSequenceType()) {
						throw new InvalidSettingsException("Cannot mix sequence types (eg. AA versus NA) in sequence column on row: "+r.getKey().getString());
					} else {
						st = sv.getSequenceType();
					}
					seq_map.put(new UniqueID(), sv);
				}
			}
			
			final String rowid = "Alignment1";
			logWarningAboutAlignments(seq_map, st, rowid);
    		c.addRow(new DataCell[] { runAlignmentProgram(seq_map, rowid, st)} );
    		return new BufferedDataTable[] {c.close()};
    	}
	}
    
	private File findMafft() throws InvalidSettingsException {
		String[] attempts = new String[] { "mafft.bat", "mafft", "mafft.exe" };
		// first attempt failed, so now try ExternalProgram to see if it can find it!
		File mafft_folder = getMafftFolder();
		Collection<File> folders_to_try = ExternalProgram.addSystemPathExecutablePaths();
		List<String> strs_to_try = new ArrayList<String>();
		if (mafft_folder != null)
			strs_to_try.add(mafft_folder.getAbsolutePath());
		for (File f : folders_to_try) {
			strs_to_try.add(f.getAbsolutePath());
		}
		File mafft = ExternalProgram.findReallyHard(attempts, strs_to_try.toArray(new String[0]));
		return mafft;
	}

	/**
	 * Append the relevant arguments to the chosen command line for the MAFFT algorithm chosen by user configuration
	 * 
	 * @param cmdLine  command line instance to modify
	 * @param algo     mafft algorithm chosen
	 * @throws InvalidSettingsException
	 */
	private void addAlgoOptions(final CommandLine cmdLine, final String algo) throws InvalidSettingsException {
		if (algo == null || algo.length() < 1 || algo.equals(TRADEOFFS[0])) {
			cmdLine.addArgument("--auto");
		} else if (algo.equals(TRADEOFFS[2])) {
			cmdLine.addArgument("--retree");
			cmdLine.addArgument("1");
		} else if (algo.equals(TRADEOFFS[3])) {
			cmdLine.addArgument("--retree");
			cmdLine.addArgument("2");
		} else if (algo.equals(TRADEOFFS[4])) {
			cmdLine.addArgument("--maxiterate");
			cmdLine.addArgument("1000");
		} else if (algo.equals(TRADEOFFS[5])) {
			cmdLine.addArgument("--localpair");
			cmdLine.addArgument("--maxiterate");
			cmdLine.addArgument("1000");
		} else if (algo.equals(TRADEOFFS[6])) {
			cmdLine.addArgument("--globalpair");
			cmdLine.addArgument("--maxiterate");
			cmdLine.addArgument("1000");
		} else if (algo.equals(TRADEOFFS[7])) {
			cmdLine.addArgument("--genafpair");
			cmdLine.addArgument("--maxiterate");
			cmdLine.addArgument("1000");
		} else if (algo.equals(TRADEOFFS[1])) {
			String[] params = m_user_defined.getStringValue().split("\\s+");
			for (String param : params) {
				cmdLine.addArgument(param);
			}
		} else {
			throw new InvalidSettingsException("Unknown algorithm: "+algo);
		}
	}
	
	private File getMafftFolder() {
		File root = new File(m_root.getStringValue());
	
		File[] paths =  new File[] { root, new File(root, "mafft-win") };
		for (File p : paths) {
			if (p.exists() && p.isDirectory()) {
				File got_batch = new File(p, "mafft.bat");
				if (got_batch.exists() && got_batch.canExecute())
					return p;
			}
		}
		
		return null;
	}
	
	 /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

    	final int seq_idx = inSpecs[0].findColumnIndex(m_input_sequences.getStringValue());
    	DataTableSpec  outSpec = make_output_spec(inSpecs[0], seq_idx);
        return new DataTableSpec[]{outSpec};
    }

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		 m_root.saveSettingsTo(settings);
         m_input_sequences.saveSettingsTo(settings);
         m_log.saveSettingsTo(settings);
         m_algo.saveSettingsTo(settings);
         m_user_defined.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		 m_root.validateSettings(settings);
         m_input_sequences.validateSettings(settings);
         m_log.validateSettings(settings);
         m_algo.validateSettings(settings);
         m_user_defined.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		 m_root.loadSettingsFrom(settings);
         m_input_sequences.loadSettingsFrom(settings);
         m_log.loadSettingsFrom(settings);
         m_algo.loadSettingsFrom(settings);
         m_user_defined.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
		super.reset();
	}

	@Override
	public List<String> getAlignmentRowIDs() {
		ArrayList<String> ret = new ArrayList<String>();
		ret.addAll(m_view_model.keySet());
		return ret;
	}

	@Override
	public AlignmentValue getAlignment(String row_id) {
		return m_view_model.get(row_id);
	}

	@Override
	protected File getAlignmentProgram() {
		return prog;
	}

	@Override
	protected String getAlignmentLogName() {
		return "Mafft";
	}

	@Override
	public CommandLine makeCommandLineArguments(final File fasta_file,
			final SequenceType alignment_sequence_type) throws Exception {
		CommandLine cmdLine = new CommandLine(prog);
    	addAlgoOptions(cmdLine, m_algo.getStringValue());
    	cmdLine.addArgument(fasta_file.getName());
    	return cmdLine;
	}

	@Override
	public DataCell makeAlignmentCellAndPopulateResultsMap(LogOutputStream tsv,
			SequenceType st, String row_id) throws IOException {
		assert(st != null && tsv != null && row_id != null);
		DataCell alignment_cell;
		if (st.isProtein())
               alignment_cell = AlignmentCellFactory.createCell(tsv.toString(), AlignmentType.AL_AA);
		else
               alignment_cell = AlignmentCellFactory.createCell(tsv.toString(), AlignmentType.AL_NA);
		if (alignment_cell != null && alignment_cell instanceof AlignmentValue) {
			// since a row_id must be unique, something is very wrong if the row_id is already in the map
			assert(!m_view_model.containsKey(row_id));
			m_view_model.put(row_id, (AlignmentValue) alignment_cell);
		}
		return alignment_cell;
	}

}
