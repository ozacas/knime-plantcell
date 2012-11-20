package au.edu.unimelb.plantcell.core;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class PreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public PreferencePage() {
		super(GRID);
		setTitle("PlantCell");
		setDescription("PlantCell extension: http://www.plantcell.unimelb.edu.au");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {

		 addField(new MyDirectoryFieldEditor(Cache.PREF_KEY_FOLDER, 
				 "Directory to cache WWW data", getFieldEditorParent()));

	     addField(new IntegerFieldEditor(
	                Cache.PREF_KEY_FRESHNESS,
	                "Ignore data older than... (days)", getFieldEditorParent()));
	     
	     addField(new MyDirectoryFieldEditor(PreferenceConstants.PREFS_BLAST_FOLDER, 
	    		 	"NCBI BLAST+ folder", getFieldEditorParent()));
	     addField(new MyDirectoryFieldEditor(PreferenceConstants.PREFS_GLPK_FOLDER, 
	    		 	"GNU GLPK 4.x folder", getFieldEditorParent()));
	     addField(new MyDirectoryFieldEditor(PreferenceConstants.PREFS_JEMBOSS_FOLDER, 
	    		 	"mEMBOSS folder", getFieldEditorParent()));
	     addField(new MyDirectoryFieldEditor(PreferenceConstants.PREFS_JRE_FOLDER,
	    		 	"Java (JRE 1.5 or later) folder", getFieldEditorParent()));
	    	
	     addField(new MyDirectoryFieldEditor(PreferenceConstants.PREFS_JALVIEW_FOLDER,
	    		 	"JalView folder", getFieldEditorParent()));
	     addField(new MyDirectoryFieldEditor(PreferenceConstants.PREFS_AUGUSTUS_FOLDER,
	    		    "Augustus folder", getFieldEditorParent()));
	 }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
		 new PreferenceInitializer().initializeDefaultPreferences();
		 setPreferenceStore(KNIMEUIPlugin.getDefault().getPreferenceStore());
	}
	
}