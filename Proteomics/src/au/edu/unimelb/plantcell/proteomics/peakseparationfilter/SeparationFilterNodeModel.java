package au.edu.unimelb.plantcell.proteomics.peakseparationfilter;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
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
import au.edu.unimelb.plantcell.proteomics.spectra.clustering.SortedPeakList;


/**
  * <code>NodeModel</code> for the peak separation filter node, which looks for particular
 * peaks a certain mass apart and either accepts or rejects the input rows based on the presence/abscence of these peaks
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class SeparationFilterNodeModel extends NodeModel {
    
	// the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Peak Separation Filter");
        
    public final static String CFGKEY_SPECTRA_COLUMN = "spectra-column";
    public final static String CFGKEY_LOGIC = "accept-or-reject";
    public final static String CFGKEY_MASS_DISTANCES = "mass-distance";
    public final static String CFGKEY_DISTANCE_TOLERANCE = "distance-tolerance";

    public final static String[] LOGICAL_OPERATIONS = new String[] { "accept only if any matching distance", "accept only if all matching distances",
    																"reject if any matching distance", "reject if all matching distances" };
    
    public SettingsModelString m_spectra        = new SettingsModelString(CFGKEY_SPECTRA_COLUMN, "");
    public SettingsModelString m_logic          = new SettingsModelString(CFGKEY_LOGIC, LOGICAL_OPERATIONS[0]);	// accept spectra with ANY matching specified distance(s)
    public SettingsModelDouble m_tolerance      = new SettingsModelDouble(CFGKEY_DISTANCE_TOLERANCE, 0.05);
    public SettingsModelString m_distances      = new SettingsModelString(CFGKEY_MASS_DISTANCES, "");
    
    /**
     * Constructor for the node model.
     */
    protected SeparationFilterNodeModel() {
        super(1, 1);
    }
    
    /**
     * {@inheritDoc}
     */
	@Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	MyDataContainer c = new MyDataContainer(exec.createDataContainer(make_output_spec(inData[0].getSpec())), "Row");
    	logger.info("Processing "+inData[0].getRowCount()+" for filtering spectra by (pairwise) distances between peaks.");
    	int spectra_idx = inData[0].getSpec().findColumnIndex(m_spectra.getStringValue());
    	if (spectra_idx < 0) {
    		throw new InvalidSettingsException("Cannot find spectra column: "+m_spectra.getStringValue()+" - reconfigure?");
    	}
    	int accepted = 0;
    	int done = 0;
    	double[] distances = getUserSpecifiedDistances();
    	if (distances == null || distances.length < 1) {
    		throw new InvalidSettingsException("You must specify at least one peak separation! Reconfigure!");
    	}
		logger.info("Found "+distances.length+" peak separations to search for.");
		final HashSet<Double> accepted_distances = new HashSet<Double>();
		MatchedPeaksCallback mpcb = new MatchedPeaksCallback() {

			@Override
			public boolean acceptHit(double mz1, double mz2, double accepted_distance) {
				accepted_distances.add(accepted_distance);
				return true;
			}
			
		};
		
    	for (DataRow r : inData[0]) {
    		DataCell spectra_cell = r.getCell(spectra_idx);
    		if (spectra_cell == null || spectra_cell.isMissing()) {
    			continue;
    		}
    		SpectraValue sv = (SpectraValue) spectra_cell;
    		if (sv.getNumPeaks() > 0) {
        		accepted_distances.clear();
    			processSpectra(sv, mpcb);
	    		if (implementFilterLogic(accepted_distances, distances)) {
	    			accepted++;
	    			logger.debug("Accepted "+sv.getID()+ "(row "+r.getKey().getString()+") as it has required peak(s) separation");
	    			c.addRow(r);
	    			exec.checkCanceled();
	    		}
    		}
    		done++;
    		if (done % 1000 == 0) {
    			exec.checkCanceled();
    			exec.setProgress(((double)done) / inData[0].getRowCount());
    		}
    	}
    	
    	double pc = (((double)accepted) / inData[0].getRowCount()) * 100.0d;
    	logger.info("After filtering, we retained "+pc+"% of input rows.");
        return new BufferedDataTable[]{c.close()};
    }

	/**
	 * Returns true if the chosen spectra has peaks which are the required distance apart, false otherwise
	 * 
	 * @param sv
	 * @return
	 */
	private void processSpectra(SpectraValue sv, final MatchedPeaksCallback mpcb) throws InvalidSettingsException {
		if (sv == null) {
			throw new InvalidSettingsException("Spectra cannot be non-existant!");
		}
		// TODO... implement fragment state charge handling?
		
		// get the peaks for consideration...
		SortedPeakList spl = new MySortedPeakList(sv);			// removes zero intensity peaks
		if (!spl.hasPeaks()) {									// any peaks left?
			return;
		}
		double[] sorted_mz          = spl.getMZ();				// by increasing m/z
		double[] sorted_intensities = spl.getIntensities();		// intensities match sorted_mz array! ;-)
		double max_distance         = getMaximumDistanceFromUserDistances();
		double tolerance            = m_tolerance.getDoubleValue();
		double[] distances          = getUserSpecifiedDistances();
		for (int i=0; i<sorted_mz.length; i++) {
			double mz     = sorted_mz[i];
			double min_mz = mz - max_distance - tolerance;
			double max_mz = mz + max_distance + tolerance;
			findSuitableOtherPeak(mz, min_mz, max_mz, i, distances, sorted_mz, sorted_intensities, mpcb);
		}
	}

	private boolean implementFilterLogic(final Set<Double> accepted_distances, final double[] distances) throws InvalidSettingsException {
		String method = m_logic.getStringValue();
		if (method.equals(LOGICAL_OPERATIONS[0])) {
			return (accepted_distances.size() > 0);
		} else if (method.equals(LOGICAL_OPERATIONS[1])) {
			return (accepted_distances.size() == distances.length); 
		} else if (method.equals(LOGICAL_OPERATIONS[2])) {
			return (accepted_distances.size() == 0);
		} else if (method.equals(LOGICAL_OPERATIONS[3])) {
			return (accepted_distances.size() != distances.length);
		} else { 
			throw new InvalidSettingsException("Unknown method: "+method);
		}
	}

	private double[] getUserSpecifiedDistances() throws InvalidSettingsException {
		String[] distances = m_distances.getStringValue().split("\\s+");
		double[] ret = new double[distances.length];
		Arrays.fill(ret, -1.0d);
		int i = 0;
		for (String s : distances) {
			try {
				Double d = Double.valueOf(s.trim());
				ret[i++] = d;
			} catch (NumberFormatException nfe) {
				throw new InvalidSettingsException(nfe);
			}
		}
		return ret;
	}

	/**
	 * NB: input peak list MUST be sorted in increasing m/z
	 * @param mz
	 * @param min_mz
	 * @param max_mz
	 * @param i			mz belongs to i'th peak (relative to zero) in mzs
	 * @param user_distances
	 * @param mzs		peaklist m/z's
	 * @param itys		peaklist intensities
	 * @return
	 */
	private boolean findSuitableOtherPeak(double mz, double min_mz, double max_mz, int i,
			double[] user_distances, double[] mzs, double[] itys, final MatchedPeaksCallback mpcb) {
		// NB: since the peaklist is already sorted we need scan only in both directions from the i'th peak
		// until the current peak is outside [min_mz, max_mz]
		int j = i-1;
		while (j>=0 && mzs[j] >= min_mz) {
			if (hasAcceptableDistance(mz, mzs[j], user_distances, mpcb)) {
				return true;
			}
			j--;
		}
		j = i+1;
		while (j<=mzs.length-1 && mzs[j] <= max_mz) {
			if (hasAcceptableDistance(mz, mzs[j], user_distances, mpcb)) {
				return true;
			}
			j++;
		}
		return false;
	}

	private boolean hasAcceptableDistance(double mz, double mz2, double[] user_distances, final MatchedPeaksCallback mpcb) {
		assert(user_distances != null && user_distances.length > 0 && mpcb != null);
		
		double tolerance = m_tolerance.getDoubleValue();
		for (double distance : user_distances) {
			double sep = Math.abs(mz - mz2);
			if (sep  >= (distance-tolerance) && sep <= (distance+tolerance)) {
				return mpcb.acceptHit(mz, mz2, distance);
			}
		}
		return false;
	}

	/**
	 * Compute the maximum distance of interest to the user from the chosen peak. Used to eliminate peaks quickly.
	 * 
	 * @return maximum distance from user configured settings
	 */
	private double getMaximumDistanceFromUserDistances() {
		double max = 0.0d;
		try {
			for (double d : getUserSpecifiedDistances()) {
				if (d > max) {
					max = d;
				}
			}
		} catch (InvalidSettingsException e) {
			// fallthru and return 0.0
			max = 0.0d;
		}
		return max;
	}

	private DataTableSpec make_output_spec(DataTableSpec inSpec) {
		return inSpec;
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
        
        return new DataTableSpec[]{make_output_spec(inSpecs[0])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_logic.saveSettingsTo(settings);
    	m_distances.saveSettingsTo(settings);
    	m_spectra.saveSettingsTo(settings);
    	m_tolerance.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_logic.loadSettingsFrom(settings);
    	m_distances.loadSettingsFrom(settings);
    	m_spectra.loadSettingsFrom(settings);
    	m_tolerance.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_logic.validateSettings(settings);
    	m_distances.validateSettings(settings);
    	m_spectra.validateSettings(settings);
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
    
    /**
     * Refuses peaks which have no intensity
     * @author acassin
     *
     */
    public class MySortedPeakList extends SortedPeakList {

		public MySortedPeakList(SpectraValue sv) {
			super(sv);
		}
    	
		public boolean acceptPeak(double mz, double intensity) {
			return (intensity > 0.0d);
		}
   	}

}

