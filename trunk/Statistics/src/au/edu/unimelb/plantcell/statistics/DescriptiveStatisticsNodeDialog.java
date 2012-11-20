package au.edu.unimelb.plantcell.statistics;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.util.ColumnFilter;

/**
 * <code>NodeDialog</code> for the "DescriptiveStatistics" Node.
 * Basic descriptive and inferential statistics support built using the apache commons math v3 library.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class DescriptiveStatisticsNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring Ranker node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected DescriptiveStatisticsNodeDialog() {
        super();
         
        addDialogComponent(new DialogComponentColumnFilter(
        		new SettingsModelFilterString(DescriptiveStatisticsNodeModel.CFGKEY_COLUMNS), 0, true, new ColumnFilter() {

					@Override
					public boolean includeColumn(DataColumnSpec colSpec) {
						return colSpec.getType().isCompatible(DoubleValue.class);
					}

					@Override
					public String allFilteredMsg() {
						return "No numeric columns available!";
					}
        			
        		}
        		));
        
    }
}

