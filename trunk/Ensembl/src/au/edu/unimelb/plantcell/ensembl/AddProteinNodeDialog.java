package au.edu.unimelb.plantcell.ensembl;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

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
public class AddProteinNodeDialog extends AddHomologueNodeDialog {
	
    /**
     * New pane for configuring the EnsembleAddHomologue node.
     */
    protected AddProteinNodeDialog() {
    	super();
    }
}

