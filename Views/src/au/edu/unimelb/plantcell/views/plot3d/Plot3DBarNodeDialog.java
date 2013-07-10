package au.edu.unimelb.plantcell.views.plot3d;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

/**
 * <code>NodeDialog</code> for the "Plot3DBar" Node.
 * Using jzy3d, this node produces a 3d bar plot using the supplied input columns.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class Plot3DBarNodeDialog extends DefaultNodeSettingsPane {
	
    protected Plot3DBarNodeDialog() {
        super();
        ColumnFilter cf = new ColumnFilter() {

			@Override
			public String allFilteredMsg() {
				return "No numeric columns to use for data in the graph!";
			}

			@Override
			public boolean includeColumn(DataColumnSpec arg0) {
				return (arg0.getType().isCompatible(DoubleValue.class) ||
						arg0.getType().isCompatible(IntValue.class));
			}
			
		};
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(Plot3DBarNodeModel.CFGKEY_X, ""), "X Data", 0, cf));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(Plot3DBarNodeModel.CFGKEY_Y, ""), "Y Data", 0, cf));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(Plot3DBarNodeModel.CFGKEY_Z, ""), "Z Data", 0, cf));
    
        createNewTab("Plot for axis (optional)");
        addDialogComponent(new DialogComponentStringSelection(
        		new SettingsModelString(Plot3DBarNodeModel.CFGKEY_OVERLAY_AXIS, "X"),
        		"Axis for overlay", new String[] { "X", "Y", "Z" }
        		));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(Plot3DBarNodeModel.CFGKEY_OVERLAY_DATA, "<None>"), "Column to use for overlay data", 0, false, true, cf
        		));
    }
}

