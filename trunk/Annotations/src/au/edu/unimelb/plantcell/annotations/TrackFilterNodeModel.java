package au.edu.unimelb.plantcell.annotations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;
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
import au.edu.unimelb.plantcell.core.cells.CoordinateSystem;
import au.edu.unimelb.plantcell.core.cells.SequenceAnnotation;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.core.cells.TrackCreator;
import au.edu.unimelb.plantcell.core.regions.RegionInterface;
import au.edu.unimelb.plantcell.core.regions.RegionsAnnotation;



/**
 * Implements selection of regions of interest (and sequences with those regions of interest!) as defined by the dialog
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class TrackFilterNodeModel extends NodeModel {
    public final static String FILTER_HIT_TRACK_A = "TrackFilter:hits(A)";
    public final static String FILTER_HIT_TRACK_B = "TrackFilter:hits(B)";
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Track Filter");
        
    public final static String CFGKEY_OPERATION = "operation";
    public final static String CFGKEY_TRACK1    = "track1";
    public final static String CFGKEY_TRACK2    = "track2";
    public final static String CFGKEY_ANNOTATIONS_FROM = "annotated-sequences";
    
    private final SettingsModelString m_operation= new SettingsModelString(CFGKEY_OPERATION, "intersection");
    private final SettingsModelString m_track1   = new SettingsModelString(CFGKEY_TRACK1, "");
    private final SettingsModelString m_track2   = new SettingsModelString(CFGKEY_TRACK2, "");
    private final SettingsModelString m_sequence = new SettingsModelString(CFGKEY_ANNOTATIONS_FROM, "Annotated Sequence");
   
    // not-persisted state
    private final HashMap<String,Integer> m_colnames2idx = new HashMap<String,Integer>();		// created by make_output_spec()
    
    /**
     * Constructor for the node model.
     */
    protected TrackFilterNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        logger.info("Identifying "+m_operation.getStringValue()+" in "+m_sequence.getStringValue());
        logger.info("Processing tracks: A="+m_track1.getStringValue()+", B="+m_track2.getStringValue());
        
        int seq_idx = inData[0].getSpec().findColumnIndex(m_sequence.getStringValue());
        if (seq_idx < 0)
        	throw new InvalidSettingsException("Cannot find column: "+m_sequence.getStringValue()+", re-configure?");
        
        boolean made_output_spec = false;
        MyDataContainer c1 = null;
        MyDataContainer c2 = null;
        
        RowIterator it = inData[0].iterator();
        int done = 0;
        int rows_with_missing_tracks = 0;
        while (it.hasNext()) {
        	DataRow r = it.next();
        	DataCell seq_cell = r.getCell(seq_idx);
        	if (seq_cell == null || seq_cell.isMissing())
        		continue;
        	
        	SequenceValue sv = (SequenceValue) seq_cell;
        	Track t1 = sv.getTrackByName(m_track1.getStringValue(), null);
        	Track t2 = sv.getTrackByName(m_track2.getStringValue(), null);
        	if (t1 == null || !t1.hasAnnotation() ||
        			t2 == null || !t2.hasAnnotation() ||
        			!t1.hasRegions() || !t2.hasRegions()) {
        		//logger.warn("No regions of interest for row "+r.getKey().getString()+", ignored.");
        		rows_with_missing_tracks++;
        		continue;
        	}
        	SequenceAnnotation   sa1 = t1.getAnnotation();
        	SequenceAnnotation   sa2 = t2.getAnnotation();
        	//logger.info("Found "+a1.countAnnotations()+" regions in A in row "+r.getKey().getString());
        	//logger.info("Found "+a2.countAnnotations()+" regions in B in row "+r.getKey().getString());
        	
        	// here we cannot make the output table until we get the first row since we dont know what the annotations look like
        	// until then
        	if (!made_output_spec) {
                DataTableSpec[] outSpec = make_output_spec(inData[0].getSpec(),
                		inData[0].getSpec().getColumnSpec(seq_idx).getProperties(), sa1, sa2);
                c1 = new MyDataContainer(exec.createDataContainer(outSpec[0]), "Event");
                c2 = new MyDataContainer(exec.createDataContainer(outSpec[1]), "Seq");
                made_output_spec = true;
        	}
        	
        	// HACK: O(n^n) complexity for now
        	SequenceCell hits = report_events(c1, sv, sa1, sa2);
        	if (hits != null) {
        		DataCell[] cells = new DataCell[1];
        		cells[0] = hits;
        		c2.addRow(cells);
        	}
        	
        	if (done++ % 100 == 0) {
        		exec.checkCanceled();
        		exec.setProgress(((double) done) / inData[0].getRowCount());
        	}
        }
        
        // in the odd event of no rows to filter we make the same spec
        if (!made_output_spec) {
        	 DataTableSpec[] outSpec = make_output_spec(inData[0].getSpec(),
             		inData[0].getSpec().getColumnSpec(seq_idx).getProperties(), null, null);
             c1 = new MyDataContainer(exec.createDataContainer(outSpec[0]), "Event");
             c2 = new MyDataContainer(exec.createDataContainer(outSpec[1]), "Seq");
        }
        
        // 
        if (rows_with_missing_tracks > 0) {
        	logger.warn(rows_with_missing_tracks+" rows are missing required tracks to compute \'"+m_operation.getStringValue()+"\': they were skipped.");
        }
        return new BufferedDataTable[]{c1.close(), c2.close()};
    }

    private TrackCreator getTrackCreator() {
    	return new TrackCreator() {

			@Override
			public Track createTrack(String name)
					throws InvalidSettingsException {
				RegionsAnnotation ra = new RegionsAnnotation();
				Track t = new Track(name, ra, CoordinateSystem.OFFSET_FROM_START);
				return t;
			}
		};
    }
	private SequenceCell report_events(MyDataContainer c1, SequenceValue sv,
			SequenceAnnotation sa1, SequenceAnnotation sa2) throws InvalidSettingsException {
		String             op = m_operation.getStringValue().toLowerCase();
		int               cnt = 0;
		SequenceCell       sc = new SequenceCell(sv);
		Track            hits_a = sc.getTrackByName(FILTER_HIT_TRACK_A, getTrackCreator());
		Track			 hits_b = sc.getTrackByName(FILTER_HIT_TRACK_B, getTrackCreator());
		
		if (op.indexOf("not overlapping") >= 0) {
			DenseBitVector bv_b = asBitVector(sv.getLength(), sa2.getRegions());
			RegionsAnnotation results = (RegionsAnnotation) hits_a.getAnnotation();
			for (RegionInterface a : sa1.getRegions()) {
				DenseBitVector bv_a = new DenseBitVector(sv.getLength());
				bv_a.set(a.getZStart(), a.getZEnd());
				if (!bv_a.intersects(bv_b)) {
					report_event(c1, sv, a, null, "A does not overlap any region in B");
					results.addRegion(a);
					cnt++;
				}
			}
		} else if (op.startsWith("a empty")) {		// A empty, B not
			if (sa1.countAnnotations() == 0 && sa2.countAnnotations() > 0) {
				report_event(c1, sv, null, null, "A empty, B not");
				cnt++;
			}
		} else {
			if (sa1.countAnnotations() == 0 || sa2.countAnnotations() == 0)
				return null;
			
			for (RegionInterface a : sa1.getRegions()) {
				DenseBitVector bv_a = asBitVector(sv.getLength(), a);
				RegionsAnnotation a_results = (RegionsAnnotation) hits_a.getAnnotation();
				RegionsAnnotation b_results = (RegionsAnnotation) hits_b.getAnnotation();
				for (RegionInterface b : sa2.getRegions()) {
					//logger.info("Comparing: "+a+" to "+b);
					DenseBitVector bv_b = new DenseBitVector(sv.getLength());
					bv_b.set(b.getZStart(), b.getZEnd());
					if (op.indexOf("overlaps") >= 0) {
						DenseBitVector anded = bv_a.and(bv_b);
						if (anded.cardinality() > 0) {
							a.setRegionOfInterest(anded);
							b.setRegionOfInterest(anded);
							report_event(c1, sv, a, b, "A overlaps B");
							cnt++;
							a_results.addRegion(a);
							b_results.addRegion(b);
						}
					} else if (op.startsWith("a completely within")) {
						DenseBitVector res = bv_a.and(bv_b);
						if (res.cardinality() >= bv_a.cardinality()) {
							report_event(c1, sv, a, b, "A completely within B");
							cnt++;
							a_results.addRegion(a);
							b_results.addRegion(b);
						}
					} else { // b completely within a?
						DenseBitVector res = bv_a.and(bv_b);
						if (res.cardinality() >= bv_b.cardinality()) {
							report_event(c1, sv, a, b, "B completely within A");
							cnt++;
							a_results.addRegion(b);
							b_results.addRegion(a);
						}
					}
				}
			}
		}
		
		// cleanup empty tracks
		if (hits_a.countAnnotations() == 0)
			sc.removeTrackByName(FILTER_HIT_TRACK_A);
		if (hits_b.countAnnotations() == 0)
			sc.removeTrackByName(FILTER_HIT_TRACK_B);
		
		return (cnt > 0) ? sc : null;
	}

	private DenseBitVector asBitVector(int length, RegionInterface a) {
		assert(length > 0);
		DenseBitVector ret = new DenseBitVector(length);
		ret.set(a.getZStart(), a.getZEnd());
		return ret;
	}

	private DenseBitVector asBitVector(int length, List<RegionInterface> regions) {
		assert(length > 0);
		DenseBitVector ret = new DenseBitVector(length);
		
		for (RegionInterface ri : regions) {
			DenseBitVector v = asBitVector(length, ri);
			ret.or(v);
		}
		return ret;
	}

	private void report_event(MyDataContainer c1, SequenceValue sv, 
							RegionInterface a, RegionInterface b, String msg) {
		DataCell[] cells = new DataCell[c1.getTableSpec().getNumColumns()];
		cells[0] = new StringCell(sv.getID());
		cells[1] = new StringCell(m_track1.getStringValue());
		cells[2] = new StringCell(m_track2.getStringValue());

		for (int i=3; i<cells.length; i++) {
			cells[i] = DataType.getMissingCell();
		}
		Map<String,DataCell> cmap = a.asCells("A");
		for (String key : cmap.keySet()) {
			Integer idx = m_colnames2idx.get(key);
			if (idx != null)
				cells[idx.intValue()] = cmap.get(key);
		}
		
		if (b != null) {
			cmap     = b.asCells("B");
			for (String key : cmap.keySet()) {
				Integer idx = m_colnames2idx.get(key);
				if (idx != null)
					cells[idx.intValue()] = cmap.get(key);
			}
		}
		c1.addRow(cells);
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
     * Computes an output table spec (should not be used at configure time due to dynamic column creation based on annotations present)
     * @param dataTableSpec
     * @param isp
     * @param a may be null
     * @param b may be null
     * @return
     * @throws InvalidSettingsException
     */
    private DataTableSpec[] make_output_spec(DataTableSpec dataTableSpec, DataColumnProperties isp, 
    		SequenceAnnotation a, SequenceAnnotation b) throws InvalidSettingsException {
    	assert(isp != null);
    	
    	List<DataColumnSpec> cols;
    	if (a != null) 
    		cols = a.asColumnSpec("A");
    	else 
    		cols = new ArrayList<DataColumnSpec>();
    	if (b != null) 
    		cols.addAll(b.asColumnSpec("B"));
    	
    	DataColumnSpec[] outcols = new DataColumnSpec[3 + cols.size() ];
    	outcols[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
    	outcols[1] = new DataColumnSpecCreator("A: Track", StringCell.TYPE).createSpec();
    	outcols[2] = new DataColumnSpecCreator("B: Track", StringCell.TYPE).createSpec();
    	
    	int idx = 3;
    	for (DataColumnSpec cs : cols) {
    		outcols[idx++] = cs;
    	}
    	
    	m_colnames2idx.clear();
    	for (int i=0; i<outcols.length; i++) {
    		m_colnames2idx.put(outcols[i].getName(), new Integer(i));
    	}
    	
    	DataColumnSpec[] cols2 = new DataColumnSpec[1];
    	DataColumnSpecCreator dcsc = new DataColumnSpecCreator("Filtered Sequences", SequenceCell.TYPE);
    	
    	TrackColumnPropertiesCreator tcpc = new TrackColumnPropertiesCreator(isp,
				new Track[] { new Track(FILTER_HIT_TRACK_A, getTrackCreator()),
						      new Track(FILTER_HIT_TRACK_B, getTrackCreator())
				});
		
    	dcsc.setProperties(tcpc.getProperties());
    	cols2[0] = dcsc.createSpec();
    	return new DataTableSpec[] { new DataTableSpec(outcols), new DataTableSpec(cols2) };
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_operation.saveSettingsTo(settings);
    	m_track1.saveSettingsTo(settings);
    	m_track2.saveSettingsTo(settings);
    	m_sequence.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_operation.loadSettingsFrom(settings);
    	m_track1.loadSettingsFrom(settings);
    	m_track2.loadSettingsFrom(settings);
    	m_sequence.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_operation.validateSettings(settings);
    	m_track1.validateSettings(settings);
    	m_track2.validateSettings(settings);
    	m_sequence.validateSettings(settings);
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

