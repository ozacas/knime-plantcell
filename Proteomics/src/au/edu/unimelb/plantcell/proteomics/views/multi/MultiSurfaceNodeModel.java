package au.edu.unimelb.plantcell.proteomics.views.multi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelFilterString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.la4j.factory.CRSFactory;
import org.la4j.io.MatrixMarketStream;
import org.la4j.io.MatrixStream;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.functor.MatrixFunction;

import au.edu.unimelb.plantcell.proteomics.views.MaximumMatrixProcedure;
import au.edu.unimelb.plantcell.proteomics.views.MinimumMatrixProcedure;


/**
 * This is the model implementation of MultiSurface.
 * Represents multiple surfaces each with different properties, surface datapoints are taken from the input data table.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MultiSurfaceNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Multi Surface");
        
    static final String CFGKEY_X = "X";
    static final String CFGKEY_Y = "Y";
    static final String CFGKEY_Z = "Z";
    
    private final SettingsModelString       m_x = new SettingsModelString(CFGKEY_X, "");
    private final SettingsModelString       m_y = new SettingsModelString(CFGKEY_Y, "");
    private final SettingsModelFilterString m_z = new SettingsModelFilterString(CFGKEY_Z);
    

    // private state (persisted)
    private final HashMap<String,Matrix> m_surfaces = new HashMap<String,Matrix>();
    private double x_min, x_max, y_min, y_max;		// NB: each Z column may have its own bounds, so we dont cache that here
    // private state (not persisted)
    private final HashMap<String,Matrix> m_matrix_cache = new HashMap<String,Matrix>();
    
    /**
     * Constructor for the node model.
     */
    protected MultiSurfaceNodeModel() {
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
   
        logger.info("Constructing "+getSurfaceCount()+" surfaces.");
        reset();
        
        int x_idx = inData[0].getSpec().findColumnIndex(m_x.getStringValue());
    	if (x_idx < 0)
    		throw new InvalidSettingsException("Cannot find column: "+m_x.getStringValue()+" - reconfigure?");
    	int y_idx = inData[0].getSpec().findColumnIndex(m_y.getStringValue());
    	if (y_idx < 0)
    		throw new InvalidSettingsException("Cannot find column: "+m_y.getStringValue()+" - reconfigure?");
    	
    	DataColumnDomain x_domain = inData[0].getSpec().getColumnSpec(x_idx).getDomain();
    	DataColumnDomain y_domain = inData[0].getSpec().getColumnSpec(y_idx).getDomain();
    	if (!x_domain.hasBounds() || !y_domain.hasBounds()) {
    		logger.warn("Missing lower or upper bounds on X/Y columns - recomputing from row data... please be patient.");
    		x_min = Double.POSITIVE_INFINITY;
    		x_max = Double.NEGATIVE_INFINITY;
    		y_min = Double.POSITIVE_INFINITY;
    		y_max = Double.NEGATIVE_INFINITY;
    		for (DataRow r : inData[0]) {
    			DataCell x_cell = r.getCell(x_idx);
        		DataCell y_cell = r.getCell(y_idx);
        		double x = ((DoubleValue)x_cell).getDoubleValue();
        		double y = ((DoubleValue)y_cell).getDoubleValue();
        		if (x <= x_min)
        			x_min = x;
        		if (x >= x_max)
        			x_max = x;
        		if (y <= y_min)
        			y_min = y;
        		if (y >= y_max)
        			y_max = y;
    		}
    	} else {
	    	x_min = ((DoubleValue)x_domain.getLowerBound()).getDoubleValue();
	    	x_max = ((DoubleValue)x_domain.getUpperBound()).getDoubleValue();
	    	y_min = ((DoubleValue)y_domain.getLowerBound()).getDoubleValue();
	    	y_max = ((DoubleValue)y_domain.getUpperBound()).getDoubleValue();
    	}
    	
        for (String z_col : m_z.getIncludeList()) {
        	int n_rows = 10000;
        	int n_cols = 10000;
        	Matrix m = new CRSFactory().createMatrix(n_rows, n_cols);	// sparse so dont panic (yet!) ;-)
        	int z_idx = inData[0].getSpec().findColumnIndex(z_col);
        	if (z_idx < 0)
        		throw new InvalidSettingsException("Cannot find column: "+z_col+" - reconfigure?");
        	double z_min = ((DoubleValue)inData[0].getSpec().getColumnSpec(z_idx).getDomain().getLowerBound()).getDoubleValue();
        	double z_max = ((DoubleValue)inData[0].getSpec().getColumnSpec(z_idx).getDomain().getUpperBound()).getDoubleValue();
        	logger.info("Loading surface {"+m_x.getStringValue()+", "+m_y.getStringValue()+", "+z_col+"}");
        	logger.info("Bounds: X ["+x_min+".."+x_max+"] Y["+y_min+".."+y_max+"] Z["+z_min+".."+z_max+"]");
        	
        	int min_x_bin = Integer.MAX_VALUE;
        	int max_x_bin = Integer.MIN_VALUE;
        	int min_y_bin = Integer.MAX_VALUE;
        	int max_y_bin = Integer.MIN_VALUE;
        	for (DataRow r : inData[0]) {
        		DataCell x_cell = r.getCell(x_idx);
        		DataCell y_cell = r.getCell(y_idx);
        		DataCell z_cell = r.getCell(z_idx);
        	
        		int x_bin = getBin(x_cell, x_min, x_max, n_rows);
        		int y_bin = getBin(y_cell, y_min, y_max, n_cols);
        		
        		if (x_bin < 0 || y_bin < 0 || z_cell == null || z_cell.isMissing())
        			continue;
        		double z = ((DoubleValue)z_cell).getDoubleValue();
        		if (x_bin < min_x_bin)
        			min_x_bin = x_bin;
        		if (x_bin > max_x_bin)
        			max_x_bin = x_bin;
        		if (y_bin < min_y_bin) 
        			min_y_bin = y_bin;
        		if (y_bin > max_y_bin)
        			max_y_bin = y_bin;
        		m.set(x_bin, y_bin, z);
        	}
        	logger.info("Set bin ranges: X ["+min_x_bin+", "+max_x_bin+"] Y["+min_y_bin+", "+max_y_bin+"]");
        	m_surfaces.put(z_col, m);
        }
       
        return new BufferedDataTable[]{};
    }

    private int getBin(DataCell cell, double min, double max, int max_bin) {
		if (cell == null || cell.isMissing())
			return -1;
		double val = ((DoubleValue)cell).getDoubleValue();
		if (val < min || val > max)
			return -1;
		double range = range(min, max);
		if (range <= 0.0)
			return -1;
		float range_per_bin = ((float)range) / max_bin;
		int bin = (int) ((val - min) / range_per_bin);
		if (bin >= max_bin)
			bin = max_bin - 1;
		return bin;
	}

    private double range(double x_min, double x_max) {
    	if ((x_min < 0.0 && x_max < 0.0) || (x_max >= 0.0 && x_min >= 0.0)) {
    		return Math.abs(x_max) - Math.abs(x_min);
    	} else {
    		return Math.abs(x_min) + Math.abs(x_max);
    	}
	}
    
	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	m_surfaces.clear();
    	m_matrix_cache.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_x.saveSettingsTo(settings);
    	m_y.saveSettingsTo(settings);
    	m_z.saveSettingsTo(settings);
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
    	
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	reset();
    	
    	File f = new File(internDir, "multi.surface.internals");
    	BufferedReader rdr = new BufferedReader(new FileReader(f));
    	
    	HashMap<String,File> matrix_files = new HashMap<String,File>();
    	int n = Integer.valueOf(rdr.readLine());
    	x_min = Double.valueOf(rdr.readLine());
    	x_max = Double.valueOf(rdr.readLine());
    	y_min = Double.valueOf(rdr.readLine());
    	y_max = Double.valueOf(rdr.readLine());
    	
    	for (int i=0; i<n; i++) {
    		String line = rdr.readLine();
    		int idx = line.indexOf('=');
    		
    		String fname = line.substring(0, idx);
    		String surface = line.substring(idx+1);
    		matrix_files.put(surface, new File(internDir, fname));
    	}
    	rdr.close();
    	
    	for (String surface : matrix_files.keySet()) {
    		File matrixf = matrix_files.get(surface);
    		MatrixStream ms = new MatrixMarketStream(new FileInputStream(matrixf));
        	Matrix m = ms.readMatrix();
        	m_surfaces.put(surface, m);
    	}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	File f = new File(internDir, "multi.surface.internals");
    	PrintWriter pw = new PrintWriter(new FileWriter(f));
    	HashMap<String,File> matrix_files = new HashMap<String,File>();
    	pw.println(m_surfaces.size());
    	pw.println(x_min);
    	pw.println(x_max);
    	pw.println(y_min);
    	pw.println(y_max);
    	
    	int idx = 1;
    	for (String surface : m_surfaces.keySet()) {
    		String fname = "matrix."+idx++;
    		matrix_files.put(surface, new File(internDir, fname));
    		pw.println(fname+"="+surface);
    	}
    	pw.close();
    	
    	for (String surface : matrix_files.keySet()) {
    		Matrix m = getMatrix(surface, "Linear");
    		File matrixf = matrix_files.get(surface);
    		MatrixStream ms = new MatrixMarketStream(new FileOutputStream(matrixf));
        	ms.writeMatrix(m);
    	}
    }

    /**
     * As computed during execute() - minimal X value encountered on all surfaces
     * @return
     */
    public double getXMin() {
    	return x_min;
    }
    
    /**
     * As computed during execute() - maximum X value encountered on all surfaces
     * @return
     */
    public double getXMax() {
    	return x_max;
    }
    
    /**
     * As computed during execute() - minimal Y value encountered on all surfaces
     * @return
     */
    public double getYMin() {
    	return y_min;
    }
    
    /**
     * As computed during execute() - maximal Y value encountered on all surfaces
     * @return
     */
    public double getYMax() {
    	return y_max;
    }
    
    private boolean isCachedMatrix(final String surface, final String transform) {
    	if (m_matrix_cache.containsKey(transform+ " "+surface)) {
    		return true;
    	}
    	// if transform is linear then the original matrix can be returned...
    	if (transform.startsWith("None")) {
    		return true;
    	}
    	return false;
    }
    
    public Matrix getCachedMatrix(final String surface, final String transform) {
    	Matrix m = m_matrix_cache.get(transform + " " + surface);
    	if (m != null)
    		return m;
    	if (transform.startsWith("None")) {
    		return m_surfaces.get(surface);
    	}
    	return null;
    }
    
    public void deleteExistingCachedMatrix(final String surface) {
    	ArrayList<String> candidates = new ArrayList<String>();
    	for (String s : m_matrix_cache.keySet()) {
    		if (s.endsWith(surface)) {
    			candidates.add(s);
    		}
    	}
    	
    	for (String s : candidates) {
    		m_matrix_cache.remove(s);
    	}
    }
    
    public Matrix getMatrix(final String surface_name, final String transform) {
    	Matrix m = m_surfaces.get(surface_name);
    	if (m == null)
    		return null;
    	Matrix t = null;
    	
    	// apply transform (nothing is done iff None or something unsupported)
    	if (isCachedMatrix(surface_name, transform)) 
    		t = getCachedMatrix(surface_name, transform);
    	else {
    		deleteExistingCachedMatrix(surface_name);		// free up memory before we start building new matrices
    	
	    	if (transform.startsWith("Log10")) {
				t = m.copy().transform(new MatrixFunction() {
	
					@Override
					public double evaluate(int r, int c, double val) {
						return (val > 0.0) ? Math.log(val) : 0.0;
					}
					
				});
			} else if (transform.startsWith("Recip")) {
				t = m.copy().transform(new MatrixFunction() {
	
					@Override
					public double evaluate(int r, int c, double val) {
						return (val > 0.0) ? 1/val : 0.0;
					}
					
				});
			} else if (transform.startsWith("Square")) {
				t = m.copy().transform(new MatrixFunction() {
	
					@Override
					public double evaluate(int r, int c, double val) {
						return (val > 0.0) ? Math.sqrt(val) : 0.0;
					}
					
				});
			}
	    	
	    	
    	}
    	
    	// return transformed ***copy*** computed above?
    	if (t != null) {
    		m_matrix_cache.put(transform+" "+surface_name, t);
    		return t;
    	}
    	// else ***copy*** of the original matrix
    	return m.copy();
    }
    
    public double getMinimum(final String surface_name) {
    	return getMinimum(surface_name, "None");
    }
    
    public double getMinimum(final String surface_name, final String transform) {
    	Matrix m = getMatrix(surface_name, transform);
    	MinimumMatrixProcedure mp = new MinimumMatrixProcedure();
    	m.each(mp);
    	return mp.min;
    }
    
    public double getMaximum(final String surface_name) {
    	return getMaximum(surface_name, "None");
    }
    
    private double getMaximum(final String surface_name, final String transform) {
		Matrix m = getMatrix(surface_name, transform);
		MaximumMatrixProcedure mp = new MaximumMatrixProcedure();
		m.each(mp);
		return mp.max;
	}

	public String getXLabel() {
    	return m_x.getStringValue();
    }
    
    public String getYLabel() {
    	return m_y.getStringValue();
    }
    
    public Collection<String> getZNames() {
    	return m_z.getIncludeList();
    }
    
	public int getSurfaceCount() {
		return m_z.getIncludeList().size();
	}

}

