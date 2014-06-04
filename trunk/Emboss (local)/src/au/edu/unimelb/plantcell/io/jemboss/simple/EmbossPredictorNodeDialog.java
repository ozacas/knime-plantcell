package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;

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
public class EmbossPredictorNodeDialog extends StandardEmbossDialog {
	/**
	 * Predictors are those emboss programs which produce GFF as output ie. feature recognition
	 */
	private final List<ACDApplication> predictors = new ArrayList<ACDApplication>();
	
    @SuppressWarnings("unchecked")
	protected EmbossPredictorNodeDialog() {
        super();
        
        refreshPredictorsIfNeeded();
        List<String> progs = new ArrayList<String>();
        for (ACDApplication prog : predictors) {
        	progs.add(prog.getOneLineSummary());
        }
        final SettingsModelString s_prog = new SettingsModelString(EmbossPredictorNodeModel.CFGKEY_PROGRAM, predictors.get(0).getOneLineSummary());
        addDialogComponent(new DialogComponentStringSelection(
        			s_prog, "EMBOSS Program to run",
        			progs
        		));
        s_prog.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				String descr = s_prog.getStringValue();
				int idx = descr.indexOf(":");
				if (idx < 0) 
					return;
				String name = descr.substring(0, idx);
				for (ACDApplication prog : predictors) {
					if (prog.getName().equals(name)) {
						String[] fields = prog.getFields(true);	// true: exclude first input & output
						
						changeAdvancedTab(prog, fields);
						return;
					}
				}
			}
        	
        });

        addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(EmbossPredictorNodeModel.CFGKEY_SEQUENCE, "Annotated Sequence"),
        		"Sequences to process", 0, SequenceValue.class
        		));
       
        addAdvancedTab();
    }

	private void refreshPredictorsIfNeeded() {
		 if (predictors.size() == 0) {
	        	try {
		        	predictors.addAll(EmbossPredictorNodeModel.getEmbossPrograms(new EmbossProgramSelector() {
		
						@Override
						public boolean accept(ACDApplication appl) {
							return (appl.hasSection("input") && 
				        			appl.getSection("input").hasUnalignedSequenceInput() && 
				        			appl.hasGFFoutput());
						}
		        		
		        	}));
	        	} catch (IOException ioe) {
	        		ioe.printStackTrace();
	        		predictors.clear();
	        	}
		 }
	}
}

