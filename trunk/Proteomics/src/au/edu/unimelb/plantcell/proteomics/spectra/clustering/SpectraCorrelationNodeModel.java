package au.edu.unimelb.plantcell.proteomics.spectra.clustering;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.expasy.jpl.core.ms.spectrum.BinnedPeakListImpl;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.msmatch.PeakListMatcher;
import org.expasy.jpl.msmatch.PeakListMatcherImpl;
import org.expasy.jpl.msmatch.model.AlgoModel;
import org.expasy.jpl.msmatch.model.BinnedPeakListAlgoModel;
import org.expasy.jpl.msmatch.model.PeakListBiGraphAlgoModel;
import org.expasy.jpl.msmatch.model.PeakListDiffMatrixAlgoModel;
import org.expasy.jpl.msmatch.scorer.BinCorrScorer;
import org.expasy.jpl.msmatch.scorer.BinNCorrScorer;
import org.expasy.jpl.msmatch.scorer.CorrScorer;
import org.expasy.jpl.msmatch.scorer.NCorrScorer;
import org.expasy.jpl.msmatch.scorer.PeakListMatchScorer;
import org.expasy.jpl.msmatch.scorer.SPCScorer;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DoubleCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;


/**
 * This is the model implementation of spectra correlation based on code found in javaprotlib
 * 
 * @author Andrew Cassin
 */
public class SpectraCorrelationNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Spectra Correlator");
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_SPECTRA              = "spectra-column-1";
	static final String CFGKEY_SPECTRA2             = "spectra-column-2";
	static final String CFGKEY_ALGORITHM            = "algorithm";
	static final String CFGKEY_SCORE_FUNCTION       = "scoring-function";
	static final String CFGKEY_THRESHOLD            = "score-threshold";
	
	public final static String[] ALGORITHMS = new String[] { "M/Z differences between all peak pairs", "Bipartite graph comparison", "Binned peak-list comparison"  };
	public final static String[] SCORE_FUNCS= new String[] {  
		// the commented out options dont seem to be producing a score: always zero
		// "Correlation of spectral bins", "Normalised correlation of spectral bins", 
		"Normalised correlation of peaks", "Correlation of peaks", "Shared Peak Count (SPC)" };

 
    // persisted node state
    private final SettingsModelString m_col_1   = new SettingsModelString(CFGKEY_SPECTRA, "");
    private final SettingsModelString m_col_2   = new SettingsModelString(CFGKEY_SPECTRA2, "");
    private final SettingsModelString m_algorithm = new SettingsModelString(CFGKEY_ALGORITHM, ALGORITHMS[0]);
    private final SettingsModelString m_score_func= new SettingsModelString(CFGKEY_SCORE_FUNCTION, SCORE_FUNCS[0]);
    private final SettingsModelDouble m_threshold = new SettingsModelDouble(CFGKEY_THRESHOLD, 0.8);
    
    // not persisted
    private boolean m_bpl_versus_bpl;		// two peaklist's to be compared should be binned first?
    private boolean m_pl_versus_pl;			// or just left alone? only one can be set during execute()
    
    /**
     * Constructor for the node model.
     */
    protected SpectraCorrelationNodeModel() {
        // two incoming port(s): the spectra to be correlated and one outgoing port
        super(2, 1);
    }

  
  
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	DataTableSpec[] outSpec = make_output_spec(new DataTableSpec[] { inData[0].getSpec(), inData[1].getSpec() });
    	
    	MyDataContainer out = new MyDataContainer(exec.createDataContainer(outSpec[0]), "Pair");
    	int idx_col1 = inData[0].getSpec().findColumnIndex(m_col_1.getStringValue());
    	int idx_col2 = inData[1].getSpec().findColumnIndex(m_col_2.getStringValue());
    	if (idx_col1 < 0 || idx_col2 < 0)
    		throw new InvalidSettingsException("Unable to find spectra columns: re-configure?");
    	
    	RowIterator it2 = inData[1].iterator();
    	long n = inData[0].getRowCount() * inData[1].getRowCount();
    	logger.info("Correlating "+n+" pairs of spectra.");
    	StatusMonitor sm = new StatusMonitor(exec, n);
    	
    	int batch_size = 200;
    	logger.info("Batch size: "+batch_size);
    	
    	while (it2.hasNext()) {
    		// create a batch of peak lists to search against
    		List<SpectraValue> batch = new ArrayList<SpectraValue>();
    		do {
    			DataRow r = it2.next();
    			DataCell c= r.getCell(idx_col2);
    			if (c==null || c.isMissing() || !(c instanceof SpectraValue))
    				continue;
    			batch.add((SpectraValue)c);
    		} while (it2.hasNext() && batch.size() < batch_size);
    		
    		// search the entire batch against all input spectra from the first port
    		try {
    			report_batch_correlations(inData[0], batch, out, idx_col1, exec, sm);
    			// update node progress for user
        		sm.update(0);
    		} catch (Exception e) {
    			e.printStackTrace();
    			throw e;
    		}
    	}
    	
    	if (sm.getFailed() > 0) {
    		logger.warn("Some pair-wise spectral comparisons failed (maybe a bug): "+sm.getFailed()+" in total.");
    	}
    	return new BufferedDataTable[] {out.close()};
    }

    private void report_batch_correlations(BufferedDataTable table,
			List<SpectraValue> batch, MyDataContainer out, int col_idx, ExecutionContext exec, StatusMonitor sm) throws Exception {
    	assert(col_idx >= 0 && batch != null && table != null && exec != null && sm != null);
    	
    	// 1. create the matcher (ensure that one of m_pl_versus_pl or m_bpl_versus_bpl is set)
    	PeakListMatcher matcher = PeakListMatcherImpl.newInstance();
    	matcher.setAlgoModel(make_algorithm(m_algorithm.getStringValue()));
    	matcher.setScorer(make_score_func(m_score_func.getStringValue()));
    	
    	// 2. run batch rows from the second port against the top input port
		for (SpectraValue sv: batch) {
			int done = 0;
			for (DataRow r: table) {
				DataCell c= r.getCell(col_idx);
				if (c == null || c.isMissing() || !(c instanceof SpectraValue)) {
					continue;
				}
				SpectraValue s2 = (SpectraValue) c;
				try {
					compare_spectra(matcher, sv, s2, out);
				} catch (InvalidSettingsException ise) {
					throw ise;
				} catch (Exception e) {
					sm.failed(e);
					// continue without further user warning/errors
				}
				done++;
			}
			sm.update(done);
		}
	}

    /**
     * 
     * @param sfunc 	
     * @return never null
     */
	private PeakListMatchScorer make_score_func(String sfunc) {
		assert(sfunc != null);
		
		if (sfunc.startsWith("Correlation of spectral bins")) {
			return BinCorrScorer.getInstance();
		} else if (sfunc.startsWith("Normalised correlation of spectral bins")) {
			return BinNCorrScorer.getInstance();
		} else if (sfunc.startsWith("Correlation of peaks")) {
			return CorrScorer.getInstance();
		} else if (sfunc.startsWith("Normalised correlation of peaks")) {
			return NCorrScorer.getInstance();
		} else { // assume shared peak count
			return SPCScorer.getInstance();
		}
	}



	/**
	 * 
	 * @param algorithm
	 * @return never null
	 */
	private AlgoModel make_algorithm(String algorithm) {
		assert(algorithm != null);
		
		m_pl_versus_pl = true;
		m_bpl_versus_bpl = false;
		if (algorithm.startsWith("Bipartite")) {
			return PeakListBiGraphAlgoModel.newInstance(PeakListBiGraphAlgoModel.RELATIVE_INTENSITY_DIFF);
		} else if (algorithm.startsWith("M/Z")) {
			return PeakListDiffMatrixAlgoModel.newInstance(PeakListBiGraphAlgoModel.RELATIVE_INTENSITY_DIFF);
		} else {
			m_pl_versus_pl = false;
			m_bpl_versus_bpl = true;
			return BinnedPeakListAlgoModel.getInstance();
		}
	}




	private void compare_spectra(PeakListMatcher matcher, SpectraValue sv, SpectraValue s2, MyDataContainer out) throws Exception {
		assert(matcher != null && sv != null && s2 != null && out != null);
		
		// dont correlate spectra against themselves...
		if (sv.getID().equals(s2.getID()))
			return;
		
		// 1. convert to PeakList form as required by javaprotlib
		// NB: this is a bit dumb since we will repeatedly do this for one of the spectra...
		SortedPeakList sv1 = new SortedPeakList(sv);
		SortedPeakList sv2 = new SortedPeakList(s2);
		PeakListImpl pl1, pl2;
		pl1 = new PeakListImpl.Builder(sv1.getMZ()).intensities(sv1.getIntensities()).build();
		pl2 = new PeakListImpl.Builder(sv2.getMZ()).intensities(sv2.getIntensities()).build();
		
		// 2. score using javaprotlib
		if (m_bpl_versus_bpl) {
			matcher.computeMatch(new BinnedPeakListImpl.Builder(pl1).build(), 
					new BinnedPeakListImpl.Builder(pl2).build()
					);
		} else if (m_pl_versus_pl) {
			matcher.computeMatch(pl1, pl2);
		} else {
			throw new InvalidSettingsException("Match type: is not currently supported!");
		}
		
		// 3. report if comparison threshold is met
		double score = matcher.getScore();
		if (score >= m_threshold.getDoubleValue()) {
			out.addRow(new DataCell[] {
					new StringCell(sv.getID()),
					new StringCell(s2.getID()),
					new DoubleCell(score)
			});
		}
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
    	return make_output_spec(inSpecs);
    }

    private DataTableSpec[] make_output_spec(DataTableSpec[] inSpecs) {
    	DataColumnSpec[] cols = new DataColumnSpec[3];
    	cols[0] = new DataColumnSpecCreator("Spectra 1 ID", StringCell.TYPE).createSpec();
    	cols[1] = new DataColumnSpecCreator("Spectra 2 ID", StringCell.TYPE).createSpec();
    	cols[2] = new DataColumnSpecCreator("Score: "+m_score_func.getStringValue(), DoubleCell.TYPE).createSpec();
    	
		return new DataTableSpec[] { new DataTableSpec(cols) };
	}




	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_col_1.saveSettingsTo(settings);
        m_col_2.saveSettingsTo(settings);
        m_algorithm.saveSettingsTo(settings);
        m_score_func.saveSettingsTo(settings);
        m_threshold.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_col_1.loadSettingsFrom(settings);
        m_col_2.loadSettingsFrom(settings);
        m_algorithm.loadSettingsFrom(settings);
        m_score_func.loadSettingsFrom(settings);
        m_threshold.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_col_1.validateSettings(settings);
        m_col_2.validateSettings(settings);
        m_algorithm.validateSettings(settings);
        m_score_func.validateSettings(settings);
        m_threshold.validateSettings(settings);
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

    public class StatusMonitor {
    	private ExecutionContext m_exec;
    	private long m_n;
    	private long m_done;
    	private long m_failed;
    	private boolean m_shown_first_fail = false;
    	
    	public StatusMonitor(ExecutionContext exec, long n) {
    		m_exec = exec;
    		m_n = n;
    		m_done = 0;
    	}
    	
    	public void update(int done) throws Exception {
    		if (done > 0) {
    			logger.info("Correlated "+done+" input spectra in current batch.");
    		}
    		m_exec.checkCanceled();
    		m_done += done;
    		m_exec.setProgress(((double)m_done)/m_n);
    	}
    	
    	public void failed(Exception e) {
    		if (!m_shown_first_fail && e != null) {
    			m_shown_first_fail = true;
    			logger.warn(e);
    			e.printStackTrace();
    		}
    		m_failed++;
    	}
    	
    	public long getFailed() {
    		return m_failed;
    	}
    }
}

