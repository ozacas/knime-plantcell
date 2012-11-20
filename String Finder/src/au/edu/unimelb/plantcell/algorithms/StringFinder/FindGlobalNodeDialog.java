package au.edu.unimelb.plantcell.algorithms.StringFinder;

import java.util.Arrays;

import org.knime.core.node.defaultnodesettings.*;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;

/**
 * <code>NodeDialog</code> for the "Find (2 ports)" Node.
 * Used to match a set of strings against a column in the input table. All input columns are preserved, but the matches are appended to the table.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class FindGlobalNodeDialog extends DefaultNodeSettingsPane {
	
    // NB: must match the node model code... HACK!
	private static String[] output_vec = new String[] { 
		"Matches (collection)", "Match Positions (collection)", "Unique Match Count", "Unique Matches", "Match Count", 
		"Start Positions (collection)", "Extent of matches (collection)",
		"Match Extent (substring)", "Match Extent (position)", "Patterns (successful, distinct)", "Non-overlapping matches (collection)",
		"Match Start Position Density (Bit Vector)", "Match Position Density (Bit Vector)", "Non-overlapping match count",
		"Number of matches per position (collection)", "Unique Match Distribution", "Pattern distribution (successful only)",
		"Input String Coverage (%)", "Highlight Matches (HTML, single colour)", "Annotate sequences"
	};
	
  
	protected FindGlobalNodeDialog() {
		super();
        init(this, true);
    }
    
    @SuppressWarnings("unchecked")
    protected static void init(DefaultNodeSettingsPane self, boolean two_input_ports) {
    	 Arrays.sort(output_vec);
    	 
         self.addDialogComponent(new DialogComponentBoolean(
         		new SettingsModelBoolean(FindGlobalNodeModel.CFG_ONLY_ROWS, false), "Only output matching rows"));
         self.addDialogComponent(new DialogComponentBoolean(
         		new SettingsModelBoolean(FindGlobalNodeModel.CFG_AS_REGEXP, false), "Treat search strings as regular expression?"));
         self.addDialogComponent(new DialogComponentColumnNameSelection(
         		new SettingsModelString(FindGlobalNodeModel.CFG_INPUT_STRINGS, ""), "Column to search:", 0, StringValue.class));
         SettingsModelString sms = new SettingsModelString(FindGlobalNodeModel.CFG_MATCHER_STRINGS, "");
         if (two_input_ports) {
        	 self.addDialogComponent(new DialogComponentColumnNameSelection(
         		sms, "Column with search strings:", 1, StringValue.class));
         } else {
        	 self.addDialogComponent(new DialogComponentColumnNameSelection(
        			 sms, "Collection of search strings: ", 0, new ColumnFilter() {

						@Override
						public boolean includeColumn(DataColumnSpec colSpec) {
							return (colSpec.getType().isCollectionType() && 
									colSpec.getType().getCollectionElementType().isCompatible(StringValue.class));
						}

						@Override
						public String allFilteredMsg() {
							return "No list or set columns to use for strings to search!";
						}
        				 
        			 }
        			 ));
         }
         self.addDialogComponent(new DialogComponentStringListSelection(
         		new SettingsModelStringArray(FindGlobalNodeModel.CFG_OUTPUT_FORMAT, new String[] {"Matches (collection)"}), "Required output", output_vec));
    }
}

