package au.edu.unimelb.plantcell.gp;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * This is the model implementation of GenePattern.
 * Nodes to support remote invocation of a GenePattern instance for key analyses (heatmap, clustering etc.)
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class HeatmapNodeModel extends AbstractGPNodeModel {
    
	public final static String CFGKEY_GP_URL    = "genepattern-website";
	public final static String CFGKEY_GP_USER   = "genepattern-user-email";
	public final static String CFGKEY_GP_PASSWD = "genepattern-user-password";
	public final static String CFGKEY_DESCR_COL = "description-column";
	private static final int MAX_CELLS = 1000 * 1000;
	
	private SettingsModelString m_gp_url = new SettingsModelString(CFGKEY_GP_URL, "http://genepattern.broadinstitute.org/gp");
	private SettingsModelString m_gp_user= new SettingsModelString(CFGKEY_GP_USER, "");
	private SettingsModelString m_gp_passwd = new SettingsModelString(CFGKEY_GP_PASSWD, "");
	private SettingsModelColumnName m_descr_col = new SettingsModelColumnName(CFGKEY_DESCR_COL, "");
	
	// internal state -- persisted to ensure the view can survive knime sessions
	private String m_dataset;		// always in genepattern GCT format
	
    /**
     * Constructor for the node model.
     */
    protected HeatmapNodeModel() {
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
    	if (inData[0].getRowCount() * n_cols > MAX_CELLS) {
    		throw new Exception("Cant plot that many heatmap cells!");
    	}
    	if (n_cols < 1 || inData[0].getRowCount() < 1) {
    		throw new Exception("Gotta have some data to plot a heatmap!");
    	}
    	
    	if (m_descr_col.useRowID()) {
	    	m_dataset = make_gct_matrix(inData[0].getRowCount(), n_cols, m_numeric_columns, inData[0].iterator(),
	    			new RowDescriptionFilter() {

						@Override
						public String getRowDescription(DataRow r) {
							return r.getKey().getString();
						}
	    		
	    	});
    	} else {
	    	final int descr_idx = inData[0].getDataTableSpec().findColumnIndex(m_descr_col.getColumnName());
	    
	    	m_dataset = make_gct_matrix(inData[0].getRowCount(), n_cols, m_numeric_columns, inData[0].iterator(), new RowDescriptionFilter() {

				@Override
				public String getRowDescription(DataRow r) {
					if (descr_idx < 0) 
						return "";
					return r.getCell(descr_idx).toString();
				}
	    		
	    	});
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
         m_descr_col.saveSettingsTo(settings);
       
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
         m_descr_col.loadSettingsFrom(settings);

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
         m_descr_col.validateSettings(settings);
    }

    public boolean  accessGPServer() {
    	return accessGPServer(m_gp_url.getStringValue());
    }
    
    /**
     * Returns <code>true</code> if the user-specified URL is valid, <code>false</code>
     * if connection was not possible or a problem with the network exists.
     * 
     * @return
     */
	public boolean accessGPServer(String gp_url) {
		try {
			logger.info("Checking access to GenePattern server at "+gp_url);
			URL u = new URL(gp_url);
			URLConnection conn = u.openConnection();
			conn.setConnectTimeout(20 * 1000);
			conn.setDoInput(false);
			conn.setDoOutput(false);
			conn.connect();
			logger.info("Connection established!");
			if (conn instanceof HttpURLConnection) {
				((HttpURLConnection)conn).disconnect();
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}

