package au.edu.unimelb.plantcell.core;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Logger;

import org.eclipse.jface.preference.IPreferenceStore;

import jdbm.RecordManager;
import jdbm.RecordManagerProvider;
import jdbm.htree.HTree;

/**
 * Implements a simple-minded TTL persistent cache using JDBM as the underlying
 * provider
 * @author andrew.cassin
 *
 */
public class Cache {
	// "global" preference settings from File->Preferences in the Workbench
	public static final String PREF_KEY_FOLDER    = "au.edu.unimelb.plantcell.core.cache.folder";	// where cache data is to be stored
	public static final String PREF_KEY_FRESHNESS = "au.edu.unimelb.plantcell.core.cache.freshness"; // ignore cached data older than this (days)
	
	// properties which this implementation supports
	public final static String CACHE_MAX_SIZE = jdbm.RecordManagerOptions.CACHE_SIZE;				// integer
	public final static String CACHE_AUTO_COMMIT = jdbm.RecordManagerOptions.AUTO_COMMIT;			// true or false
	
	// internal state
	private HTree m_db;
	private RecordManager m_recman;
	private int m_freshness;			// in days (180 is default)
	private boolean m_enabled;
	
	/**
	 * Construct a cache from the specified path (basename) and cache properties. If 
	 * something goes wrong an Exception will be thrown.
	 * 
	 * The only way to get a valid cache object: takes the KNIME preferences and service
	 * name into account. If caching is disabled (no folder specified) then the cache
	 * object will have zero-effect.
	 * 
	 * @param service_to_be_cached eg. 'uniprot' (determines cache filenames)
	 * @param service_basename (cache creates two files which share the basename: .lg/.db)
	 * @param p
	 * @throws Exception
	 */
	public Cache(String service_basename, Properties p) throws Exception {
		IPreferenceStore prefs = CorePlugin.getDefault().getPreferenceStore();
		m_freshness            = prefs.getInt(PREF_KEY_FRESHNESS);
		String folder          = prefs.getString(PREF_KEY_FOLDER);
		if (folder.length() > 0) {
			// ensure the folder exists (be silent if it fails)
			try {
				new File(folder).mkdir();
			} catch (Exception e) {
				// BE SILENT AND CONTINUE (since it probably exists already anyway)
			}
			
			// setup the final name for the cache files
			folder += "/" + service_basename;
		}
		m_enabled = (m_freshness > 0 && folder != null && folder.length() > 0);
		if (!m_enabled)
			return;
	
		/**
		 * The use of Class.forName() inside JDBM RecordManagerFactory causes problems with the Eclipse classloader. So
		 * HACK: instead we construct by doing what JDBM was doing internally (will likely break with JDBM3)
		 */
		try {
	            @SuppressWarnings("rawtypes")
				Class clazz = Class.forName("jdbm.recman.Provider", true, Cache.class.getClassLoader());
	            RecordManagerProvider factory = (RecordManagerProvider) clazz.newInstance();
	            m_recman = factory.createRecordManager( folder, p );
		} catch (Exception e) {
			Logger.getAnonymousLogger().warning(e.getMessage());
			throw e;
		}
	}
	
	public void init() throws Exception {
		if (!m_enabled)
			return;
		
		if (m_recman == null)
			throw new Exception("Cache has been destroyed, construct a new cache!");
		
		// try to the load the cache and if that fails, create it
		long recid = m_recman.getNamedObject("objs");
		// load existing cache?
		if (recid != 0) {
			m_db = HTree.load(m_recman, recid);
		} else {	// create new cache...
			m_db = HTree.createInstance(m_recman);
			m_recman.setNamedObject("objs", m_db.getRecid());
		}
	}
	
	/**
	 * Has the user requested caching?
	 * @return
	 */
	public boolean isEnabled() {
		return m_enabled;
	}
	
	/**
	 * Returns the maximum age of an object (in days) for which <code>containsKey()</code> returns true
	 * 
	 * @throws Exception
	 */
	public int getFreshness() {
		return m_freshness;
	}
	
	public void shutdown() throws Exception {
		if (!m_enabled)
			return;
		
		m_recman.close();
		m_db = null;
		m_recman = null;
	}
	
	public void put(String primary_key, String value) throws IOException {
		if (!m_enabled)
			return;
		
		String now = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date());
		m_db.put(primary_key, now + "\n" + value);
	}
	
	/**
	 * Returns true if the object with the specified primary key is present in the
	 * cache AND it has not expired, false otherwise. This implementation will
	 * delete stale objects which are tested, providing limited garbage collection
	 * 
	 * @param primary_key
	 * @return
	 */
	public boolean contains(String primary_key) throws Exception {
		if (!m_enabled)
			return false;
		
		String s = (String) m_db.get(primary_key);		// cant call get() as it removes the date
		if (s == null)
			return false;
		// object stale?
		String val = s.substring(0, s.indexOf('\n'));
		Date     d = DateFormat.getDateInstance().parse(val);
		Date   now = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		c.add(Calendar.DATE, -m_freshness);
		if (d.before(c.getTime())) {
			m_db.remove(primary_key);		// stale object
			return false;
		}
		
		// nope, must be good
		return true;
	}
	
	/**
	 * Return a (potentially stale) object with the given primary key
	 * 
	 * @param primary_key
	 * @return  <code>null</code> if there is no cached object available
	 * @throws Exception
	 */
	public String get(String primary_key) throws Exception {
		assert(primary_key != null && primary_key.length() > 0);
		if (!m_enabled)
			return null;

		String value = (String) m_db.get(primary_key);
		if (value == null)
			return null;
		
		int idx = value.indexOf('\n');		// caller does not get the object date it was added to the cache
		assert(idx >= 0);
		return value.substring(idx+1);
	}
}
