package au.edu.unimelb.plantcell.views.ms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.CompileableComposite;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.la4j.factory.CRSFactory;
import org.la4j.io.MatrixMarketStream;
import org.la4j.io.MatrixStream;
import org.la4j.matrix.Matrices;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.functor.MatrixFunction;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.io.read.spectra.AbstractXMLMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.BasicPeakList;
import au.edu.unimelb.plantcell.io.read.spectra.BinaryDataArrayMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.BinaryMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.PrecursorMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.RunMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.SelectedIonMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.SpectrumMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.XMLMatcher;

/**
 * This is the model implementation of MassSpecSurface.
 * Presents a surface representing RT, m/z and heatmap (eg. identified MS2 spectra) using the sexy jzy3d library. Useful for QA and assessment of mzML-format datasets.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MassSpecSurfaceNodeModel extends NodeModel {
    private final NodeLogger logger = NodeLogger.getLogger("MS Surface");
    
	// node configuration
	static final String CFGKEY_FILES = "mzml-files";
	static final String CFGKEY_RT_MIN= "rt-minimum";		// in seconds
	static final String CFGKEY_RT_MAX= "rt-maximum";
	static final String CFGKEY_MZ_MIN= "mz-minimum";		// in m/z units
	static final String CFGKEY_MZ_MAX= "mz-maximum";
	static final String CFGKEY_THRESHOLD_METHOD = "threshold-method";
	static final String CFGKEY_THRESHOLD = "intensity-threshold";		// value depends on method
	
	static final String[] THRESHOLD_METHODS = new String[] { "Minimum percentge of total ion current (per spectrum)", 
		"Absolute intensity", "Accept all peaks", "Reject intense peaks" };
	
	private final SettingsModelStringArray m_files = new SettingsModelStringArray(CFGKEY_FILES, new String[] {});
	private final SettingsModelDouble m_rt_min = new SettingsModelDouble(CFGKEY_RT_MIN, 300.0);
	private final SettingsModelDouble m_rt_max = new SettingsModelDouble(CFGKEY_RT_MAX, 1200.0);
	private final SettingsModelDouble m_mz_min = new SettingsModelDouble(CFGKEY_MZ_MIN, 100.0);
	private final SettingsModelDouble m_mz_max = new SettingsModelDouble(CFGKEY_MZ_MAX, 2000.0);
	private final SettingsModelString m_threshold_method = new SettingsModelString(CFGKEY_THRESHOLD_METHOD, THRESHOLD_METHODS[0]);
	private final SettingsModelDouble m_threshold = new SettingsModelDouble(CFGKEY_THRESHOLD, 0.1);
	
	
	// internal state -- persisted via saveInternals()
	Matrix m_surface;
	Matrix m_ms2_heatmap;
	
	// internal state -- not persisted
	private double m_rt_bin_width, m_mz_bin_width;
	
	// to avoid recomputing the surface we cache a reference to the last matrix (ie. after transformation)
	// and if the matrix hasn't changed, we dont recompute the surface we just return this member instead
	private CompileableComposite m_opengl_surface;
	private Matrix m_opengl_last_matrix;
	
    /**
     * Constructor for the node model.
     */
    protected MassSpecSurfaceNodeModel() {
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	logger.info("Creating surface... please wait.");
    	logger.info("RT limits: ["+m_rt_min.getDoubleValue()+","+m_rt_max.getDoubleValue()+"]");
    	logger.info("M/Z limits: ["+m_mz_min.getDoubleValue()+","+m_mz_max.getDoubleValue()+"]");
    	logger.info("Processing "+m_files.getStringArrayValue().length+" mzML files.");
    	
    	double rt_range = m_rt_max.getDoubleValue() - m_rt_min.getDoubleValue();
		double mz_range = m_mz_max.getDoubleValue() - m_mz_min.getDoubleValue();
    	int n_rt_bins = (int) Math.floor(rt_range / 0.1) + 1;	
    	int n_mz_bins = (int) Math.floor(mz_range / 0.1) + 1;
		logger.info("Calculating surface using "+n_rt_bins+" equal-sized RT bins and "+n_mz_bins+" equal-sized MZ bins.");

		
    	setBins(rt_range / n_rt_bins, mz_range / n_mz_bins);
    	m_opengl_surface = null;
    	m_opengl_last_matrix = null;
    	try {
	    	m_surface = new CRSFactory().createMatrix(n_rt_bins, n_mz_bins);
	    	m_ms2_heatmap = new CRSFactory().createMatrix(n_rt_bins, n_mz_bins);
	    	
	    	for (String fName : m_files.getStringArrayValue()) {
	    		logger.info("Processing file: "+fName);
	    		exec.checkCanceled();
	    		
	    		process_file(new File(fName), exec);
	    	}
    	} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		} catch (OutOfMemoryError mem) {
			mem.printStackTrace();
			throw mem;
		}
    
    	double non_zeroes = m_surface.fold(Matrices.asSumFunctionAccumulator(0.0, new MatrixFunction() {

			@Override
			public double evaluate(int arg0, int arg1, double arg2) {
				return (arg2 > 0.0) ? 1.0 : 0.0;
			}
    		
    	}));
    	
    	logger.info("Matrix contains "+(int)non_zeroes+" peaks, "+(non_zeroes/(n_rt_bins*n_mz_bins)*100.0d)+"% of the matrix.");
    	logger.info("Data matrix created.");
        return new BufferedDataTable[]{};
    }

    private void setBins(double rt_bin_width, double mz_bin_width) {
		m_rt_bin_width = rt_bin_width;
		m_mz_bin_width = mz_bin_width;
		logger.info("RT bin width: "+m_rt_bin_width+", MZ bin width: "+m_mz_bin_width);
	}
    
    private int getRTBin(double rt) {
    	rt -= m_rt_min.getDoubleValue();
    	if (rt < 0.0d || rt > m_rt_max.getDoubleValue())
    		return -1;
    	
    	int ret = ((int) Math.floor(rt / m_rt_bin_width));
    	if (ret >= m_surface.rows())			// RT outside desired range?
    		return -1;
    	//System.out.println("RT bin: "+ret);
    	return ret;
    }
    
    private int getMZBin(double mz) {
    	mz -= m_mz_min.getDoubleValue();
    	if (mz < 0.0d || mz > m_mz_max.getDoubleValue())
    		return -1;
    	
    	int ret = ((int) Math.floor(mz / m_mz_bin_width));
    	if (ret >= m_surface.columns())		// MZ outside desired range?
    		return -1;
    	
    	//System.out.println("MZ bin: "+ret);
    	return ret;
    }

	private void process_file(final File mzmlFile, final ExecutionContext exec) throws Exception {
    	Map<String,AbstractXMLMatcher> start_map = new HashMap<String,AbstractXMLMatcher>();
    	PeakThresholdFilter ptf = null;
    	String method = m_threshold_method.getStringValue();
    	if (method.equals(THRESHOLD_METHODS[0])) {
    		// percentage of the TIC
    		ptf = new PeakThresholdFilter() {
    			private BasicPeakList last = null;
    			private double last_sum_of_intensity = Double.NaN;
    			
				@Override
				public boolean accept(BasicPeakList pbl, Peak p,
						double threshold) {
					double sum_of_intensity = 0.0;
				
					if (pbl == last) {
						sum_of_intensity = last_sum_of_intensity;
					} else {
						last = pbl;
						for (Peak p2 : pbl.getPeaks()) {
							sum_of_intensity += p2.getIntensity();
						}
						last_sum_of_intensity = sum_of_intensity;
					}
					
					return (p.getIntensity() >= sum_of_intensity * (threshold / 100.d));
				}
    			
    		};
    	} else if (method.equals(THRESHOLD_METHODS[1])) {
    		ptf = new PeakThresholdFilter() {

				@Override
				public boolean accept(BasicPeakList pbl, Peak p,
						double threshold) {
					return (p.getIntensity() >= threshold);
				}
    			
    		};
    	} else if (method.equals(THRESHOLD_METHODS[3]))		{ // reject intense peaks? 
    		ptf = new PeakThresholdFilter() {

				@Override
				public boolean accept(BasicPeakList pbl, Peak p,
						double threshold) {
					return (p.getIntensity() < threshold);
				}
    			
    		};
    	}
    	SpectrumReader sr = new SpectrumReader(ptf);
		start_map.put("spectrum",  sr);
		start_map.put("binary", new BinaryMatcher());
		start_map.put("precursor", new PrecursorMatcher());
		start_map.put("selectedIon", new SelectedIonMatcher());
		start_map.put("binaryDataArray", new BinaryDataArrayMatcher());
		start_map.put("run", new RunMatcher());
		
		Map<String,AbstractXMLMatcher> end_map = new HashMap<String,AbstractXMLMatcher>();
		end_map.putAll(start_map);
		
		HashSet<String> ok_to_save = new HashSet<String>();
		ok_to_save.add("spectrum");
		ok_to_save.add("binaryDataArray");
		ok_to_save.add("binary");
		ok_to_save.add("precursor");
		ok_to_save.add("selectedIon");
		
		FileInputStream in = new FileInputStream(mzmlFile);
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        XMLStreamReader parser = factory.createXMLStreamReader(in);
        Stack<AbstractXMLMatcher> object_stack = new Stack<AbstractXMLMatcher>();
        
        for (int event = parser.next();
                event != XMLStreamConstants.END_DOCUMENT;
                event = parser.next()) {
          /**
      	   * Cant call getLocalName() unless its an element so...
      	   */
      	  if (event != XMLStreamConstants.START_ELEMENT && event != XMLStreamConstants.END_ELEMENT) {
      		  // need to pass encoded binary data to binary matcher?
      		  if (event == XMLStreamConstants.CHARACTERS && !object_stack.isEmpty()) {
      			  object_stack.peek().addCharacters(parser.getText());
      		  }
      		  continue;
      	  }
      	 
      	  String localName = parser.getLocalName();
          if (event == XMLStreamConstants.START_ELEMENT) {
        	    if (start_map.containsKey(localName)) {
	        	    AbstractXMLMatcher xm = start_map.get(localName);
	        	    assert(xm != null);
	        		object_stack.push(xm);		// must be done BEFORE processElement()
	        		xm.processElement(logger, parser, object_stack);
        	    } else if (localName.equals("cvParam") && !object_stack.isEmpty()) {
        	    	XMLMatcher xm = object_stack.peek();
        	    	// TODO: check XML namespace?
        	    	String cvRef = parser.getAttributeValue(null, "cvRef");
        	    	String accession = parser.getAttributeValue(null, "accession");
        	    	String name = parser.getAttributeValue(null, "name");
        	    	String value= parser.getAttributeValue(null, "value");
        	    	String unitAccsn = parser.getAttributeValue(null, "unitAccession");
        	    	String unitName = parser.getAttributeValue(null, "unitName");
        	    	xm.addCVParam(value, name, accession, cvRef, unitAccsn, unitName);
        	    }
          } else if (event == XMLStreamConstants.END_ELEMENT && end_map.containsKey(localName)) {
        		XMLMatcher xm = object_stack.pop();
        		if (ok_to_save.contains(localName)) {
        			xm.save(logger, null, null, mzmlFile);
        			exec.checkCanceled();
        		}
          }
        }
        parser.close();
        
        logger.info("Generating MS2 surface");
        int n_points = 0;
        for (String ms2 : sr.getMS2Scans()) {
        	double rt = sr.getRetentionTime(ms2);
        	double mz = sr.getMZ(ms2);
        	
        	if (!Double.isNaN(rt) && !Double.isNaN(mz)) {
        		int r = getRTBin(rt);
        		int c = getMZBin(mz);
        		if (r >= 0 && r<m_ms2_heatmap.rows() && c >= 0 && c<m_ms2_heatmap.columns()) {
        			m_ms2_heatmap.set(r, c, m_ms2_heatmap.get(r, c)+1.0);
        			n_points++;
        		}
        	}
        }
        logger.info("MS2 surface has "+n_points+" data points.");
        
        logger.info("Processed "+sr.getTotalMS1()+" MS1 scans, "+sr.getBadMS1()+" scans had no usable RT information.");
        sr.logPeakSummary(logger);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	m_surface = null;
    	m_ms2_heatmap = null;
    	m_opengl_surface = null;
    	m_opengl_last_matrix = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        // TODO: generated method stub
        return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_files.saveSettingsTo(settings);
         m_rt_min.saveSettingsTo(settings);
         m_rt_max.saveSettingsTo(settings);
         m_mz_min.saveSettingsTo(settings);
         m_mz_max.saveSettingsTo(settings);
         m_threshold_method.saveSettingsTo(settings);
         m_threshold.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	  m_files.loadSettingsFrom(settings);
          m_rt_min.loadSettingsFrom(settings);
          m_rt_max.loadSettingsFrom(settings);
          m_mz_min.loadSettingsFrom(settings);
          m_mz_max.loadSettingsFrom(settings);
          m_threshold_method.loadSettingsFrom(settings);
          m_threshold.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_files.validateSettings(settings);
    	
    	if (m_rt_min.getDoubleValue() > m_rt_max.getDoubleValue())
    		throw new InvalidSettingsException("RT minimum must be greater than RT max!");
    	if (m_mz_min.getDoubleValue() > m_mz_max.getDoubleValue())
    		throw new InvalidSettingsException("M/Z minimum must be greater than M/Z max!");
    	if (m_mz_min.getDoubleValue() < 0.0)
    		throw new InvalidSettingsException("Minimum MZ cannot be negative!");
    	if (m_rt_min.getDoubleValue() < 0.0)
    		throw new InvalidSettingsException("Retention Time cannot be negative!");
    	if (m_mz_max.getDoubleValue() < 0.0)
    		throw new InvalidSettingsException("Maximum MZ cannot be negative!");
    	if (m_rt_max.getDoubleValue() < 0.0)
    		throw new InvalidSettingsException("Retention Time cannot be negative!");
    	
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	reset();
    	File f = new File(internDir, "ms.surface.matrix");
    	MatrixStream ms = new MatrixMarketStream(new FileInputStream(f));
    	m_surface = ms.readMatrix(Matrices.CRS_FACTORY);
    	f = new File(internDir, "ms2.surface.matrix");
    	ms = new MatrixMarketStream(new FileOutputStream(f));
    	m_ms2_heatmap = ms.readMatrix(Matrices.CRS_FACTORY);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	File f = new File(internDir, "ms.surface.matrix");
    	MatrixStream ms = new MatrixMarketStream(new FileOutputStream(f));
    	ms.writeMatrix(m_surface);
    	f = new File(internDir, "ms2.surface.matrix");
    	ms = new MatrixMarketStream(new FileOutputStream(f));
    	ms.writeMatrix(m_ms2_heatmap);
    }

    public class SpectrumReader extends SpectrumMatcher {
    	private double  rt = Double.NaN;
    	private int bad_ms1 = 0;
    	private int total_ms1 = 0;
    	private PeakThresholdFilter peak_filter;
    	private final SummaryStatistics accepted_peak_stats = new SummaryStatistics();
    	// for keeping track of which MS1 gave birth to a MS2/MS3 scan...
    	private final HashMap<String,Double> scan2rt = new HashMap<String,Double>(10000);
    	private final HashMap<String,Integer> scan2mslevel = new HashMap<String,Integer>(10000);
    	private final HashMap<String,String> scan2parent = new HashMap<String,String>(10000);
    	private final HashMap<String,Double> scan2mz = new HashMap<String,Double>(10000);
    	
    	// constructor
    	public SpectrumReader() {
    		super(true);		// MUST load MS1 ;-)
    		peak_filter = null;
    	}
    	
    	public void logPeakSummary(NodeLogger logger) {
			logger.info("Accepted peaks: "+accepted_peak_stats.getN());
			logger.info("Minimum peak intensity accepted: "+accepted_peak_stats.getMin());
			logger.info("Maximum peak intensity accepted: "+accepted_peak_stats.getMax());
			logger.info("Mean accepted peak intensity: "+accepted_peak_stats.getMean());
			logger.info("SD of accepted peak intensity: "+accepted_peak_stats.getStandardDeviation());
		}

		public SpectrumReader(PeakThresholdFilter ptf) {
    		this();
    		peak_filter = ptf;
    	}
    	
    	@Override
    	public boolean hasMinimalMatchData() {
    		if (!super.hasMinimalMatchData())
    			return false;
    		
    		return (hasPeaks() && !Double.isNaN(rt));
    	}
    	
    	public int getTotalMS1() {
    		return total_ms1;
    	}
    	
    	public int getBadMS1() {
    		return bad_ms1;
    	}
    	
    	@Override
    	public void save(NodeLogger logger, MyDataContainer file_container,
    			MyDataContainer scan_container, File xml_file) {
    		// always store a scan into the internal state regardless of how much data was recorded...
    		scan2mslevel.put(getID(), getMSLevel());
    		String parent_id = getParentSpectrumID();
    		if (parent_id != null)
    			scan2parent.put(getID(), parent_id);
			
    		if (getMSLevel() == 1) {
    			double val = unbox_cell(getRT());
    			scan2rt.put(getID(), new Double(val));
    		} else if (getMSLevel() >= 2) {
    			double mz = unbox_cell(getPrecursorMZ());
    			scan2mz.put(getID(), new Double(mz));
    		}
    		
    		// NB: do NOT invoke the superclass method as it assumes the containers are non-null
    		if (hasMinimalMatchData() && getMSLevel() == 1) {
    			BasicPeakList pbl = makePeakList();
    			int rt_bin = getRTBin(rt);
    			for (Peak p : pbl.getPeaks()) {
    				int mz_bin = getMZBin(p.getMz());
    				double intensity = p.getIntensity();
    				if (rt_bin >= 0 && mz_bin >= 0 && (peak_filter == null || peak_filter.accept(pbl, p, m_threshold.getDoubleValue()))) {
    					
    					// increment peak in bin by the measured intensity
    					m_surface.set(rt_bin, mz_bin, m_surface.get(rt_bin, mz_bin)+intensity);
    					accepted_peak_stats.addValue(intensity);
    				}
    			}
    			total_ms1++;
    		} else {
    			if (getMSLevel() == 1) {
    				bad_ms1++;
    				total_ms1++;
    			}
    			
    		}
    		
    		rt = Double.NaN;
    	}
    	
		
		private double unbox_cell(DataCell dc) {
			if (dc == null || dc.isMissing())
				return Double.NaN;
			
			if (dc.getType().isCompatible(DoubleValue.class)) {
				DoubleValue dv = (DoubleValue) dc;
				return dv.getDoubleValue();
			} else if (dc.getType().isCompatible(StringValue.class)) {
				try {
					return Double.valueOf(((StringValue)dc).getStringValue());
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
					// FALLTHRU
				}
			}
			return Double.NaN;
		}

		@Override
		public void addCVParam(String value, String name, String accession, String cvRef, String unitAccession, String unitName) throws Exception {
			super.addCVParam(value, name, accession, cvRef, unitAccession, unitName);
			
			// get scan start time ie. retention time?
			if (accession.equals("MS:1000016") || name.equals("scan start time")) {
				if (!Double.isNaN(rt))
					throw new InvalidSettingsException("Already seen scan start time!");
				
				rt = Double.valueOf(value);		// must always be calculated in seconds
				if (unitName.trim().equalsIgnoreCase("minute"))
					rt *= 60.0d;
			}
		}
		
		/**********************************
		 * METHODS FOR node model to use
		 **********************************/
		public Collection<String> getMS2Scans() {
			ArrayList<String> ret = new ArrayList<String>();
			for (String s : scan2mslevel.keySet()) {
				Integer lvl = scan2mslevel.get(s);
				if (lvl != null && lvl.intValue() == 2) {
					ret.add(s);
				}
			}
			return ret;
		}
		
		public String getParent(String scanID) {
			if (scanID == null)
				return null;
			return scan2parent.get(scanID);
		}
		
		public double getMZ(String scanID) {
			Double mz = scan2mz.get(scanID);
			if (mz != null)
				return mz.doubleValue();
			return getMZ(scan2parent.get(scanID));
		}
		
		public double getRetentionTime(String scanID) {
			assert(scanID != null);
			Double rt = scan2rt.get(scanID);
			if (rt != null)
				return rt.doubleValue();
			return getRetentionTime(scan2parent.get(scanID));
		}
    }

    /**
     * Returns a deep copy of the internal state for the view to mess with (ie. transform)
     * @return the deep copy of the internal node model (row=RT, column=MZ, intensity equals sum of all peak intensity in bin)
     */
	public Matrix getSurface() {
		return m_surface.copy();
	}
	
	public CompileableComposite getOpenGLSurface(final Matrix matrix, 
									final Range x_range, final Range y_range, 
									final double z_min, final double z_range) {
		assert(matrix != null);
	
		// lazy computation
		if (m_opengl_surface != null && matrix == m_opengl_last_matrix)
			return m_opengl_surface;
		
		final int x_steps = (matrix.rows() < 500) ? matrix.rows()-1 : 500;
	    final int y_steps = (matrix.columns() < 500) ? matrix.columns()-1 : 500;
	    CompileableComposite surface = Builder.buildOrthonormalBig(
	    		new OrthonormalGrid(x_range, x_steps, y_range, y_steps), 
	    		new Mapper() {
	
	    	//
	    	// Given we have to downsample, what to report - mean, median, max, sum, ...? Go with max for now
	    	//
			@Override
			public double f(double x, double y) {
				int xdim = matrix.rows();
				int ydim = matrix.columns();
				int ix = (int) (x * xdim);
				int iy = (int) (y * ydim);
				int x_n = (int) Math.floor((double)xdim / x_steps) + 1;
				int y_n = (int) Math.floor((double)ydim / y_steps) + 1;
				int cnt = 0;
				/*double sum = 0.0;*/
				double max = Double.NEGATIVE_INFINITY;
				for (int i=ix - x_n; i<ix+x_n; i++) {
					for (int j=iy - y_n; j<iy + y_n; j++) {
						if (i<0 || i>= xdim)
							continue;
						if (j<0 || j>= ydim) 
							continue;
						double val = matrix.get(i, j);
						if (val <= 0.0)
							continue;
						//sum += val;
						if (val > max)
							max = val;
						cnt++;
					}
				}
				if (cnt < 1)
					return 0.0d;
				//return ((sum/cnt) - z_min) / z_range;*/
				
				return (max - z_min) / z_range;
			}
    	
	    });
	    
	    m_opengl_surface = surface;
	    m_opengl_last_matrix = matrix;
	    return surface;
	}
    
	/**
	 * Returns a shallow copy of the MS2 surface, since we know the view does not modify it in any way. 
	 * @return
	 * 
	 */
	public Matrix getMS2Surface() {
		return m_ms2_heatmap;
	}
	
	public double getMZmin() {
		return m_mz_min.getDoubleValue();
	}
	
	public double getMZmax() {
		return m_mz_max.getDoubleValue();
	}
	
	public double getRTmin() {
		return m_rt_min.getDoubleValue();
	}
	
	public double getRTmax() {
		return m_rt_max.getDoubleValue();
	}
}

