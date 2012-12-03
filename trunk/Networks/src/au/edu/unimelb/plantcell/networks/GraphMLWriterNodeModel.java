package au.edu.unimelb.plantcell.networks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections15.Transformer;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import au.edu.unimelb.plantcell.networks.cells.NetworkValue;
import edu.uci.ics.jung.io.GraphMLWriter;


/**
 * This is the model implementation of Creator.
 * Using JUNG, this node creates a network cell for the input data correlated above a chosen threshold for each target variable.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class GraphMLWriterNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("GraphML Writer");
    
    // dialog configuration parameters
	public static final String CFGKEY_NETWORKS      = "networks";
	public static final String CFGKEY_FILENAME = "folder-to-save-to";
        
	// private members
	private final SettingsModelString m_networks = new SettingsModelString(CFGKEY_NETWORKS, "");
	private final SettingsModelString m_folder   = new SettingsModelString(CFGKEY_FILENAME, "");
	
    /**
     * Constructor for the node model.
     */
    protected GraphMLWriterNodeModel() {
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	RowIterator    it = inData[0].iterator();
    	int idx = inData[0].getSpec().findColumnIndex(m_networks.getStringValue());
    	if (idx < 0) {
    		throw new InvalidSettingsException("Cannot locate '"+m_networks.getStringValue()+"' - reconfigure?");
    	}
    	File output_dir = new File(m_folder.getStringValue());
    	if (!output_dir.exists() || !output_dir.isDirectory())
    		throw new InvalidSettingsException("No such folder: "+output_dir.getAbsolutePath());
    	
    	int done = 0;
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		DataCell network_cell = r.getCell(idx);
    		if (network_cell == null || network_cell.isMissing())
    			continue;
    		NetworkValue nv = (NetworkValue) network_cell;
    		GraphMLWriter<MyVertex,MyEdge> gmlw = new GraphMLWriter<MyVertex,MyEdge>();
    		File out_file = new File(output_dir, r.getKey().getString()+".graphml");
    		logger.info("Saving network: "+out_file.getAbsolutePath());
    		PrintWriter pw = new PrintWriter(new FileWriter(out_file));
    		
    		// compute metadata available from edge and vertex sets: HACK -- O(n^2) algorithmic complexity
    		final Set<Object> edge_keys = new HashSet<Object>();
    		final Set<Object> vertex_keys= new HashSet<Object>();
    		Collection<MyEdge> edges = nv.getGraph().getEdges();
    		Collection<MyVertex> vertices = nv.getGraph().getVertices();
    		for (MyEdge e : edges) {
    			for (Object o : e.getPropertyKeys()) {
    				if (!edge_keys.contains(o)) {
    					edge_keys.add(o);
    				}
    			}
    		}
    		for (MyVertex v : vertices) {
    			for (Object o : v.getPropertyKeys()) {
    				if (!vertex_keys.contains(o)) {
    					vertex_keys.add(o);
    				}
    			}
    		}
    		
    		StringBuilder sb = new StringBuilder();
    		for (Object o: edge_keys) {
    			sb.append(o.toString());
    			sb.append(", ");
    		}
    		logger.info("Found edge annotations: "+sb.toString());
    		sb = new StringBuilder();
    		for (Object o : vertex_keys) {
    			sb.append(o.toString());
    			sb.append(", ");
    		}
    		logger.info("Found vertex annotations: "+sb.toString());
    		
    		// add vertex and edge metadata to GraphMLWriter instance for saving
    		gmlw.addEdgeData("id", "edge id", "e1", new Transformer<MyEdge,String>() {

				@Override
				public String transform(MyEdge e) {
					return e.getID();
				}
    			
    		});
    		for (final Object k : edge_keys) {
    			gmlw.addEdgeData(k.toString(), k.toString(), "", new Transformer<MyEdge,String>() {

					@Override
					public String transform(MyEdge e) {
						Object ret = e.getProperty(k);
						if (ret == null)
							return "";
						return ret.toString();
					}
    				
    			});
    		}
    		for (final Object k : vertex_keys) {
    			gmlw.addVertexData(k.toString(), k.toString(), "", new Transformer<MyVertex,String>() {

					@Override
					public String transform(MyVertex v) {
						Object ret = v.getProperty(k);
						if (ret == null)
							return "";
						return ret.toString();
					}
    				
    			});
    		}
    		
    		// save it (1. nodes 2. edges 3. metadata)
    		gmlw.save(nv.getGraph(), pw);
    		
    		pw.close();
    		logger.info("Saved "+out_file.getAbsolutePath());
    		
    		exec.checkCanceled();
    		exec.setProgress(((double)done++) / inData[0].getRowCount());
    	}
    	
    	return new BufferedDataTable[] { };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }


    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        return new DataTableSpec[]{ };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_networks.saveSettingsTo(settings);
    	m_folder.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_networks.loadSettingsFrom(settings);
    	m_folder.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
       	m_networks.loadSettingsFrom(settings);
    	m_folder.loadSettingsFrom(settings);
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

