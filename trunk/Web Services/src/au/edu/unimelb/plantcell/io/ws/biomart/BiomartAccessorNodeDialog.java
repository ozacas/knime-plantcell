package au.edu.unimelb.plantcell.io.ws.biomart;

import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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

import au.edu.unimelb.plantcell.io.ws.biomart.soap.Attribute;
import au.edu.unimelb.plantcell.io.ws.biomart.soap.Dataset;
import au.edu.unimelb.plantcell.io.ws.biomart.soap.Filter;
import au.edu.unimelb.plantcell.io.ws.biomart.soap.Mart;
import au.edu.unimelb.plantcell.io.ws.biomart.soap.PortalServiceImpl;



public class BiomartAccessorNodeDialog extends DefaultNodeSettingsPane {
	private final static PortalServiceImpl port;
	static {
    	port = BiomartAccessorNodeModel.getService().getPortalServiceImplPort();
	};
	
	public BiomartAccessorNodeDialog() 	{
		final SettingsModelString                sms_db = new SettingsModelString(BiomartAccessorNodeModel.CFGKEY_DB, "");
		final SettingsModelString           sms_dataset = new SettingsModelString(BiomartAccessorNodeModel.CFGKEY_DATASET, "");
		final DialogComponentStringSelection dc_dataset = new DialogComponentStringSelection(sms_dataset, "Dataset name", new String[] { "No server available!" });
		final DialogComponentStringListSelection dc_filters = new DialogComponentStringListSelection(
				new SettingsModelStringArray(BiomartAccessorNodeModel.CFGKEY_FILTER, new String[0]), 
				"Only show data where... ", new ArrayList<String>(), ListSelectionModel.MULTIPLE_INTERVAL_SELECTION, false, 10);
		final DialogComponentStringListSelection dc_what = new DialogComponentStringListSelection(
				new SettingsModelStringArray(BiomartAccessorNodeModel.CFGKEY_WHAT, new String[0]),
				"Select the columns to output", getAttributes(sms_dataset.getStringValue(), sms_db.getStringValue()), 
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
				for (Dataset d :  getDatasets(m)) {
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
				List<Dataset> datasets = getDatasets(m);
				if (datasets != null) {
					Dataset ds = null;
					for (Dataset d : datasets) {
						if (d.getName().equals(sms_dataset.getStringValue()) || 
								d.getDisplayName().equals(sms_dataset.getStringValue())) {
							List<Filter> filters = getFilters(m, d);
							ArrayList<String> newItems = new ArrayList<String>();
							for (Filter f : filters) {
								newItems.add(f.getDisplayName());
							}
							if (newItems.size() == 0)
								newItems.add("No filters available for this dataset!");
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
				dc_what.replaceListItems(items);
			}
			
		});
		this.setHorizontalPlacement(true);
		addDialogComponent(dc_filters);
		Container ctr = dc_filters.getComponentPanel().getParent();
		JTable p = new JTable(new FilterTableModel());
		
		ctr.add(new JScrollPane(p));
		this.setHorizontalPlacement(false);
		
		createNewGroup("Report these columns (attributes)...");
		addDialogComponent(dc_what);
		
		createNewGroup("Advanced users only");
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
			List<Dataset> datasets = getDatasets(m);
			
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
					ret.add(a.getName());
				}
			}
		}
		
		if (ret.size() == 0) {
			ret.add("No attributes available!");
		}
		return ret;
	}
	
	protected List<Filter> getFilters(final Mart m, final Dataset ds) {
		assert(m != null && ds != null);
		
		ArrayList<Filter> ret = new ArrayList<Filter>();
		ret.addAll(port.getFilters(ds.getName(), null, null));
		
		return ret;
	}
	
	private List<Dataset> getDatasets(final Mart m) {
		assert(m != null);
		ArrayList<Dataset> ret = new ArrayList<Dataset>();
		for (Dataset ds : port.getDatasets(m.getName())) {
			if (!ds.isIsHidden())
				ret.add(ds);
		}
		
		return ret;
	}
	
	protected Map<String,Mart> getMarts() {
		HashMap<String,Mart> ret = new HashMap<String,Mart>();

    	for (Mart m : port.getMarts(null)) {
    		if (!m.isIsHidden()) {
    			ret.put(m.getDisplayName(), m);
    		}
    	}
    	if (ret.size() > 0) {
    		return ret;
    	} else {
    		ret.put("Server not available", null);
    		return ret;
    	}
	}
}
