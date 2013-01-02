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
		 prefs.setDefault(PreferenceConstants.PREF_KEY_FOLDER, "");
		 prefs.setDefault(PreferenceConstants.PREF_KEY_FRESHNESS, 180);
		 prefs.setDefault(PreferenceConstants.PREFS_BLAST_FOLDER, "c:/blast");
		 prefs.setDefault(PreferenceConstants.PREFS_GLPK_FOLDER, "c:/cygwin/bin");
		 prefs.setDefault(PreferenceConstants.PREFS_JEMBOSS_FOLDER, "c:/mEMBOSS");
		 prefs.setDefault(PreferenceConstants.PREFS_JRE_FOLDER, "c:/Program Files/Java/jdk6");
		 prefs.setDefault(PreferenceConstants.PREFS_JALVIEW_FOLDER, "c:/Program Files/JalView");
		 prefs.setDefault(PreferenceConstants.PREFS_AUGUSTUS_FOLDER, "c:/augustus.2.5.5");
		 
		 // and set the current values to the corresponding default...
		 /*prefs.setToDefault(PreferenceConstants.PREF_KEY_FOLDER);
		 prefs.setToDefault(PreferenceConstants.PREF_KEY_FRESHNESS);
		 prefs.setToDefault(PreferenceConstants.PREFS_BLAST_FOLDER);
		 prefs.setToDefault(PreferenceConstants.PREFS_GLPK_FOLDER);
		 prefs.setToDefault(PreferenceConstants.PREFS_JEMBOSS_FOLDER);
		 prefs.setToDefault(PreferenceConstants.PREFS_JRE_FOLDER);
		 prefs.setToDefault(PreferenceConstants.PREFS_JALVIEW_FOLDER);
		 prefs.setToDefault(PreferenceConstants.PREFS_AUGUSTUS_FOLDER);*/

	}

}
