package au.edu.unimelb.plantcell.proteomics.fragmentation;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

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
public class PeptideFragmentationNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring InsilicoDigestor node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected PeptideFragmentationNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(PeptideFragmentationNodeModel.CFGKEY_SEQUENCE, "Sequence"), 
        		"Protein Sequence column", 0, StringValue.class
        		));   
        
        addDialogComponent(new DialogComponentStringSelection(
        		new SettingsModelString(PeptideFragmentationNodeModel.CFGKEY_ENZYME, "Trypsin"),
        		"Protease", PeptideFragmentationNodeModel.getAvailableProteases()
        		));
        
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDouble(PeptideFragmentationNodeModel.CFGKEY_MINMW, 122.0),
        		"Minimum mass to report", 10.0
        		));
        
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDouble(PeptideFragmentationNodeModel.CFGKEY_MAXMW, 2000.0),
        		"Maximum mass to report", 10.0
        		));
      
    }
}

