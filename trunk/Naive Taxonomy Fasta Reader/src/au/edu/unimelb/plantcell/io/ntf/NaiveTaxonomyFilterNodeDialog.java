package au.edu.unimelb.plantcell.io.ntf;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.data.StringValue;

/**
 * <code>NodeDialog</code> for the "NaiveTaxonomyFilter" Node.
 * Used to extract sequences from a FASTA file which match a given column of taxa. Regular expressions can be provided to match the taxa entry from the description in the FASTA file. Taxa desired form the input to the node.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class NaiveTaxonomyFilterNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring NaiveTaxonomyFilter node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected NaiveTaxonomyFilterNodeDialog() {
        super();
        
       this.createNewGroup("FASTA File to process");
       addDialogComponent(
    	  new DialogComponentFileChooser(
    		   new SettingsModelString(NaiveTaxonomyFilterNodeModel.CFGKEY_FASTA, "nr.fasta"), "fasta-file-history", JFileChooser.OPEN_DIALOG, false));
       this.closeCurrentGroup();

       addDialogComponent(
    	  new DialogComponentString(
    		   new SettingsModelString(NaiveTaxonomyFilterNodeModel.CFGKEY_RE_TAXA, ""), "Taxonomy Regular Expression"));
       
       addDialogComponent(
    	    	  new DialogComponentString(
    	    		   new SettingsModelString(NaiveTaxonomyFilterNodeModel.CFGKEY_RE_ACCSN_DESCR, ""), "Accession and Description Regular Expression"));
    	       
       addDialogComponent(
    	  new DialogComponentColumnNameSelection(
    		   new SettingsModelString(NaiveTaxonomyFilterNodeModel.CFGKEY_TAXA_COL, "Taxonomy Name"), "Taxonomy column", 0, StringValue.class));
       
    }
    
    @Override 
    public boolean closeOnESC() {
    	return true;
    }

}

