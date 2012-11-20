package au.edu.unimelb.plantcell.core;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.jface.preference.DirectoryFieldEditor;

/**
 * Overrides to give a convenient experience to the user if some or all external applications are not
 * available
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class MyDirectoryFieldEditor extends DirectoryFieldEditor {

	public MyDirectoryFieldEditor(String s, String s2, Composite c1) {
		super(s, s2, c1);
	}
	
	@Override
	public boolean isValid() {
		return true;
	}
	
	@Override 
	public boolean isEmptyStringAllowed() {
		return true;
	}
	
	@Override
	protected void refreshValidState() {
		super.refreshValidState();
	}
}
