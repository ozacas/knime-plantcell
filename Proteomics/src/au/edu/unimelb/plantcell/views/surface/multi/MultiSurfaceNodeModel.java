package au.edu.unimelb.plantcell.views.surface.multi;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import org.knime.core.data.DataCell;
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
import org.la4j.matrix.Matrix;


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
    

    // private state
    private final HashMap<String,Matrix> m_surfaces = new HashMap<String,Matrix>();
    private double x_min, x_max, y_min, y_max;		// NB: each Z column may have its own bounds, so we dont cache that here
    
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
    	
    	// TODO: check that bounds are present?
    	double x_min = ((DoubleValue)inData[0].getSpec().getColumnSpec(x_idx).getDomain().getLowerBound()).getDoubleValue();
    	double x_max = ((DoubleValue)inData[0].getSpec().getColumnSpec(x_idx).getDomain().getUpperBound()).getDoubleValue();
    	double y_min = ((DoubleValue)inData[0].getSpec().getColumnSpec(y_idx).getDomain().getLowerBound()).getDoubleValue();
    	double y_max = ((DoubleValue)inData[0].getSpec().getColumnSpec(y_idx).getDomain().getUpperBound()).getDoubleValue();
    	
    	
        for (String z_col : m_z.getIncludeList()) {
        	Matrix m = new CRSFactory().createMatrix(10000, 10000);	// sparse so dont panic (yet!) ;-)
        	int z_idx = inData[0].getSpec().findColumnIndex(z_col);
        	if (z_idx < 0)
        		throw new InvalidSettingsException("Cannot find column: "+z_col+" - reconfigure?");
        	double z_min = ((DoubleValue)inData[0].getSpec().getColumnSpec(z_idx).getDomain().getLowerBound()).getDoubleValue();
        	double z_max = ((DoubleValue)inData[0].getSpec().getColumnSpec(z_idx).getDomain().getUpperBound()).getDoubleValue();
        	double z_range = range(z_min, z_max);
        	logger.info("Loading surface {"+m_x.getStringValue()+", "+m_y.getStringValue()+", "+z_col+"}");
        	logger.info("Bounds: X ["+x_min+".."+x_max+"] Y["+y_min+".."+y_max+"] Z["+z_min+".."+z_max+"]");
        	
        	for (DataRow r : inData[0]) {
        		DataCell x_cell = r.getCell(x_idx);
        		DataCell y_cell = r.getCell(y_idx);
        		DataCell z_cell = r.getCell(z_idx);
        	
        		int x_bin = getBin(x_cell, x_min, x_max, m.rows());
        		int y_bin = getBin(y_cell, y_min, y_max, m.columns());
        		
        		if (x_bin < 0 || y_bin < 0 || z_cell == null || z_cell.isMissing())
        			continue;
        		double z = ((DoubleValue)z_cell).getDoubleValue();
        		m.set(x_bin, y_bin, z);
        	}
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
		int bin = (int) (val - min / range);
		if (bin >= max_bin-1)
			bin = max_bin-1;
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
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    public double getXMin() {
    	return x_min;
    }
    
    public double getXMax() {
    	return x_max;
    }
    
    public double getYMin() {
    	return y_min;
    }
    
    public double getYMax() {
    	return y_max;
    }
    
    public Matrix getMatrix(String surface_name) {
    	return m_surfaces.get(surface_name).copy();
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

