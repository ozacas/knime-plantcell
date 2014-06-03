package au.edu.unimelb.plantcell.proteomics.views.multi;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

/**
 * <code>NodeDialog</code> for the "MultiSurface" Node.
 * Represents multiple surfaces each with different properties, surface datapoints are taken from the input data table.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MultiSurfaceNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring MultiSurface node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected MultiSurfaceNodeDialog() {
        super();
        
        final ColumnFilter cf = new ColumnFilter() {

			@Override
			public String allFilteredMsg() {
				return "No suitable numeric columns available!";
			}

			@Override
			public boolean includeColumn(DataColumnSpec arg0) {
				return (arg0 != null && arg0.getType().isCompatible(DoubleValue.class));
			}
        	
        };
        
        createNewGroup("Plane of surface is defined by...");
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(MultiSurfaceNodeModel.CFGKEY_X, ""), "X", 0, true, false, cf));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(MultiSurfaceNodeModel.CFGKEY_Y, ""), "Y", 0, true, false, cf));
        
        createNewGroup("Columns to provide Z-values for each surface");
        addDialogComponent(new DialogComponentColumnFilter(new SettingsModelFilterString(MultiSurfaceNodeModel.CFGKEY_Z), 0, false, cf));
    }
}

