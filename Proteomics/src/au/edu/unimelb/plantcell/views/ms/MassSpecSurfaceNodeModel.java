package au.edu.unimelb.plantcell.views.ms;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.expasy.jpl.core.ms.lc.RetentionTime;
import org.expasy.jpl.core.ms.lc.RetentionTime.RTUnit;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
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
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.la4j.factory.CRSFactory;
import org.la4j.matrix.Matrices;
import org.la4j.matrix.functor.MatrixFunction;

import au.edu.unimelb.plantcell.io.read.spectra.AbstractXMLMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.BasicPeakList;
import au.edu.unimelb.plantcell.io.read.spectra.BinaryDataArrayMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.BinaryMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.PrecursorMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.RunMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.SelectedIonMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.XMLMatcher;
import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;

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
	static final String CFGKEY_SPECTRA_COLUMN = "ms2-spectra-column";	// if processing input table then which column to use?
	static final String CFGKEY_SPECTRA_FROM   = "ms2-spectra-from";		// mzml file(s), input table or both
	
	// order of the items in these three arrays is VERY important to the code
	static final String[] THRESHOLD_METHODS = new String[] { "Minimum percentage of total ion current (per spectrum)", 
		"Absolute intensity", "Accept all peaks", "Reject intense peaks" };
	static final String[] MS2_DISPLAY_METHODS = new String[] { "All points colour black", "Spectral Quality Score (Xrea)", "MS/MS precursor charge (predicted)", "User defined" };
	static final String[] MS2_SPECTRA_FROM = new String[] { "mzML input files only", "input table (set column below)", "both" };
	
	// persisted configuration state
	private final SettingsModelStringArray m_files       = new SettingsModelStringArray(CFGKEY_FILES, new String[] {});
	private final SettingsModelDouble m_rt_min           = new SettingsModelDouble(CFGKEY_RT_MIN, 300.0);
	private final SettingsModelDouble m_rt_max           = new SettingsModelDouble(CFGKEY_RT_MAX, 1200.0);
	private final SettingsModelDouble m_mz_min           = new SettingsModelDouble(CFGKEY_MZ_MIN, 100.0);
	private final SettingsModelDouble m_mz_max           = new SettingsModelDouble(CFGKEY_MZ_MAX, 2000.0);
	private final SettingsModelString m_threshold_method = new SettingsModelString(CFGKEY_THRESHOLD_METHOD, THRESHOLD_METHODS[0]);
	private final SettingsModelDouble m_threshold        = new SettingsModelDouble(CFGKEY_THRESHOLD, 0.1);
	private final SettingsModelString m_display_method   = new SettingsModelString(CFGKEY_DISPLAY_METHOD, MS2_DISPLAY_METHODS[0]);
	private final SettingsModelString m_spectra_from     = new SettingsModelString(CFGKEY_SPECTRA_FROM, MS2_SPECTRA_FROM[0]);
	private final SettingsModelString m_spectra_col      = new SettingsModelString(CFGKEY_SPECTRA_COLUMN, "");	// only used iff m_spectra_from != mzML input files
	
	// internal state -- persisted via saveInternals()
	private final HashMap<File,SurfaceMatrixAdapter> m_surfaces = new HashMap<File,SurfaceMatrixAdapter>();
	private final HashMap<File,SurfaceMatrixAdapter> m_ms2_heatmaps = new HashMap<File,SurfaceMatrixAdapter>();
	
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
    	int spectra_idx = -1;
    	if (!m_spectra_from.getStringValue().equals(MS2_SPECTRA_FROM[0])) {
    		spectra_idx = inData[0].getSpec().findColumnIndex(m_spectra_col.getStringValue());
    		if (spectra_idx < 0) {
    			throw new InvalidSettingsException("Cannot locate MS/MS spectra column: "+m_spectra_col.getStringValue());
    		}
    	}
    	
     	int n_bins = calcBinWidths();
    	try {
	    	for (String fName : m_files.getStringArrayValue()) {
	    		logger.info("Processing file: "+fName);
	    		File f = new File(fName);
	    		SurfaceMatrixAdapter surface     = new SurfaceMatrixAdapter(f, new CRSFactory().createMatrix(n_bins, n_bins), false);		// false: mark as mzML surface
		    	surface.setBounds(m_mz_min.getDoubleValue(), m_mz_max.getDoubleValue(), m_rt_min.getDoubleValue(), m_rt_max.getDoubleValue());
		    	surface.setKey(null);		// recompute the key
		    	SurfaceMatrixAdapter ms2_heatmap = new SurfaceMatrixAdapter(f, new CRSFactory().createMatrix(n_bins, n_bins), true);		// true: ms2 heatmap
		    	ms2_heatmap.setBounds(m_mz_min.getDoubleValue(), m_mz_max.getDoubleValue(), m_rt_min.getDoubleValue(), m_rt_max.getDoubleValue());
		    	ms2_heatmap.setKey(null);	// recompute the key
		    	m_surfaces.put(f, surface);
		    	m_ms2_heatmaps.put(f,  ms2_heatmap);
	    		exec.checkCanceled();
	    		
	    		/*
	    		 * 1. process each mzml file in turn, loading MS2 data if the user configured it
	    		 */
	    		process_file(f, exec, !m_spectra_from.getStringValue().equals(MS2_SPECTRA_FROM[1]));
	    		
	        	double non_zeroes = surface.getMatrix().fold(Matrices.asSumFunctionAccumulator(0.0, new MatrixFunction() {

	    			@Override
	    			public double evaluate(int arg0, int arg1, double arg2) {
	    				return (arg2 > 0.0) ? 1.0 : 0.0;
	    			}
	        		
	        	}));
	        	
	        	/*
	        	 * 2. if the user requested it, we load the MS2 spectra from the input table to merge it with the surface read from the file.
	        	 */
	        	if (!m_spectra_from.getStringValue().equals(MS2_SPECTRA_FROM[0])) {
	        		exec.checkCanceled();
	        		logger.info("Loading from input table... ");
	        		int n_spectra = 0;
	        		for (DataRow r : inData[0]) {
	        			DataCell spectra_cell = r.getCell(spectra_idx);
	        			if (spectra_cell == null || spectra_cell.isMissing() || !(spectra_cell instanceof SpectraValue))
	        				continue;
	        			SpectraValue sv = (SpectraValue) spectra_cell;
	        			Peak  precursor = sv.getPrecursor();
	        			if (precursor != null && precursor.getRT() != null && precursor.getMz() > 0.0) {
	        				double rt_in_secs = rt2secs(precursor.getRT());
	        				double mz = precursor.getMz();
	        				int row = getRTBin(f, rt_in_secs);
	        				int col = getMZBin(f, mz);
	        				ms2_heatmap.set(row, col, 1.0);
	        				n_spectra++;
	        			}
	        		}
	        		logger.info("Loaded "+n_spectra+" data points from input table.");
	        	}
	        	
	        	/*
	        	 * 3. finalise the surface matrices and store in the nodes internal state (persisted via save/load internals)
	        	 */
	        	m_surfaces.put(f, surface);
	        	m_ms2_heatmaps.put(f, ms2_heatmap);
	        	addMatrixToCache(surface);
	        	addMatrixToCache(ms2_heatmap);
	        	logger.info("Matrix contains "+(int)non_zeroes+" peak bins.");
	    	}
    	} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		} catch (OutOfMemoryError mem) {
			mem.printStackTrace();
			throw mem;
		}
    
    
    	logger.info("mzML surfaces created.");
        return new BufferedDataTable[]{};
    }
    
    /**
     * Convert the retention time into seconds. Scan-based retention time values will be returned unmodified.
     * @param rt
     * @return
     */
    private double rt2secs(final RetentionTime rt) {
		if (rt == null)
			return 0.0;
		double ret = rt.getValue();
		if (rt.getUnit().equals(RTUnit.minute))
				ret *= 60.0;
		return ret;
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
    
    public int getRTBin(File f, double rt) {
    	return getRTBin(rt, m_surfaces.get(f), m_rt_bin_width);
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
    
    public int getMZBin(final File f, double mz) {
    	return getMZBin(mz, m_surfaces.get(f), m_mz_bin_width);
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

	private void process_file(final File mzmlFile, final ExecutionContext exec, boolean want_ms2_loaded) throws Exception {
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
    		sr = new SpectrumReader<MassSpecSurfaceNodeModel>(m_surfaces.get(mzmlFile), this, ptf, m_threshold.getDoubleValue());
    	else if (display_method.equals(MS2_DISPLAY_METHODS[1])) {
    		sr = new QualitySpectrumReader<MassSpecSurfaceNodeModel>(m_surfaces.get(mzmlFile), this, ptf, m_threshold.getDoubleValue());
    	} else if (display_method.startsWith(MS2_DISPLAY_METHODS[2])) {
    		sr = new PrecursorChargeSpectrumReader<MassSpecSurfaceNodeModel>(m_surfaces.get(mzmlFile), this, ptf, m_threshold.getDoubleValue());
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
        
        if (want_ms2_loaded) {
	        logger.info("Generating MS2 surface");
	        int n_points = 0;
	        SurfaceMatrixAdapter ms2_heatmap = m_ms2_heatmaps.get(mzmlFile);
	        for (String ms2 : sr.getMS2Scans()) {
	        	double rt = sr.getRetentionTime(ms2);
	        	double mz = sr.getMZ(ms2);
	        	
	        	if (!Double.isNaN(rt) && !Double.isNaN(mz)) {
	        		int r = getRTBin(mzmlFile, rt);
	        		int c = getMZBin(mzmlFile, mz);
	        		
	        		if (r >= 0 && r<ms2_heatmap.rows() && c >= 0 && c<ms2_heatmap.columns()) {
	        			double val = ms2_heatmap.get(r, c);
	        			if (val <= 0.0) {
	        				val = sr.getMS2Score(ms2);
	        			}
	        			ms2_heatmap.set(r, c, val);
	        			n_points++;
	        		}
	        	}
	        }
	        logger.info("MS2 surface has "+n_points+" data points.");
        }
        
        logger.info("Processed "+sr.getTotalMS1()+" MS1 scans, "+sr.getBadMS1()+" scans had no usable RT information.");
        sr.logPeakSummary(logger);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	// return the matrices to the GC...
    	m_surfaces.clear();
    	m_ms2_heatmaps.clear();
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
         m_spectra_from.saveSettingsTo(settings);
         m_spectra_col.saveSettingsTo(settings);
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
          m_spectra_from.loadSettingsFrom(settings);
          m_spectra_col.loadSettingsFrom(settings);
          
          if (m_files.getStringArrayValue().length < 1)
          	throw new InvalidSettingsException("You must add mzML files to use this node (see mzML Files tab)!");
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
    	m_spectra_from.validateSettings(settings);
        m_spectra_col.validateSettings(settings);
           
      
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
    	/*File f = new File(internDir, "ms.surface.matrix");
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
    	rdr.close();*/
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	/*File f = new File(internDir, "ms.surface.matrix");
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
    	pw.close();*/
    }

    public boolean wantMS2fromMzML() {
    	return (!m_spectra_from.getStringValue().equals(MS2_SPECTRA_FROM[1]));
    }
    
    /**************************** METHODS FOR THE VIEW TO USE *******************************/

    /**
     * Returns the actual surface (NOT copied) since we know the view will downsample (ie. copy) it
     */
	public SurfaceMatrixAdapter getSurface(final File mzmlFile) {
		if (mzmlFile == null)
			return null;
		return m_surfaces.get(mzmlFile);
	}
	
	@SuppressWarnings("unused")
	private boolean hasCachedMatrix(final SurfaceMatrixAdapter in, double x_min, double x_max, double y_min, double y_max, boolean is_ms2) {
		    in.setBounds(x_min, x_max, y_min, y_max);
		    in.setKey(null);
	    	return hasCachedMatrix(in.getKey());
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
	    
		
	private SurfaceMatrixAdapter addMatrixToCache(final SurfaceMatrixAdapter surface) {
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
	public SurfaceMatrixAdapter getSurface(final File mzmlFile, final SurfaceMatrixAdapter in, 
										double rt_min, double rt_max, double mz_min, double mz_max) {
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
			
			in.setBounds(mz_min, mz_max, rt_min, rt_max);
			if (hasCachedMatrix(in.getKey())) {
				return getCachedMatrix(in.getKey());
			} else {
				SurfaceMatrixAdapter surface = new SurfaceMatrixAdapter(mzmlFile, in.getMatrix().slice(fromRow, fromColumn, lastRow, lastCol));
				// since the matrix bounds may have been adjusted (above), we must setBounds not to the input parameters but to the new matrix dimensions
				double new_mz_max = mz_min + m_mz_bin_width * n;
				double new_rt_max = rt_min + m_rt_bin_width * n;
				surface.setBounds(mz_min, new_mz_max, rt_min, new_rt_max);
				surface.setKey(in.getKey());
				logger.info("Slice has "+surface.rows()+" rows and "+surface.columns()+" columns MZ["+mz_min+", "+new_mz_max+"] RT["+rt_min+", "+new_rt_max+"]");
				logger.info("Surface has key: "+in.getKey());
				return addMatrixToCache(surface);
			}
		} else {
			logger.warn("Bogus matrix dimensions: not producing surface!");
			logger.warn("Wanted to extract RT: ["+fromRow+", "+untilRow+"] M/Z ["+fromColumn+", "+untilColumn+"]");
			return null;
		}
	}

	/**
	 * returns the list of files (ie. runs) by name for display as a combobox
	 */
	public String[] getFilesByName() {
		ArrayList<String> ret = new ArrayList<String>();
		for (File k : m_surfaces.keySet()) {
			ret.add(k.getName());
		}
		return ret.toArray(new String[0]);
	}
	
	/**
	 * returns a File which corresponds to the chosen name or <code>null</code> if one cannot be found
	 */
	public File getFileByName(final String name) {
		for (File k : m_surfaces.keySet()) {
			if (k.getName().equals(name)) 
				return k;
		}
		return null;
	}
	
	/**
	 * Returns the MS2 "heatmap" matrix. Only call this if your view uses the <code>getSurface()</code> ie. without
	 * any parameters so that the matrices have the same dimensions.
	 * 
	 * @return
	 * 
	 */
	public SurfaceMatrixAdapter getMS2Surface(final File mzmlFile) {
		calcBinWidths();
		return m_ms2_heatmaps.get(mzmlFile);
	}
	
	public SurfaceMatrixAdapter getMS2Surface(final File mzmlFile, double rt_min, double rt_max, double mz_min, double mz_max) {
		return getSurface(mzmlFile, m_ms2_heatmaps.get(mzmlFile), rt_min, rt_max, mz_min, mz_max);
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
	public SurfaceMatrixAdapter getSurface(final File mzmlFile, boolean want_ms2, double yMin, double yMax, double xMin, double xMax) {
		return getSurface(mzmlFile, !want_ms2 ? m_surfaces.get(mzmlFile) : m_ms2_heatmaps.get(mzmlFile), yMin, yMax, xMin, xMax);
	}
}

