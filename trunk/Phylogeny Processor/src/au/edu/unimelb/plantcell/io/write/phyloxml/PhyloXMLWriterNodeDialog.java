package au.edu.unimelb.plantcell.io.write.phyloxml;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * Creates a phyloxml document from the input data, decorated with data from the input table.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class PhyloXMLWriterNodeDialog extends DefaultNodeSettingsPane {

	@SuppressWarnings("unchecked")
	protected PhyloXMLWriterNodeDialog() {
        super();
        
     
        addDialogComponent(
        			new DialogComponentBoolean(
        					new SettingsModelBoolean(PhyloXMLWriterNodeModel.CFGKEY_OVERWRITE, Boolean.FALSE), "Overwrite existing output file?"));
        
        addDialogComponent(new DialogComponentBoolean(
        					new SettingsModelBoolean(PhyloXMLWriterNodeModel.CFGKEY_START_PROG, Boolean.TRUE), "Start archaeopteryx after save?"));
        
        createNewGroup("What tree do you want to decorate?");
        addDialogComponent(new DialogComponentFileChooser(
        					new SettingsModelString(PhyloXMLWriterNodeModel.CFGKEY_INFILE, ""), "Input tree file (eg. Newick/PhyloXML)"
        		));
        addDialogComponent(new DialogComponentBoolean(
        					new SettingsModelBoolean(PhyloXMLWriterNodeModel.CFGKEY_ASSUME_SUPPORT, Boolean.FALSE), "interpret internal node names as support values?"
        		));
        
        createNewGroup("Where should the decorated tree be saved? (always PhyloXML)");
        addDialogComponent(new DialogComponentFileChooser(
				new SettingsModelString(PhyloXMLWriterNodeModel.CFGKEY_OUTFILE, ""), "Output phyloxml file"
        ));
        
        createNewGroup("Tree labels should be matched against ... column?");
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(PhyloXMLWriterNodeModel.CFGKEY_TAXA, ""), "Taxa names (ie. sequences) from... ",
        		0, true, true, SequenceValue.class
        		));
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(PhyloXMLWriterNodeModel.CFGKEY_WANT_SEQUENCE, Boolean.FALSE), "include sequence in phyloxml (CAUTION: lots of data!)?"
        		));
        
        createNewGroup("Match taxa names using... (advanced users only)");
        addDialogComponent(new DialogComponentString(
        		new SettingsModelString(PhyloXMLWriterNodeModel.CFGKEY_TAXA_REGEXP, "(.*)"), "Taxa regexp (first group used to match)", true, 20));
        
        createNewTab("Decorations of output tree");
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(PhyloXMLWriterNodeModel.CFGKEY_SPECIES, ""), "Scientific species names from... ", 
        		0, false, true, StringValue.class
        		));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(PhyloXMLWriterNodeModel.CFGKEY_WANT_IMAGES, ""), "Image URLs from... ",
        		0, false, true, StringValue.class
        		));
        final ColumnFilter cf = new ColumnFilter() {

			@Override
			public boolean includeColumn(DataColumnSpec colSpec) {
				return (colSpec != null && colSpec.getType().isCollectionType() && 
						colSpec.getType().getCollectionElementType().isCompatible(IntValue.class));
			}

			@Override
			public String allFilteredMsg() {
				return "No suitable columns for domain architecture! (list/set collection required)";
			}
			
		};
		final ColumnFilter string_cf = new ColumnFilter() {

			@Override
			public boolean includeColumn(DataColumnSpec colSpec) {
				return (colSpec != null && colSpec.getType().isCollectionType() && 
						colSpec.getType().getCollectionElementType().isCompatible(StringValue.class));
			}

			@Override
			public String allFilteredMsg() {
				return "No suitable columns for domain architecture labels! (list/set collection of strings)";
			}
			
		};
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(PhyloXMLWriterNodeModel.CFGKEY_DOMAIN_LABELS, ""), "Labels for domains (collection) from... ", 
        		0, false, true, string_cf
        		));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(PhyloXMLWriterNodeModel.CFGKEY_DOMAIN_STARTS, ""), "Start positions for domains (collection) from... ",
        		0, false, true, cf
        		));
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(PhyloXMLWriterNodeModel.CFGKEY_DOMAIN_ENDS, ""), "End positions for domains (collection) from... ",
        		0, false, true, cf
        		));
    }
}

