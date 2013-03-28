package au.edu.unimelb.plantcell.io.ws.pfam;


import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.io.ws.tmhmm.AbstractWebServiceNodeModel;


/**
 * Provides access to the REST'ful API to PFAM at the Sanger Institute
 *
 * @author Andrew Cassin
 */
public class PFAMSourceNodeModel extends AbstractWebServiceNodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("PFAM");
        
    // constants
    public final static String CFGKEY_URL = "sanger-pfam-url";
    public final static String DEFAULT_URL= "http://pfam.sanger.ac.uk/";
    public final static String CFGKEY_TYPE= "query-type";
	public static final String CFGKEY_COLUMN = "input-column";
 
	public final static String[] PFAM_Task_Labels = new String[] {
		" find information about selected PFAM families eg. PF00171",
		" find PFAM annotation for my sequences",
		" find PFAM annotation for UniProt ID's eg. P00789"
	};
	
    // node state (persisted)
    private final SettingsModelString m_url = new SettingsModelString(CFGKEY_URL, DEFAULT_URL);
    private final SettingsModelString m_type= new SettingsModelString(CFGKEY_TYPE, PFAM_Task_Labels[2]);
    private final SettingsModelString m_col = new SettingsModelString(CFGKEY_COLUMN, "");
    
    /**
     * Constructor for the node model.
     */
    protected PFAMSourceNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	logger.info("Using "+m_url.getStringValue()+ " as PFAM data source.");
    	logger.info("Fetching "+m_type.getStringValue()+".");
    	
    	PFAMTask t = make_pfam_task();
    	DataTableSpec outSpec = make_output_spec(inData[0].getSpec(), t);
		BufferedDataContainer container = exec.createDataContainer(outSpec);
		int col_idx = inData[0].getSpec().findColumnIndex(m_col.getStringValue());
		if (col_idx < 0) {
			throw new InvalidSettingsException("Cannot find column: "+m_col.getStringValue()+" - reconfigure?");
		}
		t.init(logger, col_idx, new URL(m_url.getStringValue()), inData[0].getSpec());
		
		// create the column rearranger
		ColumnRearranger outputTable = new ColumnRearranger(inData[0].getDataTableSpec());
		
		// append the new column
		outputTable.append(t);
		     
		BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], outputTable, exec);
	
		// once we are done, we close the container and return its table
		container.close();
		return new BufferedDataTable[]{out};
    }
    
    /**
     * Constructs, but does not call <code>init()</code> on the newly created task based on current user configuration
     * @return
     */
    private PFAMTask make_pfam_task() {
    	String type = m_type.getStringValue().toLowerCase();
    	if (type.indexOf("selected pfam families") >= 0) 
    		return new PFAMGetFamilyTask();
    	else if (type.indexOf("uniprot") >= 0) 
    		return new PFAMUniProtGetTask();
    	else if (type.indexOf("my sequences") >= 0)
    		return new PFAMSequenceSearchTask();
    	
    	return null;
    }
    
    private DataTableSpec make_output_spec(DataTableSpec inSpec, PFAMTask t) throws IllegalArgumentException {
    	 if (t == null) {
    		 t = make_pfam_task();
    		 if (t == null)
    			 throw new IllegalArgumentException("No such task: "+m_type.getStringValue());
    	 }
    		
    	 int col_idx = inSpec.findColumnIndex(m_col.getStringValue());
    	 if (col_idx < 0)
    		 throw new IllegalArgumentException("Cannot find input column: "+m_col.getStringValue()+" - reconfigure?");
    	 try {
			t.init(logger, col_idx, new URL(m_url.getStringValue()), inSpec);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e.getMessage());
		}
    	 
         return new DataTableSpec(inSpec, t.getTableSpec());
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
        DataTableSpec out = make_output_spec(inSpecs[0], null);
        return new DataTableSpec[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_url.saveSettingsTo(settings);
         m_type.saveSettingsTo(settings);
         m_col.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_url.loadSettingsFrom(settings);
    	m_type.loadSettingsFrom(settings);
        m_col.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_url.validateSettings(settings);
        m_type.validateSettings(settings);
        m_col.validateSettings(settings);
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

	@Override
	public String getStatus(String jobID) throws Exception {
		// this method is not used since it is not a SOAP-based service
		return null;
	}

}

