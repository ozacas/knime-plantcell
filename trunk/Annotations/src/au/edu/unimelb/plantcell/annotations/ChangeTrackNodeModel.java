package au.edu.unimelb.plantcell.annotations;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
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

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;



/**
 * This is the model implementation for track delete.
 * 
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class ChangeTrackNodeModel extends NodeModel {
  
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Track Delete");
        
    public final static String[] OPERATIONS = new String[] { "Rename track", "Delete track" };
    
    public final static String CFGKEY_TRACKS           = "track1";
    public final static String CFGKEY_ANNOTATIONS_FROM = "annotated-sequences";
    public final static String CFGKEY_OPERATION        = "change-operation";
    public final static String CFGKEY_NEWNAME          = "new-track-name";
    
    private final SettingsModelString m_tracks    = new SettingsModelString(CFGKEY_TRACKS, "");
    private final SettingsModelString m_sequence  = new SettingsModelString(CFGKEY_ANNOTATIONS_FROM, "Annotated Sequence");
    private final SettingsModelString m_operation = new SettingsModelString(CFGKEY_OPERATION, OPERATIONS[0]);
    private final SettingsModelString m_newname   = new SettingsModelString(CFGKEY_NEWNAME, "enter new name here");
    
    /**
     * Constructor for the node model.
     */
    protected ChangeTrackNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        logger.info(m_operation.getStringValue()+" from "+m_sequence.getStringValue());
          
        // 1. adjust column properties consistent with the operation to be performed
        DataTableSpec[] outSpec = make_output_spec(inData[0].getSpec());
        MyDataContainer      c1 = new MyDataContainer(exec.createDataContainer(outSpec[0]), "Row");
        
        // 2. adjust the track in each sequence accordingly
        int seq_idx = inData[0].getSpec().findColumnIndex(m_sequence.getStringValue());
        String op = m_operation.getStringValue().toLowerCase().trim();
    	RowIterator it = inData[0].iterator();
    	int       done = 0;
    	int    percent = inData[0].getRowCount();
    	
        if (op.startsWith("delete")) {
        	while (it.hasNext()) {
        		DataRow  r= it.next();
        		DataCell c= r.getCell(seq_idx);
        		SequenceValue sv = (SequenceValue) c;
        		DataCell[] cells = new DataCell[r.getNumCells()];
        		for (int i=0; i<cells.length; i++) {
        			if (i != seq_idx) {
        				cells[i] = r.getCell(i);
        			} else {
        				SequenceCell sc = new SequenceCell(sv);
        				sc.removeTrackByName(m_tracks.getStringValue());
        				cells[i] = sc;
        			}
        		}
        		c1.addRow(cells);
        		if (done++ % percent == 0) {
        			exec.checkCanceled();
        			exec.setProgress(((double)done) / percent);
        		}
        	}
        } else if (op.startsWith("rename")) {
        	while (it.hasNext()) {
        		DataRow        r = it.next();
        		DataCell       c = r.getCell(seq_idx);
        		SequenceValue sv = (SequenceValue) c;
        		DataCell[] cells = new DataCell[r.getNumCells()];
        		for (int i=0; i<cells.length; i++) {
        			if (i != seq_idx) {
        				cells[i] = r.getCell(i);
        			} else {
        				SequenceCell sc = new SequenceCell(sv);
        				Track         t = sc.getTrackByName(m_tracks.getStringValue(), null);
        				if (t != null) {
        					t.setName(m_newname.getStringValue());
        				}
        				cells[i] = sc;
        			}
        		}
        		c1.addRow(cells);
        		if (done++ % percent == 0) {
        			exec.checkCanceled();
        			exec.setProgress(((double)done) / percent);
        		}
        	}
        } else {
        	throw new InvalidSettingsException("Unknown operation: "+op);
        }
        
        // 3. all done...
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
    	
        return make_output_spec(inSpecs[0]);
    }

    private DataTableSpec[] make_output_spec(DataTableSpec inSpec) throws InvalidSettingsException {
        int seq_idx = inSpec.findColumnIndex(m_sequence.getStringValue());
        if (seq_idx < 0)
        	throw new InvalidSettingsException("Cannot find column: "+m_sequence.getStringValue()+", re-configure?");
        DataColumnProperties dcp = inSpec.getColumnSpec(seq_idx).getProperties();
        if (dcp == null)
        	throw new InvalidSettingsException("No tracks to operate on!");
        String op = m_operation.getStringValue().toLowerCase().trim();
        
        String key = TrackColumnPropertiesCreator.PLANTCELL_TRACKS + ":" + m_tracks.getStringValue();
    	if (!dcp.containsProperty(key))
    		throw new InvalidSettingsException("Cannot change missing track: "+key);
    	HashMap<String,String> map = new HashMap<String,String>();
    	Enumeration<String> keys = dcp.properties();
    	
        if (op.startsWith("delete")) {
        	while (keys.hasMoreElements()) {
        		String prop = keys.nextElement();
        		if (prop.equals(key))
        			continue;
        		map.put(prop, dcp.getProperty(prop));
        	}
        	DataColumnProperties new_dcp = new DataColumnProperties();
        	DataColumnProperties ret = new_dcp.cloneAndOverwrite(map);
        	
        	return make_new_spec(inSpec, ret, seq_idx);
        } else if (op.startsWith("rename")) {
        	while (keys.hasMoreElements()) {
        		String prop = keys.nextElement();
        		if (prop.equals(key)) {
        			String text = dcp.getProperty(prop);
        			Track t = new Track(Track.fromText(text));
        			t.setName(m_newname.getStringValue());
        			map.put(TrackColumnPropertiesCreator.PLANTCELL_TRACKS+":"+t.getName(), t.asText());
        			continue;
        		}
        		map.put(prop, dcp.getProperty(prop));
        	}
        	DataColumnProperties new_dcp = new DataColumnProperties();
        	DataColumnProperties ret     = new_dcp.cloneAndOverwrite(map);
        	return make_new_spec(inSpec, ret, seq_idx);
        } else {
        	throw new InvalidSettingsException("Unknown operation: "+op);
        }
	}

	private DataTableSpec[] make_new_spec(DataTableSpec inSpec, DataColumnProperties ret, int seq_idx) {
		assert(seq_idx >= 0 && ret != null && inSpec != null);
		DataColumnSpec[] cols = new DataColumnSpec[inSpec.getNumColumns()]; 
    	for (int i=0; i<cols.length; i++) {
    		if (i == seq_idx) {
    			// HACK BUG TODO: preserve domain, colours etc for the column...???
    			DataColumnSpecCreator seq_spec = new DataColumnSpecCreator(inSpec.getColumnSpec(i).getName(), SequenceCell.TYPE);
    			seq_spec.setProperties(ret);
    			cols[i] = seq_spec.createSpec();
    		} else {
    			cols[i] = inSpec.getColumnSpec(i);
    		}
    	}
    	return new DataTableSpec[] { new DataTableSpec(cols) };
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_tracks.saveSettingsTo(settings);
    	m_sequence.saveSettingsTo(settings);
    	m_operation.saveSettingsTo(settings);
    	m_newname.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_tracks.loadSettingsFrom(settings);
    	m_sequence.loadSettingsFrom(settings);
    	m_operation.loadSettingsFrom(settings);
    	m_newname.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_tracks.validateSettings(settings);
    	m_sequence.validateSettings(settings);
    	m_operation.validateSettings(settings);
    	m_newname.validateSettings(settings);
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

