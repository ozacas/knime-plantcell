package au.edu.unimelb.plantcell.proteomics.peakseparationfilter;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentMultiLineString;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;

/**
 * <code>NodeDialog</code> for the peak separation filter node, which looks for particular
 * peaks a certain mass apart and either accepts or rejects the input rows based on the presence/absence of these peaks
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class SeparationFilterNodeDialog extends DefaultNodeSettingsPane {
	private final static String DEFAULT_DISTANCES =  "47\n147\n63";
	private final SettingsModelString distances = new SettingsModelString(SeparationFilterNodeModel.CFGKEY_MASS_DISTANCES, DEFAULT_DISTANCES );
	
	/**
     * New pane for configuring InsilicoDigestor node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
	@SuppressWarnings("unchecked")
	protected SeparationFilterNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(SeparationFilterNodeModel.CFGKEY_SPECTRA_COLUMN, ""),
        		"Input Spectra", 0, true, false, SpectraValue.class
        ));
        
        createNewGroup("Report spectra which... ");
        addDialogComponent(new DialogComponentButtonGroup(
        		new SettingsModelString(SeparationFilterNodeModel.CFGKEY_LOGIC, SeparationFilterNodeModel.LOGICAL_OPERATIONS[0]), true, null, SeparationFilterNodeModel.LOGICAL_OPERATIONS));
    
        createNewGroup("Tolerance");
        addDialogComponent(new DialogComponentNumber(
        		new SettingsModelDoubleBounded(SeparationFilterNodeModel.CFGKEY_DISTANCE_TOLERANCE, 0.05, 0, 100), "Mass tolerance (symmetric about peaks)", 0.05
        		));
	
        createNewGroup("Mass separations to try to find...");
        DialogComponent dc = new DialogComponentMultiLineString(distances, "(separate multiple by whitespace):");
        addDialogComponent(dc);
	}
}

