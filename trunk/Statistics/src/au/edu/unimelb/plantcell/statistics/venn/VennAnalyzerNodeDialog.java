package au.edu.unimelb.plantcell.statistics.venn;

import java.util.ArrayList;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * <code>NodeDialog</code> for the "VennAnalyzer" Node.
 * Performs an n-way (3 or 4 recommended) venn analysis based over the values in chosen columns based on a group-by column.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class VennAnalyzerNodeDialog extends DefaultNodeSettingsPane {
	private final SettingsModelStringArray m_cols = new SettingsModelStringArray(VennAnalyzerNodeModel.CFGKEY_VALUE_COLUMNS, new String[] {"None"});
	private final ArrayList<String> m_initial_cols = new ArrayList<String>();
	private DialogComponentStringListSelection dcs;
	
    @SuppressWarnings("unchecked")
	protected VennAnalyzerNodeDialog() {
        super();
        m_initial_cols.add("none");
        
        addDialogComponent(new DialogComponentColumnNameSelection(new SettingsModelString(VennAnalyzerNodeModel.CFGKEY_GROUPBY, ""), "Group by", 0, true, false, StringValue.class));
        
        addDialogComponent(new DialogComponentNumber(new SettingsModelIntegerBounded(VennAnalyzerNodeModel.CFGKEY_N, 4, 1, 15), "Use at most X categories", 1));
        
        dcs = new DialogComponentStringListSelection(m_cols, 
        		"Columns to use for values", m_initial_cols.toArray(new String[0]));
        dcs.setVisibleRowCount(5);
        addDialogComponent(dcs);            
    }
    
    @Override
    public void loadAdditionalSettingsFrom(NodeSettingsRO s, DataTableSpec[] specs) {
    	m_initial_cols.clear();
    	
    	for (int i=0; i<specs[0].getNumColumns(); i++) {
    		DataColumnSpec colspec = specs[0].getColumnSpec(i);
    		if (colspec.getType().isCompatible(StringValue.class) ||
    				(colspec.getType().isCollectionType() && 
    						colspec.getType().getCollectionElementType().isCompatible(StringValue.class))) {
    			m_initial_cols.add(colspec.getName());
    		}
    	}
    	
    	if (dcs != null) {
    		dcs.replaceListItems(m_initial_cols, "try this");
    	}
    	
    	try {
    		m_cols.setStringArrayValue(s.getStringArray(VennAnalyzerNodeModel.CFGKEY_VALUE_COLUMNS));
    	} catch (Exception e) {
    		// NO-OP
    	}
    }
}

