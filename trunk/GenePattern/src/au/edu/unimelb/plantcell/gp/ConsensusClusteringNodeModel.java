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
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

public class ConsensusClusteringNodeModel extends AbstractGPNodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(ConsensusClusteringNodeModel.class);
        
	public final static String CFGKEY_GP_URL    = "genepattern-website";
	public final static String CFGKEY_GP_USER   = "genepattern-user-email";
	public final static String CFGKEY_GP_PASSWD = "genepattern-user-password";
	public final static String CFGKEY_KMAX      = "kmax";
	public final static String CFGKEY_ALGORITHM = "clustering-algorithm";
	public final static String CFGKEY_DISTANCE  = "distance-measure";
	public final static String CFGKEY_RESAMPLE  = "resample";
	public final static String CFGKEY_MERGE     = "merge";
	public final static String CFGKEY_SEED      = "seed";
	public final static String CFGKEY_CLUSTERBY = "cluster-by";
	public final static String CFGKEY_NORMALISATION = "normalisation-by";
	
	public final static String[] ALGORITHMS = new String[] { "Hierarchical", "SOM", "KMeans", "NMF" };
	public final static String[] DISTANCE_MEASURES = new String[] { "Euclidean", "Pearson" };
	public final static String[] RESAMPLE_BY = new String[] {"subsample", "features", "nosampling" };
	public final static String[] MERGE = new String[] { "average", "complete", "single" };
	public final static String[] CLUSTERBY = new String[] { "columns", "rows" };
	public final static String[] NORMALISATIONS = new String[] { "row-wise", "column-wise", "both", "none" };
	
	private SettingsModelString m_gp_url = new SettingsModelString(CFGKEY_GP_URL, "http://genepattern.broadinstitute.org/gp");
	private SettingsModelString m_gp_user= new SettingsModelString(CFGKEY_GP_USER, "");
	private SettingsModelString m_gp_passwd = new SettingsModelString(CFGKEY_GP_PASSWD, "");
	
	private SettingsModelIntegerBounded m_kmax = new SettingsModelIntegerBounded(CFGKEY_KMAX, 5, 2, 20);
	private SettingsModelString m_algorithm= new SettingsModelString(CFGKEY_ALGORITHM, ALGORITHMS[0]);
	private SettingsModelString m_distance = new SettingsModelString(CFGKEY_DISTANCE, DISTANCE_MEASURES[0]);
	private SettingsModelString m_resample = new SettingsModelString(CFGKEY_RESAMPLE, RESAMPLE_BY[0]);
	private SettingsModelString m_merge    = new SettingsModelString(CFGKEY_MERGE, MERGE[0]);
	private SettingsModelString m_seed     = new SettingsModelString(CFGKEY_SEED, "12345");
	private SettingsModelString m_cluster_by = new SettingsModelString(CFGKEY_CLUSTERBY, CLUSTERBY[0]);

	private SettingsModelString m_normalisations = new SettingsModelString(CFGKEY_NORMALISATION, NORMALISATIONS[0]);
	
	// internal state -- persisted to ensure the view can survive knime sessions
	private String m_dataset;		// always in genepattern GCT format
	
    /**
     * Constructor for the node model.
     */
    protected ConsensusClusteringNodeModel() {
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
    		throw new Exception("Gotta have some data to produce a consensus!");
    	}
    	
    	m_dataset = make_gct_matrix(inData[0].getRowCount(), n_cols, m_numeric_columns, inData[0].iterator(), new RowDescriptionFilter() {

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
    	logger.info("Saved dataset to "+input_file.getAbsolutePath());
    	
    	JobResult result = null;
    
    	GPClient gpClient = make_gp_client();
    
    	HashMap<String,Integer> m_ntypes = new HashMap<String,Integer>();
    	for (int i=0; i<NORMALISATIONS.length; i++) {
    		m_ntypes.put(NORMALISATIONS[i], new Integer(1+i));
    	}

    	try {
    		logger.info("Running consensus cluster GenePattern module (may take a long time)... ");
    		result = gpClient.runAnalysis("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00030:5", new Parameter[]{
    				new Parameter("input.filename", input_file), 
    				new Parameter("kmax", m_kmax.getIntValue()), 
    				new Parameter("clustering.algorithm", m_algorithm.getStringValue()), 
    				new Parameter("distance.measure", m_distance.getStringValue()), 
    				new Parameter("resample", m_resample.getStringValue()), 
    				new Parameter("merge.type", m_merge.getStringValue()), 
    				new Parameter("seed.value", m_seed.getStringValue()), 
    				new Parameter("cluster.by", m_cluster_by.getStringValue()), 
    				new Parameter("normalize.type", "-n" + m_ntypes.get(m_normalisations.getStringValue())), 

    				// TODO.. make these settings available as well to the user
    				new Parameter("resampling.iterations", "20"), 
    				new Parameter("descent.iterations", "2000"), 
    				new Parameter("output.stub", "<input.filename_basename>"), 
    				new Parameter("normalization.iterations", "0"), 
    				new Parameter("create.heat.map", ""), 
    				new Parameter("heat.map.size", "2")});
    	
    		logger.info("Consensus cluster analysis completed successfully.");
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw new Exception("Consensus cluster analysis failed: "+e.getMessage());
    	}
    	
    	setOutputFiles(result, logger);
    	
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
         
         m_kmax.saveSettingsTo(settings);
     	 m_algorithm.saveSettingsTo(settings);
     	 m_distance.saveSettingsTo(settings);
     	 m_resample.saveSettingsTo(settings);
     	 m_merge.saveSettingsTo(settings);
     	 m_seed.saveSettingsTo(settings);
     	 m_cluster_by.saveSettingsTo(settings);
     	 m_normalisations.saveSettingsTo(settings);
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
         
         m_kmax.loadSettingsFrom(settings);
     	 m_algorithm.loadSettingsFrom(settings);
     	 m_distance.loadSettingsFrom(settings);
     	 m_resample.loadSettingsFrom(settings);
     	 m_merge.loadSettingsFrom(settings);
     	 m_seed.loadSettingsFrom(settings);
     	 m_cluster_by.loadSettingsFrom(settings);
     	 m_normalisations.loadSettingsFrom(settings);
     	 
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
         
         m_kmax.validateSettings(settings);
     	 m_algorithm.validateSettings(settings);
     	 m_distance.validateSettings(settings);
     	 m_resample.validateSettings(settings);
     	 m_merge.validateSettings(settings);
     	 m_seed.validateSettings(settings);
     	 m_cluster_by.validateSettings(settings);
     	 m_normalisations.validateSettings(settings);

    }

}
