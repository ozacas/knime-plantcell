package au.edu.unimelb.plantcell.io.ws.uniprot;

import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * <code>NodeDialog</code> for the UniProt Node: which accesses the XML records
 * maintained by www.uniprot.org
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class UniProtAccessorNodeDialog extends DefaultNodeSettingsPane {

	private final static String UNIREF_TASK = "Retrieve UniRef Entries";
	
	protected UniProtAccessorNodeDialog() {
        super();
        
        final SettingsModelString task = UniProtAccessorNodeModel.make_as_string(UniProtAccessorNodeModel.CFGKEY_TASK);
        final SettingsModelString uniref = UniProtAccessorNodeModel.make_as_string(UniProtAccessorNodeModel.CFGKEY_UNIREF);
        final SettingsModelString from_db = UniProtAccessorNodeModel.make_as_string(UniProtAccessorNodeModel.CFGKEY_FROM_ACCSN);
        final SettingsModelString to_db   = UniProtAccessorNodeModel.make_as_string(UniProtAccessorNodeModel.CFGKEY_TO_ACCSN);
        uniref.setEnabled(task.getStringValue().equals(UNIREF_TASK));
        task.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				uniref.setEnabled(task.getStringValue().equals(UNIREF_TASK));
				from_db.setEnabled(task.getStringValue().startsWith("Map"));
				to_db.setEnabled(task.getStringValue().startsWith("Map"));
			}
        	
        });
        addDialogComponent(new DialogComponentStringSelection(task, "Task to perform:", new String[] {"Retrieve UniProt Entries", "Retrieve UniRef Entries", "Retrieve UniPARC Entries", "Map Accessions" }));
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(UniProtAccessorNodeModel.CFGKEY_WANTXML, false), "include XML in results?"));

        addDialogComponent(new DialogComponentColumnNameSelection(UniProtAccessorNodeModel.make_as_string(UniProtAccessorNodeModel.CFGKEY_ACCSN_COL), "Use sequence IDs from... ", 0,  
        		new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						// for now this node does NOT support sequenceValue's (but it will soon)
						return (!colSpec.getType().isCompatible(SequenceValue.class) && 
								 colSpec.getType().isCompatible(StringValue.class));
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable Sequence ID (string) columns to use!";
					}
        	
        }));
        addDialogComponent(new DialogComponentStringSelection(uniref, "UniRef Database", new String[] {"UniRef100", "UniRef90", "UniRef50"}));
        this.createNewGroup("Accession Mapping:");
        List<String> from_dbs = AccessionMapTask.get_db_list(true);
        List<String> to_dbs   = AccessionMapTask.get_db_list(false);
        addDialogComponent(new DialogComponentStringSelection(from_db, "From:", 
        					from_dbs.toArray(new String[0])));
        addDialogComponent(new DialogComponentStringSelection(to_db, "To:",
        					to_dbs.toArray(new String[0])));

    }
}

