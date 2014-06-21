package au.edu.unimelb.plantcell.proteomics.spectra.filtering;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;

/**
 * Spectra correlation node dialog
 * 
 * @author Andrew Cassin
 */
public class SpectraFilteringNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring SpectraQualityAssessor node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected SpectraFilteringNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(SpectraFilteringNodeModel.CFGKEY_SPECTRA, ""),
        		"Spectra column", 0, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return (colSpec != null && colSpec.getType().isCompatible(SpectraValue.class));
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable spectra columns available";
					}
        			
        		}));
        
        final SettingsModelString sms = new SettingsModelString(SpectraFilteringNodeModel.CFGKEY_METHOD, SpectraFilteringNodeModel.METHODS[0]);
        final SettingsModelIntegerBounded keep_n = new SettingsModelIntegerBounded(SpectraFilteringNodeModel.CFGKEY_KEEP_N, 3, 1, 10000000);
        final SettingsModelDouble window_size    = new SettingsModelDouble(SpectraFilteringNodeModel.CFGKEY_WINDOW_SIZE, 50.0);
        final SettingsModelDouble tolerance      = new SettingsModelDouble(SpectraFilteringNodeModel.CFGKEY_TOLERANCE, 0.05);
        
        addDialogComponent(new DialogComponentStringSelection(sms, 
        		"Select a filtering method... ", SpectraFilteringNodeModel.METHODS
        		));
        
        addDialogComponent(new DialogComponentNumber(keep_n, "N (number of peaks)", 1));
        addDialogComponent(new DialogComponentNumber(window_size, "Window size (in units of the spectra)", 10.0d));
        addDialogComponent(new DialogComponentNumber(tolerance, "m/z Tolerance", 0.1));
        
        sms.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				 String meth = sms.getStringValue();
				 tolerance.setEnabled(true);
				 if (meth.startsWith("Keep highest N")) {
			        	keep_n.setEnabled(true);
			        	window_size.setEnabled(true);
			        } else if (meth.startsWith("Top N most intense")) {
			        	keep_n.setEnabled(true);
			        	window_size.setEnabled(false);
			        } else if (meth.startsWith("Centroid peaks")) {
			        	keep_n.setEnabled(false);
			        	window_size.setEnabled(!  meth.endsWith("(adaptive)"));
			        	tolerance.setEnabled(false);
			        } else {
			        	keep_n.setEnabled(false);
			        	window_size.setEnabled(false);
			        }
			}
        	
        });
    }
}

