package au.edu.unimelb.plantcell.io.usearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.ErrorLogger;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.NullLogger;
import au.edu.unimelb.plantcell.core.TempDirectory;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceUtilityFactory;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.read.fasta.BatchFastaIterator;

/**
 * Performs a local mafft alignment (only supported on windows for now)
 * 
 * @author andrew.cassin
 *
 */
public class SeqClusterNodeModel extends NodeModel {
    private final NodeLogger logger = NodeLogger.getLogger("usearch cluster (local)");
	
	public static final String CFGKEY_EXE          = "usearch-exe-path";
	public static final String CFGKEY_SEQUENCES    = "sequences-column";
	public static final String CFGKEY_LOG_STDERR   = "log-stderr";
	public static final String CFGKEY_USER_DEFINED = "user-defined-options";
	public static final String CFGKEY_THRESHOLD    = "identity-threshold";
	public static final String CFGKEY_ALGO         = "algorithm";
	
	// must be named as per the command line argument for usearch (see execute() for why...)
	public static final String[] ALGO = new String[] { "cluster_fast", "cluster_smallmem", "usearch_global", "usearch_local" };
	
	private SettingsModelString  m_exe             = new SettingsModelString(CFGKEY_EXE, "");
	private SettingsModelString  m_input_sequences = new SettingsModelString(CFGKEY_SEQUENCES, "");
	private SettingsModelBoolean m_log             = new SettingsModelBoolean(CFGKEY_LOG_STDERR, Boolean.FALSE);
	private SettingsModelString  m_user_defined    = new SettingsModelString(CFGKEY_USER_DEFINED, "");
	private SettingsModelInteger m_identity        = new SettingsModelIntegerBounded(CFGKEY_THRESHOLD, 95, 0, 100);
	private SettingsModelString  m_algo            = new SettingsModelString(CFGKEY_ALGO, ALGO[0]);
	
	
	protected SeqClusterNodeModel() {
		super(1,3);
	}
	
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
		logger.info("Performing usearch sequence clustering on "+inData[0].getRowCount()+" input rows.");
		int seq_idx = inData[0].getSpec().findColumnIndex(m_input_sequences.getStringValue());
		if (seq_idx < 0)
			throw new InvalidSettingsException("Cannot find column: "+m_input_sequences.getStringValue()+" - reconfigure?");
		
		DataTableSpec[] outSpecs = make_output_spec();
		MyDataContainer centroids = new MyDataContainer(exec.createDataContainer(outSpecs[0]), "Centroid");
		MyDataContainer consensus = new MyDataContainer(exec.createDataContainer(outSpecs[1]), "Consensus");
		MyDataContainer tbl = new MyDataContainer(exec.createDataContainer(outSpecs[2]), "Row");
		
		// 1. save sequences as FASTA for usearch
		TempDirectory td = null;
		try {
			td              = new TempDirectory();
			File out_fasta  = new File(td.asFile(), "for_usearch.fasta");
			PrintWriter  pw = new PrintWriter(out_fasta);
			SequenceType st = null;
			int saved = 0;
			for (DataRow r : inData[0]) {
				DataCell seq_cell = r.getCell(seq_idx);
				if (seq_cell == null || seq_cell.isMissing() || !(seq_cell instanceof SequenceValue))
					continue;
				SequenceValue sv = (SequenceValue) seq_cell;
				if (st != null && !sv.getSequenceType().equals(st)) 
					throw new InvalidSettingsException("Inconsistent sequence types: not all AA/NA... check your sequences!");
				else if (st == null)
					st = sv.getSequenceType();
				
				pw.println(">"+sv.getID());
				int len = sv.getLength();
				String seq = sv.getStringValue();
				for (int start=0; start < len; start += 60) {
					int end = start + 60;
					if (end > len) {
						end = len;
					}
					pw.println(seq.substring(start, end));
				}
				saved++;
			}
			pw.close();
			if (st == null)
				throw new InvalidSettingsException("No valid sequences found!");
			logger.info("Saved "+saved+" input sequences for clustering from input table.");
			
			// 2. run usearch with chosen options
			DefaultExecutor exe = new DefaultExecutor();
	    	exe.setExitValues(new int[] {0});
			exe.setStreamHandler(new PumpStreamHandler(new NullLogger(), 
					m_log.getBooleanValue() ? new ErrorLogger(logger,true) : new NullLogger()));
			
	    	CommandLine cmdLine = new CommandLine(new File(m_exe.getStringValue()));
	    	cmdLine.addArgument("-"+m_algo.getStringValue());
	    	cmdLine.addArgument(out_fasta.getName());
	    	cmdLine.addArgument("-id");
	    	cmdLine.addArgument(""+((double)m_identity.getIntValue()) / 100.0);
	    	cmdLine.addArgument("-centroids");
	    	File out_centroid = File.createTempFile("centroid_out", ".fasta", td.asFile());
	    	File out_consensus= File.createTempFile("consensus_out", ".fasta", td.asFile());
	    	cmdLine.addArgument(out_centroid.getName());
	    	cmdLine.addArgument("-consout");
	    	cmdLine.addArgument(out_consensus.getName());
	    	File uc_results = new File(td.asFile(), "uc_results.tsv");
	    	cmdLine.addArgument("-uc");
	    	cmdLine.addArgument(uc_results.getName());
	    	for (String s: m_user_defined.getStringValue().split("\\s+")) {
	    		cmdLine.addArgument(s);
	    	}
	    	
	    	logger.info("Running USEARCH, command line: "+cmdLine.toString());
	    	File pwd = out_fasta.getParentFile();
	    	logger.info("Set working directory: "+pwd.getAbsolutePath());
	    	exe.setWorkingDirectory(pwd);		// must always be this (see below!)
	    	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
	    
	    	int status = exe.execute(cmdLine);
	    	
	    	if (exe.isFailure(status))
	    		throw new Exception("USearch failed: exit status "+status);
	    	
	    	// 3. load results into output ports
	    	int n = load_fasta(centroids, out_centroid, st);
	    	logger.info("Loaded "+n+" centroid sequences from cluster results.");
	    	n = load_fasta(consensus, out_consensus, st);
	    	logger.info("Loaded "+n+" consensus sequences from cluster results.");
	    	load_uc_results(tbl, uc_results);
		} finally {
			if (td != null) {
				// 4. cleanup
		    	logger.info("Cleaning up results: "+td.asFile());
				td.deleteRecursive();
			}
		}
			
	    return new BufferedDataTable[] {centroids.close(), consensus.close(), tbl.close() };
	}
	
	private void load_uc_results(final MyDataContainer c , final File uc_results_file) throws Exception {
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(uc_results_file));
			String line;
			while ((line = rdr.readLine()) != null) {
				String[] fields = line.split("\\t+");
				if (fields.length != 10)
					throw new Exception("Expected exactly ten fields from usearch: something is wrong!");
				DataCell[] cells = new DataCell[10];
				for (int i=0; i<fields.length; i++) {
					cells[i] = new StringCell(fields[i]);
				}
				c.addRow(cells);
			}
		} finally {
			if (rdr != null)
				rdr.close();
		}
	}
	
	private int load_fasta(final MyDataContainer c, final File in, final SequenceType st) throws Exception {
		assert(c != null && in != null);
		BatchFastaIterator it = new BatchFastaIterator(in, st, 100);
		int n = 0;
		while (it.hasNext()) {
			List<SequenceValue> seqs = it.next();
			for (SequenceValue sv : seqs) {
				c.addRow(new DataCell[] { SequenceUtilityFactory.createSequenceCell(sv.getID(), sv.getStringValue(), st) });
				n++;
			}
		}
		
		return n;
	}
	
	private DataTableSpec[] make_output_spec() {
    	DataColumnSpec[] cols = new DataColumnSpec[1];
    	cols[0] = new DataColumnSpecCreator("Centroid Sequences", SequenceCell.TYPE).createSpec();
    	
    	DataColumnSpec[] cols2 = new DataColumnSpec[1];
    	cols2[0] = new DataColumnSpecCreator("Consensus Sequences", SequenceCell.TYPE).createSpec();
    	
    	DataColumnSpec[] cols3 = new DataColumnSpec[10];
    	cols3[0] = new DataColumnSpecCreator("Record Type (H: hit, S: centroid, C: cluster, N: no hit)", StringCell.TYPE).createSpec();
    	cols3[1] = new DataColumnSpecCreator("Cluster Number", StringCell.TYPE).createSpec();
    	cols3[2] = new DataColumnSpecCreator("Sequence Length (except C records: cluster size)", StringCell.TYPE).createSpec();
    	cols3[3] = new DataColumnSpecCreator("% identity with target (H records)", StringCell.TYPE).createSpec();
    	cols3[4] = new DataColumnSpecCreator("Strand (nucl. only)", StringCell.TYPE).createSpec();
    	cols3[5] = new DataColumnSpecCreator("Not used", StringCell.TYPE).createSpec();
    	cols3[6] = new DataColumnSpecCreator("Not used #2", StringCell.TYPE).createSpec();
    	cols3[7] = new DataColumnSpecCreator("Compressed alignment (= is 100% identity)", StringCell.TYPE).createSpec();
    	cols3[8] = new DataColumnSpecCreator("Query Sequence ID", StringCell.TYPE).createSpec();
    	cols3[9] = new DataColumnSpecCreator("Target Sequence ID", StringCell.TYPE).createSpec();
    	
    	return new DataTableSpec[] { new DataTableSpec(cols), new DataTableSpec(cols2), new DataTableSpec(cols3) };
	}
	
	 /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return make_output_spec();
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
         m_algo.saveSettingsTo(settings);
         m_identity.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		 m_exe.validateSettings(settings);
         m_input_sequences.validateSettings(settings);
         m_log.validateSettings(settings);
         m_user_defined.validateSettings(settings);
         m_algo.validateSettings(settings);
         m_identity.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		 m_exe.loadSettingsFrom(settings);
         m_input_sequences.loadSettingsFrom(settings);
         m_log.loadSettingsFrom(settings);
         m_user_defined.loadSettingsFrom(settings);
         m_algo.loadSettingsFrom(settings);
         m_identity.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
	}

}
