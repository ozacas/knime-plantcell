package au.edu.unimelb.plantcell.views.ms;

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
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * <code>NodeDialog</code> for the "MassSpecSurface" Node.
 * Presents a surface representing RT, m/z and heatmap (eg. identified MS2 spectra) using the sexy jzy3d library. Useful for QA and assessment of mzML-format datasets.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MassSpecSurfaceNodeDialog extends DefaultNodeSettingsPane {
	private final SettingsModelStringArray file_list = new SettingsModelStringArray(MassSpecSurfaceNodeModel.CFGKEY_FILES, new String[] {});
	
    /**
     * New pane for configuring the MassSpecSurface node.
     */
    protected MassSpecSurfaceNodeDialog() {
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
         final JButton add_button = new JButton("Add mzML files...");
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
 						
 						if (fname.endsWith(".mzml")) {
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
         
         this.addTab("mzML files", spectra_file_panel);
         
         createNewGroup("Retention time (seconds)");
         addDialogComponent(new DialogComponentNumber(
        		 new SettingsModelDouble(MassSpecSurfaceNodeModel.CFGKEY_RT_MIN, 300.0), 
        		 "Minimum", 60.0));
         addDialogComponent(new DialogComponentNumber(
        		 new SettingsModelDouble(MassSpecSurfaceNodeModel.CFGKEY_RT_MAX, 3000.0), 
        		 "Maximum", 60.0));
         
         createNewGroup("M/Z window");
         addDialogComponent(new DialogComponentNumber(
        		 new SettingsModelDouble(MassSpecSurfaceNodeModel.CFGKEY_MZ_MIN, 100.0), 
        		 "Minimum", 100.0));
         addDialogComponent(new DialogComponentNumber(
        		 new SettingsModelDouble(MassSpecSurfaceNodeModel.CFGKEY_MZ_MAX, 2000.0), 
        		 "Maximum", 100.0));
         
         createNewGroup("Peak filter");
         addDialogComponent(new DialogComponentButtonGroup(
        		 new SettingsModelString(MassSpecSurfaceNodeModel.CFGKEY_THRESHOLD_METHOD, MassSpecSurfaceNodeModel.THRESHOLD_METHODS[0]), 
        		 false, "Filter peaks which are not...", MassSpecSurfaceNodeModel.THRESHOLD_METHODS));
         addDialogComponent(new DialogComponentNumber(
        		 new SettingsModelDouble(MassSpecSurfaceNodeModel.CFGKEY_THRESHOLD, 0.1), "Threshold value", 1.0
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

