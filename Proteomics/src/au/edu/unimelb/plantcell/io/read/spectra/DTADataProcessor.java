package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;

import org.knime.core.node.ExecutionContext;

import au.edu.unimelb.plantcell.core.MyDataContainer;

public class DTADataProcessor extends AbstractDataProcessor {
	private File m_file;
	
	@Override
	public void setInput(String id) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean can(File f) throws Exception {
		m_file = f;
		String ext = f.getName().toLowerCase();
		return (ext.endsWith(".dta") || ext.endsWith(".dta.gz"));
	}

	@Override
	public void process(boolean load_spectra, ExecutionContext exec,
			MyDataContainer scan_container, MyDataContainer file_container)
			throws Exception {
		// TODO Auto-generated method stub

	}

}
