package au.edu.unimelb.plantcell.proteomics.itraq;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "ITraqAnalyzer" Node.
 * Given a (set of) proteomics runs, with identified peptides, proteins and iTRAQ quantitation values this nodes performs an analysis and provides normalised results for the user in easy-to-read format. Based on method published in the scientific literature.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatic
 */
public class ITraqAnalyzerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the ITraqAnalyzer node.
     */
    protected ITraqAnalyzerNodeDialog() {

    }
}

