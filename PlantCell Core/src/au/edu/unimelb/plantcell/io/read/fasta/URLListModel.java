package au.edu.unimelb.plantcell.io.read.fasta;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * Implements a URL list model for Java/Swing. A {@link SettingsModelStringArray}
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class URLListModel extends AbstractListModel<URL> implements ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9199419562876356502L;
	private SettingsModelStringArray m_store;
	
	public URLListModel(final SettingsModelStringArray store) {
		assert(store != null);
		m_store = store;
		m_store.addChangeListener(this);
	}
	
	@Override
	public URL getElementAt(int index) {
		try {
			String s = m_store.getStringArrayValue()[index];
			return new URL(s);
		} catch (MalformedURLException mfe) {
			// since this is called from dialog code we are silent about bad URL's until execute()
			return null;
		}
	}

	@Override
	public int getSize() {
		return  m_store.getStringArrayValue().length;
	}

	public void setAll(Collection<URL> new_urls_to_store) {
		HashSet<String> str = new HashSet<String>();
		for (URL u: new_urls_to_store) {
			str.add(u.toString());
		}
		m_store.setStringArrayValue(str.toArray(new String[0]));
	}
	
	@Override
	public void stateChanged(ChangeEvent ev) {
		// signal to list listeners that the data store has changed...
		fireContentsChanged(this, 0, getSize()-1);
	}

	/**
	 * 
	 * @return NB: a copy of the current list
	 */
	public List<URL> getAll() {
		List<URL> ret = new ArrayList<URL>();
		for (int i=0; i<getSize(); i++) {
			ret.add(getElementAt(i));
		}
		return ret;
	}

	/**
	 * Loads internal state from KNIME settings instance
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void loadSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		m_store.loadSettingsFrom(settings);
		stateChanged(new ChangeEvent(this));
	}

	/**
	 * Saves internal state to KNIME settings instance
	 * @param settings
	 */
	public void saveSettingsTo(NodeSettingsWO settings) {
		m_store.saveSettingsTo(settings);
	}
	
	
}
