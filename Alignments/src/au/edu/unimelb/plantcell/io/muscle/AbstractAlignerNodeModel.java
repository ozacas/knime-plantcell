package au.edu.unimelb.plantcell.io.muscle;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;

import au.edu.unimelb.plantcell.core.ErrorLogger;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.write.fasta.FastaWriter;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentViewDataModel;
import au.edu.unimelb.plantcell.io.ws.multialign.MultiAlignmentCell;

public abstract class AbstractAlignerNodeModel extends NodeModel implements AlignmentViewDataModel  {
	protected final static NodeLogger logger = NodeLogger.getLogger("Aligner (local)");
	
	private boolean warned_small_seqs;	// initialised by reset();

	/**
	 * After this constructor call you must subsequently call <code>reset()</code> to ensure all
	 * state is correctly initialised. This class provides a shared, protected, logger instance which all alignment nodes share
	 * 
	 * @param n_in number of input ports
	 * @param n_out number of output ports
	 */
	public AbstractAlignerNodeModel(int n_in, int n_out) {
		super(n_in, n_out);
	}
	  
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

	/**
	 * Create a suitable output table specification for the node. Called during <code>configure()</code>
	 * and <code>execute()</code> this code must handle all aligners and parameters as required by KNIME platform
	 */
	 protected DataTableSpec make_output_spec(DataTableSpec inSpec, final int seq_idx) {
    	boolean has_collection_of_input_sequences = false;
    	if (seq_idx >= 0) {
    		DataColumnSpec seqSpec = inSpec.getColumnSpec(seq_idx);
    		try {
				has_collection_of_input_sequences = isCollectionOfSequencesColumn(seqSpec);
			} catch (InvalidSettingsException e) {
				has_collection_of_input_sequences = false;
				e.printStackTrace();
			}
    		// FALLTHRU
    	}
    	
    	DataColumnSpec[] cols = new DataColumnSpec[1];
		cols[0] = new DataColumnSpecCreator(getAlignmentLogName()+ " aligned sequences", MultiAlignmentCell.TYPE).createSpec();
    	if (seq_idx < 0 || !has_collection_of_input_sequences) {
    		return new DataTableSpec(cols);
    	} else {
    		return new DataTableSpec(inSpec, new DataTableSpec(cols));
    	}	
	    	
	}
	 
	/**
	 * Subclasses are required to return a valid executable when requested. This should be idempotent
	 * @return executable path to alignment program, ready for use
	 */
	protected abstract File getAlignmentProgram();
	
	/**
	 * Subclasses are required to return a valid display name for the alignment program used. Must not be null. This
	 * should, for user convenience, be idempotent.
	 * @return
	 */
	protected abstract String getAlignmentLogName();
	
	/**
	 * Return true if alignment progress made by the executable should be reported to the KNIME console. False otherwise.
	 */
	public boolean shouldLogAlignmentProgress() {
		return true;
	}
	
	/**
	 * Must call this implementation when reset is chosen by the framework/user: otherwise abstract class state
	 * will not be reset
	 */
	 @Override
	 protected void reset() {
		 warned_small_seqs = false;
	 }
	 
	/**
	  * Report an error to the current logger
	  */
	public void error(final Object msg) {
		logger.error(msg);
	}
	
	/**
	 * Checks to see if the current cell (as obtained during the execute() row iteration) is a collection or not.
	 * 
	 * @param seqs_cell
	 * @return true if a collection cell is found, false otherwise
	 */
    public boolean isValidCollectionForAlignment(final DataCell seqs_cell) {
    	if (seqs_cell == null)
    		return false;
    	
    	// list of sequence values eg. groupby list?
    	if ((seqs_cell instanceof ListCell) || (seqs_cell instanceof SetCell)) {
    		return true;
    	}
    	return false;
	}
	    
	/**
     * Returns true if the specified columnSpec is a list-of-sequences (or set-of-sequences) false otherwise.
     * 
     * @param columnSpec
     * @return
     * @throws InvalidSettingsException if bogus input given or not configured yet
     */
    protected boolean isCollectionOfSequencesColumn(final DataColumnSpec columnSpec) throws InvalidSettingsException {
		if (columnSpec == null)
			throw new InvalidSettingsException("No sequences column to check!");
		
		if (columnSpec.getType().isCollectionType() && columnSpec.getType().getCollectionElementType().isCompatible(SequenceValue.class)) {
			return true;
		}
		return false;
	}

	    
	  /**
	     * Verify that the input executable path is valid on this system and can be run. Will throw an exception if not.
	     * 
	     * @param exe path to muscle executable (must not be null)
	     * @return a File instance to the specified executable
	     * @throws IOException thrown if the path does not exist or cannot be executed
	     */
	protected File validateAlignmentProgram(final String exe) throws IOException {
		final File f = new File(exe);
    	if (!f.exists() || !f.canExecute()) {
    		throw new IOException("Alignment program not executable: "+exe);
    	}
    	return f;
	}
	
	/**
     * Report appropriate warnings for the data in the specified map. Idea is to check the sequences and decide if there are
     * data problems before doing the alignment. At the moment, not much checking of the input sequences is done.
     * 
     * @param seq_map
     * @param st
     * @param rowid
     */
    public void logWarningAboutAlignments(
				Map<UniqueID, SequenceValue> seq_map, 
				SequenceType st,
				final String rowid) {
    	assert(rowid != null);
    	
    	if (seq_map == null || seq_map.size() < 3) {
    		if (!warned_small_seqs) {
    			logger.warn("Too few sequences (less than three) for alignment: not recommended!");
    			warned_small_seqs = true;
    		}
    	}
    	logger.info("Aligning "+seq_map.size()+ " "+st+" sequences for "+rowid);
	}
    
    /**
     * Given the sequences in the specified FASTA format file and the specified sequence type, this
     * method is required to populate a CommandLine object with the appropriate arguments and return it.
     * The method is also responsible for determination of the executable to run
     * 
     * @param fasta_file
     * @param alignment_sequence_type
     * @return
     */
    public abstract CommandLine makeCommandLineArguments(final File fasta_file, SequenceType alignment_sequence_type) throws Exception;
    
    /**
     * Responsible for returning a new DataCell for the KNIME output table from the output of the alignment program
     * @param tsv the aligned output
     * @param st the type of alignment sequences (AA or NA)
     * @param row_id the row_id of the current row (which may be a little bogus if all sequence rows are being aligned at once)
     */
    public abstract DataCell makeAlignmentCellAndPopulateResultsMap(final LogOutputStream tsv, 
			final SequenceType st, final String row_id) throws IOException;
    	
    
	/**
     * Run muscle on the sequences specified in <code>seq_map</code> (which should be at least 3) using the specified muscle executable.
     * 
     * @param seq_map
     * @param muscle_exe	Existing executable for the muscle program
     * @param rowid			Used to help the user track down problems (by logging the rowid of the problem)
     * @param st			Type of sequences (AA, NA) to be aligned
     * @return
     */
    public DataCell runAlignmentProgram(final Map<UniqueID, SequenceValue> seq_map, final String rowid, final SequenceType st) {
    	File f = null;
		
		// run alignment program and return result if run went ok, else log errors
		try {
			f = File.createTempFile("output_aligner", ".fasta");
			FastaWriter fw = new FastaWriter(f, seq_map);
			fw.write();
			
			DefaultExecutor exe = new DefaultExecutor();
	    	exe.setExitValues(new int[] {0});
	    	LogOutputStream tsv = new LogOutputStream() {
	    		private StringBuffer sb = new StringBuffer(1024 * 1024);
	    		
				@Override
				protected void processLine(String arg0, int arg1) {
					if (arg0.startsWith(">")) {
						String id = arg0.substring(1);
						try {
							SequenceValue sv = seq_map.get(new UniqueID(id));
							sb.append(">"+sv.getID());
						} catch (InvalidSettingsException ise) {
							ise.printStackTrace();
							sb.append(arg0);
						}
					} else {
						sb.append(arg0);
					}
					sb.append("\n");
				}
	    		
				@Override
				public String toString() {
					return sb.toString();
				}
	    	};
	    	LogOutputStream errors = shouldLogAlignmentProgress() ? new ErrorLogger(logger, true) : new LogOutputStream() {

				@Override
				protected void processLine(String arg0, int arg1) {
				}
	    		
	    	};
	    	exe.setStreamHandler(new PumpStreamHandler(tsv, errors));
	    	exe.setWorkingDirectory(f.getParentFile());		// must match addQueryDatabase() semantics
	    	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
	    	
	    	CommandLine cmdLine = makeCommandLineArguments(f, st);
	    	exe.setWorkingDirectory(f.getParentFile());
	    	String logname = getAlignmentLogName();
	    	logger.info("Running "+logname+", command line: "+cmdLine.toString());
        	int exitCode = exe.execute(cmdLine);
        	logger.info("got exit code: "+exitCode+" from "+logname);
        	
        	if (exe.isFailure(exitCode)) {
        		logger.error(logname+" failed to align sequences in row "+rowid+" - check console messages and input data");
        		return DataType.getMissingCell();
        	}
        	
        	return makeAlignmentCellAndPopulateResultsMap(tsv, st, rowid);
		} catch (Exception e) {
			e.printStackTrace();
			return DataType.getMissingCell();
		} finally {
			if (f != null)
				f.delete();
		}
    }

}
