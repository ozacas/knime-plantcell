package au.edu.unimelb.plantcell.io.read.fasta;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Supports reading a single Fasta file using the superclass (useful for loops)
 * @author acassin
 *
 */
public class SingleFastaReaderNodeModel extends FastaReaderNodeModel {

	public final static String CFGKEY_SINGLE_FASTA = "single-fasta";
	
	private final SettingsModelString m_single_fasta = new SettingsModelString(CFGKEY_SINGLE_FASTA, "");
	
	public SingleFastaReaderNodeModel() {
		super();
	}
	
	@Override
	public List<URL> getURLList() throws InvalidSettingsException {
	    	List<URL> ret = new ArrayList<URL>();
	    	try {
	    		ret.add(new URL(m_single_fasta.getStringValue()));
			} catch (MalformedURLException e) {
				try {
					ret.add(new File(m_single_fasta.getStringValue()).toURI().toURL());
				} catch (MalformedURLException e1) {
					throw new InvalidSettingsException(e1);
				}
			}
	    	return ret;
	}
	
	@Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
		super.saveSettingsTo(settings);
		m_single_fasta.saveSettingsTo(settings);
	}
	
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		super.loadValidatedSettingsFrom(settings);
		m_single_fasta.loadSettingsFrom(settings);
	}
	
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		super.validateSettings(settings);
		m_single_fasta.validateSettings(settings);
	}
}