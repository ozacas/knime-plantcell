package au.edu.unimelb.plantcell.algorithms.StringFinder;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "1 input port" Find Node.
 * Used to match a set of strings against a column in the input table. All input columns are preserved, but the matches are appended to the table.
 *
 * This node dialog derives from {@link FindGlobalNodeDialog}
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class FindLocalNodeDialog extends DefaultNodeSettingsPane {
	
	protected FindLocalNodeDialog() {
		super();
        FindGlobalNodeDialog.init(this, false);
    }
}

