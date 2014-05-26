package au.edu.unimelb.plantcell.io.read.fasta;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "SingleFastaReader" Node.
 * This nodes reads sequences from the user-specified FASTA file and outputs three columns per sequence: * n1) Accession * n2) Description - often not accurate in practice * n3) Sequence data * n * nNo line breaks are preserved.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class SingleFastaReaderNodeDialog extends FastaReaderNodeDialog {
    /**
     * Establish the configurable parameters associated with reading the FASTA file. Note how we can
     * tailor the regular expressions to match the description line as we see fit. If any fail to match,
     * no sequence will be output - so you can use this to select just sequences of interest.
     */
	protected SingleFastaReaderNodeDialog() {    	
        addAdvancedSettings();
        createNewTab("FASTA File");
        createNewGroup("Select the file to read...");
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString(SingleFastaReaderNodeModel.CFGKEY_SINGLE_FASTA, ""), 
        		"FASTA file to read... ", JFileChooser.OPEN_DIALOG, false));
        
        createNewGroup("Sequences in the file are... ");
        String[] seqTypes = au.edu.unimelb.plantcell.core.cells.SequenceType.getSeqTypes();
        addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(FastaReaderNodeModel.CFGKEY_SEQTYPE, seqTypes[0]), "AA/NA?", seqTypes));
        
        selectTab("FASTA File");        
    }

	@Override
	protected JPanel addFastaFileList() {
		// this node does not permit more than one file to be processed
		return null;
	}
	
	@Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
		// superclass implementation is not suitable, so we NO-OP here...
	}
	
	@Override
    public void saveAdditionalSettingsTo(NodeSettingsWO settings) {
		// superclass implementation is not suitable, so we NO-OP here...
		settings.addStringArray(SingleFastaReaderNodeModel.CFGKEY_FASTA, new String[] {});
	}
}
