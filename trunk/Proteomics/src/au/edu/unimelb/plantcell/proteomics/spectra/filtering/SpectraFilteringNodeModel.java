package au.edu.unimelb.plantcell.proteomics.spectra.filtering;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.expasy.jpl.core.mol.chem.MassCalculator;
import org.expasy.jpl.core.ms.spectrum.editor.AbstractPeakListEditor;
import org.expasy.jpl.core.ms.spectrum.filter.AbstractPeakListFilter;
import org.expasy.jpl.core.ms.spectrum.filter.IntensityThresholdFilter;
import org.expasy.jpl.core.ms.spectrum.filter.NHighestPeaksFilter;
import org.expasy.jpl.core.ms.spectrum.filter.NPeakGroupsPerWindowFilter;
import org.expasy.jpl.core.ms.spectrum.filter.PrecursorFilter;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * This is the model implementation of spectra correlation based on code found in javaprotlib
 * 
 * @author Andrew Cassin
 */
public class SpectraFilteringNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Spectra Filtering");
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_SPECTRA              = "spectra-column";
	static final String CFGKEY_METHOD               = "filtering-method";
	static final String CFGKEY_KEEP_N               = "keep-n-peaks";
	static final String CFGKEY_WINDOW_SIZE          = "window-size";
	static final String CFGKEY_TOLERANCE            = "mz-tolerance";
	
	public static final String[] METHODS = new String[] { "Remove precursor peaks (monoisotopic)", 
													"Retain 95% of the highest intensity peaks",
													"Top 10 most intense peaks", "Top 100 most intense peaks",
													"Keep highest N peaks per window of size Y",
													"Normalise to highest intensity peak", "Normalise to lowest intensity peak",
													"Normalise to total peak intensity", "Log-transform peak intensities", 
													"Transform peak intensities to ranks", "Square-root transform peak intensities"
												};
	static {
		Arrays.sort(SpectraFilteringNodeModel.METHODS);
	}
	
    // persisted node state
    private final SettingsModelString m_col            = new SettingsModelString(CFGKEY_SPECTRA, "");
    private final SettingsModelString m_method         = new SettingsModelString(CFGKEY_METHOD, METHODS[0]);
    private final SettingsModelIntegerBounded m_keep_n = new SettingsModelIntegerBounded(CFGKEY_KEEP_N, 3, 1, 100000000);
    private final SettingsModelDouble m_window_size    = new SettingsModelDouble(CFGKEY_WINDOW_SIZE, 50.0);
    private final SettingsModelDouble m_tolerance      = new SettingsModelDouble(CFGKEY_TOLERANCE, 0.05);
    
    /**
     * Constructor for the node model.
     */
    protected SpectraFilteringNodeModel() {
        // one incoming port: the spectra to be filtered and one outgoing port
        super(1, 1);
        if (m_method.getStringValue().startsWith("Keep highest N")) {
        	m_keep_n.setEnabled(true);
        	m_window_size.setEnabled(true);
        } else {
        	m_keep_n.setEnabled(false);
        	m_window_size.setEnabled(false);
        }
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	final int idx = inData[0].getSpec().findColumnIndex(m_col.getStringValue());
    	if (idx < 0)
    		throw new InvalidSettingsException("Cannot find column: "+m_col.getStringValue()+" - reconfigure?");
    	
    	
    	ColumnRearranger rearranger = new ColumnRearranger(inData[0].getSpec());
    	BufferedDataTable out = null;
    	if (m_method.getStringValue().toLowerCase().indexOf("transform") < 0) {
    		final AbstractPeakListFilter filter = make_filter(m_method.getStringValue());
    		MyFilterCellFactory cf = new MyFilterCellFactory(idx, filter, m_method.getStringValue());
    		rearranger.append(cf);
    		out = exec.createColumnRearrangeTable(inData[0], rearranger, exec.createSubProgress(1.0));
        	
        	if (cf.getFailed() > 0) {
        		logger.warn("Failed to process "+cf.getFailed()+" spectra ie. missing values present in output table. Problem with the data?");
        	}
    	} else {
    		final AbstractPeakListEditor editor = make_editor(m_method.getStringValue());
    		MyEditorCellFactory cf = new MyEditorCellFactory(idx, editor, m_method.getStringValue());
    		rearranger.append(cf);
    		out = exec.createColumnRearrangeTable(inData[0], rearranger, exec.createSubProgress(1.0));
    	}
    	
    	return new BufferedDataTable[] {out};
    }

	private AbstractPeakListEditor make_editor(String stringValue) {
		// TODO Auto-generated method stub
		return null;
	}


	private AbstractPeakListFilter make_filter(String meth) {
		AbstractPeakListFilter filter = null;
		if (meth.startsWith("Remove precursor peaks")) {
			filter = PrecursorFilter.newInstance(m_tolerance.getDoubleValue(), m_tolerance.getDoubleValue());
			((PrecursorFilter)filter).setMassCalc(MassCalculator.getMonoAccuracyInstance());
		} else if (meth.startsWith("Top 10 most intense")) {
			filter = new NHighestPeaksFilter(10.0d);
		} else if (meth.startsWith("Top 100 most intense")) {
			filter = new NHighestPeaksFilter(100.0d);
		} else if (meth.startsWith("Retain 95%")) {
			filter = new NHighestPeaksFilter(0.95d);
		} else if (meth.startsWith("Keep highest N")) {
			filter = new NPeakGroupsPerWindowFilter(m_keep_n.getIntValue(), m_window_size.getDoubleValue(), m_tolerance.getDoubleValue());
		} else if (meth.startsWith("Normalise")) {
			filter = new NormalisationFilter(meth);
		} else {
			logger.warn("Unknown filter method: "+meth+". Assuming intensity threshold of 5 units");
			filter = new IntensityThresholdFilter(5);
		}
		
		return filter;
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
    	return null;
    }


	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_col.saveSettingsTo(settings);
        m_method.saveSettingsTo(settings);
        m_keep_n.saveSettingsTo(settings);
        m_window_size.saveSettingsTo(settings);
        m_tolerance.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_col.loadSettingsFrom(settings);
        m_method.loadSettingsFrom(settings);
        m_keep_n.loadSettingsFrom(settings);
        m_window_size.loadSettingsFrom(settings);
        m_tolerance.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_col.validateSettings(settings);
        m_method.validateSettings(settings);
        m_keep_n.validateSettings(settings);
        m_window_size.validateSettings(settings);
        m_tolerance.validateSettings(settings);
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

