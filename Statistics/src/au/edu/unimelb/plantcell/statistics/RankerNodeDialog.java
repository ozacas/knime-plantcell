package au.edu.unimelb.plantcell.statistics;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

/**
 * <code>NodeDialog</code> for the "Ranker" Node.
 * Basic descriptive and inferential statistics support built using the apache commons math v3 library.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class RankerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring Ranker node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected RankerNodeDialog() {
        super();
         
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(RankerNodeModel.CFGKEY_RANK_COLUMN, ""),
        		"Values to rank... ", 0, true, false, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						if (colSpec.getType().isCompatible(DoubleValue.class))
        					return true;
        				return false;
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable numeric columns to rank!";
					}
        			
        		}));
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelColumnName(RankerNodeModel.CFGKEY_GROUPBY_COLUMN, ""), "Separate ranks for each ...", 0, false, true, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return (colSpec != null && colSpec.getType().isCompatible(StringValue.class));
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable columns for grouping!";
					} 
        			
        		}));
        
        addDialogComponent(new DialogComponentButtonGroup(
        		new SettingsModelString(RankerNodeModel.CFGKEY_TIE_METHOD, "sequential"), 
        		true, "How to handle ties?",
        		new String[] { "sequential               ", "random", "minimum", "minimum consecutive", "maximum", "average" }));		// spaces ensure border title is visible
       
        addDialogComponent(new DialogComponentButtonGroup(new SettingsModelString(RankerNodeModel.CFGKEY_NaN_METHOD, "fixed"), 
        		true, "How to handle missing/NaN?", new String[] {"minimal                      ", "maximal", "remove", "fixed"}));// spaces ensure border title is visible
        		
    }
}

