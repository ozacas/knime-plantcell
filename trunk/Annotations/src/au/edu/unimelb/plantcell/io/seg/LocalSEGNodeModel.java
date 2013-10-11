package au.edu.unimelb.plantcell.io.seg;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.NullLogger;
import au.edu.unimelb.plantcell.core.TempDirectory;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceUtilityFactory;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.read.fasta.BatchFastaIterator;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.io.write.fasta.FastaWriter;

/**
 * Performs a local SEG filtering on input sequences 
 * 
 * @author andrew.cassin
 *
 */
public class LocalSEGNodeModel extends NodeModel {
    private final NodeLogger logger = NodeLogger.getLogger("SEG Filter (local)");
	
	public static final String CFGKEY_EXE         = "seg executable";
	public static final String CFGKEY_SEQUENCES    = "sequences-column";
	public static final String CFGKEY_LOG_STDERR   = "log-stderr";
	public static final String CFGKEY_USER_DEFINED = "user-defined-options";
	
	
	private SettingsModelString m_exe = new SettingsModelString(CFGKEY_EXE, "");
	private SettingsModelString m_input_sequences = new SettingsModelString(CFGKEY_SEQUENCES, "");
	private SettingsModelBoolean m_log = new SettingsModelBoolean(CFGKEY_LOG_STDERR, Boolean.FALSE);
	private SettingsModelString m_user_defined = new SettingsModelString(CFGKEY_USER_DEFINED, "");
	
	
	protected LocalSEGNodeModel() {
		super(1,1);
	}
	
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
		logger.info("Performing SEG filtering on "+inData[0].getRowCount()+" input rows.");
		
		final int seqs_idx = inData[0].getDataTableSpec().findColumnIndex(m_input_sequences.getStringValue());
		if (seqs_idx < 0)
			throw new InvalidSettingsException("Cannot find input sequences - re-configure?");
		
		MyDataContainer c = new MyDataContainer(exec.createDataContainer(new DataTableSpec(make_output_spec())), "Seq");
				
			
		File f = null;	
		BatchSequenceRowIterator bfi = new BatchSequenceRowIterator(inData[0].iterator(), seqs_idx, 1000, BatchFastaIterator.DEFAULT_TOTAL_MB_LIMIT, null);
		
		int done = 0;
		SequenceType one_type_only = null;
		while (bfi.hasNext()) {
			Map<UniqueID,SequenceValue> seqs = bfi.nextAsMap();
			TempDirectory td = new TempDirectory();
			exec.checkCanceled();
			
			// validate that every sequence is of the same type!
			for (SequenceValue sv : seqs.values()) {
				if (one_type_only == null) {
					one_type_only = sv.getSequenceType();
				} else {
					if (!one_type_only.equals(sv.getSequenceType()))
						throw new InvalidSettingsException("Only a single type of sequence is permitted per SEG node!");
				}
			}
			
			try {
				f = File.createTempFile("input_seg", ".fasta", td.asFile());
				FastaWriter fw = new FastaWriter(f, seqs);
				fw.write();
				
				final File out = File.createTempFile("output_seg", ".fasta", td.asFile());
				final PrintWriter pw_out = new PrintWriter(new FileWriter(out));
				
				// run seg and return result if run went ok, else log errors
				DefaultExecutor exe = new DefaultExecutor();
    	    	exe.setExitValues(new int[] {0});
    	    	LogOutputStream fasta = new LogOutputStream() {	    	    		
					@Override
					protected void processLine(String arg0, int arg1) {
						pw_out.println(arg0);
					}
    	    		
					@Override
					public String toString() {
						return out.getAbsolutePath();
					}
    	    	};
    	    	LogOutputStream errors = m_log.getBooleanValue() ? new ErrorLogger(logger, true) : new NullLogger();
    	    	exe.setStreamHandler(new PumpStreamHandler(fasta, errors));
    	    	exe.setWorkingDirectory(f.getParentFile());		// must match addQueryDatabase() semantics
    	    	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
    	    	File prog = new File(m_exe.getStringValue());
    	    	CommandLine cmdLine = new CommandLine(prog);
    	    	
    	    	cmdLine.addArgument(f.getName());
    	    	cmdLine.addArguments(m_user_defined.getStringValue());
    	    	if (m_user_defined.getStringValue().indexOf("-x") < 0)
    	    		cmdLine.addArgument("-x");
    	    	
    	    	exe.setWorkingDirectory(f.getParentFile());
    	    	logger.info("Running SEG, command line: "+cmdLine.toString());
            	int exitCode = exe.execute(cmdLine);
            	logger.info("got exit code: "+exitCode+" from SEG");
            	pw_out.close();
            	
            	if (exe.isFailure(exitCode)) {
            		throw new Exception("SEG failed to filter!");
            	}
            	
            	// load results from output file into output table
            	BatchFastaIterator seq_it = new BatchFastaIterator(out, SequenceType.AA, 1000);
            	while (seq_it.hasNext()) {
            		Map<UniqueID,SequenceValue> masked_seqs = seq_it.nextAsMap();
            		for (UniqueID uid : masked_seqs.keySet()) {
            			SequenceValue sv = masked_seqs.get(uid);
            			String     accsn = seqs.get(new UniqueID(sv.getID())).getID();
            			SequenceCell  sc = SequenceUtilityFactory.createSequenceCell(accsn+"_masked", sv.getStringValue(), SequenceType.AA);
            			c.addRow(new DataCell[] { sc });
            		}
            	}
			} catch (IOException|InvalidSettingsException ioe) {
				logger.error("Cannot seg!", ioe);
				throw ioe;
			} finally {
				if (td != null)
					td.deleteRecursive();
			}
			
			done += seqs.size();
			exec.setProgress(((double)done)/inData[0].getRowCount());
		}

	
	    return new BufferedDataTable[]{c.close()};
	}
    
	private DataColumnSpec make_output_spec() {
    	DataColumnSpec[] cols = new DataColumnSpec[1];
    	cols[0] = new DataColumnSpecCreator("Filtered Sequences", SequenceCell.TYPE).createSpec();
    	
    	return cols[0];
	}
	
	 /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

    	DataColumnSpec  col = make_output_spec();
        return new DataTableSpec[]{new DataTableSpec(new DataColumnSpec[] { col })};
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
		 m_exe.saveSettingsTo(settings);
         m_input_sequences.saveSettingsTo(settings);
         m_log.saveSettingsTo(settings);
         m_user_defined.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		 m_exe.validateSettings(settings);
         m_input_sequences.validateSettings(settings);
         m_log.validateSettings(settings);
         m_user_defined.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		 m_exe.loadSettingsFrom(settings);
         m_input_sequences.loadSettingsFrom(settings);
         m_log.loadSettingsFrom(settings);
         m_user_defined.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
	}

}
