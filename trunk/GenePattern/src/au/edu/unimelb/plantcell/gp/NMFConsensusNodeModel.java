package au.edu.unimelb.plantcell.gp;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.Parameter;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of GenePattern.
 * Nodes to support remote invocation of a GenePattern instance for key analyses (heatmap, clustering etc.)
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class NMFConsensusNodeModel extends AbstractGPNodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(NMFConsensusNodeModel.class);
        
	public final static String CFGKEY_GP_URL    = "genepattern-website";
	public final static String CFGKEY_GP_USER   = "genepattern-user-email";
	public final static String CFGKEY_GP_PASSWD = "genepattern-user-password";
	public final static String CFGKEY_K_INIT    = "initial-k";
	public final static String CFGKEY_K_FINAL   = "final-k";
	public final static String CFGKEY_CLUSTERINGS = "num-clusterings";
	public final static String CFGKEY_MAX_ITERATIONS = "max-iterations";
	public final static String CFGKEY_ERROR_FN  = "error-function";
	public final static String CFGKEY_SEED      = "seed";
	public final static String CFGKEY_STOP_CONV = "stop-convergence";
	public final static String CFGKEY_STOP_FREQ = "stop-frequency";
	public final static String[] ERROR_FUNCTIONS = new String[] { "divergence", "euclidean" };
		
		
	
	private SettingsModelString m_gp_url = new SettingsModelString(CFGKEY_GP_URL, "http://genepattern.broadinstitute.org/gp");
	private SettingsModelString m_gp_user= new SettingsModelString(CFGKEY_GP_USER, "");
	private SettingsModelString m_gp_passwd = new SettingsModelString(CFGKEY_GP_PASSWD, "");
	
	private SettingsModelIntegerBounded m_k_init = new SettingsModelIntegerBounded(CFGKEY_K_INIT, 2, 2, 50);
	private SettingsModelIntegerBounded m_k_final= new SettingsModelIntegerBounded(CFGKEY_K_FINAL, 5, 2, 50);
	private SettingsModelIntegerBounded m_clusterings = new SettingsModelIntegerBounded(CFGKEY_CLUSTERINGS, 20, 5, 100);
	private SettingsModelIntegerBounded m_max_iterations = new SettingsModelIntegerBounded(CFGKEY_MAX_ITERATIONS, 2000, 100, 100000);
	private SettingsModelString m_error_function = new SettingsModelString(CFGKEY_ERROR_FN, ERROR_FUNCTIONS[0]);
	private SettingsModelString m_seed = new SettingsModelString(CFGKEY_SEED, "123456789");
	private SettingsModelString m_stop_conv = new SettingsModelString(CFGKEY_STOP_CONV, "40");
	private SettingsModelString m_stop_freq = new SettingsModelString(CFGKEY_STOP_FREQ, "10");
	
	// internal state -- persisted to ensure the view can survive knime sessions
	private String m_dataset;		// always in genepattern GCT format
	
    /**
     * Constructor for the node model.
     */
    protected NMFConsensusNodeModel() {
        super(1, 0);
    }

 
	/**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	// all this node does is prepare the input data (numeric only columns) in genepattern GCT
    	// format so the view can invoke GP and display it, so there is not much to do in this node really...
    	// more details on this format can be found at http://www.broadinstitute.org/cancer/software/genepattern/tutorial/gp_fileformats.html
    	Map<Integer,String> m_numeric_columns = new HashMap<Integer,String>();
    	
    	for (int i=0; i<inData[0].getDataTableSpec().getNumColumns(); i++) {
    		DataColumnSpec cspec = inData[0].getDataTableSpec().getColumnSpec(i);
    		if (cspec.getType().isCompatible(DoubleValue.class)) {
    			m_numeric_columns.put(new Integer(i), cspec.getName());
    		}
    	}
    	int n_cols = m_numeric_columns.keySet().size();
    
    	if (n_cols < 1 || inData[0].getRowCount() < 1) {
    		throw new Exception("Gotta have some data to plot a heatmap!");
    	}
    	
    	m_dataset = make_gct_matrix(inData[0].getRowCount(), n_cols, m_numeric_columns, inData[0].iterator(), new RowDescriptionFilter() {

			@Override
			public String getRowDescription(DataRow r) {
				return "";
			}
    		
    	});
    	final File input_file = File.createTempFile("genepattern", ".gct");
    	input_file.deleteOnExit();
    	FileWriter fw = new FileWriter(input_file);
    	fw.write(m_dataset);
    	fw.close();
    	logger.info("Saved data matrix to "+input_file.getAbsolutePath());
    	
    	try {
    		logger.info("Running NMF consensus analysis... please wait this may take hours");
    		setGPParameters(m_gp_url.getStringValue(), 
    				m_gp_user.getStringValue(), m_gp_passwd.getStringValue());
    		GenePatternRunnable gpr = new GenePatternRunnable(make_gp_client(), new GenePatternResultHandler() {

				@Override
				public void processResult(JobResult res) {
					try {
						logger.info("Processing output files for job #"+res.getJobNumber());
						setOutputFiles(res, logger);
					} catch (Exception ex) {
						logger.error(ex);
					}
				}
    			
    		},
    				"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00057:4",
    				new Parameter[]{new Parameter("dataset.filename", input_file), 
					new Parameter("k.initial", String.valueOf(m_k_init.getIntValue())), 
					new Parameter("k.final", String.valueOf(m_k_final.getIntValue())), 
					new Parameter("num.clusterings", String.valueOf(m_clusterings.getIntValue())), 
					new Parameter("max.num.iterations", String.valueOf(m_max_iterations.getIntValue())), 
					new Parameter("error.function", m_error_function.getStringValue()), 
					
					new Parameter("random.seed",      m_seed.getStringValue()), 
					new Parameter("stop.convergence", m_stop_conv.getStringValue()), 
					new Parameter("stop.frequency",   m_stop_freq.getStringValue()),
				
					new Parameter("output.file.prefix", "<dataset.filename_basename>")}
    		);
    		
    		Thread t = new Thread(gpr);
    		t.start();
    		
    		do {
    			Thread.sleep(10 * 1000);
    			exec.checkCanceled();
    		} while (!t.getState().equals(Thread.State.TERMINATED));
    		
    		if (gpr.lastRunOk()) {
    			logger.info("Downloading files from GP server...");
    			downloadFiles();
    			logger.info("NMF Consensus analysis completed.");
    		} else 
    			logger.error("NMF Consensus analysis failed!");
    	} catch (Exception e) {
    		e.printStackTrace();
    		logger.error(e);
    	}
    	return new BufferedDataTable[]{};
    }

  
    public boolean hasDataset() {
    	return (m_dataset != null && m_dataset.length() > 0);
    }
    
    public String getDatasetAsGCT() {
    	return m_dataset.trim();		// remove excess whitespace to ensure gp is happy
    }
  
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_gp_url.saveSettingsTo(settings);
         m_gp_user.saveSettingsTo(settings);
         m_gp_passwd.saveSettingsTo(settings);       
         
         m_k_init.saveSettingsTo(settings);
         m_k_final.saveSettingsTo(settings);
         m_clusterings.saveSettingsTo(settings);
         m_max_iterations.saveSettingsTo(settings);
         m_error_function.saveSettingsTo(settings);
         m_seed.saveSettingsTo(settings);
         m_stop_conv.saveSettingsTo(settings);
         m_stop_freq.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	 m_gp_url.loadSettingsFrom(settings);
         m_gp_user.loadSettingsFrom(settings);
         m_gp_passwd.loadSettingsFrom(settings);

         m_k_init.loadSettingsFrom(settings);
         m_k_final.loadSettingsFrom(settings);
         m_clusterings.loadSettingsFrom(settings);
         m_max_iterations.loadSettingsFrom(settings);
         m_error_function.loadSettingsFrom(settings);
         m_seed.loadSettingsFrom(settings);
         m_stop_conv.loadSettingsFrom(settings);
         m_stop_freq.loadSettingsFrom(settings);

         setGPParameters(m_gp_url.getStringValue(), 
	        		m_gp_user.getStringValue(), 
	        		m_gp_passwd.getStringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	 m_gp_url.validateSettings(settings);
         m_gp_user.validateSettings(settings);
         m_gp_passwd.validateSettings(settings);
         
         m_k_init.validateSettings(settings);
         m_k_final.validateSettings(settings);
         m_clusterings.validateSettings(settings);
         m_max_iterations.validateSettings(settings);
         m_error_function.validateSettings(settings);
         m_seed.validateSettings(settings);
         m_stop_conv.validateSettings(settings);
         m_stop_freq.validateSettings(settings);
    }

}

