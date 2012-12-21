package au.edu.unimelb.plantcell.statistics.correlation;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * <code>NodeDialog</code> for the "LinearCorrelationCalc" Node.
 * Unlike the builtin knime node, this can perform large scale correlations by not storing the results into a matrix. I use it for AffyMetrix 21k gene correlations
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class CorrelationCalcNodeDialog extends DefaultNodeSettingsPane {

    protected CorrelationCalcNodeDialog() {
        super();
        
        DialogComponentStringListSelection dls = 
        	new DialogComponentStringListSelection(
            		new SettingsModelStringArray(CorrelationCalcNodeModel.CFGKEY_METHODS, CorrelationCalcNodeModel.METHODS), "Correlation methods to run", CorrelationCalcNodeModel.METHODS);
        dls.setVisibleRowCount(4);
        addDialogComponent(dls);
        
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDoubleBounded(CorrelationCalcNodeModel.CFGKEY_MAGNITUDE, 0.0, 0.0, 1.0), 
        		"Minimum magnitude (r) to report", 0.1));
        
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(CorrelationCalcNodeModel.CFGKEY_REPORT_VALUES, false), "Report correlated values? (yes if checked)"
        		));
    }
}

