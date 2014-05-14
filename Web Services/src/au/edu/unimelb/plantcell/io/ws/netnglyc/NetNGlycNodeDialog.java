package au.edu.unimelb.plantcell.io.ws.netnglyc;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

public class NetNGlycNodeDialog extends DefaultNodeSettingsPane {
	
	public NetNGlycNodeDialog() {
		addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(NetNGlycNodeModel.CFGKEY_SEQUENCE, "Sequence"), 
				"Protein sequences from...", 0, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						if (colSpec.getType().isCompatible(SequenceValue.class)) {
							return true;
						}
						
						return false;
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable Sequence columns!";
					}
			
		}));
	}
}
