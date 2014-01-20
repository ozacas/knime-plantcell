package au.edu.unimelb.plantcell.io.ws.biomart;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang.SerializationUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.servers.biomart.Dataset;
import au.edu.unimelb.plantcell.servers.biomart.Filter;
import au.edu.unimelb.plantcell.servers.biomart.Mart;
import au.edu.unimelb.plantcell.servers.biomart.PortalServiceImpl;

import com.sun.org.apache.xml.internal.security.utils.Base64;


/**
 * Dialog for selecting the database (mart), dataset (if a mart has more than one) and filtering
 * the data as required by the user eg. a particular proteome or pathway or whatever. Usual swing junk.
 * 
 * @author andrew.cassin
 *
 */
public class BiomartAccessorNodeDialog extends DefaultNodeSettingsPane {
	private final static PortalServiceImpl port;
	private final FilterTableModel ftm = new FilterTableModel();
	private final MyTable             p = new MyTable(ftm);		// current user-defined filters
	
	static {
    	port = BiomartAccessorNodeModel.getService().getPortalServiceImplPort();
	};	
	
	public BiomartAccessorNodeDialog() 	{
		final SettingsModelString                sms_db = new SettingsModelString(BiomartAccessorNodeModel.CFGKEY_DB, "");
		final SettingsModelString           sms_dataset = new SettingsModelString(BiomartAccessorNodeModel.CFGKEY_DATASET, "");
		final DialogComponentStringSelection dc_dataset = new DialogComponentStringSelection(sms_dataset, "Dataset name", new String[] { "No server available!" });
		final SettingsModelStringArray sms_filters = new SettingsModelStringArray(BiomartAccessorNodeModel.CFGKEY_FILTER, new String[0]);
		
		final SettingsModelStringArray sms_what = new SettingsModelStringArray(BiomartAccessorNodeModel.CFGKEY_WHAT, new String[0]);
		
	
		final DialogComponentStringListSelection dc_filters = new DialogComponentStringListSelection(
				sms_filters, 
				"Only show data where... ", new ArrayList<String>(), ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, false, 10);
		
		
		final DialogComponentStringListSelection dc_what = new DialogComponentStringListSelection(
				sms_what, "Select the columns to output", BiomartAccessorNodeModel.getAttributesAsString(port, sms_dataset.getStringValue(), sms_db.getStringValue()), 
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
							Collections.sort(newItems);
							dc_filters.replaceListItems(newItems, (String) null);
							dc_filters.setSizeComponents(300, 300);
							((FilterTableModel)p.getModel()).clear();
						}
							
					}
					
				}
			}
			
		});
		sms_dataset.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				List<String> items = BiomartAccessorNodeModel.getAttributesAsString(port, sms_dataset.getStringValue(), sms_db.getStringValue());
				dc_what.replaceListItems(items, items.toArray(new String[0]));
			}
			
		});
		
		this.setHorizontalPlacement(true);
		addDialogComponent(dc_filters);
		Container ctr = dc_filters.getComponentPanel().getParent();
		JPanel button_panel = new JPanel();
		button_panel.setLayout(new BoxLayout(button_panel, BoxLayout.Y_AXIS));
		JButton add_filter = new JButton("Add Filter >>");
		p.setRowEditorModel(ftm);
		p.setPreferredSize(new Dimension(400,200));
		
		add_filter.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent ev) {
				String[] cur_filters = sms_filters.getStringArrayValue();
				FilterTableModel ftm = ((FilterTableModel)p.getModel());
				if (cur_filters != null && cur_filters.length > 0) {
					for (String filter_name: cur_filters) {
						Filter f = BiomartAccessorNodeModel.getFilter(port, sms_db.getStringValue(), sms_dataset.getStringValue(), filter_name);
						
						if (f != null && !f.isIsHidden() && !ftm.alreadyHasFilter(f)) {
							ftm.append(f);
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
	
	
	@Override
	public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] inSpecs) {
		FilterTableModel              ftm = (FilterTableModel) p.getModel();
		Map<String,Object> wanted_filters = new HashMap<String,Object>();
		
		try {
			ftm.clear();
			String[] filters = settings.getStringArray(BiomartAccessorNodeModel.CFGKEY_WANTED_FILTER);
			for (int i=0; i<filters.length; i++) {
				String name = filters[i].substring(0, filters[i].indexOf('='));
				Object o = SerializationUtils.deserialize(Base64.decode(filters[i].substring(filters[i].indexOf('=')+1)));
				wanted_filters.put(name,o);
				Logger.getAnonymousLogger().info("Loaded filter for "+name+" to "+o.toString());
			}
			
			String db = settings.getString(BiomartAccessorNodeModel.CFGKEY_DB);
			String dataset = settings.getString(BiomartAccessorNodeModel.CFGKEY_DATASET);
			Mart m = BiomartAccessorNodeModel.getMart(port, db);
			Dataset ds = BiomartAccessorNodeModel.getDataset(port, m, dataset);
			if (ds == null || m == null) {
				// be silent: new node probably just put onto canvas...
			} else {
				List<Filter> known_filters = BiomartAccessorNodeModel.getFilters(port, m, ds);
				Map<String,Filter> known_filter_map = new HashMap<String,Filter>();
				for (Filter f : known_filters) {
					known_filter_map.put(f.getName(), f);
				}
				
				for (String filter_name : wanted_filters.keySet()) {
					Filter f = known_filter_map.get(filter_name);
					if (f == null)
						continue;
					ftm.append(f);
					int r = ftm.getRowCount()-1;
					ftm.setValueAt(wanted_filters.get(filter_name), r, 5);
				}
			}
		} catch (Exception ex) {
			// assume no filters set but report exception anyway...
			wanted_filters.clear();
			ex.printStackTrace();
		}
	}
	
	@Override
	public void saveAdditionalSettingsTo(final NodeSettingsWO settings) {
		FilterTableModel ftm = (FilterTableModel) p.getModel();
		ArrayList<String> user_filters = new ArrayList<String>();
		for (int i=0; i<ftm.getRowCount(); i++) {
			Object val   = ftm.getFilterUserValue(i);
			byte[] data = SerializationUtils.serialize((Serializable) val);
			// NB: cannot use the f.getDisplayName() as this would cause parsing errors on load
			String name = ftm.getFilter(i).getName();
			user_filters.add(name+"="+Base64.encode(data));
			Logger.getAnonymousLogger().info("Saved filter for "+name+" to "+data);

		}
		settings.addStringArray(BiomartAccessorNodeModel.CFGKEY_WANTED_FILTER, user_filters.toArray(new String[0]));
	}
}
