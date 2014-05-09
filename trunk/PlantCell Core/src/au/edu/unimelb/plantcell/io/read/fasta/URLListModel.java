package au.edu.unimelb.plantcell.io.read.fasta;

import java.io.File;
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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * Implements a URL list model for Java/Swing. A {@link SettingsModelStringArray}. The model
 * only keeps a single string representation for each URL and duplicates are automagically removed.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class URLListModel extends AbstractListModel<URL> implements ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9199419562876356502L;
	
	/**
	 * Used to implement the backing store to the list, with additional methods as required to implement the dialog
	 */
	private SettingsModelStringArray m_store;
	
	/**
	 * Sole constructor which requires the backing store to be specified. Registers the instance with
	 * the store so that if the store is changed directly, the model listeners are notified as per the contract.
	 * 
	 * @param store
	 */
	public URLListModel(final SettingsModelStringArray store) {
		assert(store != null);
		m_store = store;
		m_store.addChangeListener(this);
	}
	
	/**
	 * Return the URL for the specified list index
	 */
	@Override
	public URL getElementAt(int index) {
		try {
			String s = m_store.getStringArrayValue()[index];
			return new URL(s);
		} catch (MalformedURLException mfe) {
			mfe.printStackTrace();
			// since this is called from dialog code we are silent about bad URL's until execute()
			return null;
		}
	}

	@Override
	public int getSize() {
		return  m_store.getStringArrayValue().length;
	}

	/**
	 * Replace all current urls in the model with the specified list (which may be empty or null).
	 * URL's whose string representation is identical will be automatically de-duped.
	 * 
	 * @param new_urls_to_store
	 */
	public void setAll(final Collection<URL> new_urls_to_store) {
		HashSet<String> str = new HashSet<String>();
		if (new_urls_to_store != null) {
			for (URL u: new_urls_to_store) {
				str.add(u.toString());
			}
		}
		m_store.setStringArrayValue(str.toArray(new String[0]));
	}
	
	@Override
	public void stateChanged(ChangeEvent ev) {
		// signal to list listeners that the data store has changed...
		fireContentsChanged(this, 0, getSize()-1);
	}

	/**
	 * Returns a copy of the URL form of each element in the current model. Note that modification
	 * of the returned list (or URL's) will have no effect on the model.
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
     * When the settings instance contains File paths rather than URLs we need to convert so the current codebase is happy.
     * This is a backward compatibility feature for existing data analysis pipelines which will be removed in the future.
     * 
     * @param settings
     * @param ulist the object to be modified with URL counterparts for the specified files (may be bogus if File's dont exist)
     * @throws InvalidSettingsException 
     */
    private void convertFileToURLs(final NodeSettingsRO settings) throws InvalidSettingsException {
		assert(settings != null);
		SettingsModelStringArray arr = new SettingsModelStringArray(FastaReaderNodeModel.CFGKEY_FASTA, new String[] {});
		arr.loadSettingsFrom(settings);
		ArrayList<URL> new_url_list = new ArrayList<URL>();
		for (String s : arr.getStringArrayValue()) {
			File f = new File(s);
			try {
				new_url_list.add(f.toURI().toURL());
			} catch (MalformedURLException mfe) {
				mfe.printStackTrace();
			}
		}
		setAll(new_url_list);
	}
    
	/**
	 * Loads internal state from KNIME settings instance
	 * @param settings
	 * @throws InvalidSettingsException
	 */
	public void loadSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		// for backward compatibility we handle File's by converting to URL form
		// this code is tricky: consider the case where someone save's/exports a workflow on one platform and then loads on another...
		if (!settings.containsKey(FastaReaderNodeModel.CFGKEY_INTERNAL_USE_URLS) || 
				!settings.getBoolean(FastaReaderNodeModel.CFGKEY_INTERNAL_USE_URLS)) {
			convertFileToURLs(settings);
		} else {
			m_store.loadSettingsFrom(settings);
		}
		stateChanged(new ChangeEvent(this));
	}

	/**
	 * Saves internal state to KNIME settings instance
	 * @param settings
	 */
	public void saveSettingsTo(NodeSettingsWO settings) {
		// validate model and issue warning if bogus URL's detected for save (user may wish to correct or report bug etc...)
		validateCurrentModel();
		
		// save!
		SettingsModelBoolean use_urls = new SettingsModelBoolean(FastaReaderNodeModel.CFGKEY_INTERNAL_USE_URLS, Boolean.TRUE);
    	use_urls.saveSettingsTo(settings);
		m_store.saveSettingsTo(settings);
	}

	/**
	 * Checks the current model to see if there are any malformed URL's (issues a warning if there are). 
	 */
	private void validateCurrentModel() {
		for (String url : m_store.getStringArrayValue()) {
			try {
				URL u = new URL(url);
			} catch (MalformedURLException mfe) {
				NodeLogger.getLogger("URL List").warn("Encountered invalid URL during save: "+url);
			}
		}
	}
	
	
}
