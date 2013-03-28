package au.edu.unimelb.plantcell.proteomics.spectra.clustering;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;

/**
 * Spectra correlation node dialog
 * 
 * @author Andrew Cassin
 */
public class SpectraCorrelationNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring SpectraQualityAssessor node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected SpectraCorrelationNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(SpectraCorrelationNodeModel.CFGKEY_SPECTRA, ""),
        		"Spectra column (topmost port)", 0, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return (colSpec != null && colSpec.getType().isCompatible(SpectraValue.class));
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable spectra columns available (top port!)";
					}
        			
        		}));
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(SpectraCorrelationNodeModel.CFGKEY_SPECTRA2, ""),
        		"Spectra column (bottom port)", 0, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return (colSpec != null && colSpec.getType().isCompatible(SpectraValue.class));
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable spectra columns available (bottom port!)";
					}
        			
        		}));
        
        addDialogComponent(new DialogComponentStringSelection(
        		new SettingsModelString(SpectraCorrelationNodeModel.CFGKEY_ALGORITHM, SpectraCorrelationNodeModel.ALGORITHMS[0]), 
        		"Algorithm (use default if unsure)", SpectraCorrelationNodeModel.ALGORITHMS
        		));
        
        addDialogComponent(new DialogComponentStringSelection(
        		new SettingsModelString(SpectraCorrelationNodeModel.CFGKEY_SCORE_FUNCTION, SpectraCorrelationNodeModel.SCORE_FUNCS[0]), 
        		"Scoring function (use default if unsure)", SpectraCorrelationNodeModel.SCORE_FUNCS
        		));
        
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDouble(SpectraCorrelationNodeModel.CFGKEY_THRESHOLD, 0.8), "Minimum score to report", 0.1
        		));
    }
}

