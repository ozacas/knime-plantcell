package au.edu.unimelb.plantcell.io.read.mascot;

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
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;


/**
 * <code>NodeDialog</code> for the "MascotReader" Node.
 * Using the MascotDatFile open-source java library, this node provides an interface to that, to provide convenient access to MatrixScience Mascot datasets
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class MascotReaderNodeDialog extends DefaultNodeSettingsPane {
	private static SettingsModelString f_resulttype = MascotReaderNodeModel.make_as_string(MascotReaderNodeModel.CFGKEY_RESULTTYPE);
	private static SettingsModelNumber f_ci = (SettingsModelNumber)MascotReaderNodeModel.make(MascotReaderNodeModel.CFGKEY_CONFIDENCE);
	private final SettingsModelStringArray file_list = new SettingsModelStringArray(MascotReaderNodeModel.CFGKEY_FILES, new String[] { "c:/temp/F1082140.dat" });

	
	public static void set_controls() {
		f_ci.setEnabled(f_resulttype.getStringValue().trim().toLowerCase().startsWith("confident"));
	}
	
    /**
     * New pane for configuring MascotReader node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected MascotReaderNodeDialog() {
        super();
        
        set_controls();
       
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
        
        final JPanel file_panel = new JPanel();
        file_panel.setLayout(new BorderLayout());
       
        file_panel.add(new JScrollPane(flist), BorderLayout.CENTER);
        final JPanel button_panel = new JPanel();
        button_panel.setLayout(new GridLayout(2, 1));
        final JButton add_button = new JButton("Add Mascot DAT files...");
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
						if (fname.endsWith(".dat")) {
							return true;
						}
						return false;
					}

					@Override
					public String getDescription() {
						return "Mascot DAT files";
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
				List<String>       sel_files = flist.getSelectedValuesList();
				HashSet<String> sel_set = new HashSet<String>();
				
				for (String f : sel_files) {
					sel_set.add(f);
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
        file_panel.add(button_panel, BorderLayout.EAST);
        
        this.addTab("Mascot DAT files", file_panel);
        addMascotProcessingSettings();
       
    }
    
    protected void addMascotProcessingSettings() {
    	 DialogComponentButtonGroup bg = new DialogComponentButtonGroup(f_resulttype, true, "Report which peptide hits per query?", 
         		MascotReaderNodeModel.RESULT_TYPES);
         bg.setToolTipText("Which peptide identifications per spectra do you want to see?");
         addDialogComponent(bg);
         f_resulttype.addChangeListener(new ChangeListener() {
         	public void stateChanged(ChangeEvent ce) {
         		set_controls();
         	}
         });
         
         addDialogComponent(new DialogComponentNumberEdit(f_ci,"Identity Threshold Confidence", 5));
         
         addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(MascotReaderNodeModel.CFGKEY_WANT_SPECTRA, true), "Want MS/MS spectra?"));
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

