package au.edu.unimelb.plantcell.hhsuite;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

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
    protected HHSuiteNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentString(
        		new SettingsModelString(HHSuiteNodeModel.CFGKEY_ALIGNMENT, ""), "Alignment column to search"));
        addDialogComponent(new DialogComponentString(
        		new SettingsModelString(HHSuiteNodeModel.CFGKEY_SEARCH_DATABASE, ""), "Search database (HHM file format)"));   
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelIntegerBounded(HHSuiteNodeModel.CFGKEY_CPU_CORES, 2, 1, 16), "Number of CPU cores to use", 1));
    }
}

