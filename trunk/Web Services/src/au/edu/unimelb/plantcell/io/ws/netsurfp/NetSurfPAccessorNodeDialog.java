package au.edu.unimelb.plantcell.io.ws.netsurfp;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * <code>NodeDialog</code> for the "NetPhosAccessor" Node.
 * Implements support for the NetPhos PTM web service
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class NetSurfPAccessorNodeDialog extends DefaultNodeSettingsPane {

	protected NetSurfPAccessorNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(NetSurfPAccessorNodeModel.CFGKEY_SEQUENCE_COL, "Sequence"), "Sequence column", 0 , true, false, new ColumnFilter() {

			@Override
			public boolean includeColumn(DataColumnSpec colSpec) {
				return colSpec.getType().isCompatible(SequenceValue.class);
			}

			@Override
			public String allFilteredMsg() {
				return "No suitable biological sequence columns to process! (see String2Sequence node for more info.)";
			}
        	
        }));
   
    }
}

