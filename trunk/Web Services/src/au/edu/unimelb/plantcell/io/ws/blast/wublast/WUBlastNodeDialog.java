package au.edu.unimelb.plantcell.io.ws.blast.wublast;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionPanel;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * <code>NodeDialog</code> for the "WUBlast" Node.
 * Performs a WU-Blast with the chosen parameters using the EBI webservices. Rate controlled so as not to overload EBI computer systems.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class WUBlastNodeDialog extends DefaultNodeSettingsPane {

	// data downloaded from ebi.ac.uk (PERSISTED once downloaded)
	private static String[] m_ebi_programs = new String[] { "blastp" };
	private static String[] m_ebi_databases = new String[] { "UniProt/SwissProt" };
	private static String[] m_ebi_matrices = new String[] { "BLOSUM62" };
	private static String[] m_ebi_sort     = new String[] { "pvalue" };
	private static String[] m_ebi_sensitivity = new String[] { "Normal" };
	private static String[] m_ebi_statistics  = new String[] { "sump" };
	private static String[] m_ebi_filters     = new String[] { "None" };
	private static boolean  m_is_loaded = false;
	
	// persisted state which holds the current setting once the dialog is loaded
	private String m_db, m_matrix, m_filter, m_sort, m_statistics, m_sensitivity, m_programs;
	private String m_sequence, m_db_filter, m_email, m_eval;
	private int m_alignments, m_scores, m_concurrent_blasts;
	private boolean m_image_summary;
	
	// swing widgets on the interface - the entire dialog uses no KNIME dialog code since that wont provide the required flexibility in this case
	private ColumnSelectionPanel d_sequence;
	private JComboBox d_programs, d_matrix, d_eval_cutoff, d_filters, d_sensitivity, d_sort, d_statistics;
	private JTextField d_email, d_db_filter;
	private JList  d_db;
	private JComboBox d_alignments, d_scores;
	private JSpinner d_batch_size;
	private JPanel m_main_panel, m_adv_panel;	// each on separate tabs in the dialog
	private JCheckBox d_image_summary;
	
	// other state -- not persisted
	private DataTableSpec m_incoming_spec;
	private boolean m_ui_initialized;
	
    /**
     * New pane for configuring WUBlast node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected WUBlastNodeDialog() {
        super();
        m_ui_initialized = false;
    }
    
    /**
     * This method does NOT establish the current setting for the widgets, it is just about construction only
     */
    protected void setup_ui() {
    	m_main_panel = new JPanel();
    	m_main_panel.setLayout(new BoxLayout(m_main_panel, BoxLayout.Y_AXIS));
    	m_adv_panel = new JPanel();
    	m_adv_panel.setLayout(new BoxLayout(m_adv_panel, BoxLayout.Y_AXIS));
    	this.addTab("Options", m_main_panel);
    	this.addTab("Advanced", m_adv_panel);
    	
        // sequence column selection
        ColumnFilter string_cols = new ColumnFilter() {

			@Override
			public boolean includeColumn(DataColumnSpec colSpec) {
				return colSpec.getType().isCompatible(SequenceValue.class);
			}

			@Override
			public String allFilteredMsg() {
				return "No suitable string columns for BLAST'ing!";
			}
        };
        this.d_sequence = new ColumnSelectionPanel((Border)null, string_cols, false, false);
        
        add_widget(m_main_panel, "Sequence column to BLAST", d_sequence);
        
        // type of sequence to blast (must be suitable for blast program chosen by user)
        // seqtype is now computed by the model based on the blast program chosen
       // d_seqtype = new JComboBox(new String[] {"protein", "dna"});
       // add_widget(m_main_panel, "Sequence type", d_seqtype);
        
        d_email = new JTextField();
        add_widget(m_main_panel, "Email Address (required by EBI)", d_email);
        
        d_programs = new JComboBox(m_ebi_programs);
        add_widget(m_main_panel, "BLAST Program to use", d_programs);
        
        d_matrix = new JComboBox(m_ebi_matrices);
        add_widget(m_main_panel, "Scoring matrix", d_matrix);
        
        d_eval_cutoff = new JComboBox(new String[] {"1e-200", "1e-100", "1e-50", "1e-10", "1e-5", "1e-4", "1e-3", "1e-2", "1e-1", "1.0", "10", "100", "1000"});
        add_widget(m_main_panel, "E-value cutoff", d_eval_cutoff);
        
        JPanel db_panel = new JPanel();
        db_panel.setBorder(BorderFactory.createTitledBorder("Blast Databases (filter by entering text into the box)"));
        db_panel.setLayout(new BoxLayout(db_panel, BoxLayout.Y_AXIS));
        d_db_filter = new JTextField();
        final JLabel warning_label = new JLabel("");
        warning_label.setForeground(Color.RED);
        add_widget(db_panel, "Filter databases shown:", d_db_filter);
        db_panel.add(warning_label);
        d_db = new JList(make_model(this));
        d_db.setVisibleRowCount(10);
        final WUBlastNodeDialog m_dlg = this;
        d_db_filter.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				d_db.setModel(make_model(m_dlg));
			}
        	
        });
        add_widget(db_panel, "Available EBI databases", new JScrollPane(d_db));
        m_main_panel.add(db_panel);
       
        d_filters = new JComboBox(m_ebi_filters);
        add_widget(m_adv_panel, "Low complexity sequence filtering algorithm", d_filters);
        
        d_sensitivity = new JComboBox(m_ebi_sensitivity);
        add_widget(m_adv_panel, "Sensitivity", d_sensitivity);
        
        d_sort = new JComboBox(m_ebi_sort);
        add_widget(m_adv_panel, "Sort", d_sort);
        
        d_statistics = new JComboBox(m_ebi_statistics);
        add_widget(m_adv_panel, "Statistics", d_statistics);
        
        d_alignments = new JComboBox(new String[] {"5", "10", "20", "50", "100", "150", "200", "500" });
        add_widget(m_adv_panel, "Number of reported alignments (per BLAST)", d_alignments);
        
        d_scores = new JComboBox(new String[] {"5", "10", "20", "50", "100", "150", "200", "500" });
        add_widget(m_adv_panel, "Number of reported scores (per BLAST)", d_scores );
        
        d_batch_size = new JSpinner(new SpinnerNumberModel(10, 1, 25, 5));
        add_widget(m_adv_panel, "EBI Batch Size (# of concurrent blasts)", d_batch_size);
        
        d_image_summary = new JCheckBox("Save image summary?");
        add_widget(m_adv_panel, "", d_image_summary);
        
        m_ui_initialized = true;
    }
    
    private void add_widget(JPanel what_to, String lbl, JComponent c) {
	   JPanel p = new JPanel();
       p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
       p.add(new JLabel(lbl));
       p.add(Box.createHorizontalStrut(10));
       if (c instanceof JScrollPane) {
    	   p.add(c);
    	   p.add(Box.createGlue());
       } else {
    	   p.add(Box.createGlue());
    	   p.add(c);
       }
       what_to.add(p);
       what_to.add(Box.createVerticalStrut(5));
       what_to.add(Box.createVerticalGlue());
	}

    /**
     * returns a {@link JList} compatible model to represent the list of available (and 
     * currently filtered) database models
     */
    public static ListModel make_model(final WUBlastNodeDialog dlg) {
    	String filter = dlg.d_db_filter.getText().toLowerCase().trim();
		ArrayList<String> matches = new ArrayList<String>();

		if (filter.length() >= 3) {		// minimum length requirement before attempting a filter
			for (String s : m_ebi_databases) {
				if (s.toLowerCase().indexOf(filter) >= 0) {
					matches.add(s);
				}
			}
			if (matches.size() > 0) {
				dlg.setWarning("Showing "+matches.size()+"/"+m_ebi_databases.length+" databases.");
				return new EBIDatabaseModel(matches);
			} else {
				dlg.setWarning("No database matches for: "+filter+" showing all databases.");
				return new EBIDatabaseModel(m_ebi_databases);
			}
		} else if (filter.length() == 0) {
			dlg.setWarning(m_ebi_databases.length+" databases shown.");
			return new EBIDatabaseModel(m_ebi_databases);
		} else {
			dlg.setWarning("Filtering requires at least 3 characters, showing all databases");
			return new EBIDatabaseModel(m_ebi_databases);
		}
    }
    
    /**
     * Informs the user of the current status of the databases available
     * @param new_warning_message
     */
    private void setWarning(String new_warning_message) {
    	
    }
    
	/**
     * Fetches the internal dialog state (persisted to the node settings) which is critical to the correct operation
     * of the node: EBI provide an automatically updated list of blast programs, databases and other key parameters. We fetch them and persist
     * them as the user action proceeds. The fetch from EBI is ONLY DONE ONCE per node so the settings are reprodicible. Users
     * can delete the node and re-create to update node configuration if required. Maybe reset would be cleaner?
     * 
     * @throws NotConfigurableException if the network is not available for some reason
     */
    private void fetch_ebi_settings() throws NotConfigurableException {
    	synchronized (m_ebi_programs) {
    		// already loaded the data?
    		if (m_is_loaded) 
    			return;
	    	
	    	Logger.getAnonymousLogger().info("Downloading BLAST settings from EBI... please wait a few moments");
			
			try {
				m_ebi_databases   = WUBlastNodeModel.get_printable_names("database");
				m_ebi_programs    = WUBlastNodeModel.get_printable_names("program");
				m_ebi_filters     = WUBlastNodeModel.get_printable_names("filter");
				m_ebi_matrices    = WUBlastNodeModel.get_printable_names("matrix");
				m_ebi_sensitivity = WUBlastNodeModel.get_printable_names("sensitivity");
				m_ebi_sort        = WUBlastNodeModel.get_printable_names("sort");
				m_ebi_statistics  = WUBlastNodeModel.get_printable_names("stats");
				m_is_loaded       = true;
		    	Logger.getAnonymousLogger().info("Got BLAST settings from EBI.");
			} catch (Exception e) {
				e.printStackTrace();
				throw new NotConfigurableException("Unable to contact EBI: "+e.getMessage()+" check your Internet connection!");
			}
		
    	}
    }
    
    @Override
    public void saveAdditionalSettingsTo(NodeSettingsWO s) {
    	if (m_ui_initialized) {
	    	s.addString(WUBlastNodeModel.CFGKEY_PROGRAMS, d_programs.getSelectedItem().toString());
	    	s.addString(WUBlastNodeModel.CFGKEY_MATRIX, d_matrix.getSelectedItem().toString());
	    	s.addString(WUBlastNodeModel.CFGKEY_FILTER, d_filters.getSelectedItem().toString());
	    	s.addString(WUBlastNodeModel.CFGKEY_SENSITIVITY, d_sensitivity.getSelectedItem().toString());
	    	s.addString(WUBlastNodeModel.CFGKEY_SORT, d_sort.getSelectedItem().toString());
	    	s.addString(WUBlastNodeModel.CFGKEY_STATS, d_statistics.getSelectedItem().toString());
	    	Object sel = d_db.getSelectedValue();
	    	s.addString(WUBlastNodeModel.CFGKEY_DB, sel != null ? sel.toString() : "");
	    	s.addString(WUBlastNodeModel.CFGKEY_SEQUENCE_COL, d_sequence.getSelectedColumn());
	    	s.addString(WUBlastNodeModel.CFGKEY_EMAIL, d_email.getText());
	    	s.addInt(WUBlastNodeModel.CFGKEY_NUM_ALIGNMENTS, new Integer(d_alignments.getSelectedItem().toString()).intValue());
	    	s.addInt(WUBlastNodeModel.CFGKEY_NUM_SCORES, new Integer(d_scores.getSelectedItem().toString()).intValue());
	    	s.addBoolean(WUBlastNodeModel.CFGKEY_SAVE_IMAGE, d_image_summary.isSelected());
	    	s.addString(WUBlastNodeModel.CFGKEY_EVAL_THRESHOLD, d_eval_cutoff.getSelectedItem().toString());
	    	s.addInt(WUBlastNodeModel.CFGKEY_EBI_BATCH_SIZE, ((Integer)d_batch_size.getValue()).intValue());
	    	//s.addString(WUBlastNodeModel.CFGKEY_STYPE, d_seqtype.getSelectedItem().toString());
	    	s.addString(WUBlastNodeModel.CFGKEY_FILTERSTR, d_db_filter.getText());
    	}
    	// by saving this data with the NODE (and not the workflow) we ensure that we dont have to go back to EBI for it
    	s.addBoolean(WUBlastNodeModel.CFGKEY_EBI_LOADED, true);
    	s.addStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_PROGS, m_ebi_programs);
    	s.addStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_DATABASES, m_ebi_databases);
    	s.addStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_FILTERS, m_ebi_filters);
    	s.addStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_MATRICES, m_ebi_matrices);
    	s.addStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_SENSITIVITY, m_ebi_sensitivity);
    	s.addStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_SORT, m_ebi_sort);
    	s.addStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_STATISTICS, m_ebi_statistics);
    }
    
    @Override 
    public void loadAdditionalSettingsFrom(NodeSettingsRO s, DataTableSpec[] specs) 
    				throws NotConfigurableException {
    	if (specs != null && specs.length > 0) {
    		m_incoming_spec = specs[0];
    	} else {
     		throw new NotConfigurableException("No input table columns!");
    	}
    
    	try {
	    	m_programs = s.getString(WUBlastNodeModel.CFGKEY_PROGRAMS);
	    	m_matrix = s.getString(WUBlastNodeModel.CFGKEY_MATRIX);
	    	m_filter = s.getString(WUBlastNodeModel.CFGKEY_FILTER);
	    	m_sensitivity = s.getString(WUBlastNodeModel.CFGKEY_SENSITIVITY);
	    	m_sort = s.getString(WUBlastNodeModel.CFGKEY_SORT);
	    	m_statistics = s.getString(WUBlastNodeModel.CFGKEY_STATS);
	    	m_db = s.getString(WUBlastNodeModel.CFGKEY_DB);
	    	m_sequence = s.getString(WUBlastNodeModel.CFGKEY_SEQUENCE_COL);
	    	m_email = s.getString(WUBlastNodeModel.CFGKEY_EMAIL);
	    	m_alignments = s.getInt(WUBlastNodeModel.CFGKEY_NUM_ALIGNMENTS);
	    	m_scores = s.getInt(WUBlastNodeModel.CFGKEY_NUM_SCORES);
	    	m_image_summary = s.getBoolean(WUBlastNodeModel.CFGKEY_SAVE_IMAGE);
	    	m_eval = s.getString(WUBlastNodeModel.CFGKEY_EVAL_THRESHOLD);
	    	m_concurrent_blasts = s.getInt(WUBlastNodeModel.CFGKEY_EBI_BATCH_SIZE);
	    	//m_seqtype = s.getString(WUBlastNodeModel.CFGKEY_STYPE);
	    	m_db_filter = s.getString(WUBlastNodeModel.CFGKEY_FILTERSTR);
    	} catch (InvalidSettingsException e) {
    		e.printStackTrace();
    		throw new NotConfigurableException(e.getMessage());
    	}
    	
    	// attempt to load internal state from cache (just test for one of the settings)
    	m_is_loaded = false;
		try {
			if (s.containsKey(WUBlastNodeModel.CFGKEY_EBI_LOADED)) {
				m_is_loaded = s.getBoolean(WUBlastNodeModel.CFGKEY_EBI_LOADED);
			}
			
			if (m_is_loaded) {
				m_ebi_programs    = s.getStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_PROGS);
				m_ebi_databases   = s.getStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_DATABASES);
				m_ebi_filters     = s.getStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_FILTERS);
				m_ebi_matrices    = s.getStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_MATRICES);
				m_ebi_sensitivity = s.getStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_SENSITIVITY);
				m_ebi_sort        = s.getStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_SORT);
				m_ebi_statistics  = s.getStringArray(WUBlastNodeModel.CFGKEY_EBI_SETTINGS_STATISTICS);
				return;
			}
    			
    			// else FALLTHRU to fetch the data
    	} catch (InvalidSettingsException ise) {
    			ise.printStackTrace();
    			// fallthru to get online or barf...
    	}
    		
        // else from EBI
	    try {
	    		fetch_ebi_settings();
	    } catch (Exception e) {
	    		e.printStackTrace();
	    		throw new NotConfigurableException("This node requires a working Internet connection to run BLAST!");
	    }
    	
    }

    @Override
    public void onOpen() {
    	this.removeTab("Options");
    	this.removeTab("Advanced");
    	setup_ui();
    	try {
    		init_widget_settings();		// sets initial selected widget values based on member variables
    	} catch (NotConfigurableException e) {
    		e.printStackTrace();
    	}
    	this.setSelected("Options");
    }
    
    private void init_widget_settings() throws NotConfigurableException {
		if (m_incoming_spec != null) {
			// must always update the list of columns in response to change regardless of internal state
			if (m_sequence != null) {
				d_sequence.update(m_incoming_spec, m_sequence);
			} else if (m_incoming_spec.getNumColumns() > 0) {
				// else try to choose the last column (hope it passes the columnfilter!)
				d_sequence.update(m_incoming_spec, m_incoming_spec.getColumnSpec(m_incoming_spec.getNumColumns()-1).getName());
			} else {
				throw new NotConfigurableException("No suitable sequence columns (see columns to sequence node)!");
			}
		}
		if (d_sequence != null && d_sequence.getNrItemsInList() < 1) 
			throw new NotConfigurableException("No suitable columns with sequence data (see Columns2Sequence node)");
		
		//if (m_seqtype != null) 
		//	d_seqtype.setSelectedItem(m_seqtype);
		if (m_programs != null) 
			d_programs.setSelectedItem(m_programs);
		if (m_matrix != null)
			d_matrix.setSelectedItem(m_matrix);
		if (m_eval != null) 
			d_eval_cutoff.setSelectedItem(m_eval);
		if (m_filter != null) {
			//Logger.getAnonymousLogger().info("Initializing low complexity filter to "+m_filter);
			d_filters.setSelectedItem(m_filter);
		}
		if (m_sensitivity != null) 
			d_sensitivity.setSelectedItem(m_sensitivity);
		if (m_sort != null)
			d_sort.setSelectedItem(m_sort);
		if (m_statistics != null)
			d_statistics.setSelectedItem(m_statistics);
		if (m_email != null)
			d_email.setText(m_email);
		if (m_db_filter != null) {
			d_db_filter.setText(m_db_filter);
			d_db.setModel(make_model(this));
		}
		if (m_db != null)
			d_db.setSelectedValue(m_db, true);
		d_alignments.setSelectedItem(String.valueOf(m_alignments));
		d_scores.setSelectedItem(String.valueOf(m_scores));
		d_batch_size.setValue(m_concurrent_blasts);
		d_image_summary.setSelected(m_image_summary);
	}

	@Override
    public void onClose() {
    	m_main_panel = null;
    	m_adv_panel  = null;
    	this.removeTab("Options");
    	this.removeTab("Advanced");
    	super.onClose();
    }
}

