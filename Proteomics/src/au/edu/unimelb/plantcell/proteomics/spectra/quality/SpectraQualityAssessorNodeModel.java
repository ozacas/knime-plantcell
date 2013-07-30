package au.edu.unimelb.plantcell.proteomics.spectra.quality;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;


/**
 * This is the model implementation of SpectraQualityAssessor.
 * Implements the 'Xrea' algorithm in the paper entitled "Quality Assessment of Tandem Mass Spectra Based on Cumulative Intensity Normalization" in the journal of proteome research. May implement other algorithms at a future date.
 *
 * @author Andrew Cassin
 */
public class SpectraQualityAssessorNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Quality rank Spectra");
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_SPECTRA              = "spectra-column";
	static final String CFGKEY_ADJUSTMENT_THRESHOLD = "xrea-adjustment-threshold";		// 85% TIC threshold before Xrea adjustment takes place
	static final String CFGKEY_ADJUSTMENT_PEAKS     = "xrea-adjustment-peaks";			// no more than 5 peaks must comprise xrea-adjustment-threshold
	
    /** initial default count value. */
    static final String DEFAULT_SPECTRA = "Spectra";
   
    private final SettingsModelString m_spectra              = new SettingsModelString(CFGKEY_SPECTRA, DEFAULT_SPECTRA);
    private final SettingsModelDoubleBounded m_adj_threshold = new SettingsModelDoubleBounded(CFGKEY_ADJUSTMENT_THRESHOLD, 0.85, 0.0, 1.0);
    private final SettingsModelIntegerBounded m_adj_peaks    = new SettingsModelIntegerBounded(CFGKEY_ADJUSTMENT_PEAKS, 10, 0, 100);

    /**
     * Constructor for the node model.
     */
    protected SpectraQualityAssessorNodeModel() {
        // one incoming port and one outgoing port
        super(1, 1);
    }

    private ColumnRearranger createColumnRearranger(DataTableSpec in) {
    	ColumnRearranger c = new ColumnRearranger(in);
    	final int index = in.findColumnIndex(m_spectra.getStringValue());
    	
    	DataColumnSpec newColSpec = new DataColumnSpecCreator("Xrea Value", DoubleCell.TYPE).createSpec();
    	DataColumnSpec bigPeakSpec= new DataColumnSpecCreator("Dominant Peak Adjusted Xrea Value", DoubleCell.TYPE).createSpec();
    	
    
    	CellFactory cf = new SingleCellFactory(newColSpec) {
    		public DataCell getCell(DataRow r) {
    			if (index<0)
    				return DataType.getMissingCell();
    			DataCell c = r.getCell(index);
    			if (c.isMissing() || !(c instanceof SpectraValue))
    				return DataType.getMissingCell();
    			SpectraValue spectrum = (SpectraValue) c;
    			if (spectrum.getMSLevel() < 2)
    				return DataType.getMissingCell();
    			
    			return new DoubleCell(new XreaScore(spectrum).getRawQualityScore());	
    		}
    	};
    	
    	CellFactory cf2 = new SingleCellFactory(bigPeakSpec) {
    		public DataCell getCell(DataRow r) {
    			DataCell c = r.getCell(index);
    			if (c.isMissing() || !(c instanceof SpectraValue)) {
    				return DataType.getMissingCell();
    			}
    			SpectraValue spectrum = (SpectraValue) c;
    			if (spectrum.getMSLevel() < 2)
    				return DataType.getMissingCell();
    			
    			return new DoubleCell(new XreaScore(spectrum).getAdjustedQualityScore(m_adj_threshold.getDoubleValue(), m_adj_peaks.getIntValue()));
    		}
    	};
    	c.append(cf);
    	c.append(cf2);
    	
    	return c;
    }
   
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	ColumnRearranger c = createColumnRearranger(inData[0].getDataTableSpec());
    	BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], c, exec);
    	logger.info("Processed "+inData[0].getRowCount()+" rows.");
    	return new BufferedDataTable[] {out};
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
        
    	DataColumnSpec c = inSpecs[0].getColumnSpec(m_spectra.getStringValue());
    	if (c==null || !c.getType().isCompatible(SpectraValue.class)) {
    		throw new InvalidSettingsException("No suitable spectra column found!");
    	}
    	ColumnRearranger cr = createColumnRearranger(inSpecs[0]);
    	DataTableSpec result = cr.createSpec();
    	return new DataTableSpec[] {result};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_spectra.saveSettingsTo(settings);
        m_adj_threshold.saveSettingsTo(settings);
        m_adj_peaks.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_spectra.loadSettingsFrom(settings);
        m_adj_threshold.loadSettingsFrom(settings);
        m_adj_peaks.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_spectra.validateSettings(settings);
        m_adj_threshold.validateSettings(settings);
        m_adj_peaks.validateSettings(settings);
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

