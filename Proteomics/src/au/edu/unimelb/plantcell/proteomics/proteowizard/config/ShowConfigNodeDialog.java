package au.edu.unimelb.plantcell.proteomics.proteowizard.config;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "AnalystWiffConverter" Node.
 * Using a JAX-WS web service, this node converts a wiff file (optionally a .wiff.scan file too) to an open-format and then loads it as per Spectra Reader.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ShowConfigNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the XCaliburRawConverter node.
     */
    protected ShowConfigNodeDialog() {
    	   super();
          
    }
    
}

