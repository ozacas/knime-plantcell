package au.edu.unimelb.plantcell.annotations;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue;

/**
 * Convert a sequence to tabular format based on user-chosen items of interest
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class Alignment2SequenceNodeDialog extends DefaultNodeSettingsPane {
    private final SettingsModelString sms = new SettingsModelString(Alignment2SequenceNodeModel.CFGKEY_ALIGNMENT_COL, "");
   
	protected Alignment2SequenceNodeDialog() {
        super();
      
        addDialogComponent(
        		new DialogComponentColumnNameSelection(sms,
        				"Alignment column", 0,  true, false, new ColumnFilter() {

							@Override
							public boolean includeColumn(DataColumnSpec colSpec) {
								if (colSpec.getType().isCompatible(AlignmentValue.class))
									return true;
								return false;
							}

							@Override
							public String allFilteredMsg() {
								return "No suitable alignment column to use!";
							}
        			
        		}));
        
        
    }
}

