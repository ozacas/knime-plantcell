package au.edu.unimelb.plantcell.io.read.spectra;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileFilter;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * Dialog for the spectra reader. 
 * 
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class SpectraReaderNodeDialog extends DefaultNodeSettingsPane {

	private final SettingsModelStringArray file_list = new SettingsModelStringArray(SpectraReaderNodeModel.CFGKEY_FILES, new String[] { "c:/temp/crap.fasta" });

    /**
     * New pane for configuring SpectraReader node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
	protected SpectraReaderNodeDialog() {
        super();
        
        final JList<String> flist = new JList<String>(file_list.getStringArrayValue());
        file_list.addChangeListener( new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				final String[] cur_files = file_list.getStringArrayValue();
				flist.setModel(new ListModel<String>() {
					private ArrayList<ListDataListener> m_l = new ArrayList<ListDataListener>();
					
					@Override
					public void addListDataListener(ListDataListener l) {
						m_l.add(l);
					}

					@Override
					public String getElementAt(int index) {
						return cur_files[index];
					}

					@Override
					public int getSize() {
						return cur_files.length;
					}

					@Override
					public void removeListDataListener(ListDataListener l) {
						m_l.remove(l);
					}
					
				});
			}
        	
        });
        
        final JPanel spectra_file_panel = new JPanel();
        spectra_file_panel.setLayout(new BorderLayout());
       
        spectra_file_panel.add(new JScrollPane(flist), BorderLayout.CENTER);
        final JPanel button_panel = new JPanel();
        button_panel.setLayout(new GridLayout(2, 1));
        final JButton add_button = new JButton("Add Spectra files...");
        final JButton remove_button = new JButton("Remove Selected");
        add_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser open_dialog = new JFileChooser();
				open_dialog.setMultiSelectionEnabled(true);
				FileFilter filter = new FileFilter() {

					@Override
					public boolean accept(File arg0) {
						if (arg0.isDirectory())
							return true;
						String fname = arg0.getName().toLowerCase();
						
						if (fname.endsWith(".xml") || fname.endsWith(".mzxml") || fname.endsWith(".mzml") ||
								fname.endsWith(".mgf") || fname.endsWith(".mgf.gz") || fname.endsWith(".dta") || 
								fname.endsWith(".dta.gz")) {
							return true;
						}
						return false;
					}

					@Override
					public String getDescription() {
						return "Spectra files";
					}
				};
				
			    open_dialog.setFileFilter(filter);
				int ret = open_dialog.showOpenDialog(null);
				if (ret == JFileChooser.APPROVE_OPTION) {
					HashSet<String> files = new HashSet<String>();
					for (String f : file_list.getStringArrayValue()) {
						files.add(f);
					}
					File[] new_files = open_dialog.getSelectedFiles();
					for (File f : new_files) {
						files.add(f.getAbsolutePath());
					}
					file_list.setStringArrayValue(files.toArray(new String[0]));
				}
			}
        	
        });
        
        remove_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				List<String> sel_files = flist.getSelectedValuesList();
				HashSet<String> sel_set = new HashSet<String>();
				
				for (String f : sel_files) {
					sel_set.add(f.toString());
				}
			
				HashSet<String> new_files = new HashSet<String>();
				for (String o : file_list.getStringArrayValue()) {
					if (!sel_set.contains(o)) {
						new_files.add(o.toString());
					}
				}
				
				file_list.setStringArrayValue(new_files.toArray(new String[0]));
			}
        	
        });
        
        button_panel.add(add_button);
        button_panel.add(remove_button);
        spectra_file_panel.add(button_panel, BorderLayout.EAST);
        
        this.addTab("Spectra files", spectra_file_panel);
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(SpectraReaderNodeModel.CFGKEY_LOAD_SPECTRA, false), "Load spectra into output (NB: slower)?"));
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(SpectraReaderNodeModel.CFGKEY_LOAD_CHROMATOGRAM, false), "View chromatograms (slow, mzML only)?"));
        
        createNewGroup("File Formats to load");
        setHorizontalPlacement(true);
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(SpectraReaderNodeModel.CFGKEY_MZML, true), "mzML"));
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(SpectraReaderNodeModel.CFGKEY_MZXML, true), "mzXML"));
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(SpectraReaderNodeModel.CFGKEY_MGF, true), "Mascot Generic Format (MGF)")); 
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(SpectraReaderNodeModel.CFGKEY_DTA, true), "Sequest DTA Format"
        		));
        
    }
    
    
    
    @Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
    	try {
    		file_list.loadSettingsFrom(settings);
    	} catch (InvalidSettingsException ex) {
    		ex.printStackTrace();
    	}
    }
    
    @Override
    public void saveAdditionalSettingsTo(NodeSettingsWO settings) {
    	file_list.saveSettingsTo(settings);
    }
}

