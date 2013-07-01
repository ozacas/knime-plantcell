package au.edu.unimelb.plantcell.core;



/**
 * Constant definitions for plug-in preferences. Every preference setting must also appear in other source
 * files: see Preference*.java
 */
public class Preferences {

		/**
		 * Where is the NCBI BLAST+ software located? (the root of the install directory NOT the bin folder)
		 */
	   public static final String PREFS_BLAST_FOLDER   = "au.edu.unimelb.plantcell.prefs.blast.folder";
	   
	   /**
	    * Where is the GNU GLPK software located? (root of the install directory)
	    */
	   public static final String PREFS_GLPK_FOLDER    = "au.edu.unimelb.plantcell.prefs.glpk.folder";
	   
	   /**
	    * Where is the jEMBOSS software located? (root of the install directory)
	    */
	   public static final String PREFS_JEMBOSS_FOLDER = "au.edu.unimelb.plantcell.prefs.jemboss.folder";
	   
	   public static final String PREFS_JALVIEW_FOLDER = "au.edu.unimelb.plantcell.prefs.jalview.folder";
	   
	   public static final String PREFS_JRE_FOLDER     = "au.edu.unimelb.plantcell.prefs.jre.folder";
	   
	   /**
	    * Gene prediction software? 
	    */
	   public static final String PREFS_AUGUSTUS_FOLDER= "au.edu.unimelb.plantcell.prefs.augustus.folder";
	   
	   public static final String PREF_KEY_FOLDER      = Cache.PREF_KEY_FOLDER;
	   
	   public static final String PREF_KEY_FRESHNESS   = Cache.PREF_KEY_FRESHNESS;

	   /**
	    * Settings for the spectra renderer to use (see the spectra reader and its output column for details)
	    */
	   public static final String PREFS_SPECTRA_MIN_MZ = "au.edu.unimelb.plantcell.prefs.spectra.mz_min";			// m/z
	   public static final String PREFS_SPECTRA_MAX_MZ = "au.edu.unimelb.plantcell.prefs.spectra.mz_max";			// m/z
	   public static final String PREFS_SPECTRA_BIN_SIZE = "au.edu.unimelb.plantcell.prefs.spectra.bin_width";		// in m/z units
	   public static final String PREFS_SPECTRA_THRESHOLD = "au.edu.unimelb.plantcell.prefs.spectra.threshold";		// in intensity units
	   
}
