package au.edu.unimelb.plantcell.io.read.alignments;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "LocalMuscle" Node.
 * Supports running a local muscle executable and integration into the KNIME-PlantCell platform
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AlignmentReaderNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the LocalMuscle node.
     */
    protected AlignmentReaderNodeDialog() {
    	addDialogComponent(new DialogComponentFileChooser(
    			new SettingsModelString(AlignmentReaderNodeModel.CFGKEY_FILE, ""), 
    			"alignment-input-file", JFileChooser.OPEN_DIALOG, false, ".aln|.fasta|.fa|.phy|.phylip|.clustal|clustalw"
    	));
    	
    	// HACK: must match NodeModel.execute()!
    	String[] labels = new String[] { "AA", "NA" };
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(AlignmentReaderNodeModel.CFGKEY_TYPE, "AA"), "Amino acids or nucleotides?", 
    			false, labels, labels
    			));
    	addDialogComponent(new DialogComponentStringSelection(
    			new SettingsModelString(AlignmentReaderNodeModel.CFGKEY_FORMAT, ""), 
    			"alignment-format", AlignmentReaderNodeModel.FILE_FORMATS
    			));
    	
    }
}

