package au.edu.unimelb.plantcell.proteomics.digestion;

import javax.swing.ListSelectionModel;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * <code>NodeDialog</code> for the "InsilicoDigestor" Node.
 * Often using javaprotlib, a few useful nodes for Proteomics Mass Spectrometry
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class InsilicoDigestorNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring InsilicoDigestor node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected InsilicoDigestorNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(InsilicoDigestorNodeModel.CFGKEY_SEQUENCE, "Sequence"), 
        		"Protein Sequence column", 0, SequenceValue.class
        		));   
        
        addDialogComponent(new DialogComponentStringSelection(
        		new SettingsModelString(InsilicoDigestorNodeModel.CFGKEY_ENZYME, "Trypsin"),
        		"Protease", InsilicoDigestorNodeModel.getAvailableProteases()
        		));
        
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(InsilicoDigestorNodeModel.CFGKEY_MASSMONO, true), "report monoisotopic digested peptide mass?"));
   
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDouble(InsilicoDigestorNodeModel.CFGKEY_MINMW, 122.0),
        		"Minimum mass to report", 10.0
        		));
        
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDouble(InsilicoDigestorNodeModel.CFGKEY_MAXMW, 2000.0),
        		"Maximum mass to report", 10.0
        		));
        
        createNewGroup("Modifications to consider");
        addDialogComponent(new DialogComponentStringListSelection(new SettingsModelStringArray(InsilicoDigestorNodeModel.CFGKEY_VMODS, new String[] {}), 
        		"Variable modifications", InsilicoDigestorNodeModel.getAvailableModifications(), 
        		ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, false, 10
        		));
    }
}

