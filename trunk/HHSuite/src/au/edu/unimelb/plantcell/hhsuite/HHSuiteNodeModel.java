package au.edu.unimelb.plantcell.hhsuite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
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
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.ErrorLogger;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue;


/**
 * This is the model implementation of HHSuite. See http://toolkit.tuebingen.mpg.de/hhpred
 * 
 * Provides remote homology detection (more sensitive than BLAST) with most of the performance. 
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class HHSuiteNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("HHSuite");
    
    
	public static final String CFGKEY_ALIGNMENT           = "alignment-column";
	public static final String CFGKEY_DB_TYPE             = "type-of-database";			// create new or existing database?
	public static final String CFGKEY_CPU_CORES           = "number-of-cpu-cores";
    public static final String CFGKEY_DB_NEW              = "new-database";				// eg. *folder* containing .HHM files
    public static final String CFGKEY_DB_EXISTING         = "existing-database";		// eg. *filename*


	public static final String[] DATABASE_ITEMS = new String[] { "Make database from individual HHM files", "Use existing database" };
	
    private final SettingsModelString m_alignment         = new SettingsModelString(CFGKEY_ALIGNMENT, "");
    private final SettingsModelIntegerBounded m_cpu_cores = new SettingsModelIntegerBounded(CFGKEY_CPU_CORES, 2, 1, 16);
    private final SettingsModelString m_db_type           = new SettingsModelString(CFGKEY_DB_TYPE, "");
    private final SettingsModelString m_db_new            = new SettingsModelString(CFGKEY_DB_NEW, "");
    private final SettingsModelString m_db_existing       = new SettingsModelString(CFGKEY_DB_EXISTING, "");

    /**
     * Constructor for the node model.
     */
    protected HHSuiteNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	DataTableSpec outTable = make_output_spec(inData[0].getSpec());
        MyDataContainer c = new MyDataContainer(exec.createDataContainer(outTable), "Row");
        
        logger.info("Searching alignments from "+m_alignment.getStringValue()+" using HHSearch.");
        
        int align_col = inData[0].getSpec().findColumnIndex(m_alignment.getStringValue());
        if (align_col < 0) {
        	throw new InvalidSettingsException("Cannot locate input column: "+m_alignment.getStringValue());
        }
        RowIterator it = inData[0].iterator();
        
        File db = make_database(m_db_type.getStringValue(), m_db_new.getStringValue(), m_db_existing.getStringValue());
        
        logger.info("Searching multiple alignments in input data");
        Map<String,String> env = new HashMap<String,String>();
        env.put("nodosfilewarning", "0");
        
        while (it.hasNext()) {
        	DataRow   r = it.next();
        	
        	DataCell ac = r.getCell(align_col);
        	if (ac == null || ac.isMissing() || !(ac instanceof AlignmentValue))
        		continue;
        	AlignmentValue av = (AlignmentValue) ac;
        	if (av.getSequenceCount() < 3) {
        		String  rid = r.getKey().getString();
        		logger.warn("Cannot search with less than 3 sequences in an alignment... skipping row "+rid);
        		continue;
        	}
        
        	File tmp_alignment_file = make_alignment_file(av);
        	CommandLine cl = new CommandLine("c:/hhsuite/bin/hhsearch.exe");
        	cl.addArgument("-cpu");
        	cl.addArgument(String.valueOf(m_cpu_cores.getIntValue()));
        	cl.addArgument("-i");
        	cl.addArgument(tmp_alignment_file.getAbsolutePath());
        	cl.addArgument("-d");
        	cl.addArgument(db.getName());		// NB: current working directory is set to the folder with the database
        	DefaultExecutor exe = new DefaultExecutor();
	    	exe.setExitValues(new int[] {0, 1});
	    	exe.setStreamHandler(new PumpStreamHandler(new LogOutputStream() {

				@Override
				protected void processLine(String arg0, int arg1) {
					logger.info(arg0);
				}
	    		
	    	}, new ErrorLogger(logger)));
	    	exe.setWorkingDirectory(db.getParentFile());		// arbitrary choice
	    	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
	    	
	    	logger.info("Running: "+cl.toString());
	    	int exitCode = exe.execute(cl, env);
	    	logger.info("got exit code: "+exitCode+" from HHSearch");
	    	//tmp_alignment_file.delete();
	    	if (exe.isFailure(exitCode)) {
	    		if (exe.getWatchdog().killedProcess())
	    			throw new Exception("HHSearch failed - watchdog says no...");
	    		else
	    			throw new Exception("HHSearch failed - check console messages and input data");
	    	}
        }
        
        // once we are done, we close the container and return its table
        return new BufferedDataTable[]{c.close()};
    }

    private File make_alignment_file(AlignmentValue av) throws IOException {
		// save out the aligned sequences as a suitably formatted aligned FASTA file and return it
    	File tempFile = File.createTempFile("aligned-fasta", ".fas");
    	PrintWriter pw = new PrintWriter(new FileWriter(tempFile));
    	for (int i=0; i<av.getSequenceCount(); i++) {
    		pw.println(">"+av.getIdentifier(i).getName());
    		pw.println(av.getAlignedSequenceString(i).trim());
    	}
    	pw.close();
    	logger.info("Saved alignment to: "+tempFile.getAbsolutePath());
    	return tempFile;
	}

	private File make_database(String db_type, String db_new, String db_existing) throws InvalidSettingsException, IOException {
		if (db_type.startsWith("Make database from individual HHM")) {
			String db = db_new;
			File dir = new File(db);
			if (dir.isDirectory()) {
				File[] hhm_files = dir.listFiles(new FileFilter() {

					@Override
					public boolean accept(File arg0) {
						return (!arg0.isDirectory() && 
									arg0.canRead() && 
									arg0.getName().toLowerCase().endsWith(".hhm"));
					}
					
				});
				
				File out_file = new File(dir, "hhsearch.concat.db");
				if (out_file.exists()) {
					logger.warn("Found existing search database, not re-creating: "+out_file.getAbsolutePath());
					return out_file;
				}
				
				PrintWriter pw = new PrintWriter(new FileWriter(out_file));
				logger.info("Saving concatenated HHM database... please wait, this may take a while.");
				for (File f : hhm_files) {
					BufferedReader bfr = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
					String line;
					while ((line = bfr.readLine()) != null) {		// NB: readLine() removes the newline
						pw.println(line);
					}
					bfr.close();
				}
				pw.close();
				logger.info("Concatenated database saved.");
				return out_file;
			} else {
				throw new InvalidSettingsException(db+" must be a folder to search for .HHM files (not recursive)");
			}
		} else {
			// TODO...
			throw new InvalidSettingsException("Dont know how to make/use database: "+db_type);
		}
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
 
        return new DataTableSpec[]{make_output_spec(inSpecs[0])};
    }

    private DataTableSpec make_output_spec(DataTableSpec inSpec) {
    	DataColumnSpec[] outCols = new DataColumnSpec[1];
    	outCols[0] = new DataColumnSpecCreator("Number of hits", IntCell.TYPE).createSpec();
    	
		return new DataTableSpec(inSpec, new DataTableSpec(outCols));
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_cpu_cores.saveSettingsTo(settings);
    	m_alignment.saveSettingsTo(settings);
    	m_db_new.saveSettingsTo(settings);
    	m_db_existing.saveSettingsTo(settings);
    	m_db_type.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_cpu_cores.loadSettingsFrom(settings);
    	m_alignment.loadSettingsFrom(settings);
    	m_db_new.loadSettingsFrom(settings);
    	m_db_existing.loadSettingsFrom(settings);
    	m_db_type.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_cpu_cores.validateSettings(settings);
    	m_alignment.validateSettings(settings);
    	m_db_type.validateSettings(settings);
    	m_db_new.validateSettings(settings);
    	m_db_existing.validateSettings(settings);
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

}

