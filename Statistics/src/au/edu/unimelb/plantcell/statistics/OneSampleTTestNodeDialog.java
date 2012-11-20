package au.edu.unimelb.plantcell.statistics;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * <code>NodeDialog</code> for the "Paired T-Test" Node.
 * Basic descriptive and inferential statistics support built using the apache commons math v3 library.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class OneSampleTTestNodeDialog extends DefaultNodeSettingsPane {

    @SuppressWarnings({ "unchecked" })
	protected OneSampleTTestNodeDialog() {
        super();
         
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDoubleBounded(OneSampleTTestNodeModel.CFGKEY_ALPHA, 0.05, 0, 1.0),
        		"Alpha (significance level)", 0.1
        		));
        
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDouble(OneSampleTTestNodeModel.CFGKEY_MU, 1.0d),
        		"mu (mean)", 1.0d));
        
        addDialogComponent(new DialogComponentColumnFilter(
        		new SettingsModelFilterString(OneSampleTTestNodeModel.CFGKEY_COLUMN_CLASSES), 0, true, DoubleValue.class
        		));
        
        addDialogComponent(new DialogComponentButtonGroup(
        		new SettingsModelString(OneSampleTTestNodeModel.CFGKEY_FROM, "columns"),
        		true, "Sample data from... ", new String[] { "columns" }
        		));
        
    }
}

