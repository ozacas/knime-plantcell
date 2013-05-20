package au.edu.unimelb.plantcell.io.write.fasta;

import javax.swing.JFileChooser;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.IntValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

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

    @SuppressWarnings("unchecked")
	protected FastaWriterNodeDialog() {
        super();
        
        SettingsModelString filename = (SettingsModelString) FastaWriterNodeModel.make(FastaWriterNodeModel.CFGKEY_FILE);
        SettingsModelString seq      = (SettingsModelString) FastaWriterNodeModel.make(FastaWriterNodeModel.CFGKEY_SEQ);
        SettingsModelBoolean overwrite= (SettingsModelBoolean) FastaWriterNodeModel.make(FastaWriterNodeModel.CFGKEY_OVERWRITE);
        
        addDialogComponent(new DialogComponentFileChooser(filename, "file-history", JFileChooser.SAVE_DIALOG, ".fasta|.fa|.fas"));
        
        addDialogComponent(new DialogComponentBoolean(overwrite, "Overwrite OK?"));
        
        addDialogComponent(new DialogComponentColumnNameSelection(seq, "Sequences from... ", 0, SequenceValue.class));
        
        addDialogComponent(new DialogComponentColumnNameSelection (
        		new SettingsModelString(FastaWriterNodeModel.CFGKEY_FILENAME_SUFFIX, ""), "Suffix for save to multiple files: ", 0, false, true,
        		new ColumnFilter() {

					@Override
					public String allFilteredMsg() {
						return "No suitable suffix columns available (must contain no more than 40 suffixes approx.)!";
					}

					@Override
					public boolean includeColumn(DataColumnSpec cs) {
						if ((cs.getType().isCompatible(StringValue.class) && cs.getDomain().hasValues()) ||
								cs.getType().isCompatible(IntValue.class)) {
							return true;
						}
						return false;
					}
        			
        		}));
    }
}

