package au.edu.unimelb.plantcell.annotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.cells.SequenceAnnotation;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.regions.RegionInterface;



/**
 * This is the model implementation of RegionAnalyzer.
 * Various nodes for analysis of sequence regions * n
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ExtractTrackNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Track 2 Columns");
        
    public final static String CFGKEY_SEQUENCE = "sequence-column";
    public final static String CFGKEY_TRACK    = "track";
    
    private final SettingsModelString m_sequence = new SettingsModelString(CFGKEY_SEQUENCE, "");
    private final SettingsModelString m_track    = new SettingsModelString(CFGKEY_TRACK, "");
    
    /**
     * Constructor for the node model.
     */
    protected ExtractTrackNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	logger.info("Extracting features from track "+m_track.getStringValue()+" in column: "+m_sequence.getStringValue());
    
    	int seq_idx = inData[0].getSpec().findColumnIndex(m_sequence.getStringValue());
    	if (seq_idx < 0)
    		throw new InvalidSettingsException("Cannot find sequence column: "+m_sequence.getStringValue()+": reconfigure?");
    	
    	boolean made_table_spec = false;
    	RowIterator it = inData[0].iterator();
    	MyDataContainer c1 = null;
    	
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		DataCell c = r.getCell(seq_idx);
    		if (c == null || c.isMissing())
    			continue;
    		SequenceValue sv = (SequenceValue) c;
    		Track t = sv.getTrackByName(m_track.getStringValue(), null);
    		if (t == null)
    			continue;
    		
    		if (!made_table_spec) {
    			DataTableSpec outSpec = make_output_spec(inData[0].getSpec(), t);
    	    	c1 = new MyDataContainer(exec.createDataContainer(outSpec), "Row");
    	    	made_table_spec = true;
    		}
    		
    		SequenceAnnotation sa = t.getAnnotation();
    		for (RegionInterface ri : sa.getRegions()) {
    			Map<String,DataCell> cells = ri.asCells(t.getName());
    			DataCell[] dc = new DataCell[c1.getTableSpec().getNumColumns()];
    			for (int i=0; i<dc.length; i++) {
    				dc[i] = DataType.getMissingCell();
    			}
    			dc[0] = new StringCell(sv.getID());
    			SequenceValue feature = ri.getFeatureSequence(sv);
    			if (feature != null)
    				dc[1] = new SequenceCell(feature);
    			
    			for (String key : cells.keySet()) {
    				int idx = c1.getTableSpec().findColumnIndex(key);
    				if (idx >= 0)
    					dc[idx] = cells.get(key);
    			}
    			c1.addRow(dc);
    		}
    	}
    	
    	// if no input rows, then made_table_spec will still be false so...
    	if (!made_table_spec) {
    		DataTableSpec outSpec = make_output_spec(inData[0].getSpec(), null);
	    	c1 = new MyDataContainer(exec.createDataContainer(outSpec), "Row");
    	}
        return new BufferedDataTable[]{c1.close()};
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


	private DataTableSpec make_output_spec(DataTableSpec inSpec, Track t) {
		assert(inSpec != null && t != null);
		SequenceAnnotation sa = null;
		if (t != null) 	{ // need to handle case where no rows have a suitable track!
			 sa = t.getAnnotation();
		}
		
		ArrayList<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
		cols.add(new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator("Feature (sequence subset)", SequenceCell.TYPE).createSpec());
		if (sa != null) {
			cols.addAll(sa.asColumnSpec(t.getName()));
		}
		return new DataTableSpec(cols.toArray(new DataColumnSpec[0]));
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_sequence.saveSettingsTo(settings);
    	m_track.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.loadSettingsFrom(settings);
    	m_track.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.validateSettings(settings);
    	m_track.validateSettings(settings);
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

