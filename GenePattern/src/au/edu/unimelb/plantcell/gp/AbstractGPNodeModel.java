package au.edu.unimelb.plantcell.gp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.genepattern.client.GPClient;
import org.genepattern.webservice.JobResult;
import org.genepattern.webservice.WebServiceException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * Abstract baseclass for all GenePattern nodes which provides common support methods
 * eg. output file fetching and other genepattern-specific functionality. It is
 * recommended that all GP nodes derive themselves from this class and then override
 * for specific requirements.
 * 
 * @author andrew.cassin
 *
 */
public abstract class AbstractGPNodeModel extends org.knime.core.node.NodeModel {
	// the logger instance
    protected static final NodeLogger logger = NodeLogger
            .getLogger(AbstractGPNodeModel.class);
    
    private final static String INTERNAL_MODEL = "internal-model-key";
    private final static String JOB_RESULT     = "job-result";
    
	
	private String m_site_url, m_username, m_passwd;  // not persisted in base class
	private int    m_job_result;					  // persisted via saveInternals()
	private File[] m_downloaded_files;
	
	protected AbstractGPNodeModel(int in_ports, int out_ports) {
		super(in_ports, out_ports);
		m_job_result = -1;
	}
	
    /**
     * Many GP nodes need to access the GCT code (TODO: cleaner abstraction!)
     * 
     * @return
     * @throws IllegalArgumentException 
     */
    protected  String make_gct_matrix(int n_rows, int n_cols, 
    		Map<Integer,String> numeric_columns, RowIterator it, RowDescriptionFilter rdf) throws IllegalArgumentException {
    	StringBuffer dataset = new StringBuffer(100 * 1024);
    	dataset.append("#1.2\n");
    	dataset.append(n_rows);
    	dataset.append('\t');
    	dataset.append(n_cols);
    	dataset.append('\n');
    	
    	// save output column names
    	StringBuffer line = new StringBuffer(1024);
    	line.append("Name\tDescription\t");
    	Map<String,Integer> name2idx = new HashMap<String,Integer>();
    	Integer[] cols = numeric_columns.keySet().toArray(new Integer[0]);
    	Arrays.sort(cols);
    	for (Integer i : cols) {
    		String s = numeric_columns.get(i);
    		if (name2idx.containsKey(s)) {
    			throw new IllegalArgumentException("Duplicate column: "+s+" - not permitted! Fix column names!");
    		}
    		name2idx.put(s, i);
    		line.append(s);
    		line.append('\t');
    	}
        dataset.append(line.toString().trim());
    	dataset.append('\n');
    	assert(name2idx.size() == numeric_columns.size());
    	
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		String rid = r.getKey().getString();
    		
    		String descr = rdf.getRowDescription(r);
			if (descr.indexOf("\t") >= 0) {
				throw new IllegalArgumentException("No tab characters are permitted in the description!");
			}
    		
    		line = new StringBuffer(1024);
    		line.append(rid);
    		line.append('\t');
    		line.append(descr);
    		line.append('\t');
    		for (Integer i : cols) {
    			DataCell cell = r.getCell(i.intValue());
    			if (cell == null || cell == DataType.getMissingCell()) {
    				line.append('\t');
    			} else {
    				if (cell.toString().indexOf("\t") >= 0) {
    					throw new IllegalArgumentException("No tab characters are permitted (found on row "+rid+")!");
    				}
    				line.append(cell.toString());
    				line.append('\t');
    			}
    		}
    		
    		// cant use String.trim() as that will strip columns with missing values too... so
    		dataset.append(mytrim(line.toString()));
    		dataset.append('\n');
    	}
    	
    	return dataset.toString();
    }

    /**
     * Remove at most one trailing tab and any other whitespace characters
     * from the end of line. Does not remove leading whitespace.
     */
    private String mytrim(String line) {
    	int trim_idx = line.length();
    	boolean got_tab = false;
    	for (int idx = line.length()-1; idx>=0; idx--) {
    		char c = line.charAt(idx);
    		if (c == '\t' && !got_tab) {
    			trim_idx--;
    			got_tab = true;
    			continue;
    		} else if (c != '\t') {
    			if (Character.isWhitespace(c)) {
    				trim_idx--;
    				continue;
    			}
    			break;
    		}
    	}
    	return line.substring(0, trim_idx);
    }

	 /**
     * {@inheritDoc}
     */
	@Override
	protected void loadInternals(File internDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		File file = new File(internDir, JOB_RESULT + ".xml");
	
		FileInputStream fin = new FileInputStream(file);
		ModelContentRO mc = ModelContent.loadFromXML(fin);
		fin.close();
		try {
			m_job_result = mc.getInt(JOB_RESULT);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CanceledExecutionException(e.getMessage());
		}
	}

	 /**
     * {@inheritDoc}
     */
	@Override
	protected void saveInternals(File internDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		 ModelContent modelContent = new ModelContent(INTERNAL_MODEL);
		 modelContent.addInt(JOB_RESULT, m_job_result);
		 File file = new File(internDir, JOB_RESULT + ".xml");
	     FileOutputStream fos = new FileOutputStream(file);
	     modelContent.saveToXML(fos);
	     fos.close();
	}
	
	protected void setOutputFiles(JobResult result, NodeLogger logger) throws Exception {
    	m_job_result = result.getJobNumber();
	}

	public boolean hasOutputFiles() {
		try {
			if (m_job_result < 0)
				return false;
			File[] files = downloadFiles();
			return (files != null && files.length > 0);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	protected abstract void saveSettingsTo(NodeSettingsWO settings);

	@Override
	protected abstract void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException;

	@Override
	protected abstract void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException;

	public void setGPParameters(String site_url, String username, String passwd) {
		assert(site_url != null && site_url.startsWith("http"));
		assert(username != null);
		assert(passwd != null);
		
		m_site_url = site_url;
		m_username = username;
		m_passwd   = passwd;
	}
	
	protected GPClient make_gp_client() throws WebServiceException {
		String passwd_supplied = "";
		if (m_passwd.length() > 0) {
			passwd_supplied = " [PASSWORD SUPPLIED]";
		}
		Logger.getAnonymousLogger().info("Connecting to GenePattern: "+m_site_url+" as "+m_username+passwd_supplied);
		return new GPClient(m_site_url, m_username, m_passwd);
	}
	
	 /**
      * {@inheritDoc}
      */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{};
    }
    
	 /**
      * {@inheritDoc}
      */
	@Override
	protected void reset() {
		// NO-OP
	}

	/**
	 * Downloads all output files, for the last invocation of the node (if not already done in the current session). 
	 * Throws an exception if the GP server cannot be
	 * contacted or the files downloaded (eg. it has been removed from the server)
	 * The file to download is as returned from the output of a GenePattern module/pipeline.
	 * 
	 * @param folder where to place the downloaded files (must have sufficient room!)
	 * @return the list of output files or code<null> if something goes wrong
	 * @throws WebServiceException, IOException
	 */
	public File[] downloadFiles(File folder) throws IOException, WebServiceException {
		if (m_job_result < 0) {
			throw new IOException("No job results available - please re-execute the node!");
		}
		if (m_downloaded_files == null || m_downloaded_files.length < 1) {
			GPClient gpc = make_gp_client();
			JobResult jr = gpc.createJobResult(m_job_result);
			logger.info("Located job "+m_job_result);
			logger.info("Downloading results to "+folder.getAbsolutePath());
			File[] ret = jr.downloadFiles(folder.getAbsolutePath());
			for (File f : ret) {
				f.deleteOnExit();
			}
			m_downloaded_files = ret;
			return ret;
		}
		// else
			return m_downloaded_files;
	}

	public File[] downloadFiles() throws IOException, WebServiceException {
		if (m_downloaded_files != null && m_downloaded_files.length > 0) {
			return m_downloaded_files;
		}
		File tmp = File.createTempFile("gp_data_folder", "folder.d");
		tmp.delete(); // TODO HACK BUG: race condition and folder left-over?
		return downloadFiles(tmp.getAbsoluteFile());
	}
	
	public File downloadFile(String sel) throws IOException, WebServiceException {
		if (m_downloaded_files == null || m_downloaded_files.length < 1) {
			try {
				downloadFiles();
				// FALLTHRU...
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		// search each output file for the same name as sel
		for (File f : m_downloaded_files) {
			if (f.getAbsolutePath().endsWith(sel)) {
				return f;
			}
		}
		return null;		// file not found
	}

	public void getOutputFiles(ArrayList<String> items) throws IOException, WebServiceException {
		downloadFiles();
		for (File f : m_downloaded_files) {
			items.add(f.getName());
		}
	}
}
