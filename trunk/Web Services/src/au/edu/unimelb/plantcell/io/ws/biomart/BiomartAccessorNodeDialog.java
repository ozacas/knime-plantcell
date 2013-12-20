package au.edu.unimelb.plantcell.io.ws.biomart;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.servers.biomart.Attribute;
import au.edu.unimelb.plantcell.servers.biomart.Dataset;
import au.edu.unimelb.plantcell.servers.biomart.Filter;
import au.edu.unimelb.plantcell.servers.biomart.Mart;
import au.edu.unimelb.plantcell.servers.biomart.PortalServiceImpl;


/**
 * Dialog for selecting the database (mart), dataset (if a mart has more than one) and filtering
 * the data as required by the user eg. a particular proteome or pathway or whatever. Usual swing junk.
 * 
 * @author andrew.cassin
 *
 */
public class BiomartAccessorNodeDialog extends DefaultNodeSettingsPane {
	private final static PortalServiceImpl port;
	static {
    	port = BiomartAccessorNodeModel.getService().getPortalServiceImplPort();
	};	
	
	public BiomartAccessorNodeDialog() 	{
		final SettingsModelString                sms_db = new SettingsModelString(BiomartAccessorNodeModel.CFGKEY_DB, "");
		final SettingsModelString           sms_dataset = new SettingsModelString(BiomartAccessorNodeModel.CFGKEY_DATASET, "");
		final DialogComponentStringSelection dc_dataset = new DialogComponentStringSelection(sms_dataset, "Dataset name", new String[] { "No server available!" });
		final SettingsModelStringArray sms_filters = new SettingsModelStringArray(BiomartAccessorNodeModel.CFGKEY_FILTER, new String[0]);
		final FilterTableModel ftm = new FilterTableModel();
		final JTable p = new JTable(ftm);
		final SettingsModelStringArray sms_what = new SettingsModelStringArray(BiomartAccessorNodeModel.CFGKEY_WHAT, new String[0]);
		
	
		final DialogComponentStringListSelection dc_filters = new DialogComponentStringListSelection(
				sms_filters, 
				"Only show data where... ", new ArrayList<String>(), ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, false, 10);
		final DialogComponentStringListSelection dc_what = new DialogComponentStringListSelection(
				sms_what, "Select the columns to output", getAttributes(sms_dataset.getStringValue(), sms_db.getStringValue()), 
						ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, true, 10);
		
		HashSet<String> mart_names = new HashSet<String>();
		for (Mart m : port.getMarts(null)) {
			mart_names.add(m.getGroup());
		}
		ArrayList<String> name_list = new ArrayList<String>(mart_names);
		Collections.sort(name_list);
		
		this.setHorizontalPlacement(true);
		addDialogComponent(new DialogComponentStringSelection(
				sms_db, "Available biomarts", 
				name_list.toArray(new String[0])
				));
		
		sms_db.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				ArrayList<String> newItems = new ArrayList<String>();
				Mart m = BiomartAccessorNodeModel.getMart(port, sms_db.getStringValue());
				for (Dataset d :  BiomartAccessorNodeModel.getDatasets(port, m)) {
					newItems.add(d.getName());
				}
				dc_dataset.replaceListItems(newItems, null);
			}

			
		});
		addDialogComponent(dc_dataset);
		
		
		createNewGroup("Filter dataset by... ");
		sms_dataset.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				final Mart m = BiomartAccessorNodeModel.getMart(port, sms_db.getStringValue());
				List<Dataset> datasets = BiomartAccessorNodeModel.getDatasets(port, m);
				if (datasets != null) {
					for (Dataset d : datasets) {
						if (d.getName().equals(sms_dataset.getStringValue()) || 
								d.getDisplayName().equals(sms_dataset.getStringValue())) {
							List<Filter> filters = BiomartAccessorNodeModel.getFilters(port, m, d);
							ArrayList<String> newItems = new ArrayList<String>();
							for (Filter f : filters) {
								newItems.add(f.getDisplayName());
							}
							if (newItems.size() == 0)
								newItems.add("No filters available!");
							dc_filters.replaceListItems(newItems, (String) null);
						}
							
					}
					
				}
			}
			
		});
		sms_dataset.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				List<String> items = getAttributes(sms_dataset.getStringValue(), sms_db.getStringValue());
				dc_what.replaceListItems(items, items.toArray(new String[0]));
			}
			
		});
		
		this.setHorizontalPlacement(true);
		addDialogComponent(dc_filters);
		Container ctr = dc_filters.getComponentPanel().getParent();
		JPanel button_panel = new JPanel();
		button_panel.setLayout(new BoxLayout(button_panel, BoxLayout.Y_AXIS));
		JButton add_filter = new JButton("Add Filter >>");
		
		p.setPreferredSize(new Dimension(400,200));
		p.getColumnModel().getColumn(5).setCellEditor(new FilterValueEditor());
		
		add_filter.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ev) {
				String[] cur_filters = sms_filters.getStringArrayValue();
				if (cur_filters != null && cur_filters.length > 0) {
					for (String filter_name: cur_filters) {
						Filter f = BiomartAccessorNodeModel.getFilter(port, sms_db.getStringValue(), sms_dataset.getStringValue(), filter_name);
						if (f != null && !f.isIsHidden()) {
							((FilterTableModel)p.getModel()).append(f);
						}
					}
				}
			}
			
		});
		button_panel.add(add_filter);
		button_panel.add(Box.createRigidArea(new Dimension(10,10)));
		JButton b = new JButton("Remove selected <<");
		b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ev) {
				FilterTableModel ftm = new FilterTableModel();
				FilterTableModel old = (FilterTableModel) p.getModel();
				
				int[] sel = p.getSelectedRows();
				for (int i=0; i<old.getRowCount(); i++) {
					boolean is_selected = false;
					for (int j=0; j<sel.length; j++) {
						if (i == sel[j]) {
							is_selected = true; break;
						}
					}
					if (!is_selected) {
						ftm.append(old.getFilter(i));
					}
				}
				p.setModel(ftm);
			}
			
		});
		button_panel.add(b);
		ctr.add(button_panel);
		
		p.setPreferredScrollableViewportSize(new Dimension(400,200));
		p.setFillsViewportHeight(true);
		JScrollPane sp = new JScrollPane(p);
		sp.setPreferredSize(new Dimension(400,200));
		ctr.add(sp);
		this.setHorizontalPlacement(false);
		
		createNewGroup("Report these columns (attributes)...");
		addDialogComponent(dc_what);
		final SettingsModelString attr_descr = new SettingsModelString("_crap", "");
		// since most datasets have no description for the attributes we dont display this for now...
		/*DialogComponentMultiLineString lbl = new DialogComponentMultiLineString(attr_descr, "", true, 30, 10);
		sms_what.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent ev) {
				/*String[] cols = sms_what.getStringArrayValue();
				List<Attribute> attrs = BiomartAccessorNodeModel.getAttributes(port, sms_dataset.getStringValue(), sms_db.getStringValue());
				StringBuilder sb = new StringBuilder();
				for (Attribute a : attrs) {
					for (String col : cols) {
						String attr_name_from_col = col.substring(0, col.indexOf(':'));
						if (a.getName().equals(attr_name_from_col)) {
							sb.append(a.getName()+": "+a.getDescription()+"\n");
							break;
						}
					}
				}
				if (sb.length() < 1)
					sb.append("Select column(s) at left, to see its description.");
				attr_descr.setStringValue(sb.toString());
			}
			
		});
		addDialogComponent(lbl);*/
		
		createNewTab("Advanced");
		addDialogComponent(
				new DialogComponentString(
						new SettingsModelString(BiomartAccessorNodeModel.CFGKEY_URL, ""), "Biomart Server")
				);
		addDialogComponent(
				new DialogComponentNumber(
						new SettingsModelIntegerBounded(BiomartAccessorNodeModel.CFGKEY_ROWLIMIT, 1000, 0, Integer.MAX_VALUE),
						"Maximum number of rows to fetch (0 unlimited)", 100
				));
		
	}
	
	protected List<String> getAttributes(final String db, final String mart_name) {
		ArrayList<String> ret = new ArrayList<String>();
	
		Mart m = BiomartAccessorNodeModel.getMart(port, mart_name);
		if (m != null) {
			List<Dataset> datasets = BiomartAccessorNodeModel.getDatasets(port, m);
			
			Dataset my_dataset = null;
			for (final Dataset ds : datasets) {
				if (ds.getName().equals(db) || ds.getDisplayName().equals(db)) {
					my_dataset = ds;
					break;
				}
			}
			
			if (my_dataset != null) {
				List<Attribute> attributes = port.getAttributes(my_dataset.getName(), m.getConfig(), null, true);
				for (final Attribute a : attributes) {
					ret.add(a.getName()+": "+a.getDisplayName());
				}
			}
		}
		
		if (ret.size() == 0) {
			ret.add("No attributes available!");
		}
		return ret;
	}
	
}
