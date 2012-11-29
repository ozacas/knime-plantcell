package au.edu.unimelb.plantcell.networks;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

import au.edu.unimelb.plantcell.core.MyDataContainer;


/**
 * This is the model implementation of Creator.
 * Using JUNG, this node creates a network cell for the input data correlated above a chosen threshold for each target variable.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class CreatorNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Network Creator");
    
    // dialog configuration parameters
	public static final String CFGKEY_SOURCE      = "source";
	public static final String CFGKEY_DESTINATION = "destination";
	public static final String CFGKEY_DISTANCE    = "distance";
        
	// private members
	private final SettingsModelString m_source = new SettingsModelString(CFGKEY_SOURCE, "");
	private final SettingsModelString m_destination = new SettingsModelString(CFGKEY_DESTINATION, "");
	private final SettingsModelColumnName m_distance = new SettingsModelColumnName(CFGKEY_DISTANCE, "");

    /**
     * Constructor for the node model.
     */
    protected CreatorNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	MyDataContainer c = new MyDataContainer(exec.createDataContainer(make_output_spec(inData[0].getSpec())), "Net");
    	RowIterator    it = inData[0].iterator();
    	
    	boolean no_distance = (m_distance.getColumnName() == null);
    	int distance_idx = -1;
    	if (! no_distance) {
    		distance_idx = inData[0].getSpec().findColumnIndex(m_distance.getColumnName());
    	}
    	int source_idx = inData[0].getSpec().findColumnIndex(m_source.getStringValue());
    	int dest_idx   = inData[0].getSpec().findColumnIndex(m_destination.getStringValue());
    	Graph<MyVertex, MyEdge> g = new SparseGraph<MyVertex,MyEdge>();
    	
    	int done = 0;
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		DataCell src_cell = r.getCell(source_idx);
    		DataCell dst_cell = r.getCell(dest_idx);
    		if (src_cell == null || dst_cell == null || src_cell.isMissing() || dst_cell.isMissing()) 
    			continue;
    		String source = src_cell.toString();
    		String dest   = dst_cell.toString();
    		MyVertex my_src = new MyVertex(source);
    		MyVertex my_dest= new MyVertex(dest);
    		boolean added_src = false;
    		boolean added_dst = false;
    		if (!g.containsVertex(my_src)) {
    			g.addVertex(my_src);
    			added_src = true;
    		}
    		if (!g.containsVertex(my_dest)) {
    			g.addVertex(my_dest);
    			added_dst = true;
    		}

    		if (!added_src && !added_dst)
    			throw new InvalidSettingsException("Multiple paths (ie. rows) between "+source+" -> "+ dest+": are not permitted!");
    		
    		MyEdge e = new MyEdge();
    		if (dest_idx >= 0) {
    			try {
    				Double d = Double.valueOf(r.getCell(distance_idx).toString());
    				e.setDistance(d.doubleValue());
    			} catch (Exception e2) {
    				// assume default distance ie. zero
    			}
    		}
    		g.addEdge(e, my_src, my_dest, EdgeType.UNDIRECTED);
    		
    		done++;
    		if (done % 100 == 0) {
    			exec.checkCanceled();
    			exec.setProgress(((double)done) / inData[0].getRowCount());
    		}
    	}
    	
    	c.addRow(new DataCell[] { new StringCell(""), new NetworkCell(g) });
    	
    	return new BufferedDataTable[] { c.close() };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    private DataTableSpec make_output_spec(DataTableSpec spec) {
    	DataColumnSpec[] cols = new DataColumnSpec[2];
    	cols[0] = new DataColumnSpecCreator("Source node", StringCell.TYPE).createSpec();
    	cols[1] = new DataColumnSpecCreator("Network", NetworkCell.TYPE).createSpec();
    	
    	return new DataTableSpec(cols);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        return new DataTableSpec[]{ make_output_spec(inSpecs[0]) };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_source.saveSettingsTo(settings);
    	m_destination.saveSettingsTo(settings);
    	m_distance.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_source.loadSettingsFrom(settings);
        m_destination.loadSettingsFrom(settings);
        m_distance.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
	    m_source.validateSettings(settings);
        m_destination.validateSettings(settings);
        m_distance.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
    }

}

