package au.edu.unimelb.plantcell.blast;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "BLASTPlus" Node.
 * Supports local execution of NCBI BLAST+ executables (which must be  installed separately)
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/
 */
public class BLASTPlusNodeDialog extends AbstractNodeDialog {

    /**
     * New pane for configuring BLASTPlus node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected BLASTPlusNodeDialog() {
        super();
        
        createNewGroup("Which sequences do you want to BLAST?");
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString(BLASTPlusNodeModel.CFGKEY_QUERY_DATABASE, "c:/temp/query.fa"),
        		"blast-database", 0, ".fasta|.fa"));
       
        // hook for common widgets to all blast nodes
        init();
    }
  
}

