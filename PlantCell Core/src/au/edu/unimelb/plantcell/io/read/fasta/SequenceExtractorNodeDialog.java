package au.edu.unimelb.plantcell.io.read.fasta;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.*;
import org.knime.core.node.util.ColumnSelectionPanel;

import javax.swing.BoxLayout;
import javax.swing.border.Border;
import javax.swing.JButton;
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
public class SequenceExtractorNodeDialog extends DefaultNodeSettingsPane {
	@SuppressWarnings("unchecked")
	private final ColumnSelectionPanel csp_files = new ColumnSelectionPanel((Border) null, StringValue.class);
	@SuppressWarnings("unchecked")
	private final ColumnSelectionPanel csp_accsn = new ColumnSelectionPanel((Border) null, StringValue.class);
	private final SettingsModelStringArray file_list = new SettingsModelStringArray(FastaReaderNodeModel.CFGKEY_FASTA, new String[] { "c:/temp/crap.fasta" });

    /**
     * Establish the configurable parameters associated with reading the FASTA file. Note how we can
     * tailor the regular expressions to match the description line as we see fit. If any fail to match,
     * no sequence will be output - so you can use this to select just sequences of interest.
     */
	protected SequenceExtractorNodeDialog() {
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
        final JPanel col_panel = new JPanel();
        col_panel.setLayout(new BoxLayout(col_panel, BoxLayout.Y_AXIS));
        JPanel file_panel = new JPanel();
        file_panel.setLayout(new BoxLayout(file_panel, BoxLayout.X_AXIS));
        file_panel.add(new JLabel("FASTA Files from..."));
        csp_files.setRequired(true);
        csp_accsn.setRequired(true);

        file_panel.add(csp_files);
        JPanel accsn_panel = new JPanel();
        accsn_panel.setLayout(new BoxLayout(accsn_panel, BoxLayout.X_AXIS));
        accsn_panel.add(new JLabel("Accessions from..."));
        accsn_panel.add(csp_accsn);
        col_panel.add(file_panel);
        col_panel.add(accsn_panel);
        
        fasta_file_panel.add(col_panel, BorderLayout.NORTH);
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
						return "FASTA files (.fa/.fasta with/without gzip)";
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
        
        addTabAt(0, "FASTA Files", fasta_file_panel);
        selectTab("FASTA Files");
    }
    
    @Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
    	try {
    		file_list.loadSettingsFrom(settings);
    		String cur_file_column = settings.getString(SequenceExtractorNodeModel.CFGKEY_FILE_COLUMN);
    		String cur_accsn_column= settings.getString(SequenceExtractorNodeModel.CFGKEY_ACCSN_COLUMN);
    		csp_files.update(specs[0], cur_file_column);
    		csp_accsn.update(specs[0], cur_accsn_column);
    	} catch (InvalidSettingsException ex) {
    		ex.printStackTrace();
    	}
    }
    
    @Override
    public void saveAdditionalSettingsTo(NodeSettingsWO settings) {
    	file_list.saveSettingsTo(settings);
    	settings.addString(SequenceExtractorNodeModel.CFGKEY_FILE_COLUMN, csp_files.getSelectedColumn());
    	settings.addString(SequenceExtractorNodeModel.CFGKEY_ACCSN_COLUMN, csp_accsn.getSelectedColumn());
    }
}
