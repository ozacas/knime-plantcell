package au.edu.unimelb.plantcell.networks;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "NetworkViz" Node.
 * Support for network visualisation and analysis (jung, cytoscape, gephi)
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class NetworkVizNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the NetworkViz node.
     */
    protected NetworkVizNodeDialog() {

    }
}

