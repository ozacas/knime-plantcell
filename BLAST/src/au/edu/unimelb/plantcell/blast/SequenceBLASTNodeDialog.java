package au.edu.unimelb.plantcell.blast;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * <code>NodeDialog</code> for the "BLASTPlus" Node.
 * Supports local execution of NCBI BLAST+ executables (which must be  installed separately)
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/
 */
public class SequenceBLASTNodeDialog extends AbstractNodeDialog {

    /**
     * New pane for configuring BLASTPlus node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
	protected SequenceBLASTNodeDialog() {
        super();
        
        createNewGroup("Which sequences do you want to BLAST?");
        
        addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(BLASTPlusNodeModel.CFGKEY_QUERY_DATABASE, "Sequence"), 
        		"Query Sequence", 0, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return (colSpec.getType().isCompatible(SequenceValue.class));
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable sequence columns available!";
					}
        			
        		}));
       
        init();
    }
}

