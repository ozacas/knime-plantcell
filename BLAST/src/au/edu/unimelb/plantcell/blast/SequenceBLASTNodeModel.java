package au.edu.unimelb.plantcell.blast;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.io.write.fasta.FastaWriter;
import au.edu.unimelb.plantcore.core.regions.BlastHitRegion;


/**
 * This is the model implementation of BLASTPlus.
 * Supports local execution of NCBI BLAST+ executables (which must be  installed separately)
 *
 * @author http://www.plantcell.unimelb.edu.au/
 */
public class SequenceBLASTNodeModel extends BLASTPlusNodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Sequence BLAST+");

	public static final String CFGKEY_ACCESSION = "sequence-accessions";
	
        
    private SettingsModelString m_column      = new SettingsModelString(CFGKEY_QUERY_DATABASE, "");
    
    // not persisted
    private File m_tmp_fasta;
    
    /**
     * Constructor for the node model.
     */
    protected SequenceBLASTNodeModel() {
            super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	logger.info("Locating blast program...");
    	File blast = find_blast_program();
    	if (blast == null) {
    		throw new InvalidSettingsException("Unable to locate BLAST program - re-configure?");
    	}
    	
    	/**
    	 *  this node is tricky: it doesnt specify an output table spec until the fields are 
    	 *  known from the stream or, if an error occurs, a silly output table is created to keep KNIME happy
    	 */
    	
    	logger.info("Running "+blast.getAbsolutePath());
    	CommandLine cmdLine = new CommandLine(blast);
    	
    	// this temp file must be setup
    	m_tmp_fasta = File.createTempFile("ncbi_blast_tmp", ".fasta");	// file does not exist yet but will be populated per blast batch...
    	
    	
    	// run makeblastdb if needed
    	make_blastdb(new File(getBLASTDatabase()));
    	
    	addArguments(cmdLine);
    
    	RowIterator it = inData[0].iterator();
    	int seq_idx = inData[0].getSpec().findColumnIndex(m_column.getStringValue());
    	if (seq_idx < 0) {
    		throw new InvalidSettingsException("Unable to locate column: "+m_column.getStringValue()+" - reconfigure?");
    	}
    	
    	// for the BLAST from fasta file node, we know that there is no tracks to begin
    	// with (cant be: no input ports to the node) so we dont have to add to the properties
    	DataColumnSpec[] col = new DataColumnSpec[] { mergeColumnProperties(inData[0].getSpec().getColumnSpec(seq_idx).getProperties(), 
    			new Track[] {
				new Track(Track.BLAST_LONGEST_TRACK,   getTrackCreator()),
				new Track(Track.BLAST_BEST_EVAL_TRACK, getTrackCreator()),
				new Track(Track.BLAST_TOP20_TRACK,     getTrackCreator()) }
    	) };
		final MyDataContainer c_seq = new MyDataContainer(exec.createDataContainer(new DataTableSpec(col)), "Query");
		
    	
    	TabbedCSVLogger tsv = new TabbedCSVLogger(logger, exec);
    	
    	BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(it,
    			seq_idx, 1000, 1000 * 1000, getSequenceProcessor(exec));
    	
    	try {
    		int done = 0;
	    	while (bsi.hasNext()) {
	    		Map<UniqueID,SequenceValue> batch_map = bsi.nextAsMap();
	    		if (batch_map == null)
	    			break;
	    		new FastaWriter(m_tmp_fasta, batch_map).write();
    			logger.info("Running batch of "+batch_map.size()+" sequences: "+cmdLine.toString());
    			tsv.setBatch(batch_map);
    			DefaultExecutor exe = new DefaultExecutor();
    	    	exe.setExitValues(new int[] {0, 1});
    	    	exe.setStreamHandler(new PumpStreamHandler(tsv, new ErrorLogger(logger)));
    	    	exe.setWorkingDirectory(m_tmp_fasta.getParentFile());		// must match addQueryDatabase() semantics
    	    	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
    	    	
            	int exitCode = exe.execute(cmdLine);
            	logger.info("got exit code: "+exitCode+" from BLAST");
            	
            	if (exe.isFailure(exitCode)) {
            		if (exe.getWatchdog().killedProcess())
            			throw new Exception("BLAST failed - watchdog says no...");
            		else
            			throw new Exception("BLAST failed - check console messages and input data");
            	}
            	
            	// user want annotated sequences?
    	    	tsv.walkResults(new BlastResultsWalker() {

    	    		@Override
    				public void hit(SequenceValue hit, List<BlastHitRegion> hits) {
    	    			if (!reportHits())
    						return;
    		    		BlastHitRegion longest_bh = find_longest_bh(hits);
    		    		BlastHitRegion lowest_eval= find_best_eval_bh(hits);
    		    		List<BlastHitRegion> top20= find_best_20(hits);
    		    		
    		    		// add non-null result tracks to sc
    		    		SequenceCell sc;
    					try {
    						sc = new SequenceCell(hit);
    			    		addTrack(sc, Track.BLAST_BEST_EVAL_TRACK, lowest_eval);
    			    		addTrack(sc, Track.BLAST_LONGEST_TRACK,   longest_bh);
    			    		addTrack(sc, Track.BLAST_TOP20_TRACK,     top20);
    			    		
    			    		c_seq.addRow(new DataCell[] { sc });
    					} catch (InvalidSettingsException e) {
    						e.printStackTrace();
    					}
    				}

    				@Override
    				public void nohit(SequenceValue hit) {
    					if (!reportNoHits())
    						return;
    					try {
    						c_seq.addRow(new DataCell[] { new SequenceCell(hit) });
    					} catch (InvalidSettingsException e) {
    						e.printStackTrace();
    					}
    				}
    	    		
    	    	});
    	    	
    	    	done += batch_map.size();
    	    	exec.setProgress(((double)done) / inData[0].getRowCount());
    	    	exec.checkCanceled();
    		}	
    	} finally {
    		m_tmp_fasta.delete();
    	}
    	
        return new BufferedDataTable[]{tsv.getTSVTable(), c_seq.close()};
    }
    
	/**
     * {@inheritDoc}
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
    	super.saveSettingsTo(settings);
    	m_column.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	super.loadValidatedSettingsFrom(settings);
    	m_column.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	super.validateSettings(settings);
    	m_column.validateSettings(settings);
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

    @Override
    public String getQueryDatabase() {
    	return m_tmp_fasta.getName();
    }
}

