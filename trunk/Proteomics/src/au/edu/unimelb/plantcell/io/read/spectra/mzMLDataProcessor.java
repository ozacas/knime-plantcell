package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

import uk.ac.ebi.jmzml.xml.Constants;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;


/**
 * Implements support for mzML spectra and scan/file statistics, based on the 
 * jMZML library: http://code.google.com/p/jmzml/. This library supports visual display 
 * of spectra/chromatograms which this node will soon offer (for mzML files only) 
 * 
 * @author andrew.cassin
 *
 */
public class mzMLDataProcessor extends AbstractDataProcessor {
	private File m_file;
	private MzMLUnmarshaller m_um;
	private NodeLogger logger;
	
	public mzMLDataProcessor(NodeLogger l) {
		assert(l != null);
		logger = l;
	}
	
	@Override
	public boolean can(File f) {
		m_file = f;
		String ext = f.getName().toLowerCase();
		
		if (ext.endsWith(".xml") || ext.endsWith(".mzxml")) {
	        return true;
		}
		return false;
	}

	@Override
	public void process(boolean load_spectra, ExecutionContext exec, 
			MyDataContainer scan_container, MyDataContainer file_container) throws Exception {
		
		// first output the file port
		process_file(exec, file_container);
		
		// now output the scan port
		process_scans(exec, scan_container);
	}
	
	protected void process_scans(ExecutionContext exec, MyDataContainer scan_container) throws Exception {
		
	}
	
	/**
	 * Report the file summary
	 * @param exec
	 * @param fc
	 * @param file_seq
	 * @param fh
	 * @throws Exception
	 */
	protected void process_file(ExecutionContext exec, MyDataContainer fc) throws Exception {
    	
    }

    @Override
    public boolean finish() {
    	super.finish();
    	m_file = null;
    	m_um   = null;
    	return true;	
    }
    
	@Override
	public void setInput(String id) {
		// does nothing (m_filename is set by can() above)
	}

}
