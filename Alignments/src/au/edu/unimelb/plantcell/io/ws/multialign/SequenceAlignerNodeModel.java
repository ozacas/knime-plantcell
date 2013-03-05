package au.edu.unimelb.plantcell.io.ws.multialign;

import jaligner.Alignment;
import jaligner.Sequence;
import jaligner.SmithWatermanGotoh;
import jaligner.matrix.Matrix;
import jaligner.matrix.MatrixLoader;
import jaligner.matrix.MatrixLoaderException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import neobio.alignment.CrochemoreLandauZivUkelsonGlobalAlignment;
import neobio.alignment.CrochemoreLandauZivUkelsonLocalAlignment;
import neobio.alignment.IncompatibleScoringSchemeException;
import neobio.alignment.InvalidScoringMatrixException;
import neobio.alignment.InvalidSequenceException;
import neobio.alignment.NeedlemanWunsch;
import neobio.alignment.PairwiseAlignment;
import neobio.alignment.PairwiseAlignmentAlgorithm;
import neobio.alignment.ScoringMatrix;
import neobio.alignment.SmithWaterman;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.container.CloseableRowIterator;
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
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;



/**
 * This is the model implementation of SequenceAligner.
 * Performs an alignment, performed by http://jaligner.sourceforge.net of two sequences using the chosen parameters
 *
 * @author Andrew Cassin
 */
public class SequenceAlignerNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(SequenceAlignerNodeModel.class);
    
    final static String CFG_ALIGN_TYPE         = "alignment-type";
    final static String CFG_BUILTIN_MATRIX     = "builtin-matrix";
    final static String CFG_GAP_PENALTY_OPEN   = "gap-penalty-open";
    final static String CFG_GAP_PENALTY_EXTEND = "gap-penalty-extend";
    final static String CFG_ACCSN_COL          = "accession-column";
    final static String CFG_SEQ_COL            = "sequence-column";
    final static String CFG_SEQ2_COL           = "sequence2-column";
    final static String CFG_IS_PAIRWISE        = "pairwise?";
    final static String CFG_WANTED             = "wanted-output-columns";
    final static String CFG_ALIGN_SELF         = "align-self?";
    final static String[] DEF_ALIGNMENT_FORMATS= new String[] {"FASTA", "CLUSTALW", "BLAST" };
    
    private final static String PLUGIN_ID = "platform:/plugin/au.edu.unimelb.plantcell.io.ws.multialign";
    
    private SettingsModelString     m_align_type;
    private SettingsModelDouble     m_gap_penalty_open;
    private SettingsModelDouble     m_gap_penalty_extend;
    private SettingsModelString     m_seq_col;
    private SettingsModelString     m_builtin_matrix;
    private SettingsModelString     m_is_pairwise;
    private SettingsModelString     m_seq2_col;
    private SettingsModelStringArray m_wanted;
    private SettingsModelBoolean    m_align_self;		// align(seq1, seq1) yes iff true
    
    private Matrix m_jalign_matrix;	// both scoring matrices are computed, even if only one is used during execute()
    private ScoringMatrix m_neobio_matrix;
    
    /**
     * Constructor for the node model.
     */
    protected SequenceAlignerNodeModel() {
    	// one incoming, one outgoing port
        super(1, 1);
        
        m_align_type         = (SettingsModelString) make(CFG_ALIGN_TYPE);
        m_gap_penalty_open   = (SettingsModelDouble) make(CFG_GAP_PENALTY_OPEN);
        m_gap_penalty_extend = (SettingsModelDouble) make(CFG_GAP_PENALTY_EXTEND);
        m_seq_col            = (SettingsModelString) make(CFG_SEQ_COL);
        m_builtin_matrix     = (SettingsModelString) make(CFG_BUILTIN_MATRIX);
        m_is_pairwise        = (SettingsModelString) make(CFG_IS_PAIRWISE);
        m_seq2_col           = (SettingsModelColumnName) make(CFG_SEQ2_COL);
        m_seq2_col.setEnabled(!m_is_pairwise.isEnabled());
        m_wanted             = (SettingsModelStringArray) make(CFG_WANTED);
        m_align_self         = (SettingsModelBoolean) make(CFG_ALIGN_SELF);
        
        m_jalign_matrix = null;
        m_neobio_matrix = null;
    }

    public static SettingsModel make(String field_name) {
    	if (field_name.equals(CFG_ALIGN_TYPE)) {
    		return new SettingsModelString(CFG_ALIGN_TYPE, "local");
    	} else if (field_name.equals(CFG_BUILTIN_MATRIX)) {
    			return new SettingsModelString(CFG_BUILTIN_MATRIX, "PAM250");
    	} else if (field_name.equals(CFG_GAP_PENALTY_OPEN)) {
    		return new SettingsModelDoubleBounded(CFG_GAP_PENALTY_OPEN, 10.0, 0, 1000);
    	} else if (field_name.equals(CFG_GAP_PENALTY_EXTEND)) {
    		return new SettingsModelDoubleBounded(CFG_GAP_PENALTY_EXTEND, 2.0, 0, 1000);
    	} else if (field_name.equals(CFG_ACCSN_COL)) {
    		return new SettingsModelColumnName(CFG_ACCSN_COL, "");
    	} else if (field_name.equals(CFG_SEQ_COL)) {
    		return new SettingsModelColumnName(CFG_SEQ_COL, "");
    	} else if (field_name.equals(CFG_IS_PAIRWISE)) {
    		return new SettingsModelString(CFG_IS_PAIRWISE, "1col");
    	} else if (field_name.equals(CFG_SEQ2_COL)) {
    		return new SettingsModelColumnName(CFG_SEQ2_COL, "");
    	} else if (field_name.equals(CFG_WANTED)) {
    		return new SettingsModelStringArray(CFG_WANTED, new String[] { "Accessions", "Original Sequences", "Score" });
    	} else if (field_name.equals(CFG_ALIGN_SELF)) {
    		return new SettingsModelBoolean(CFG_ALIGN_SELF, false);
    	}
    	return null;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	int sequence_col    = inData[0].getDataTableSpec().findColumnIndex(m_seq_col.getStringValue());
    	if (sequence_col < 0) {
    		sequence_col = useSequenceColumn(inData[0].getDataTableSpec(), 2);
    		if (sequence_col < 0) {
    			throw new Exception("Cannot locate first sequence column: re-configure?");
    		}
    	}
    	boolean is_pairwise = m_is_pairwise.getStringValue().equals("1col");
         
        // log summary of node execution
        double gap_open_penalty   = m_gap_penalty_open.getDoubleValue();
        double gap_extend_penalty = m_gap_penalty_extend.getDoubleValue();
        logger.info(m_is_pairwise.getStringValue());
       
        int n_rows = inData[0].getRowCount();
        int n_align = is_pairwise ? (n_rows * n_rows - n_rows) : n_rows;
        
        logger.info("Sequence Alignment... beginning execution");
        logger.info("Alignment type: "          + m_align_type.getStringValue());
        logger.info("Number of alignments to be performed: "+ n_align);
        logger.info("Scoring matrix used: "+m_builtin_matrix.getStringValue());
        if (m_align_type.getStringValue().toLowerCase().contains("jalign")) {		// only specified for JAligner for now...
        	logger.info("Gap open penalty: " + gap_open_penalty);
        	logger.info("Gap extend penalty: " + gap_extend_penalty);
        }
        logger.info("Pairwise? "+is_pairwise);
        logger.info("Align same sequence?"+m_align_self.getBooleanValue());
        
        // compute the matrix for both neobio and jaligner (even though only one of them will be used, FOR NOW)
        try {
        	m_jalign_matrix = MatrixLoader.load(m_builtin_matrix.getStringValue());
        	String      mat = getMatrix(m_builtin_matrix.getStringValue());
        	m_neobio_matrix = new ScoringMatrix(new StringReader(mat));
        } catch (MatrixLoaderException mle) {
        	throw new Exception("Aborting! Cannot load matrix from: "+m_builtin_matrix.getStringValue());
        }
        
        // execute based on supplied columns...
        if (is_pairwise) {
        	return execute_pairwise(inData, exec, gap_open_penalty, gap_extend_penalty, n_rows, sequence_col);
        } else {
        	return execute_pairs(inData, exec, gap_open_penalty, gap_extend_penalty, n_rows, sequence_col);
        }
    }

	private int useSequenceColumn(DataTableSpec inSpec, int idx_from_last) {
		assert(idx_from_last >= 1);
		
		int got = 0;
		for (int i=inSpec.getNumColumns()-1; i >= 0; i--) {
			DataColumnSpec cs = inSpec.getColumnSpec(i);
			if (cs.getType().isCompatible(SequenceValue.class)) {
				got++;
				if (got == idx_from_last) {
					if (logger != null) {
						logger.warn("Using '"+cs.getName()+"' column for biological sequences.");
					}
				
					return i;
				}
				// else continue for more columns...
			}
		}
		return -1;
	}

	protected BufferedDataTable[] execute_pairwise(final BufferedDataTable[] inData,
            final ExecutionContext exec, double gap_open_penalty, 
    		double gap_extend_penalty, int n_rows, int sequence_col) throws Exception {
        
        DataTableSpec outputSpec = AlignmentReporter.getTableSpec(m_wanted.getStringArrayValue());
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        
        List<SequenceValue> vec = new ArrayList<SequenceValue>();
        RowIterator rows = inData[0].iterator();
        while (rows.hasNext()) {
        	DataRow r = rows.next();
        	DataCell c = r.getCell(sequence_col);
        	if (c == null || c.isMissing())
        		continue;
        	SequenceValue sv   = (SequenceValue) c;
        	vec.add(sv);
        }
       
        int done   = 0;
        int align_id= 1;
        for (SequenceValue a1 : vec) {
        	for (SequenceValue a2 : vec) {
        		if (m_align_self.getBooleanValue() || a1 != a2) {	
        			container.addRowToTable(do_alignment(align_id++, a1, a2, m_align_type.getStringValue(), outputSpec, gap_open_penalty, gap_extend_penalty));
        			done++;
        		}
        		// check if the execution monitor was canceled
            	if (done % 30 == 0) {
            		exec.checkCanceled();
            		exec.setProgress(done / (double)(n_rows*n_rows), 
            				"Done " + done + " alignments");
            	}
        	}
        }
        
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }
 
	protected BufferedDataTable[] execute_pairs(final BufferedDataTable[] inData,
    											final ExecutionContext exec, double gap_open_penalty, 
    											double gap_extend_penalty, int n_rows, int sequence_col) throws Exception {
    	
    	int sequence2_col = inData[0].getDataTableSpec().findColumnIndex(m_seq2_col.getStringValue());
    	if (sequence2_col < 0) {
    		sequence2_col = useSequenceColumn(inData[0].getSpec(), 1);
    		if (sequence2_col < 0)
    			throw new Exception("Cannot locate sequence2 column: have you configured the node correctly?");
    	}
    	if (sequence_col == sequence2_col) {
    		throw new Exception("Cannot use the same column for both sequences!");
    	}
    	
    	// in this case we can just append the results to the columns, no need to output everything we did for the pairwise case
    	
        DataTableSpec alignment_spec = AlignmentReporter.getTableSpec(m_wanted.getStringArrayValue());
        
        BufferedDataContainer container = exec.createDataContainer(alignment_spec, false, 0);
        
        int done   = 0;
        CloseableRowIterator it = inData[0].iterator();
        
        int align_no = 1;
     
        while (it.hasNext()) {
        	DataRow r = it.next();
        	
        	DataCell c1 = r.getCell(sequence_col);
        	DataCell c2 = r.getCell(sequence2_col);
        	if (c1.isMissing() || c2.isMissing()) 
        		continue;
        	SequenceValue[] vec = null;
        	if (c2.getType().isCollectionType()) {
        		if (c2 instanceof ListCell) {
	        		ListCell l2 = (ListCell) c2;
	        		if (l2.size() < 1) 
	        			continue;
	        		vec = new SequenceValue[l2.size()];
	        		int cnt = 0;
	        		for (DataCell c : l2) {
	        			vec[cnt++] = (SequenceValue) c;
	        		}
	        		l2 = null;
        		} else if (c2 instanceof SetCell) {
        			SetCell s2 = (SetCell) c2;
        			if (s2.size()< 1) 
        				continue;
        			vec = new SequenceValue[s2.size()];
        			int cnt = 0;
        			for (DataCell c : s2) {
        				vec[cnt++] = (SequenceValue) c;
        			}
        		} else {
        			throw new Exception("Unknown collection cell: "+c2.getType().toString()+ ", aborting!");
        		}
        	} else {
        		vec = new SequenceValue[] { (SequenceValue) c2 };
        	}
        	SequenceValue s1 = (SequenceValue) c1;
        	for (SequenceValue s2 : vec) {
	        	//logger.info("Lengths for sequences: " + str1.length() + " " + str2.length() );
	        	container.addRowToTable(do_alignment(align_no, s1, s2, m_align_type.getStringValue(), alignment_spec, gap_open_penalty, gap_extend_penalty));
	        	align_no++;
        	}
        	vec = null;
        	done++;
        		
        	// check if the execution monitor was canceled
        	if (done % 30 == 0) {
        		exec.checkCanceled();
        		exec.setProgress(done / (double)n_rows, 
        				"Done " + done + " rows");
        	}
        	r = null;
        }
        
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }
    
    private DataRow do_alignment(int align_id, SequenceValue a1, SequenceValue a2, String align_type, 
    				DataTableSpec spec, double gap_open_penalty, double gap_extend_penalty) 
    					throws FileNotFoundException, IOException, InvalidSequenceException, IncompatibleScoringSchemeException, InvalidScoringMatrixException {
    
		AlignmentReporter ar;
		if (align_type.indexOf("Local - JAligner") >= 0) {
			Sequence seq1 = new Sequence(a1.getStringValue());
			Sequence seq2 = new Sequence(a2.getStringValue());
			Alignment a = SmithWatermanGotoh.align(seq1, seq2, m_jalign_matrix, (float) gap_open_penalty, (float) gap_extend_penalty);
			ar = new AlignmentReporter(a, a1, a2);
		} else {
			StringReader sr1 = new StringReader(a1.getStringValue());
			StringReader sr2 = new StringReader(a2.getStringValue());
			PairwiseAlignmentAlgorithm algorithm;
			if (align_type.startsWith("Local")) {
				algorithm = align_type.endsWith("Waterman") ? new SmithWaterman() : new CrochemoreLandauZivUkelsonLocalAlignment();
			} else {
				algorithm = align_type.endsWith("Wunsch") ? new NeedlemanWunsch() : new CrochemoreLandauZivUkelsonGlobalAlignment();
			}
		
			// TODO: neobio does not support gap open/extend cost model... fix this?
			algorithm.setScoringScheme (m_neobio_matrix);
			algorithm.loadSequences (sr1, sr2);
	
			// now compute the alignment
			PairwiseAlignment alignment = algorithm.getPairwiseAlignment();
			sr1.close();
			sr2.close();
			
	    	ar = new AlignmentReporter(alignment, a1, a2);
		}
    	return ar.getRow(align_id, spec);
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
        DataTableSpec ts = AlignmentReporter.getTableSpec(m_wanted.getStringArrayValue());
        return new DataTableSpec[]{ts};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_align_type.saveSettingsTo(settings);
        m_gap_penalty_open.saveSettingsTo(settings);
        m_gap_penalty_extend.saveSettingsTo(settings);
        m_seq_col.saveSettingsTo(settings);
        m_seq2_col.saveSettingsTo(settings);
        m_builtin_matrix.saveSettingsTo(settings);
        m_is_pairwise.saveSettingsTo(settings);
        m_wanted.saveSettingsTo(settings);
        m_align_self.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_align_type.loadSettingsFrom(settings);
        m_gap_penalty_open.loadSettingsFrom(settings);
        m_gap_penalty_extend.loadSettingsFrom(settings);
        m_seq_col.loadSettingsFrom(settings);
        m_seq2_col.loadSettingsFrom(settings);
        m_builtin_matrix.loadSettingsFrom(settings);
        m_is_pairwise.loadSettingsFrom(settings);
        m_wanted.loadSettingsFrom(settings);
        if (settings.containsKey(CFG_ALIGN_SELF)) {
        	m_align_self.loadSettingsFrom(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
    	m_align_type.validateSettings(settings);
        m_gap_penalty_open.validateSettings(settings);
        m_gap_penalty_extend.validateSettings(settings);
        m_seq_col.validateSettings(settings);
        m_seq2_col.validateSettings(settings);
        m_builtin_matrix.validateSettings(settings);
        m_is_pairwise.validateSettings(settings);
        m_wanted.validateSettings(settings);
        if (settings.containsKey(CFG_ALIGN_SELF)) {
        	m_align_self.loadSettingsFrom(settings);
        }
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

    /**
     * Returns the list of available scoring matrices (shared between neobio & jaligner)
     * @return list of available matrix names eg. BLOSUM62, PAM250 etc.
     */
	public static String[] getBuiltinScoringMatrices() {
		ArrayList<String> ret = new ArrayList<String>();
		JarInputStream jis = null;
		try {
			// HACK: must match eclipse plugin ID
			URL        url = new URL(PLUGIN_ID+"/lib/jaligner.jar");
			jis = new JarInputStream(url.openStream());
			ZipEntry ze = null;
	        while ((ze = jis.getNextEntry()) != null) {
	        	String name = ze.getName();
	        	if (name.startsWith("jaligner/matrix/matrices/")) {
	        		ret.add(name.substring("jaligner/matrix/matrices/".length()));
	        	}
	        }
	        jis.close();
	        return ret.toArray(new String[0]);
		} catch (Exception e) {
			e.printStackTrace();
			if (jis != null) {
				try { jis.close(); } catch (Exception e2) { e2.printStackTrace(); }
			}
			return new String[] { };
		} 
		
	}


	/**
	 * Returns an input stream to the jAligner (NCBI) scoring matrix of the specified name (case sensitive) 
	 * eg. BLOSUM62
	 *   
	 * @param stringValue the name of the NCBI-compatible scoring matrix (in uppercase)
	 * @return a handle to the data or null if it does not exist
	 */
    private String getMatrix(String stringValue) throws IOException {
    	JarInputStream       jis = null;
    	try {
	    	URL        url = new URL(PLUGIN_ID+"/lib/jaligner.jar");
	    	jis = new JarInputStream(url.openStream());
	        ZipEntry              ze= null;
	        while ((ze = jis.getNextEntry()) != null) {
	        	if (ze.getName().equals("jaligner/matrix/matrices/"+stringValue)) {
	        		BufferedReader br = new BufferedReader(new InputStreamReader(jis));
	        		StringBuffer   sb = new StringBuffer(4 * 1024);
	        		String       line = null;
	        		while ((line = br.readLine()) != null) {
	        			sb.append(line);
	        			sb.append('\n');
	        		}
	        		return sb.toString();
	        	}
	        }
		
			return null;
    	} finally {
    		if (jis != null) {
    			jis.close();
    		}
    	}
	}

}

