package au.edu.unimelb.plantcell.io.mafft;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
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
import au.edu.unimelb.plantcell.core.NullLogger;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.write.fasta.FastaWriter;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentCellFactory;
import au.edu.unimelb.plantcell.io.ws.multialign.MultiAlignmentCell;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue.AlignmentType;

/**
 * Performs a local mafft alignment (only supported on windows for now)
 * 
 * @author andrew.cassin
 *
 */
public class LocalMafftNodeModel extends NodeModel {
    private final NodeLogger logger = NodeLogger.getLogger("MAFFT Aligner (local)");
	
	public static final String CFGKEY_ROOT         = "mafft root folder";
	public static final String CFGKEY_SEQUENCES    = "sequences-column";
	public static final String CFGKEY_LOG_STDERR   = "log-stderr";
	public static final String CFGKEY_ALGO         = "algorithm";
	public static final String CFGKEY_USER_DEFINED = "user-defined-options";
	
	// order is VERY important and cannot be changed without care!
	public static final String[] TRADEOFFS = new String[] { "Auto", "User-defined", "FFT-NS-1", "FFT-NS-2", "FFT-NS-i (max 1000 cycles)", "L-INS-i", "G-INS-i" };
	
	private SettingsModelString m_root = new SettingsModelString(CFGKEY_ROOT, "");
	private SettingsModelString m_input_sequences = new SettingsModelString(CFGKEY_SEQUENCES, "");
	private SettingsModelBoolean m_log = new SettingsModelBoolean(CFGKEY_LOG_STDERR, Boolean.FALSE);
	private SettingsModelString  m_algo = new SettingsModelString(CFGKEY_ALGO, TRADEOFFS[0]);
	private SettingsModelString m_user_defined = new SettingsModelString(CFGKEY_USER_DEFINED, "");
	
	
	protected LocalMafftNodeModel() {
		super(1,1);
		m_user_defined.setEnabled(m_algo.getStringValue().equals(TRADEOFFS[1]));
	}
	
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
		logger.info("Performing alignments on "+inData[0].getRowCount()+" input rows.");
		
		ColumnRearranger rearranger = new ColumnRearranger(inData[0].getSpec());
		BufferedDataTable out = null;
		final int seqs_idx = inData[0].getDataTableSpec().findColumnIndex(m_input_sequences.getStringValue());
		if (seqs_idx < 0)
			throw new InvalidSettingsException("Cannot find input sequences - re-configure?");
		
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
				
				
				// run mafft and return result if run went ok, else log errors
				try {
					f = File.createTempFile("input_mafft", ".fasta");
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
	    	    	LogOutputStream errors = m_log.getBooleanValue() ? new ErrorLogger(logger, true) : new NullLogger();
	    	    	exe.setStreamHandler(new PumpStreamHandler(tsv, errors));
	    	    	exe.setWorkingDirectory(f.getParentFile());		// must match addQueryDatabase() semantics
	    	    	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
	    	    	File prog = new File(getMafftFolder(), "mafft.bat");
	    	    	CommandLine cmdLine = new CommandLine(prog);
	    	    	
	    	    	addAlgoOptions(cmdLine, m_algo.getStringValue());
	    	    	
	    	    	cmdLine.addArgument(f.getName());
	    	    	
	    	    	exe.setWorkingDirectory(f.getParentFile());
	    	    	logger.info("Running MAFFT, command line: "+cmdLine.toString());
	            	int exitCode = exe.execute(cmdLine);
	            	logger.info("got exit code: "+exitCode+" from MAFFT");
	            	
	            	if (exe.isFailure(exitCode)) {
	            		logger.error("MAFFT failed to align sequences in row "+r.getKey().getString()+" - check console messages and input data");
	            		return DataType.getMissingCell();
	            	}
	            	
					return AlignmentCellFactory.createCell(tsv.toString(), st.isProtein() ? AlignmentType.AL_AA : AlignmentType.AL_NA);
				} catch (IOException|InvalidSettingsException ioe) {
					logger.error("Cannot mafft!", ioe);
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
		} else if (algo.equals(TRADEOFFS[1])) {
			String[] params = m_user_defined.getStringValue().split("\\s+");
			for (String param : params) {
				cmdLine.addArgument(param);
			}
		} else {
			throw new InvalidSettingsException("Unknown algorithm: "+algo);
		}
	}
	
	private File getMafftFolder() throws InvalidSettingsException {
		File root = new File(m_root.getStringValue());
	
		File[] paths =  new File[] { root, new File(root, "mafft-win") };
		for (File p : paths) {
			if (p.exists() && p.isDirectory()) {
				File got_batch = new File(p, "mafft.bat");
				if (got_batch.exists() && got_batch.canExecute())
					return p;
			}
		}
		
		throw new InvalidSettingsException("Cannot locate MAFFT folder: check configuration!");
	}
	
	private DataColumnSpec make_output_spec() {
    	DataColumnSpec[] cols = new DataColumnSpec[1];
    	cols[0] = new DataColumnSpecCreator("Alignment of input sequences (MAFFT)", MultiAlignmentCell.TYPE).createSpec();
    	
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
    
	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
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
	}

}
