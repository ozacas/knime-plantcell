package au.edu.unimelb.plantcell.annotations;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceType;

/**
 * Converts two columns (accession, sequence) into a sequence cell (dialog implementation)
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class String2SequenceNodeDialog extends DefaultNodeSettingsPane {
	
    @SuppressWarnings("unchecked")
	protected String2SequenceNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentStringSelection(
        		new SettingsModelString(String2SequenceNodeModel.CFGKEY_TYPE, SequenceType.AA.toString()), 
        		"Sequence Type?", SequenceType.getSeqTypes() ));
        
        // using SettingsModelColumnName ensures a row id can be used as the accsn (it typically has the right properties)
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelColumnName(String2SequenceNodeModel.CFGKEY_ACCSN_COL, "Accession"), 
        		"Accession from... ", 0, StringValue.class));
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(String2SequenceNodeModel.CFGKEY_SEQUENCE_COL, "Sequence"), 
        		"Sequence from... ", 0, StringValue.class));
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(String2SequenceNodeModel.CFGKEY_DESCR, "None"),
        		"Description from... (optional)", 0, false, true, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return colSpec.getType().isCompatible(StringValue.class);
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable string columns to show for the description!";
					}
        			
        		}));
    }
}

