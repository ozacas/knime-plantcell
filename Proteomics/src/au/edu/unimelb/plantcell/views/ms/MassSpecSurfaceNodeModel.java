package au.edu.unimelb.plantcell.views.ms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

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
import org.la4j.factory.CRSFactory;
import org.la4j.io.MatrixMarketStream;
import org.la4j.io.MatrixStream;
import org.la4j.matrix.Matrices;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.functor.MatrixFunction;

import au.edu.unimelb.plantcell.io.read.spectra.AbstractXMLMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.BasicPeakList;
import au.edu.unimelb.plantcell.io.read.spectra.BinaryDataArrayMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.BinaryMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.PrecursorMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.RunMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.SelectedIonMatcher;
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
	static final String CFGKEY_DISPLAY_METHOD = "display-method";
	
	static final String[] THRESHOLD_METHODS = new String[] { "Minimum percentage of total ion current (per spectrum)", 
		"Absolute intensity", "Accept all peaks", "Reject intense peaks" };
	static final String[] MS2_DISPLAY_METHODS = new String[] { "All points colour black", "Spectral Quality Score (Xrea)", "MS/MS precursor charge (predicted)" };
	
	private final SettingsModelStringArray m_files = new SettingsModelStringArray(CFGKEY_FILES, new String[] {});
	private final SettingsModelDouble m_rt_min = new SettingsModelDouble(CFGKEY_RT_MIN, 300.0);
	private final SettingsModelDouble m_rt_max = new SettingsModelDouble(CFGKEY_RT_MAX, 1200.0);
	private final SettingsModelDouble m_mz_min = new SettingsModelDouble(CFGKEY_MZ_MIN, 100.0);
	private final SettingsModelDouble m_mz_max = new SettingsModelDouble(CFGKEY_MZ_MAX, 2000.0);
	private final SettingsModelString m_threshold_method = new SettingsModelString(CFGKEY_THRESHOLD_METHOD, THRESHOLD_METHODS[0]);
	private final SettingsModelDouble m_threshold = new SettingsModelDouble(CFGKEY_THRESHOLD, 0.1);
	private final SettingsModelString m_display_method = new SettingsModelString(CFGKEY_DISPLAY_METHOD, MS2_DISPLAY_METHODS[0]);
	
	
	// internal state -- persisted via saveInternals()
	private SurfaceMatrixAdapter m_surface;
	private SurfaceMatrixAdapter m_ms2_heatmap;
	private double m_rt_bin_width, m_mz_bin_width;
	
	// internal state -- not persisted
    private final HashMap<String,SurfaceMatrixAdapter> m_matrix_cache = new HashMap<String,SurfaceMatrixAdapter>();
	
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
    	
     	int n_bins = calcBinWidths();
    	try {
	    	m_surface     = new SurfaceMatrixAdapter(new CRSFactory().createMatrix(n_bins, n_bins), false);		// false: mark as mzML surface
	    	m_surface.setBounds(m_rt_min.getDoubleValue(), m_rt_max.getDoubleValue(), m_mz_min.getDoubleValue(), m_mz_max.getDoubleValue());
	    	m_surface.setKey(null);
	    	m_ms2_heatmap = new SurfaceMatrixAdapter(new CRSFactory().createMatrix(n_bins, n_bins), true);		// true: ms2 heatmap
	    	m_ms2_heatmap.setBounds(m_rt_min.getDoubleValue(), m_rt_max.getDoubleValue(), m_mz_min.getDoubleValue(), m_mz_max.getDoubleValue());
	    	m_ms2_heatmap.setKey(null);
	    	
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
    
    	Matrix in = m_surface.getMatrix();
    	double non_zeroes = in.fold(Matrices.asSumFunctionAccumulator(0.0, new MatrixFunction() {

			@Override
			public double evaluate(int arg0, int arg1, double arg2) {
				return (arg2 > 0.0) ? 1.0 : 0.0;
			}
    		
    	}));
    	
    	addMatrixToCache(m_surface);
    	addMatrixToCache(m_ms2_heatmap);
    	
    	logger.info("Matrix contains "+(int)non_zeroes+" peaks, "+(non_zeroes/(in.rows()*in.columns())*100.0d)+"% of the matrix.");
    	logger.info("Data matrix created.");
        return new BufferedDataTable[]{};
    }
    
    private int calcBinWidths() {
    	return calcBinWidths(m_rt_min.getDoubleValue(), m_rt_max.getDoubleValue(), m_mz_min.getDoubleValue(), m_mz_max.getDoubleValue());
	}
    
    private int calcBinWidths(double rt_min, double rt_max, double mz_min, double mz_max) {
    	double rt_range = rt_max - rt_min;
		double mz_range = mz_max - mz_min;
		
    	int n_bins = (int) Math.floor((rt_range > mz_range ? rt_range : mz_range) / 0.1) + 1;	
		logger.info("Calculating surface using "+n_bins+" equal-sized bins for RT & MZ data.");
		m_rt_bin_width = rt_range / n_bins;
		m_mz_bin_width = mz_range / n_bins;
		logger.info("RT bin width: "+m_rt_bin_width+", MZ bin width: "+m_mz_bin_width);
		return n_bins;
    }
    
    public int getRTBin(double rt) {
    	return getRTBin(rt, m_surface, m_rt_bin_width);
    }
    
    /**
     * NB: must not rely on class members and ONLY use the parameters provided
     * @param rt
     * @param surface
     * @param min
     * @param max
     * @param bin_width
     * @return
     */
    public int getRTBin(double rt, final SurfaceMatrixAdapter surface, double bin_width) {
    	rt -= surface.getYMin();
    	if (rt < 0.0d || rt > surface.getYMax())
    		return -1;
    	
    	int ret = ((int) Math.floor(rt / bin_width));
    	if (ret >= surface.rows())			// RT outside desired range?
    		return -1;
    	//System.out.println("RT bin: "+ret);
    	return ret;
    }
    
    public int getMZBin(double mz) {
    	return getMZBin(mz, m_surface, m_mz_bin_width);
    }
    
    /**
     * NB: must not rely on class members and ONLY use the parameters provided
     * @param mz
     * @param surface
     * @param min
     * @param max
     * @param bin_width
     * @return
     */
    public int getMZBin(double mz, final SurfaceMatrixAdapter surface, double bin_width) {
    	mz -= surface.getXMin();
    	if (mz < 0.0d || mz > surface.getXMax())
    		return -1;
    	
    	int ret = ((int) Math.floor(mz / bin_width));
    	if (ret >= surface.columns())		// MZ outside desired range?
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
    	SpectrumReader<MassSpecSurfaceNodeModel> sr = null;
    	String display_method = m_display_method.getStringValue();
    	if (display_method.startsWith("All"))
    		sr = new SpectrumReader<MassSpecSurfaceNodeModel>(m_surface, this, ptf, m_threshold.getDoubleValue());
    	else if (display_method.equals(MS2_DISPLAY_METHODS[1])) {
    		sr = new QualitySpectrumReader<MassSpecSurfaceNodeModel>(m_surface, this, ptf, m_threshold.getDoubleValue());
    	} else if (display_method.startsWith(MS2_DISPLAY_METHODS[2])) {
    		sr = new PrecursorChargeSpectrumReader<MassSpecSurfaceNodeModel>(m_surface, this, ptf, m_threshold.getDoubleValue());
    	} else {
    		throw new InvalidSettingsException("MS2 scoring method "+m_display_method.getStringValue()+" is not implemented!");
    	}
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
        			double val = m_ms2_heatmap.get(r, c);
        			if (val <= 0.0) {
        				val = sr.getMS2Score(ms2);
        			}
        			m_ms2_heatmap.set(r, c, val);
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
         m_display_method.saveSettingsTo(settings);
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
          m_display_method.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_files.validateSettings(settings);
    	m_threshold_method.validateSettings(settings);
    	m_display_method.validateSettings(settings);
    	
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
    	m_surface = new SurfaceMatrixAdapter(ms.readMatrix(Matrices.CRS_FACTORY));
    	f = new File(internDir, "ms2.surface.matrix");
    	ms = new MatrixMarketStream(new FileInputStream(f));
    	m_ms2_heatmap = new SurfaceMatrixAdapter(ms.readMatrix(Matrices.CRS_FACTORY));
    	f = new File(internDir, "matrix.settings");
    	BufferedReader rdr = new BufferedReader(new FileReader(f));
    	m_rt_bin_width = Double.valueOf(rdr.readLine());
    	m_mz_bin_width = Double.valueOf(rdr.readLine());
    	m_surface.readInternals(rdr);
    	m_ms2_heatmap.readInternals(rdr);
    	rdr.close();
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
    	ms.writeMatrix(m_surface.getMatrix());
    	f = new File(internDir, "ms2.surface.matrix");
    	ms = new MatrixMarketStream(new FileOutputStream(f));
    	ms.writeMatrix(m_ms2_heatmap.getMatrix());
    	f = new File(internDir, "matrix.settings");
    	PrintWriter pw = new PrintWriter(new FileWriter(f));
    	pw.println(m_rt_bin_width);
    	pw.println(m_mz_bin_width);
    	m_surface.saveInternals(pw);
    	m_ms2_heatmap.saveInternals(pw);
    	pw.close();
    }


    /**
     * Returns the actual surface (NOT copied)
     */
	public SurfaceMatrixAdapter getSurface() {
		if (m_surface == null)
			return null;
		return m_surface;
	}
	
	@SuppressWarnings("unused")
	private boolean hasCachedMatrix(SurfaceMatrixAdapter in, double rt_min, double rt_max, double mz_min, double mz_max, boolean is_ms2) {
		    String key = in.makeKey(rt_min, rt_max, mz_min, mz_max);
	    	return hasCachedMatrix(key);
	}
	 
	private boolean hasCachedMatrix(String key) {
		 	if (m_matrix_cache.containsKey(key)) {
	    		return true;
	    	}
	    	return false;
	}
	    
	public SurfaceMatrixAdapter getCachedMatrix(final String matrix_name) {
	    	SurfaceMatrixAdapter m = m_matrix_cache.get(matrix_name);
	    	if (m != null)
	    		return m;
	    	return null;
	}
	    
		
	private SurfaceMatrixAdapter addMatrixToCache(SurfaceMatrixAdapter surface) {
		 	m_matrix_cache.put(surface.getKey(), surface);
			return surface;
	}
	    
	/**
	 * If you call this method then you MUST call the corresponding <code>getMS2Surface(rt_min,rt_max,mz_min,mz_max)</code> or your matrices will not have the same dimensions!
	 * 
	 * @param rt_min
	 * @param rt_max
	 * @param mz_min
	 * @param mz_max
	 * @return
	 */
	public SurfaceMatrixAdapter getSurface(SurfaceMatrixAdapter in, double rt_min, double rt_max, double mz_min, double mz_max) {
		assert(rt_min < rt_max && mz_min < mz_max && rt_max > 0.0 && mz_max > 0.0);
		
		// node not executed?
		if (in == null) {
			return null;
		}
		
		// else...
		int fromRow     = getRTBin(rt_min, in, m_rt_bin_width);
		if (fromRow < 0)
			fromRow = 0;
		int untilRow    = getRTBin(rt_max, in, m_rt_bin_width);		
		if (untilRow == -1)
			untilRow = in.rows();
		else 
			untilRow++;				// +1 to ensure last bin is >=rt_max
		int fromColumn  = getMZBin(mz_min, in, m_mz_bin_width);
		if (fromColumn < 0)
			fromColumn = 0;
		int untilColumn = fromColumn + (untilRow - fromRow);
		if (untilColumn < 0)
			untilColumn = in.columns();
		else 
			untilColumn++;
		int n = Math.max(untilColumn - fromColumn, untilRow - fromRow);
		if (n > 0 && fromRow >= 0 && fromColumn >= 0) {
			int lastRow = Math.min(in.rows()-1, fromRow + n);
			int lastCol = Math.min(in.rows()-1, fromColumn + n);
			
			String key = in.makeKey(rt_min, rt_max, mz_min, mz_max);
			if (hasCachedMatrix(key)) {
				return getCachedMatrix(key);
			} else {
				SurfaceMatrixAdapter surface = new SurfaceMatrixAdapter(in.getMatrix().slice(fromRow, fromColumn, lastRow, lastCol));
				// since the matrix bounds may have been adjusted (above), we must setBounds not to the input parameters but to the new matrix dimensions
				double new_mz_max = mz_min + m_mz_bin_width * n;
				double new_rt_max = rt_min + m_rt_bin_width * n;
				surface.setBounds(mz_min, new_mz_max, rt_min, new_rt_max);
				surface.setKey(key);
				logger.info("Slice has "+surface.rows()+" rows and "+surface.columns()+" columns MZ["+mz_min+", "+new_mz_max+"] RT["+rt_min+", "+new_rt_max+"]");
				logger.info("Surface has key: "+key);
				return addMatrixToCache(surface);
			}
		} else {
			logger.warn("Bogus matrix dimensions: not producing surface!");
			logger.warn("Wanted to extract RT: ["+fromRow+", "+untilRow+"] M/Z ["+fromColumn+", "+untilColumn+"]");
			return null;
		}
	}


	/**
	 * Returns the MS2 "heatmap" matrix. Only call this if your view uses the <code>getSurface()</code> ie. without
	 * any parameters so that the matrices have the same dimensions.
	 * 
	 * @return
	 * 
	 */
	public SurfaceMatrixAdapter getMS2Surface() {
		if (m_ms2_heatmap == null)
			return null;
		calcBinWidths();
		return m_ms2_heatmap;
	}
	
	public SurfaceMatrixAdapter getMS2Surface(double rt_min, double rt_max, double mz_min, double mz_max) {
		return getSurface(m_ms2_heatmap, rt_min, rt_max, mz_min, mz_max);
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

	/**
	 * Convenience wrapper around <code>getSurface(SurfaceMatrixAdapter sma,...)</code> where you dont
	 * need to know the matrix to make the call. Just which one you want. Uses a cached matrix if possible.
	 * 
	 * @param want_ms2
	 * @param yMin
	 * @param yMax
	 * @param xMin
	 * @param xMax
	 * @return
	 */
	public SurfaceMatrixAdapter getSurface(boolean want_ms2, double yMin, double yMax, double xMin, double xMax) {
		return getSurface(!want_ms2 ? m_surface : m_ms2_heatmap, yMin, yMax, xMin, xMax);
	}
}

