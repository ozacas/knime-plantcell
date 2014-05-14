package au.edu.unimelb.plantcell.io.ws.multialign;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * <code>NodeDialog</code> for the "MuscleAligner" Node.
 * 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MultiAlignerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the MuscleAligner node.
     */
    protected MultiAlignerNodeDialog() {
    	super();
        ColumnFilter cf = new ColumnFilter() {
            @Override
            public String allFilteredMsg() {
                    return "No suitable list, seq or sequence column available: check input data!";
            }

            @Override
            public boolean includeColumn(
                            DataColumnSpec colSpec) {
                    DataType dt = colSpec.getType();
                    if (dt.isCollectionType() && dt.getCollectionElementType().isCompatible(SequenceValue.class)) {
                        return true;
                    }
                    if (dt.isCompatible(SequenceValue.class)) {
                    	return true;
                    }
                    return false;
            }
        };

        addDialogComponent(new DialogComponentString(new SettingsModelString(MultiAlignerNodeModel.CFGKEY_EMAIL, "must-have@email.address.here"),
                       "Email Address", true, 30));
        
        addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(MultiAlignerNodeModel.CFGKEY_ALGO, "MUSCLE"), "Algorithm", "MUSCLE", "T-Coffee", "Clustal Omega"));
      
        addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(MultiAlignerNodeModel.CFGKEY_SEQ_COL, "Sequence"),
                                    "List of sequences", 0, cf));
      
    }
}

