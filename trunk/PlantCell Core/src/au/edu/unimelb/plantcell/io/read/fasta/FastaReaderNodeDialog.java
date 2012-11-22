package au.edu.unimelb.plantcell.io.read.fasta;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.*;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileFilter;

/**
 * <code>NodeDialog</code> for the "FastaReader" Node.
 * This nodes reads sequences from the user-specified FASTA file and outputs three columns per sequence: * n1) Accession * n2) Description - often not accurate in practice * n3) Sequence data * n * nNo line breaks are preserved.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class FastaReaderNodeDialog extends DefaultNodeSettingsPane {

	private final SettingsModelStringArray file_list = new SettingsModelStringArray(FastaReaderNodeModel.CFGKEY_FASTA, new String[] { "c:/temp/crap.fasta" });
    private final JComboBox seqtype_list = new JComboBox(au.edu.unimelb.plantcell.core.cells.SequenceType.getSeqTypes());

    /**
     * Establish the configurable parameters associated with reading the FASTA file. Note how we can
     * tailor the regular expressions to match the description line as we see fit. If any fail to match,
     * no sequence will be output - so you can use this to select just sequences of interest.
     */
    protected FastaReaderNodeDialog() {
        super();
        final JList flist = new JList(file_list.getStringArrayValue());
        file_list.addChangeListener( new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				final String[] cur_files = file_list.getStringArrayValue();
				flist.setModel(new ListModel() {
					private ArrayList<ListDataListener> m_l = new ArrayList<ListDataListener>();
					
					@Override
					public void addListDataListener(ListDataListener l) {
						m_l.add(l);
					}

					@Override
					public Object getElementAt(int index) {
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
        
        final JPanel fasta_file_panel = new JPanel();
        fasta_file_panel.setLayout(new BorderLayout());
       
        final JPanel proc_panel = new JPanel();
        proc_panel.setBorder(BorderFactory.createTitledBorder("Processing options"));
        proc_panel.setLayout(new BoxLayout(proc_panel, BoxLayout.Y_AXIS));
        final JPanel seqtype_panel = new JPanel();
        seqtype_panel.setLayout(new BoxLayout(seqtype_panel, BoxLayout.X_AXIS));
        seqtype_panel.add(new JLabel("Sequences consist of... "));
        seqtype_panel.add(seqtype_list);
        proc_panel.add(seqtype_panel);
        fasta_file_panel.add(proc_panel, BorderLayout.NORTH);
        fasta_file_panel.add(flist, BorderLayout.CENTER);
        final JPanel button_panel = new JPanel();
        button_panel.setLayout(new GridLayout(2, 1));
        final JButton add_button = new JButton("Add FASTA files...");
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
						if (fname.endsWith(".fa") || fname.endsWith(".fasta") || 
								fname.endsWith(".fasta.gz") || fname.endsWith(".fa.gz")) {
							return true;
						}
						return false;
					}

					@Override
					public String getDescription() {
						return "FASTA files";
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
				Object[]       sel_files = flist.getSelectedValues();
				HashSet<String> sel_set = new HashSet<String>();
				
				for (Object f : sel_files) {
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
        fasta_file_panel.add(button_panel, BorderLayout.EAST);
        
        setDefaultTabTitle("Advanced");
        addDialogComponent(new DialogComponentString((SettingsModelString) FastaReaderNodeModel.make(FastaReaderNodeModel.CFGKEY_ACCSN_RE), 
        		"Accession Regular Expression:"));
        addDialogComponent(new DialogComponentString((SettingsModelString) FastaReaderNodeModel.make(FastaReaderNodeModel.CFGKEY_DESCR_RE), 
        		"Description Regular Expression:"));
        
        String labels[] = new String[] {"First entry only", "All entries (as collection)"};
        String actions[]= new String[] {"single", "collection"};
        addDialogComponent(new DialogComponentButtonGroup((SettingsModelString) FastaReaderNodeModel.make(FastaReaderNodeModel.CFGKEY_ENTRY_HANDLER), "Entry Handler", false, labels, actions));
      
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(FastaReaderNodeModel.CFGKEY_MAKESTATS, 
        		Boolean.FALSE), "Compute stats for sequences (slow & memory intensive)?"));
        
        addDialogComponent(new DialogComponentBoolean(new SettingsModelBoolean(FastaReaderNodeModel.CFGKEY_USE_ACCSN_AS_ROWID, 
        		Boolean.TRUE), "Use accession as RowID?"));
        addTabAt(0, "FASTA Files", fasta_file_panel);
        selectTab("FASTA Files");
    }
    
    @Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
    	try {
    		file_list.loadSettingsFrom(settings);
    		if (settings.containsKey(FastaReaderNodeModel.CFGKEY_SEQTYPE)) {
    			seqtype_list.setSelectedItem(settings.getString(FastaReaderNodeModel.CFGKEY_SEQTYPE));
    		}
    	} catch (InvalidSettingsException ex) {
    		ex.printStackTrace();
    	}
    }
    
    @Override
    public void saveAdditionalSettingsTo(NodeSettingsWO settings) {
    	file_list.saveSettingsTo(settings);
    	Object sel = seqtype_list.getSelectedItem();
    	if (sel != null) {
    		settings.addString(FastaReaderNodeModel.CFGKEY_SEQTYPE, sel.toString());
    	}
    }
}
