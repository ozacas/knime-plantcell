package au.edu.unimelb.plantcell.core;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.workbench.ui.KNIMEUIPlugin;


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
		 IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
		 prefStore.setDefault(Cache.PREF_KEY_FOLDER, "");
		 prefStore.setDefault(Cache.PREF_KEY_FRESHNESS, 180);
		 prefStore.setDefault(PreferenceConstants.PREFS_BLAST_FOLDER, "c:/blast");
		 prefStore.setDefault(PreferenceConstants.PREFS_GLPK_FOLDER, "c:/cygwin/bin");
		 prefStore.setDefault(PreferenceConstants.PREFS_JEMBOSS_FOLDER, "c:/mEMBOSS");
		 prefStore.setDefault(PreferenceConstants.PREFS_JRE_FOLDER, "c:/Program Files/Java/jdk6");
		 prefStore.setDefault(PreferenceConstants.PREFS_JALVIEW_FOLDER, "c:/Program Files/JalView");
		 prefStore.setDefault(PreferenceConstants.PREFS_AUGUSTUS_FOLDER, "c:/augustus.2.5.5");
		 
	}

}
