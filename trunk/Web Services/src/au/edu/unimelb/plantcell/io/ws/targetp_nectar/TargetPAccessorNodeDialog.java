package au.edu.unimelb.plantcell.io.ws.targetp_nectar;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * <code>NodeDialog</code> for the "TargetPAccessor" Node.
 * Integrates the CBS TargetP web service into KNIME providing subcellular location predictions of given protein sequences
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class TargetPAccessorNodeDialog extends DefaultNodeSettingsPane {

    protected TargetPAccessorNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(TargetPAccessorNodeModel.CFGKEY_SEQUENCE, "Sequence"),
        		"Protein Sequence", 0, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return (colSpec.getType().isCompatible(SequenceValue.class));
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable sequence columns available!";
					}
        			
        		}));
        
        addDialogComponent(new DialogComponentButtonGroup(new SettingsModelString(TargetPAccessorNodeModel.CFGKEY_ORGANISM, "plant"), 
        		false, "Organism", new String[] { "plant", "non-plant" }));
        
        createNewGroup("Cut-off values for each score");
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDoubleBounded(TargetPAccessorNodeModel.CFGKEY_CTP_CUTOFF, 0.0, 0.0, 1.0), "cTP cutoff", 0.1, 5));
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDoubleBounded(TargetPAccessorNodeModel.CFGKEY_MTP_CUTOFF, 0.0, 0.0, 1.0), "mTP cutoff", 0.1, 5));
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDoubleBounded(TargetPAccessorNodeModel.CFGKEY_SP_CUTOFF, 0.0, 0.0, 1.0), "SP cutoff", 0.1, 5)); 
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDoubleBounded(TargetPAccessorNodeModel.CFGKEY_OTHER_CUTOFF, 0.0, 0.0, 1.0), "Other cutoff", 0.1, 5));
       
    }
}

