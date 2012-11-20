package au.edu.unimelb.plantcell.gp;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.client.GPClient;
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
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


public class SOMClusteringNodeModel extends AbstractGPNodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(SOMClusteringNodeModel.class);
        
    
	public final static String CFGKEY_GP_URL    = "genepattern-website";
	public final static String CFGKEY_GP_USER   = "genepattern-user-email";
	public final static String CFGKEY_GP_PASSWD = "genepattern-user-password";
	public final static String CFGKEY_GENE_DESCR= "gene-description";
	public final static String CFGKEY_CLUSTER_BY= "cluster-by";
	public final static String CFGKEY_CLUSTER_RANGE="cluster.range";
	public final static String CFGKEY_ITERATIONS = "iterations";
	public final static String CFGKEY_SEED       = "seed.range";
	public final static String CFGKEY_SOM_INIT   = "som.initialization";
	public final static String CFGKEY_SOM_NHOOD  = "som.neighbourhood";
	
	public final static String[] CLUSTER_BY = new String[] { "rows", "columns" };
	public final static String[] SOM_INIT   = new String[] { "Random_Vectors", "Random_Datapoints" };
	public final static String[] SOM_NHOOD  = new String[] { "Bubble", "Gaussian" };
	
	private SettingsModelString m_gp_url = new SettingsModelString(CFGKEY_GP_URL, "http://genepattern.broadinstitute.org/gp");
	private SettingsModelString m_gp_user= new SettingsModelString(CFGKEY_GP_USER, "");
	private SettingsModelString m_gp_passwd = new SettingsModelString(CFGKEY_GP_PASSWD, "");
	private SettingsModelColumnName m_descr = new SettingsModelColumnName(CFGKEY_GENE_DESCR, "");
	private SettingsModelString m_cluster_by= new SettingsModelString(CFGKEY_CLUSTER_BY, SOMClusteringNodeModel.CLUSTER_BY[0]);
	private SettingsModelString m_cluster_rg= new SettingsModelString(CFGKEY_CLUSTER_RANGE, "2-3");
	private SettingsModelIntegerBounded m_iterations = new SettingsModelIntegerBounded(CFGKEY_ITERATIONS, 50000, 1000, 1000 * 1000);
	private SettingsModelIntegerBounded m_seed = new SettingsModelIntegerBounded(CFGKEY_SEED, 42, 1, 1000000);
	private SettingsModelString m_som_init = new SettingsModelString(CFGKEY_SOM_INIT, SOM_INIT[0]);
	private SettingsModelString m_som_nhood= new SettingsModelString(CFGKEY_SOM_NHOOD, SOM_NHOOD[0]);
	
	// internal state -- persisted to ensure the view can survive knime sessions
	private String m_dataset;		// always in genepattern GCT format
	
	protected SOMClusteringNodeModel() {
	        super(1, 0);
	        setGPParameters(m_gp_url.getStringValue(), 
	        		m_gp_user.getStringValue(), 
	        		m_gp_passwd.getStringValue());
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
    	int n_rows = inData[0].getRowCount();
    	if (n_cols < 1 || n_rows < 1) {
    		throw new Exception("Gotta have some data to cluster using SOM!");
    	}
    	
    	
    	// heatmap code will do the required job in terms of making the data matrix
    	logger.info("Creating GenePattern data matrix (GCT format)...");
    	m_dataset = make_gct_matrix(n_rows, n_cols, m_numeric_columns, inData[0].iterator(), new RowDescriptionFilter() {

			@Override
			public String getRowDescription(DataRow r) {
				return "";
			}
    		
    	});
    	File input_file = File.createTempFile("genepattern", ".gct", new File("c:/temp"));
    	input_file.deleteOnExit();
    	FileWriter fw = new FileWriter(input_file);
    	fw.write(m_dataset);
    	fw.close();
    	logger.info("Successfully saved GCT format dataset to "+input_file.getAbsolutePath());
    	
    	// now run the analysis...
    	logger.warn("No progress information is available, so this may take a long time depending on the server... be patient!");
    	
    	logger.info("Now running SOM clustering... please wait!");
    	GPClient gp = make_gp_client();
    	JobResult result = null;
    	try {
    			result = gp.runAnalysis("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00029:2", 
    			new Parameter[]{new Parameter("dataset.filename", input_file), 
    			new Parameter("output.stub", "<dataset.filename_basename>"), // not changeable via this node
    			new Parameter("cluster.range", m_cluster_rg.getStringValue().trim()), 
    			new Parameter("seed.range", String.valueOf(m_seed.getIntValue())),
    			new Parameter("initialization", m_som_init.getStringValue().trim()), 
     		    new Parameter("neighborhood", m_som_nhood.getStringValue().trim()), 
     		    new Parameter("cluster.by", m_cluster_by.getStringValue().trim()), 
    			new Parameter("iterations", String.valueOf(m_iterations.getIntValue())),
    			
    			new Parameter("som.rows", "0"), // none of these advanced parameters are changeable (yet)
    		    new Parameter("som.cols", "0"), 
    		    new Parameter("alpha.initial", "0.1"), 
    		    new Parameter("alpha.final", "0.005"), 
    		    new Parameter("sigma.initial", "5.0"), 
    		    new Parameter("sigma.final", "0.5")});
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw new Exception("Unable to perform GenePattern SOM Clustering: authentication or GenePattern problem?\nMessage is: "+e.getMessage());
    	}
    	
    	setOutputFiles(result, logger);
    	logger.info("SOM cluster analysis complete.");
        return new BufferedDataTable[]{};
    }
    
    public boolean hasDataset() {
    	return (m_dataset.length() > 0);
    }
    
    public String getDatasetAsGCT() {
    	return m_dataset.toString().trim();		// remove excess whitespace to ensure gp is happy
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_gp_url.saveSettingsTo(settings);
         m_gp_user.saveSettingsTo(settings);
         m_gp_passwd.saveSettingsTo(settings);
         m_descr.saveSettingsTo(settings);
         m_cluster_by.saveSettingsTo(settings);
         m_cluster_rg.saveSettingsTo(settings);
         m_iterations.saveSettingsTo(settings);
         m_seed.saveSettingsTo(settings);
         m_som_init.saveSettingsTo(settings);
         m_som_nhood.saveSettingsTo(settings);
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
         m_descr.loadSettingsFrom(settings);
         m_cluster_by.loadSettingsFrom(settings);
         m_cluster_rg.loadSettingsFrom(settings);
         m_iterations.loadSettingsFrom(settings);
         m_seed.loadSettingsFrom(settings);
         m_som_init.loadSettingsFrom(settings);
         m_som_nhood.loadSettingsFrom(settings);
         
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
         m_descr.validateSettings(settings);
         m_cluster_by.validateSettings(settings);
         m_cluster_rg.validateSettings(settings);
         m_iterations.validateSettings(settings);
         m_seed.validateSettings(settings);
         m_som_init.validateSettings(settings);
         m_som_nhood.validateSettings(settings);
    }


}
