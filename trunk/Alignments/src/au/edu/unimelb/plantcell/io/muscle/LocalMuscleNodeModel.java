package au.edu.unimelb.plantcell.io.muscle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.ErrorLogger;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.write.fasta.FastaWriter;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentCellFactory;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue.AlignmentType;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentViewDataModel;
import au.edu.unimelb.plantcell.io.ws.multialign.MultiAlignmentCell;


/**
 * This is the model implementation of LocalMuscle.
 * Supports running a local muscle executable and integration into the KNIME-PlantCell platform
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class LocalMuscleNodeModel extends NodeModel implements AlignmentViewDataModel {
	private final NodeLogger logger = NodeLogger.getLogger("MUSCLE Aligner (local)");
	
	public static final String CFGKEY_EXE       = "muscle-exe";
	public static final String CFGKEY_SEQUENCES = "sequences-column";
	public static final String CFGKEY_LOG_STDERR= "log-stderr";
	public static final String CFGKEY_TRADEOFF  = "alignment-tradeoff";
	
	public static final String[] TRADEOFFS = new String[] { "Maximum accuracy", "Very fast", "Fastest possible" };
	
	/**
	 * user-configured node persisted state
	 */
	private SettingsModelString  m_exe         = new SettingsModelString(CFGKEY_EXE, "");
	private SettingsModelString  m_input_sequences = new SettingsModelString(CFGKEY_SEQUENCES, "");
	private SettingsModelBoolean m_log         = new SettingsModelBoolean(CFGKEY_LOG_STDERR, Boolean.FALSE);
	private SettingsModelString  m_performance = new SettingsModelString(CFGKEY_TRADEOFF, TRADEOFFS[0]);
	
	// not persisted (yet) but is intended for the node views
	private final Map<String,AlignmentValue> m_view_model = new HashMap<String,AlignmentValue>();
	
    /**
     * Constructor for the node model.
     */
    protected LocalMuscleNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	logger.info("Performing alignments on "+inData[0].getRowCount()+" input rows.");
    	logger.info("Using "+m_exe.getStringValue()+" to align input sequences.");
    	
    	ColumnRearranger rearranger = new ColumnRearranger(inData[0].getSpec());
    	BufferedDataTable out = null;
    	final int seqs_idx = inData[0].getDataTableSpec().findColumnIndex(m_input_sequences.getStringValue());
    	if (seqs_idx < 0)
    		throw new InvalidSettingsException("Cannot find input sequences - re-configure?");
    	reset();
    	
    	final File muscle_exe = validateMuscle(m_exe.getStringValue());
    
    	
		final SingleCellFactory appender = new SingleCellFactory(make_output_spec()) {
			boolean warned_small_seqs = false;
			
			@Override
			public DataCell getCell(DataRow r) {
				DataCell seqs_cell = r.getCell(seqs_idx);
				if (seqs_cell == null || !(seqs_cell instanceof ListCell || seqs_cell instanceof SetCell)) 
					return DataType.getMissingCell();
				
				Iterator<DataCell> it = null;
				if (seqs_cell instanceof ListCell) {
					it = ((ListCell)seqs_cell).iterator();
				} else { // must be set cell
					it = ((SetCell)seqs_cell).iterator();
				}
				
				// validate input sequences and create set of sequences to align...
				SequenceType st = SequenceType.UNKNOWN;
				final Map<UniqueID,SequenceValue> seq_map = new HashMap<UniqueID,SequenceValue>();
				while (it.hasNext()) {
					DataCell c = it.next();
					if (c instanceof SequenceValue) {
						SequenceValue sv = (SequenceValue)c;
						if (st != SequenceType.UNKNOWN && st != sv.getSequenceType()) {
							logger.error("Cannot mix sequence types (eg. AA versus NA) in alignment for row: "+r.getKey().getString());
							return DataType.getMissingCell();
						} else {
							st = sv.getSequenceType();
						}
						seq_map.put(new UniqueID(), sv);
					}
				}
				
				if (seq_map.size() < 3 && !warned_small_seqs) {
					logger.warn("Use of multiple aligners for less than three sequences is NOT recommended, continuing anyway...");
					warned_small_seqs = true;
				}
				
				logger.info("Aligning "+seq_map.size()+ " "+st+" sequences for row "+r.getKey().getString());
				
				File f = null;
				
				
				// run muscle and return result if run went ok, else log errors
				try {
					f = File.createTempFile("output_muscle", ".fasta");
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
	    	    	LogOutputStream errors = m_log.getBooleanValue() ? new ErrorLogger(logger, true) : new LogOutputStream() {

						@Override
						protected void processLine(String arg0, int arg1) {
						}
	    	    		
	    	    	};
	    	    	exe.setStreamHandler(new PumpStreamHandler(tsv, errors));
	    	    	exe.setWorkingDirectory(f.getParentFile());		// must match addQueryDatabase() semantics
	    	    	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
	    	    	
	    	    	CommandLine cmdLine = new CommandLine(muscle_exe);
	    	    	cmdLine.addArgument("-in");
	    	    	cmdLine.addArgument(f.getName());
	    	    	if (m_performance.getStringValue().equals(TRADEOFFS[0])) {
	    	    		// no arguments to add
	    	    	} else if (m_performance.getStringValue().equals(TRADEOFFS[1])) {
	    	    		cmdLine.addArgument("-maxiters");
	    	    		cmdLine.addArgument("2");
	    	    	} else {
	    	    		cmdLine.addArgument("-maxiters");
	    	    		cmdLine.addArgument("1");
	    	    		cmdLine.addArgument("-diags");
	    	    		if (st.isProtein()) {
	    	    			cmdLine.addArgument("-sv");
	    	    			cmdLine.addArgument("-distance1");
	    	    			cmdLine.addArgument("kbit20_3");
	    	    		}
	    	    	}
	    	    	exe.setWorkingDirectory(f.getParentFile());
	    	    	logger.info("Running MUSCLE, command line: "+cmdLine.toString());
	            	int exitCode = exe.execute(cmdLine);
	            	logger.info("got exit code: "+exitCode+" from MUSCLE");
	            	
	            	if (exe.isFailure(exitCode)) {
	            		logger.error("MUSCLE failed to align sequences in row "+r.getKey().getString()+" - check console messages and input data");
	            		return DataType.getMissingCell();
	            	}
	            	
	            	return makeAlignmentCellAndPopulateResultsMap(tsv, st, r.getKey().getString());
				} catch (IOException ioe) {
					ioe.printStackTrace();
					return DataType.getMissingCell();
				} finally {
					if (f != null)
						f.delete();
				}
			}
			
		};
		
		rearranger.append(appender);
		out = exec.createColumnRearrangeTable(inData[0], rearranger, exec.createSubProgress(1.0));
		
        return new BufferedDataTable[]{out};
    }


	private File validateMuscle(String stringValue) throws IOException {
		final File muscle_exe = new File(m_exe.getStringValue());
    	if (!muscle_exe.exists() || !muscle_exe.canExecute()) {
    		throw new IOException("Muscle aligner not executable: "+m_exe.getStringValue());
    	}
    	return muscle_exe;
	}

	private DataCell makeAlignmentCellAndPopulateResultsMap(final LogOutputStream tsv, 
			final SequenceType st, final String row_id) throws IOException {
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
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	m_view_model.clear();
    }

    private DataColumnSpec make_output_spec() {
    	DataColumnSpec[] cols = new DataColumnSpec[1];
    	cols[0] = new DataColumnSpecCreator("Alignment of input sequences (MUSCLE)", MultiAlignmentCell.TYPE).createSpec();
    	
    	return cols[0];
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

    	DataColumnSpec  col = make_output_spec();
        return new DataTableSpec[]{new DataTableSpec(inSpecs[0], new DataTableSpec(new DataColumnSpec[] {col}))};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_exe.saveSettingsTo(settings);
         m_input_sequences.saveSettingsTo(settings);
         m_log.saveSettingsTo(settings);
         m_performance.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
       m_exe.loadSettingsFrom(settings);
       m_input_sequences.loadSettingsFrom(settings);
       m_log.loadSettingsFrom(settings);
       m_performance.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
       m_exe.validateSettings(settings);
       m_input_sequences.validateSettings(settings);
       m_log.validateSettings(settings);
       m_performance.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }

	@Override
	public List<String> getAlignmentRowIDs() {
		ArrayList<String> ret = new ArrayList<String>();
		ret.addAll(m_view_model.keySet());
		return ret;
	}

	@Override
	public AlignmentValue getAlignment(final String row_id) {
		return m_view_model.get(row_id);
	}

}

