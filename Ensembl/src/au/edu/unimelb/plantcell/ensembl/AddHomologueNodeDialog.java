package au.edu.unimelb.plantcell.ensembl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

/**
 * <code>NodeDialog</code> for the "EnsembleAddHomologue" Node.
 * Adds homologues for the input data to the output table
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AddHomologueNodeDialog extends DefaultNodeSettingsPane {
	protected String[] m_species = AddHomologueNodeModel.DEFAULT_SPECIES;
	protected final DialogComponentStringSelection dcss = new DialogComponentStringSelection(
			new SettingsModelString(AddHomologueNodeModel.CFGKEY_SPECIES, 
					AddHomologueNodeModel.DEFAULT_SPECIES[0]), 
			"Species gene IDs", m_species);
	
    /**
     * New pane for configuring the EnsembleAddHomologue node.
     */
    protected AddHomologueNodeDialog() {
       	addDialogComponent(dcss);
    	
    	addDialogComponent(new DialogComponentColumnNameSelection(
    			new SettingsModelColumnName(AddHomologueNodeModel.CFGKEY_SEQUENCE_ID, ""), 
    			"Sequence IDs from... ", 0, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return (colSpec.getType().isCompatible(StringValue.class));
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable columns for sequence ID selection";
					}
    				
    	}));
    	
    	createNewTab("Advanced");
    	addAdvancedTab();
    }
    
    /**
     * NB: does NOT create tab itself, just the widgets for it
     */
    protected void addAdvancedTab() {
    	final SettingsModelString sms = new SettingsModelString(AddHomologueNodeModel.CFGKEY_DB_PROPS, "");
    	addDialogComponent(new DialogComponentMultiLineString(
    			sms, "Ensembl data source", true, 60, 5));
    	DialogComponentButton b = new DialogComponentButton("Reload databases");
    	b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				m_species = AddHomologueNodeModel.getGenomeSpecies(null, sms.getStringValue());
				update_list_items(m_species, dcss, null);
			}
    		
    	});
    	addDialogComponent(b);
	}

	protected void update_list_items(String[] species, final DialogComponentStringSelection dcss, String default_selection) {
    	assert(species != null && dcss != null);
		ArrayList<String> coll = new ArrayList<String>();
    	for (String s : m_species) {
			coll.add(s);
		}
		dcss.replaceListItems(coll, default_selection);
    }
    
    @Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs) {
    	if (settings.containsKey(AddHomologueNodeModel.CFGKEY_KNOWN_GENOMES)) {
    		try {
				m_species = settings.getStringArray(AddHomologueNodeModel.CFGKEY_KNOWN_GENOMES);
				update_list_items(m_species, dcss, settings.getString(AddHomologueNodeModel.CFGKEY_SPECIES));
			} catch (InvalidSettingsException e) {
				e.printStackTrace();
			}
    	}
    }
    
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) {
    	settings.addStringArray(AddHomologueNodeModel.CFGKEY_KNOWN_GENOMES, m_species);
    }
}

