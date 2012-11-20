package au.edu.unimelb.plantcell.algorithms.orthology;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "OrthologueFinder" Node.
 * Given two blast results (sequence A against database B and sequence B against database A) this node computes orthologous genes using Reciprocal Blast Hit (RBH) and other algorithms
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class OrthologueFinderNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring OrthologueFinder node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected OrthologueFinderNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentFileChooser(new SettingsModelString(OrthologueFinderNodeModel.CFGKEY_RESULTS_DB1, ""), "db1-history", ".csv|.tsv|.txt"));
        addDialogComponent(new DialogComponentFileChooser(new SettingsModelString(OrthologueFinderNodeModel.CFGKEY_RESULTS_DB2, ""), "db2-history", ".csv|.tsv|.txt"));

        addDialogComponent(new DialogComponentString(new SettingsModelString(OrthologueFinderNodeModel.CFGKEY_EPSILON, "1e-50"), "Epsilon"));            
    }
}

