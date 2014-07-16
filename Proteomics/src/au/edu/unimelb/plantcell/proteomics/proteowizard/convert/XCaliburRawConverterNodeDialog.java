package au.edu.unimelb.plantcell.proteomics.proteowizard.convert;

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
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.proteomics.proteowizard.filter.MSLevelsFilterNodeDialog;
import au.edu.unimelb.plantcell.proteomics.proteowizard.filter.MSLevelsFilterNodeModel;

/**
 * <code>NodeDialog</code> for the "XCaliburRawConverter" Node.
 * Converts XCalibur(tm) Raw files to open formats: mzML, mzXML or MGF using msconvert invoked via a SOAP webservice
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class XCaliburRawConverterNodeDialog extends MSLevelsFilterNodeDialog {
	private final SettingsModelStringArray file_list = new SettingsModelStringArray(XCaliburRawConverterNodeModel.CFGKEY_RAWFILES, new String[] { });

    /**
     * New pane for configuring the XCaliburRawConverter node.
     */
    protected XCaliburRawConverterNodeDialog() {
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
           
           final JPanel raw_file_panel = new JPanel();
           raw_file_panel.setLayout(new BorderLayout());
          
           raw_file_panel.add(new JScrollPane(flist), BorderLayout.CENTER);
           final JPanel button_panel = new JPanel();
           button_panel.setLayout(new GridLayout(2, 1));
           final JButton add_button = new JButton("Add files...");
           final JButton remove_button = new JButton("Remove Selected");
           add_button.addActionListener(new ActionListener() {

   			@Override
   			public void actionPerformed(ActionEvent arg0) {
   				JFileChooser open_dialog = new JFileChooser();
   				open_dialog.setMultiSelectionEnabled(true);
   				
   			    open_dialog.setFileFilter(getFileFilter());
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
   				sel_set.addAll(sel_files);
   				
   				HashSet<String> new_files = new HashSet<String>();
   				for (String o : file_list.getStringArrayValue()) {
   					if (!sel_set.contains(o)) {
   						new_files.add(o);
   					}
   				}
   				
   				file_list.setStringArrayValue(new_files.toArray(new String[0]));
   			}
           	
           });
           
           button_panel.add(add_button);
           button_panel.add(remove_button);
           raw_file_panel.add(button_panel, BorderLayout.EAST);
           
           addTabAt(0, getPrintableFilename(), raw_file_panel);
           selectTab(getPrintableFilename());
    }
    
    /**
     * Used in the dialog to give clues to the user as to what input is required. Must be human readable.
     * Subclasses should override for their files.
     * 
     * @return
     */
    protected String getPrintableFilename() {
    	return "XCalibur RAW files";
    }
    
    /**
     * Return those files which are suitable for selection during execute().
     * 
     * @return an instance which will display only those files suitable for the dialog. Must not be null
     */
    protected FileFilter getFileFilter() {
    	return new FileFilter() {

				@Override
				public boolean accept(File arg0) {
					if (arg0.isDirectory())
						return true;
					String fname = arg0.getName().toLowerCase();
					if (fname.endsWith(".raw")) {
						return true;
					}
					return false;
				}

				@Override
				public String getDescription() {
					return getPrintableFilename();
				}
			};
    }
    
    @Override
    public void addInputDataSources() {
    	// NO-OP since this node doesnt support reading list of files from input port
    }
    
    @Override
    public void addFilterSettings() {
    	// NO-OP since this node does not support filtering of converted data
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
    	settings.addStringArray(MSLevelsFilterNodeModel.CFGKEY_ACCEPTED_MSLEVELS, new String[]{});
    }
}

