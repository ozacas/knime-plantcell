package au.edu.unimelb.plantcell.hhsuite;

import javax.swing.JFileChooser;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue;

/**
 * <code>NodeDialog</code> for the "HHSuite" Node.
 * Provides remote homology detection (much more sensitive than BLAST) with most of the performance. 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class HHSuiteNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring HHSuite node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected HHSuiteNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(HHSuiteNodeModel.CFGKEY_ALIGNMENT, ""), "Alignment column to search", 0, true, AlignmentValue.class));
        
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelIntegerBounded(HHSuiteNodeModel.CFGKEY_CPU_CORES, 2, 1, 16), "Number of CPU cores to use", 1));
        
        final SettingsModelString db_type = new SettingsModelString(HHSuiteNodeModel.CFGKEY_DB_TYPE, HHSuiteNodeModel.DATABASE_ITEMS[0]);
        addDialogComponent(new DialogComponentButtonGroup(db_type, "Select the database", true, HHSuiteNodeModel.DATABASE_ITEMS, HHSuiteNodeModel.DATABASE_ITEMS));
        
        addDialogComponent(new DialogComponentFileChooser(new SettingsModelString(HHSuiteNodeModel.CFGKEY_DB_NEW, ""), 
        		"hhsearch-database-new", JFileChooser.OPEN_DIALOG, true));
        
        addDialogComponent(new DialogComponentString(
        		new SettingsModelString(HHSuiteNodeModel.CFGKEY_DB_EXISTING, ""), "Existing search database (HHM file format)"));
     
    }
}

