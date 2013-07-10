package au.edu.unimelb.plantcell.views.plot3d;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jzy3d.colors.Color;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
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


/**
 * This is the model implementation of Plot3DBar.
 * Using jzy3d, this node produces a 3d bar plot using the supplied input columns.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class Plot3DBarNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("3D Bar Plot");
       
    static final String CFGKEY_X = "x";
    static final String CFGKEY_Y = "y";
    static final String CFGKEY_Z = "z";
    static final String CFGKEY_QUALITY = "plot-quality";
	static final String CFGKEY_OVERLAY_AXIS = "overlay-axis";
	static final String CFGKEY_OVERLAY_DATA = "overlay-data-column";
    
    private final SettingsModelString m_x = new SettingsModelString(CFGKEY_X, "");
    private final SettingsModelString m_y = new SettingsModelString(CFGKEY_Y, "");
    private final SettingsModelString m_z = new SettingsModelString(CFGKEY_Z, "");
    private final SettingsModelString m_overlay_axis = new SettingsModelString(CFGKEY_OVERLAY_AXIS, "");
    private final SettingsModelString m_overlay_column = new SettingsModelString(CFGKEY_OVERLAY_DATA, "");
    
    
    // these are all parallel vectors ie. must be the same size
    private FloatArrayList x_fal = new FloatArrayList();		// internal model state -- persisted
    private FloatArrayList y_fal = new FloatArrayList();		// internal model state -- persisted		
    private FloatArrayList z_fal = new FloatArrayList();		// internal model state -- persisted
    private SummaryStatistics x_stats = null;					// re-computed during loadInternals
    private SummaryStatistics y_stats = null;					// re-computed during loadInternals
    private SummaryStatistics z_stats = null;					// re-computed during loadInternals
    
    @SuppressWarnings("unused")
	private String[] m_rows = null;		// row ID's used for highlighting (NOT YET implemented)
    private Color[] m_colours = null;	// row colours used for the graph
    private FloatArrayList m_overlay= null;	// used for an axis-aligned overlay plot (optional)
    
    /**
     * Constructor for the node model.
     */
    protected Plot3DBarNodeModel() {
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	logger.info("Processing input data for 3D Plot");
    	boolean has_overlay = false;
    	if (hasOverlay()) {
    		logger.info("Providing an overlay plot on the "+getOverlayAxis()+" axis.");
    		has_overlay = true;
    	}
    	int x_idx = inData[0].getSpec().findColumnIndex(m_x.getStringValue());
    	int y_idx = inData[0].getSpec().findColumnIndex(m_y.getStringValue());
    	int z_idx = inData[0].getSpec().findColumnIndex(m_z.getStringValue());
    	if (x_idx < 0 || y_idx < 0 || z_idx < 0)
    		throw new InvalidSettingsException("Cannot find input columns - re-configure?");
    	int overlay_idx = -1;
    	if (has_overlay) {
    		overlay_idx = inData[0].getSpec().findColumnIndex(m_overlay_column.getStringValue());
    	}
    	
    	logger.info("Loading "+inData[0].getRowCount()+" data points.");
    	int missing = 0;
    	int bad = 0;
    	int n_rows = inData[0].getRowCount();
    	
    	reset();
    	x_fal.ensureCapacity(n_rows);
    	y_fal.ensureCapacity(n_rows);
    	z_fal.ensureCapacity(n_rows);
    	
    	ArrayList<String> rowids = new ArrayList<String>(n_rows);
    	ArrayList<Color> colours = new ArrayList<Color>(n_rows);
    	ArrayList<Float> overlay= new ArrayList<Float>(n_rows);
    	for (DataRow r : inData[0]) {
    		DataCell x_cell = r.getCell(x_idx);
    		DataCell y_cell = r.getCell(y_idx);
    		DataCell z_cell = r.getCell(z_idx);
    		if (x_cell == null || y_cell == null || z_cell == null || x_cell.isMissing() || y_cell.isMissing() || z_cell.isMissing()) {
    			missing++;
    			continue;
    		}
    		
    		try {
    			x_fal.add(Float.valueOf(x_cell.toString()));
    			y_fal.add(Float.valueOf(y_cell.toString()));
    			z_fal.add(Float.valueOf(z_cell.toString()));
    			rowids.add(r.getKey().getString());
    			// overlay vector MUST be the same length as the axis chosen for the overlay, otherwise it wont work. Missing
    			// values are not plotted (Double.NaN is used to mark this)
    			if (has_overlay) {
	    			DataCell overlay_cell = r.getCell(overlay_idx);
	    			if (overlay_cell == null || overlay_cell.isMissing()) {
	    				overlay.add(Float.NaN);
	    			} else {
	    				try {
	    					overlay.add(Float.valueOf(overlay_cell.toString()));
	    				} catch (NumberFormatException nfe) {
	    					nfe.printStackTrace();
	    				}
	    			}
    			}
    			java.awt.Color c_awt = inData[0].getSpec().getRowColor(r).getColor();
    			colours.add(c_awt != null ? new Color(c_awt.getRed(), c_awt.getGreen(), c_awt.getBlue()) : Color.BLACK);
    		} catch (NumberFormatException nfe) {
    			bad++;
    		}
    	}
    	
    	m_rows = rowids.toArray(new String[0]);
    	m_colours = colours.toArray(new Color[0]);
    	m_overlay = null;
    	if (has_overlay) {
    		m_overlay = new FloatArrayList(overlay);
    	}
    	
    	x_stats = make_stats(x_fal.toFloatArray(), "X");
    	y_stats = make_stats(y_fal.toFloatArray(), "Y");
    	z_stats = make_stats(z_fal.toFloatArray(), "Z");
    	
    	if (missing > 0) 
    		logger.warn("Some "+missing+" datapoints (rows) contain missing values, they will not be considered.");
    	if (bad > 0) 
    		logger.warn("Some "+bad+" datapoints (rows) contain invalid numeric data - they will not be considered.");
        return null;
    }

    private SummaryStatistics make_stats(float[] vals, String axis) {
    	SummaryStatistics ret = new SummaryStatistics();
    	for (float val : vals) {
    		// TODO BUG FIXME: handling of missing values???
    		ret.addValue(val);
    	}
    	logger.info(axis+" min="+ret.getMin()+" max="+ret.getMax()+" mean="+ret.getMean());
    	return ret;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_rows = null;
        m_colours = null;
        m_overlay = null;
        x_fal.clear();
        y_fal.clear();
        z_fal.clear();
        x_fal.trim();
        y_fal.trim();
        z_fal.trim();
        x_stats = null;
        y_stats = null;
        z_stats = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_x.saveSettingsTo(settings);
    	m_y.saveSettingsTo(settings);
    	m_z.saveSettingsTo(settings);
    	m_overlay_axis.saveSettingsTo(settings);
    	m_overlay_column.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_x.loadSettingsFrom(settings);
    	m_y.loadSettingsFrom(settings);
    	m_z.loadSettingsFrom(settings);
    	m_overlay_axis.loadSettingsFrom(settings);
    	m_overlay_column.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_x.validateSettings(settings);
        m_y.validateSettings(settings);
        m_z.validateSettings(settings);
        m_overlay_axis.validateSettings(settings);
    	m_overlay_column.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	reset();
    	File f = new File(internDir, "plot3d.internals");
    	
    	try {
	    	BufferedReader rdr = new BufferedReader(new FileReader(f));
	    	// 1. datapoints
	    	int n = Integer.valueOf(rdr.readLine());
	    	//System.out.println("Loading internals: "+n+" datapoints.");
	    	for (int i=0; i<n; i++) {
	    		String[] xyz = rdr.readLine().split("\\s+");
	    		x_fal.add(Float.valueOf(xyz[0]));
	    		y_fal.add(Float.valueOf(xyz[1]));
	    		z_fal.add(Float.valueOf(xyz[2]));
	    	}
	    	//System.out.println("Loaded "+x_fal.size()+" datapoints.");
	    	
	    	// 2. overlay data (if any)
	    	n = Integer.valueOf(rdr.readLine());
	    	//System.out.println("Loading internals: "+n+" overlay datapoints.");
	    	if (n > 0) {
	    		m_overlay = new FloatArrayList();
	    		for (int i=0; i<n; i++) {
	    			m_overlay.add(Float.valueOf(rdr.readLine()));
	    		}
	    	}
	    	//System.out.println("Loaded "+x_fal.size()+" overlay datapoints.");
	    	
	    	// 3. colours (if any)
	    	n = Integer.valueOf(rdr.readLine());
	    	//System.out.println("Loading internals: "+n+" colours.");

	    	if (n > 0) {
	    		m_colours = new Color[n];
	    		for (int i=0; i<n; i++) {
	    			String[] rgb = rdr.readLine().split(",");
	    			m_colours[i] = new Color(Float.valueOf(rgb[0]), Float.valueOf(rgb[1]), Float.valueOf(rgb[2]));
	    		}
	    	}
	    	rdr.close();
    	} catch (Exception ex) {
    		ex.printStackTrace();
    		throw new IOException(ex.getMessage());
    	}
    	
    	x_stats = make_stats(x_fal.toFloatArray(), "X");
    	y_stats = make_stats(y_fal.toFloatArray(), "Y");
    	z_stats = make_stats(z_fal.toFloatArray(), "Z");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	File f = new File(internDir, "plot3d.internals");
    	PrintWriter pw = new PrintWriter(new FileWriter(f));
    	
    	// 1. datapoints
    	pw.println(x_fal.size());
    	for (int i=0; i<x_fal.size(); i++) {
    		pw.println(""+x_fal.getFloat(i)+" "+y_fal.getFloat(i)+" "+z_fal.getFloat(i));
    	}
    	// 2. overlay data (if any)
    	if (m_overlay != null) {
	    	pw.println(m_overlay.size());
	    	for (int i=0; i<m_overlay.size(); i++) {
	    		pw.println(m_overlay.getFloat(i));
	    	}
    	} else {
    		pw.println("0");
    	}
    	// 3. colours (if any)
    	if (m_colours != null) {
    		pw.println(m_colours.length);
	    	for (int i=0; i<m_colours.length; i++) {
	    		pw.println(""+m_colours[i].r+","+m_colours[i].g+","+m_colours[i].b);
	    	}
    	} else {
    		pw.println("0");
    	}
    	pw.close();
    }

    /**
     * Returns true if there is something to plot and the internal state looks valid
     * @return
     */
	public boolean hasDataPoints() {
		return x_fal.size() > 0;
	}

	/**
	 * User want to overlay data on one of the axes?
	 */
	public boolean hasOverlay() {
		String ocol = m_overlay_column.getStringValue();
		return (ocol != null && ocol.length() > 0 && !ocol.equalsIgnoreCase("<none>"));
	}
	
	/**
	 * Return the axis to overlay data on: one of X, Y or Z. Only call this if <code>hasOverlay()</code> returns true.
	 */
	public String getOverlayAxis() {
		String colName = m_overlay_axis.getStringValue();
		return colName;
	}
	
	/**
	 * Return the data for the overlay. Will return null if <code>!hasOverlay()</code>
	 */
	public float[] getOverlay1DVector() {
		if (m_overlay == null)
			return null;
		return m_overlay.toArray(new float[0]);
	}
	
	/**
	 * Return the set of 3d points (which must be pre-sized to at least countDataPoints() in length)
	 * @param x
	 * @param y
	 * @param z
	 */
	public void getDataPoints(FloatArrayList x, FloatArrayList y, FloatArrayList z, Color[] colours) {
		assert(x != null && y != null && z != null);
		
		x.addAll(x_fal);
		y.addAll(y_fal);
		z.addAll(z_fal);
		int n = countDataPoints();
		for (int i=0; i<n; i++) {
			colours[i]= m_colours[i];
		}
	}
	
	public int countDataPoints() {
		return x_fal.size();
	}

	/**
	 * Returns the printable form of the column name as specified in the node configuration...
	 * @param x_y_or_z
	 * @return
	 */
	public String getAxis(final String x_y_or_z) {
		assert(x_y_or_z != null && x_y_or_z.length() == 1);
		
		if (x_y_or_z.toLowerCase().equals("x")) {
			return m_x.getStringValue();
		} else if (x_y_or_z.toLowerCase().equals("y")) {
			return m_y.getStringValue();
		} else {
			return m_z.getStringValue();
		}
	}

	public void getStatistics(SummaryStatistics x_stats,
			SummaryStatistics y_stats, SummaryStatistics z_stats) {
		// dont compute it again, just copy the internal state
		SummaryStatistics.copy(this.x_stats, x_stats);
		SummaryStatistics.copy(this.y_stats, y_stats);
		SummaryStatistics.copy(this.z_stats, z_stats);
	}

}

