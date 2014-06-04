package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * <code>NodeDialog</code> for the "EmbossPredictor" Node.
 * Runs EMBOSS tools which take sequence(s) as input and provide a GFF output for inclusion as a annotation track on the output sequences.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class EmbossSequenceNodeDialog extends StandardEmbossDialog {
	/**
	 * Sequencers are those emboss programs which produce a new sequence as output eg. frame conversion
	 */
	private final List<ACDApplication> sequencers = new ArrayList<ACDApplication>();

    /**
     * New pane for configuring EmbossPredictor node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected EmbossSequenceNodeDialog() {
        super();
        
        refreshSequencersIfNeeded();
        List<String> progs = new ArrayList<String>();
        for (ACDApplication prog : sequencers) {
        	progs.add(prog.getOneLineSummary());
        }
        
      
        final SettingsModelString s_prog = new SettingsModelString(EmbossPlotNodeModel.CFGKEY_PROGRAM, "tmap:");
        addDialogComponent(new DialogComponentStringSelection(
        			s_prog, "EMBOSS Program to run",
        			progs
        		));
        
        final StandardEmbossDialog dlg = this;
        s_prog.addChangeListener(new ChangeListener() {
        	
			@Override
			public void stateChanged(ChangeEvent arg0) {
				for (ACDApplication prog : sequencers) {
					if (prog.getOneLineSummary().equals(s_prog.getStringValue())) {
						String[] current_fields = prog.getFields(true);	// exclude non-IO fields
						
						dlg.changeAdvancedTab(prog, current_fields);
					}
				}
			}
        	
        });

        addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(EmbossPredictorNodeModel.CFGKEY_SEQUENCE, "Annotated Sequence"),
        		"Sequences to process", 0, SequenceValue.class
        		));
       
        addAdvancedTab();
    }

	private void refreshSequencersIfNeeded() {
	     if (sequencers.size() == 0) {
	    	 try {
	        	sequencers.addAll(EmbossPredictorNodeModel.getEmbossPrograms(new EmbossProgramSelector() {

					@Override
					public boolean accept(ACDApplication appl) {
						boolean ret = (appl.hasSection("input") && 
			        			appl.getSection("input").hasUnalignedSequenceInput() && 
			        			appl.hasSequenceOutput());
						return ret;
					}
	        		
	        	}));
	    	 } catch (IOException ioe) {
	    		 sequencers.clear();
	    		 ioe.printStackTrace();
	    	 }
	     }
	}
    
   
}

