package au.edu.unimelb.plantcell.proteomics.gravy;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
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
public class GravyScorerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring InsilicoDigestor node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected GravyScorerNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(GravyScorerNodeModel.CFGKEY_SEQUENCE, "Sequence"), 
        		"Protein Sequence column", 0, StringValue.class
        		));   
        
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelIntegerBounded(GravyScorerNodeModel.CFGKEY_WINDOW_SIZE, 9, 3, 101),
        		"Size of window (AA)", 2
        		));
    }
}

