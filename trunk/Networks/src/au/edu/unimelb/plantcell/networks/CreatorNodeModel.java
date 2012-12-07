package au.edu.unimelb.plantcell.networks;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.networks.cells.GraphMLIO;
import au.edu.unimelb.plantcell.networks.cells.MyEdge;
import au.edu.unimelb.plantcell.networks.cells.MyVertex;
import au.edu.unimelb.plantcell.networks.cells.NetworkCell;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.io.GraphIOException;


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
	public static final String CFGKEY_ANNOTATE_VERTEX = "vertex-annotations";
	public static final String CFGKEY_ANNOTATE_EDGE   = "edge-annotations";
	public static final String CFGKEY_COLOUR_BY       = "color-by";
	public static final String CFGKEY_EDGE_DISTANCE = "edge-distance?";
	public static final String CFGKEY_EDGE_GRADIENT = "edge-gradient?";
    public static final String CFGKEY_TIMECOURSE    = "timecourse-data-column";
	public static final String CFGKEY_ANNOTATE_VERTEX_DEST = "destination-vertex-annotations";
	
	// private members
	private final SettingsModelString m_source = new SettingsModelString(CFGKEY_SOURCE, "");
	private final SettingsModelString m_destination = new SettingsModelString(CFGKEY_DESTINATION, "");
	private final SettingsModelString m_distance = new SettingsModelString(CFGKEY_DISTANCE, "");
	private final SettingsModelFilterString m_vertex_annotations  = new SettingsModelFilterString(CFGKEY_ANNOTATE_VERTEX);
	private final SettingsModelFilterString m_dest_vertex_annotations = new SettingsModelFilterString(CFGKEY_ANNOTATE_VERTEX_DEST);
	private final SettingsModelFilterString m_edge_annotations    = new SettingsModelFilterString(CFGKEY_ANNOTATE_EDGE);
	private final SettingsModelString m_colour_by = new SettingsModelString(CFGKEY_COLOUR_BY, "None");
	private final SettingsModelBoolean m_edge_gradient = new SettingsModelBoolean(CFGKEY_EDGE_GRADIENT, Boolean.FALSE);
	private final SettingsModelBoolean m_edge_distance = new SettingsModelBoolean(CFGKEY_EDGE_DISTANCE, Boolean.FALSE);
	private final SettingsModelString  m_timecourse    = new SettingsModelString(CFGKEY_TIMECOURSE, "");
	
	private Graph<MyVertex,MyEdge> m_graph;
	
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
    	
    	boolean no_distance = (m_distance.getStringValue() == null);
    	int distance_idx = -1;
    	if (! no_distance) {
    		distance_idx = inData[0].getSpec().findColumnIndex(m_distance.getStringValue());
    	}
    	int source_idx = inData[0].getSpec().findColumnIndex(m_source.getStringValue());
    	int dest_idx   = inData[0].getSpec().findColumnIndex(m_destination.getStringValue());
    	if (source_idx < 0 || dest_idx < 0)
    		throw new InvalidSettingsException("Source and/or destination columns not found: re-configure the node!");
    	int vector_idx = -1;
    	if (showTimecourse()) {
    		vector_idx = inData[0].getSpec().findColumnIndex(m_timecourse.getStringValue());
    		if (vector_idx < 0)
    			throw new InvalidSettingsException("Unable to locate timecourse collection column: re-configure!");
    	}
    	Graph<MyVertex, MyEdge> g = new SparseGraph<MyVertex,MyEdge>();
    	
    	logger.info("Creating Network from "+inData[0].getRowCount()+" rows.");
    	int done = 0;
    	boolean colour_edges = false;
    	boolean colour_nodes = false;
    	if (m_colour_by.getStringValue().toLowerCase().indexOf("node") >= 0) {
    		colour_nodes = true;
    	}
    	if (m_colour_by.getStringValue().toLowerCase().indexOf("edge") >= 0) {
    		colour_edges = true;
    	}
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
    		
    		if (!g.containsVertex(my_src)) {
    			// add metadata to my_src if any
    			List<String> includes = m_vertex_annotations.getIncludeList();
    			addMetadata(colour_nodes, inData[0].getSpec().getRowColor(r), includes, my_src, inData[0].getSpec(), r);
    			
    			// add vertex
    			if (vector_idx >= 0)
    				setVector(my_src, r.getCell(vector_idx));
    			g.addVertex(my_src);
    		}
    		if (!g.containsVertex(my_dest)) {
    			List<String> includes = m_dest_vertex_annotations.getIncludeList();
    			addMetadata(colour_nodes, inData[0].getSpec().getRowColor(r), includes, my_dest, inData[0].getSpec(), r);
    			if (vector_idx >= 0)
    				setVector(my_dest, r.getCell(vector_idx));
    			// add vertex
    			g.addVertex(my_dest);
    		}

    		MyEdge e = new MyEdge(my_src, my_dest);
    		// this node does not support multiple edges between nodes
    		if (g.containsEdge(e))
    			throw new InvalidSettingsException("Multiple paths (ie. rows) between "+source+" -> "+ dest+": are not permitted!");
    		if (colour_edges) {
    			Color  col = inData[0].getSpec().getRowColor(r).getColor();
    			String s = col.getRed() + "," + col.getGreen() + "," + col.getBlue();
    			e.setProperty("__colour", s);
    		}
    		// add metadata to edge if any
			List<String> includes = m_edge_annotations.getIncludeList();
			for (String inc : includes) {
				DataCell metadata_cell = r.getCell(inData[0].getSpec().findColumnIndex(inc));
				if (metadata_cell == null || metadata_cell.isMissing())
					continue;
				e.setProperty(inc, metadata_cell.toString());
			}
			
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
    	
    	setGraph(g);
		
    	c.addRow(new DataCell[] { new StringCell(""), new NetworkCell(g) });
    	
    	return new BufferedDataTable[] { c.close() };
    }

    private void addMetadata(boolean colour_nodes, ColorAttr rowColor,
			List<String> includes, MyVertex v, DataTableSpec spec, DataRow r) {
    	for (String inc : includes) {
			DataCell metadata_cell = r.getCell(spec.findColumnIndex(inc));
			if (metadata_cell == null || metadata_cell.isMissing())
				continue;
			v.setProperty(inc, metadata_cell.toString());
		}
		if (colour_nodes) {
			v.setColour(spec.getRowColor(r));
		}
	}

	private void setVector(MyVertex v, DataCell collection_cell) {
		if (collection_cell == null || collection_cell.isMissing() || !collection_cell.getType().isCollectionType()) {
			v.setSampleVector((double[]) null);
			return;
		}
		v.setSampleVector(collection_cell);
	}

	public void setGraph(Graph<MyVertex,MyEdge> g) {
    	assert(g != null);
    	m_graph = g;
    }
    
    public Graph<MyVertex, MyEdge> getGraph() {
    	return m_graph;
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
    	m_edge_annotations.saveSettingsTo(settings);
    	m_vertex_annotations.saveSettingsTo(settings);
    	m_colour_by.saveSettingsTo(settings);
    	m_edge_gradient.saveSettingsTo(settings);
    	m_edge_distance.saveSettingsTo(settings);
    	m_timecourse.saveSettingsTo(settings);
    	m_dest_vertex_annotations.saveSettingsTo(settings);
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
    	m_edge_annotations.loadSettingsFrom(settings);
    	m_vertex_annotations.loadSettingsFrom(settings);
    	m_colour_by.loadSettingsFrom(settings);
    	m_edge_gradient.loadSettingsFrom(settings);
    	m_edge_distance.loadSettingsFrom(settings);
    	m_timecourse.loadSettingsFrom(settings);
    	m_dest_vertex_annotations.loadSettingsFrom(settings);
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
    	m_edge_annotations.validateSettings(settings);
    	m_vertex_annotations.validateSettings(settings);
    	m_colour_by.validateSettings(settings);
    	m_edge_gradient.validateSettings(settings);
    	m_edge_distance.validateSettings(settings);
    	m_timecourse.validateSettings(settings);
    	m_dest_vertex_annotations.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	
    	File f = new File(internDir, "graph.graphml");
    	logger.info("loading last graph: "+f.getAbsolutePath());
    	boolean failed = false;
    	
    	try {
			Graph<MyVertex,MyEdge> g = new GraphMLIO().load(new FileReader(f));
			setGraph(g);
			return;
    	} catch (IllegalArgumentException iae) {
    		failed = true;
    		iae.printStackTrace();
    	} catch (GraphIOException gioe) {
    		failed = true;
			gioe.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
			failed = true;
		}
    	
    	if (failed) {
    		logger.warn("Unable to load graph - you will need to re-run node!");
			setGraph(null);
    	}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	File f = new File(internDir, "graph.graphml");
    	logger.info("Saving internal graph: "+f.getAbsolutePath());
        new GraphMLIO().save(getGraph(), new PrintWriter(f));
    }

	public boolean useEdgeGradient() {
		return m_edge_gradient.getBooleanValue();
	}

	public boolean showEdgeDistance() {
		return m_edge_distance.getBooleanValue();
	}

	public boolean showTimecourse() {
		String tc_column = m_timecourse.getStringValue();
		if (tc_column == null || tc_column.length() < 1 || tc_column.toLowerCase().equals("<none>"))
			return false;
		else
			return true;
	}

}

