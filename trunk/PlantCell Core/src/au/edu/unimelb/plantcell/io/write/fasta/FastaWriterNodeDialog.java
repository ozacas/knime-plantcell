package au.edu.unimelb.plantcell.io.write.fasta;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * <code>NodeDialog</code> for the "FastaWriter" Node.
 * Creates a .fasta file with the specified accession, description, and sequence columns
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class FastaWriterNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring FastaWriter node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected FastaWriterNodeDialog() {
        super();
        
        SettingsModelString filename = (SettingsModelString) FastaWriterNodeModel.make(FastaWriterNodeModel.CFGKEY_FILE);
        SettingsModelString seq      = (SettingsModelString) FastaWriterNodeModel.make(FastaWriterNodeModel.CFGKEY_SEQ);
        SettingsModelBoolean overwrite= (SettingsModelBoolean) FastaWriterNodeModel.make(FastaWriterNodeModel.CFGKEY_OVERWRITE);
        
        addDialogComponent(new DialogComponentFileChooser(filename, "file-history", JFileChooser.SAVE_DIALOG, ".fasta|.fa|.fas"));
        addDialogComponent(new DialogComponentBoolean(overwrite, "Overwrite OK?"));
        addDialogComponent(new DialogComponentColumnNameSelection(seq, "Sequences from... ", 0, SequenceValue.class));
    }
}

