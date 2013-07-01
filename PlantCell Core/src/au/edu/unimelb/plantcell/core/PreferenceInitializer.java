package au.edu.unimelb.plantcell.core;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;


/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		 IPreferenceStore prefs = CorePlugin.getDefault().getPreferenceStore();
		 
		 // establish defaults
		 prefs.setDefault(Preferences.PREF_KEY_FOLDER, "");
		 prefs.setDefault(Preferences.PREF_KEY_FRESHNESS, 180);
		 prefs.setDefault(Preferences.PREFS_BLAST_FOLDER, "c:/blast");
		 prefs.setDefault(Preferences.PREFS_GLPK_FOLDER, "c:/cygwin/bin");
		 prefs.setDefault(Preferences.PREFS_JEMBOSS_FOLDER, "c:/mEMBOSS");
		 prefs.setDefault(Preferences.PREFS_JRE_FOLDER, "c:/Program Files/Java/jdk6");
		 prefs.setDefault(Preferences.PREFS_JALVIEW_FOLDER, "c:/Program Files/JalView");
		 prefs.setDefault(Preferences.PREFS_AUGUSTUS_FOLDER, "c:/augustus.2.5.5");
		 prefs.setDefault(Preferences.PREFS_SPECTRA_BIN_SIZE, 0.05);
		 prefs.setDefault(Preferences.PREFS_SPECTRA_MIN_MZ, 100.0);
		 prefs.setDefault(Preferences.PREFS_SPECTRA_MAX_MZ, 2000.0);
		 prefs.setDefault(Preferences.PREFS_SPECTRA_THRESHOLD, 0.0);
	}

}
