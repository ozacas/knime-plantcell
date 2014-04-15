package au.edu.unimelb.plantcell.io.read.alignments;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButton;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Supports reading alignment files/URLs into the KNIME PlantCell platform
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AlignmentReaderNodeDialog extends DefaultNodeSettingsPane {

    protected AlignmentReaderNodeDialog() {
    	this.createNewGroup("How to fetch and interpret the alignment... ");
    	this.setHorizontalPlacement(true);
    	final SettingsModelString url = new SettingsModelString(AlignmentReaderNodeModel.CFGKEY_URL, "");
    	addDialogComponent(new DialogComponentString(url,
    			"Read from..."));
    	DialogComponentButton set_file_button = new DialogComponentButton("File...");
    	DialogComponentButton set_url_button  = new DialogComponentButton("URL...");
    	
    	set_file_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fc = new JFileChooser();
				FileNameExtensionFilter filter = new FileNameExtensionFilter(
				        "Alignment files", "aln", "clustal", "muscle", "clustalw", "phylip", "fasta", "fa", "fsa", "msa");
				fc.setFileFilter(filter);
				int returnVal = fc.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
				    try {
						url.setStringValue(fc.getSelectedFile().toURI().toURL().toString());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}
				}
			}
    		
    	});
    	addDialogComponent(set_file_button);
    	set_url_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String ret = JOptionPane.showInputDialog("Enter the URL here:");
				if (ret != null) {
					url.setStringValue(ret);
				}
			}
    		
    	});
    	addDialogComponent(set_url_button);
    	this.setHorizontalPlacement(false);

    	addDialogComponent(new DialogComponentStringSelection(
    			new SettingsModelString(AlignmentReaderNodeModel.CFGKEY_FORMAT, ""), 
    			"Interpret data as (file format)... ", AlignmentReaderNodeModel.FILE_FORMATS
    	));
    	this.closeCurrentGroup();
   
    	addDialogComponent(new DialogComponentButtonGroup(
    			new SettingsModelString(AlignmentReaderNodeModel.CFGKEY_TYPE, AlignmentReaderNodeModel.SEQTYPES[0]), 
    			"Amino acids or nucleotides?", 
    			false, AlignmentReaderNodeModel.SEQTYPES, AlignmentReaderNodeModel.SEQTYPES
    	));
    	
    	
    }
}

