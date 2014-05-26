package au.edu.unimelb.plantcell.io.read.fasta;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

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
	private final JComboBox<String>     seqtype_list = new JComboBox<String>(au.edu.unimelb.plantcell.core.cells.SequenceType.getSeqTypes());
	
	/**
	 * this cannot be generic since backward compatibility requires file support as well and URL's...
	 */
	@SuppressWarnings("rawtypes")
	final JList url_list = new JList();

    /**
     * Establish the configurable parameters associated with reading the FASTA file. Note how we can
     * tailor the regular expressions to match the description line as we see fit. If any fail to match,
     * no sequence will be output - so you can use this to select just sequences of interest.
     */
	protected FastaReaderNodeDialog() {
        super();
    	
        JPanel fasta_url_panel = addFastaFileList();
        addAdvancedSettings();
        if (fasta_url_panel != null)
        	addTabAt(0, "FASTA Files", fasta_url_panel);
        selectTab("FASTA Files");
        
    }
 
    @SuppressWarnings("unchecked")
    protected JPanel addFastaFileList() {
    	 SettingsModelStringArray store = new SettingsModelStringArray(FastaReaderNodeModel.CFGKEY_FASTA, new String[] { "c:/temp/crap.fasta" });
         url_list.setModel(new URLListModel(store));
         
         /**
          * Remove 'file:' from local files in this to present the data for backward compatibility. Non-file URLs
          * will be presented with the protocol scheme as per user expectations.
          */
         url_list.setCellRenderer(new DefaultListCellRenderer() {

 			/**
 			 * 
 			 */
 			private static final long serialVersionUID = -6379690728958799339L;

 			@SuppressWarnings("rawtypes")
 			@Override
 			public Component getListCellRendererComponent(JList l,
 					Object val, int idx, boolean arg3, boolean arg4) {
 				if (val instanceof URL) {
 					String text = FastaReaderNodeModel.shortenURLForDisplay((URL)val);

 					return super.getListCellRendererComponent(l, text, idx, arg3, arg4);
 				}
 				return (val != null) ? new JLabel(val.toString()) : new JLabel();
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
         fasta_file_panel.add(new JScrollPane(url_list), BorderLayout.CENTER);
         final JPanel button_panel = new JPanel();
         button_panel.setLayout(new GridLayout(5, 1));
         final JButton add_button = new JButton("Add FASTA files...");
         final JButton add_url_button = new JButton("Add FASTA URL...");
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
 					addURLIfNotAlreadyPresent(open_dialog);
 				}
 			}
         	
         });
         
         add_url_button.addActionListener(new ActionListener() {

 			@Override
 			public void actionPerformed(ActionEvent e) {
 				String default_url = "http://www.uniprot.org/uniprot/B5X0I6.fasta";
 				String ret = (String) JOptionPane.showInputDialog(null, "What URL to load?", "Add FASTA URL...", 
 						JOptionPane.QUESTION_MESSAGE, null, null, default_url);
 				if (ret != null && ret.length() > 0) {
 					addURLIfNotAlreadyPresent(ret);
 				}
 			}
         	
         });
         
         remove_button.addActionListener(new ActionListener() {

 			@Override
 			public void actionPerformed(ActionEvent arg0) {
 				URLListModel mdl = getListModel();
 				List<URL> sel_urls = url_list.getSelectedValuesList();
 				HashSet<URL> sel_set = new HashSet<URL>();
 				sel_set.addAll(sel_urls);
 			
 				HashSet<URL> new_urls = new HashSet<URL>();
 				for (URL o : mdl.getAll()) {
 					if (!sel_set.contains(o)) {
 						new_urls.add(o);
 					}
 				}
 				
 				mdl.setAll(new_urls);
 			}
         	
         });
         
         button_panel.add(add_button);
         button_panel.add(Box.createRigidArea(new Dimension(5,5)));
         button_panel.add(add_url_button);
         button_panel.add(Box.createRigidArea(new Dimension(5,5)));
         button_panel.add(remove_button);
         fasta_file_panel.add(button_panel, BorderLayout.EAST);
         
         return fasta_file_panel;
    }
    
    protected void addAdvancedSettings() {
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
	}

	/**
     * For visual clarity and backward compatibility, remove the junk portion of the textual form of the URL for 
     * user-convenience. This is only done for file: protocol URLs. Nothing is done otherwise.
     * 
     * @param url_in_text_form
     * @return
     */
    protected String trimFileProtocol(final String url_in_text_form) {
		assert(url_in_text_form != null);
		if (!url_in_text_form.startsWith("file:"))
			return url_in_text_form;
		
		String ret = url_in_text_form.substring("file:".length());
		while (ret.startsWith("/") || ret.startsWith("\\")) {
			ret = ret.substring(1);
		}
		return ret;
	}

	/**
     * Convenience wrapper to avoid list model casting all over the codebase
     * @return
     */
    protected URLListModel getListModel() {
    	return (URLListModel) url_list.getModel();
    }
    
    /**
     * Add the selected URL to the current list model. The list model will not be updated
     * (but a stacktrace printed) if any user input is not a valid URL.
     * 
     * @param url_string_to_add
     */
    protected void addURLIfNotAlreadyPresent(final String url_string_to_add) {
		List<URL> urls = getListModel().getAll();
		try {
			urls.add(new URL(url_string_to_add));
			getListModel().setAll(urls);
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		}
	}

    /**
     * Add the selected files from the chosen dialog to the current list model. Errors will
     * cause the list model to be updated.
     * 
     * @param open_dialog
     */
	private void addURLIfNotAlreadyPresent(final JFileChooser open_dialog) {
    	try {
	    	  List<URL> urls = getListModel().getAll();
	        
	          File[] new_files = open_dialog.getSelectedFiles();
	          for (File f : new_files) {
	                  urls.add(f.toURI().toURL());
	          }
	          getListModel().setAll(urls);
    	} catch (MalformedURLException mfe) {
    		mfe.printStackTrace();
    	}

	}
    
    @Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO settings, DataTableSpec[] specs) throws NotConfigurableException {
    	try {
    		URLListModel mdl = getListModel();
    		if (settings.containsKey(FastaReaderNodeModel.CFGKEY_SEQTYPE)) {
    			seqtype_list.setSelectedItem(settings.getString(FastaReaderNodeModel.CFGKEY_SEQTYPE));
    		}
    		
    		mdl.loadSettingsFrom(settings);
    	} catch (InvalidSettingsException ex) {
    		ex.printStackTrace();
    	}
    }
  

	@Override
    public void saveAdditionalSettingsTo(NodeSettingsWO settings) {
		// need to support subclasses for whom getListModel() returns null
		URLListModel url_list = getListModel();
		if (url_list != null) {
			url_list.saveSettingsTo(settings);
		}
    	Object sel = seqtype_list.getSelectedItem();
    	if (sel != null) {
    		settings.addString(FastaReaderNodeModel.CFGKEY_SEQTYPE, sel.toString());
    	}
    }
}
