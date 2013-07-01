package au.edu.unimelb.plantcell.core;

import org.eclipse.jface.preference.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

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
		
		setPreferenceStore(CorePlugin.getDefault().getPreferenceStore());
		setTitle("PlantCell Preferences");
		setDescription("PlantCell extension: http://www.plantcell.unimelb.edu.au");
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		 Composite parent = getFieldEditorParent();
		 addField(new MyDirectoryFieldEditor(Preferences.PREF_KEY_FOLDER, 
				 "Directory to cache WWW data", parent));

	     addField(new IntegerFieldEditor(
	    		 Preferences.PREF_KEY_FRESHNESS,
	                "Ignore data older than... (days)", parent));
	     
	     addField(new MyDirectoryFieldEditor(Preferences.PREFS_BLAST_FOLDER, 
	    		 	"NCBI BLAST+ folder", parent));
	     addField(new MyDirectoryFieldEditor(Preferences.PREFS_GLPK_FOLDER, 
	    		 	"GNU GLPK 4.x folder", parent));
	     addField(new MyDirectoryFieldEditor(Preferences.PREFS_JEMBOSS_FOLDER, 
	    		 	"mEMBOSS folder", parent));
	     addField(new MyDirectoryFieldEditor(Preferences.PREFS_JRE_FOLDER,
	    		 	"Java (JRE 1.5 or later) folder", parent));
	    	
	     addField(new MyDirectoryFieldEditor(Preferences.PREFS_JALVIEW_FOLDER,
	    		 	"JalView folder", parent));
	     addField(new MyDirectoryFieldEditor(Preferences.PREFS_AUGUSTUS_FOLDER,
	    		    "Augustus folder", parent));
	     
	     addField(new DoubleFieldEditor(Preferences.PREFS_SPECTRA_MIN_MZ, "Minimum spectra m/z for 1D peak density", parent));
	     addField(new DoubleFieldEditor(Preferences.PREFS_SPECTRA_MAX_MZ, "Maximum spectra m/z for 1D peak density", parent));
	     addField(new DoubleFieldEditor(Preferences.PREFS_SPECTRA_BIN_SIZE, "Bin width for 1D peak density", parent));
	     addField(new DoubleFieldEditor(Preferences.PREFS_SPECTRA_THRESHOLD, "Minimum threshold to display in 1D peak density plot", parent));
	 }

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}