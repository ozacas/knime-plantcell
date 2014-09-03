package au.edu.unimelb.plantcell.proteomics.proteinselector;

import javax.swing.ListSelectionModel;
import java.util.*;
import org.knime.core.node.defaultnodesettings.*;
import org.knime.core.data.StringValue;

/**
 * <code>NodeDialog</code> for the "RepresentativeProteinSelector" Node.
 * Selects, amongst proteins which share peptides, a representative sequence. In the future, this will provide multiple strategies for doing this: but only one for now. Designed to match the results from the ACPFG String Matcher
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class RepresentativeProteinSelectorNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring RepresentativeProteinSelector node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected RepresentativeProteinSelectorNodeDialog() {
        super();
        SettingsModelString      m_matches = (SettingsModelString) RepresentativeProteinSelectorNodeModel.make(RepresentativeProteinSelectorNodeModel.CFGKEY_PEPTIDE_MATCHES);
        SettingsModelStringArray m_strategy= (SettingsModelStringArray) RepresentativeProteinSelectorNodeModel.make(RepresentativeProteinSelectorNodeModel.CFGKEY_STRATEGY);
        SettingsModelString      m_accsn   = (SettingsModelString) RepresentativeProteinSelectorNodeModel.make(RepresentativeProteinSelectorNodeModel.CFGKEY_ACCSN);
        
        addDialogComponent(new DialogComponentColumnNameSelection(m_matches, "Peptide Match Column", 0, true, StringValue.class));
        List<String> l = new ArrayList<String>();
        for (String s : m_strategy.getStringArrayValue()) {
        	l.add(s);
        }
        addDialogComponent(new DialogComponentStringListSelection(m_strategy, "Selection Strategy", l, ListSelectionModel.SINGLE_SELECTION, true, 1));
        addDialogComponent(new DialogComponentColumnNameSelection(m_accsn, "Accession", 0, true, StringValue.class));
    }
}

