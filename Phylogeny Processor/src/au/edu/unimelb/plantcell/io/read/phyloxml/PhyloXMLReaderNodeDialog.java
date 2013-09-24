package au.edu.unimelb.plantcell.io.read.phyloxml;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * Creates a phyloxml document from the input data, decorated with data from the input table.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class PhyloXMLReaderNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	protected PhyloXMLReaderNodeDialog() {
        super();
        
        createNewGroup("What tree do you want to read?");
        addDialogComponent(new DialogComponentFileChooser(
        					new SettingsModelString(PhyloXMLReaderNodeModel.CFGKEY_INFILE, ""), "Input tree file (eg. Newick/PhyloXML)",
        					".xml|.phyloxml",
        					".newick|.nw",
        					".nexus|.nex|.nx"
        		));
        
    }
}

