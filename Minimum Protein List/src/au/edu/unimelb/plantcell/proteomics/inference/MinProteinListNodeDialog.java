package au.edu.unimelb.plantcell.proteomics.inference;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

/**
 * <code>NodeDialog</code> for the "MinProteinList" Node.
 * Uses a greedy set cover algorithm to identify the minimal set of proteins which can explain the observed peptides
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class MinProteinListNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the MinProteinList node.
     */
    @SuppressWarnings("unchecked")
	protected MinProteinListNodeDialog() {
    	final SettingsModelString matches = new SettingsModelString(MinProteinListNodeModel.CFGKEY_PEPTIDES, "Peptides");
    	final SettingsModelString accsn   = new SettingsModelString(MinProteinListNodeModel.CFGKEY_PROTEIN, "Protein");
    	final String[] items = new String[] {
    		"Minimum Set Cover (all proteins equal cost)",
    		"Minimum Set Cover (Unique Peptide Weighting, experimental)"
    	};
        addDialogComponent(new DialogComponentColumnNameSelection(accsn,   "Accession Column", 0, true, StringValue.class));
        addDialogComponent(
        		new DialogComponentColumnNameSelection(matches, "Matching Peptides Column (concatenated list of peptides or collection)", 
        				0, true, new ColumnFilter() {

							@Override
							public boolean includeColumn(DataColumnSpec colSpec) {
								if (colSpec.getType().isCollectionType() && colSpec.getType().getCollectionElementType().isCompatible(StringValue.class))
									return true;
								
								if (colSpec.getType().isCompatible(StringValue.class)) 
									return true;
								
								return false;
							}

							@Override
							public String allFilteredMsg() {
								return "No suitable columns (string or List/Set column) to select!";
							}
        			
        		}));
        addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(MinProteinListNodeModel.CFGKEY_ALGO, items[0]), "Algorithm", items));
    }
}

