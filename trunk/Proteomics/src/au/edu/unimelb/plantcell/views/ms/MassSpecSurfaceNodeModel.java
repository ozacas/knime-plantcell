package au.edu.unimelb.plantcell.views.ms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealMatrixPreservingVisitor;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
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
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

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
	
	static final String[] THRESHOLD_METHODS = new String[] { "Minimum percentge of total ion current (per spectrum)", "Absolute intensity", "Accept all peaks" };
	
	private final SettingsModelStringArray m_files = new SettingsModelStringArray(CFGKEY_FILES, new String[] {});
	private final SettingsModelDouble m_rt_min = new SettingsModelDouble(CFGKEY_RT_MIN, 300.0);
	private final SettingsModelDouble m_rt_max = new SettingsModelDouble(CFGKEY_RT_MAX, 1200.0);
	private final SettingsModelDouble m_mz_min = new SettingsModelDouble(CFGKEY_MZ_MIN, 100.0);
	private final SettingsModelDouble m_mz_max = new SettingsModelDouble(CFGKEY_MZ_MAX, 2000.0);
	private final SettingsModelString m_threshold_method = new SettingsModelString(CFGKEY_THRESHOLD_METHOD, THRESHOLD_METHODS[0]);
	private final SettingsModelDouble m_threshold = new SettingsModelDouble(CFGKEY_THRESHOLD, 0.1);
	
	
	// internal state -- persisted via saveInternals()
	RealMatrix m_surface;
	
	// internal state -- not persisted
	private double m_rt_bin_width, m_mz_bin_width;
	
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
    	int n_mz_bins = (int) Math.floor(mz_range / 0.2) + 1;
		logger.info("Calculating surface using "+n_rt_bins+" equal-sized RT bins and "+n_mz_bins+" equal-sized MZ bins.");

		
    	setBins(rt_range / n_rt_bins, mz_range / n_mz_bins);
    	m_surface = MatrixUtils.createRealMatrix(n_rt_bins, n_mz_bins);
    	
    	for (String fName : m_files.getStringArrayValue()) {
    		logger.info("Processing file: "+fName);
    		exec.checkCanceled();
    		try {
    			process_file(new File(fName), exec);
    		} catch (Exception ex) {
    			ex.printStackTrace();
    			throw ex;
    		} catch (OutOfMemoryError mem) {
    			mem.printStackTrace();
    			throw mem;
    		}
    	}
    	// how sparse is this matrix?
    	
    	double non_zeroes = m_surface.walkInColumnOrder(new RealMatrixPreservingVisitor() {
    		long n_nonzero;
    		
			@Override
			public double end() {
				return n_nonzero;
			}

			@Override
			public void start(int arg0, int arg1, int arg2, int arg3, int arg4,
					int arg5) {
				n_nonzero = 0;
			}

			@Override
			public void visit(int arg0, int arg1, double arg2) {
				if (arg2 > 0.0)
					n_nonzero++;
			}
    		
    	});
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
    	if (ret >= m_surface.getRowDimension())			// RT outside desired range?
    		return -1;
    	//System.out.println("RT bin: "+ret);
    	return ret;
    }
    
    private int getMZBin(double mz) {
    	mz -= m_mz_min.getDoubleValue();
    	if (mz < 0.0d || mz > m_mz_max.getDoubleValue())
    		return -1;
    	
    	int ret = ((int) Math.floor(mz / m_mz_bin_width));
    	if (ret >= m_surface.getColumnDimension())		// MZ outside desired range?
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
        
        
        logger.info("Processed "+sr.getTotalMS1()+" MS1 scans, "+sr.getBadMS1()+" scans had no usable RT information.");
        sr.logPeakSummary(logger);
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
    	File f = new File(internDir, "ms.surface.internals");
    	try {
    		BufferedReader rdr = new BufferedReader(new FileReader(f));
    		int n_rows = Integer.valueOf(rdr.readLine());
    		int n_cols = Integer.valueOf(rdr.readLine());
    		m_surface = MatrixUtils.createRealMatrix(n_rows, n_cols);
    		String line;
    		while ((line = rdr.readLine()) != null) {
    			String[] fields = line.split("\\s+");
    			Integer row = Integer.valueOf(fields[0]);
    			Integer col = Integer.valueOf(fields[1]);
    			Double  val = Double.valueOf(fields[2]);
    			m_surface.setEntry(row, col, val);
    		}
    	} catch (Exception ex) {
    		ex.printStackTrace();
    		throw new IOException(ex.getMessage());
    	}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	File f = new File(internDir, "ms.surface.internals");
    	PrintWriter pw = new PrintWriter(f);
    	pw.println(m_surface.getRowDimension());
    	pw.println(m_surface.getColumnDimension());
    	
    	// since the matrix is sparse (typically about 5% of bins) we only save non-zero values
    	for (int i=0; i<m_surface.getRowDimension(); i++) {
    		for (int j=0; j<m_surface.getColumnDimension(); j++) {
    			double val = m_surface.getEntry(i, j);
    			if (val > 0.0) {
    				pw.println(""+i+" "+j+" "+val);
    			}
    		}
    	}
    	pw.close();
    }

    public class SpectrumReader extends SpectrumMatcher {
    	private double  rt = Double.NaN;
    	private int bad_ms1 = 0;
    	private int total_ms1 = 0;
    	private PeakThresholdFilter peak_filter;
    	private final SummaryStatistics accepted_peak_stats = new SummaryStatistics();
    	
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
    		
    		// NB: do NOT invoke the superclass method as it assumes the containers are non-null
    		if (hasMinimalMatchData() && getMSLevel() == 1) {
    			BasicPeakList pbl = makePeakList();
    			int rt_bin = getRTBin(rt);
    			for (Peak p : pbl.getPeaks()) {
    				int mz_bin = getMZBin(p.getMz());
    				double intensity = p.getIntensity();
    				if (rt_bin >= 0 && mz_bin >= 0 && (peak_filter == null || peak_filter.accept(pbl, p, m_threshold.getDoubleValue()))) {
    					m_surface.addToEntry(rt_bin, mz_bin, intensity);
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
    }

    /**
     * Returns a deep copy of the internal state for the view to mess with (ie. transform)
     * @return the deep copy of the internal node model (row=RT, column=MZ, intensity equals sum of all peak intensity in bin)
     */
	public RealMatrix getSurface() {
		return m_surface.copy();
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

