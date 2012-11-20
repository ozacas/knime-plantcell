package au.edu.unimelb.plantcell.blast.rps;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.blast.AbstractNodeDialog;
import au.edu.unimelb.plantcell.blast.BLASTPlusNodeModel;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * <code>NodeDialog</code> for the "RPS BLAST" Node.
 * Supports local execution of NCBI BLAST+ executables (which must be  installed separately)
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/
 */
public class RpsBlastNodeDialog extends AbstractNodeDialog {

    /**
     * New pane for configuring BLASTPlus node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
	protected RpsBlastNodeDialog() {
        super();
        
        createNewGroup("Which sequences do you want to BLAST?");
        
        addDialogComponent(new DialogComponentColumnNameSelection(
        		new SettingsModelString(RpsBlastNodeModel.CFGKEY_QUERY_DATABASE, "Biological Sequence"),
        		"Query Sequence", 0, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return (colSpec.getType().isCompatible(SequenceValue.class));
					}

					@Override
					public String allFilteredMsg() {
						return "No suitable sequence columns available!";
					}
        			
        		}));
       
        // here we initialise rps_variant once the listener is setup (so that the strand button group is correctly initialised)
        final SettingsModelString rps_variant = new SettingsModelString(RpsBlastNodeModel.CFGKEY_RPS_VARIANT, "");
        final SettingsModelString strand = new SettingsModelString(RpsBlastNodeModel.CFGKEY_TBLASTN_STRAND, "both");
        
        rps_variant.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				strand.setEnabled(rps_variant.getStringValue().equals("rpstblastn"));
			}
        	
        });
        
        addDialogComponent(new DialogComponentButtonGroup(rps_variant, false, "rpsblast or rpstblastn?", new String[] { "rpsblast", "rpstblastn" } ));
        addDialogComponent(new DialogComponentButtonGroup(strand, false, "Search which strand (rpstblastn only)?", new String[] { "both", "plus", "minus"}));
        
        // dont show the program menu item since it is always rpsblast
        init(false, false, ".loo");
    }
    
    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) {
    	// need to add these to the settings to avoid a KNIME exception, not used by this node
    	settings.addString(BLASTPlusNodeModel.CFGKEY_BLAST_PROG, "rpsblast");
    	settings.addString(BLASTPlusNodeModel.CFGKEY_MATRIX, "BLOSUM62");
    }
    
    
}

