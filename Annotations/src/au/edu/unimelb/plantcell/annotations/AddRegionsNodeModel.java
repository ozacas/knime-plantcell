package au.edu.unimelb.plantcell.annotations;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
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

import au.edu.unimelb.plantcell.core.cells.SequenceCell;



/**
 * This is the model implementation of RegionAnalyzer.
 * Various nodes for analysis of sequence regions * n
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AddRegionsNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Annotate Regions");
   
    // how to merge regions with an existing track (if any)?
    public final static String[] MERGE_SEMANTICS = new String[] { "Replace existing track", "Add to existing track", "Abort" };
   
    public final static String CFGKEY_SEQUENCE_COL     = "sequence-column";
    public final static String CFGKEY_REGION_LABEL_COL = "region-labels-from";
    public final static String CFGKEY_REGION_START_COL = "region-start-from";
    public final static String CFGKEY_REGION_END_COL   = "region-end-from";
    public final static String CFGKEY_TRACK_NAME       = "track-name";
    public final static String CFGKEY_MERGE            = "merge-if-existing-track?";
    
    
    private final SettingsModelString m_sequence = new SettingsModelString(CFGKEY_SEQUENCE_COL, "Annotated Sequence");
    private final SettingsModelString m_label    = new SettingsModelString(CFGKEY_REGION_LABEL_COL, "query id");
    private final SettingsModelString m_start    = new SettingsModelString(CFGKEY_REGION_START_COL, "q. start");
    private final SettingsModelString m_end      = new SettingsModelString(CFGKEY_REGION_END_COL, "q. end");
    private final SettingsModelString m_merge    = new SettingsModelString(CFGKEY_MERGE, MERGE_SEMANTICS[0]);
    private final SettingsModelString m_track_name=new SettingsModelString(CFGKEY_TRACK_NAME, "BLAST Hits");
    
    /**
     * Constructor for the node model.
     */
    protected AddRegionsNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        logger.info("Annotating regions from "+m_sequence.getStringValue());
        logger.info("Labels for each region will be obtained from "+m_label.getStringValue());
        logger.info("Regions in sequences will extend from "+m_start.getStringValue()+" to "+m_end.getStringValue());
        
        BufferedDataContainer c1 = exec.createDataContainer(make_output_spec(inData[0].getSpec()));
        
        c1.close();
        return new BufferedDataTable[]{ c1.getTable() };
    }

	private DataTableSpec make_output_spec(DataTableSpec spec) {
    	DataColumnSpec[] cols = new DataColumnSpec[1];
    	cols[0] = new DataColumnSpecCreator("Annotated Sequences", SequenceCell.TYPE).createSpec();
    
		return new DataTableSpec(spec, new DataTableSpec(cols));
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
        return new DataTableSpec[]{ make_output_spec(inSpecs[0]) };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_sequence.saveSettingsTo(settings);
        m_label.saveSettingsTo(settings);
        m_start.saveSettingsTo(settings);
        m_end.saveSettingsTo(settings);
        m_merge.saveSettingsTo(settings);
        m_track_name.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.loadSettingsFrom(settings);
        m_label.loadSettingsFrom(settings);
        m_start.loadSettingsFrom(settings);
        m_end.loadSettingsFrom(settings);
        m_merge.loadSettingsFrom(settings);
        m_track_name.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.validateSettings(settings);
        m_label.validateSettings(settings);
        m_start.validateSettings(settings);
        m_end.validateSettings(settings);
        m_merge.validateSettings(settings);
        m_track_name.validateSettings(settings);
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

