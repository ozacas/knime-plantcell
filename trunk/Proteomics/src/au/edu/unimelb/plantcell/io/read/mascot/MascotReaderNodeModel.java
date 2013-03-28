package au.edu.unimelb.plantcell.io.read.mascot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.io.read.spectra.AbstractSpectraCell;
import au.edu.unimelb.plantcell.io.read.spectra.MyMGFPeakList;
import au.edu.unimelb.plantcell.io.read.spectra.SpectraUtilityFactory;
import com.compomics.mascotdatfile.util.interfaces.FragmentIon;
import com.compomics.mascotdatfile.util.interfaces.MascotDatfileInf;
import com.compomics.mascotdatfile.util.interfaces.QueryToPeptideMapInf;
import com.compomics.mascotdatfile.util.mascot.Parameters;
import com.compomics.mascotdatfile.util.mascot.PeptideHit;
import com.compomics.mascotdatfile.util.mascot.PeptideHitAnnotation;
import com.compomics.mascotdatfile.util.mascot.ProteinHit;
import com.compomics.mascotdatfile.util.mascot.Query;
import com.compomics.mascotdatfile.util.mascot.enumeration.MascotDatfileType;
import com.compomics.mascotdatfile.util.mascot.factory.MascotDatfileFactory;


/**
 * This is the model implementation of MascotReader.
 * Using the MascotDatFile open-source java library, this node provides an interface to that, to provide convenient access to MatrixScience Mascot datasets
 *
 * @author Andrew Cassin
 */
public class MascotReaderNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(MascotReaderNodeModel.class);
    private static int N_COLS = 15;		// number of output columns for the node
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_FILES     = "mascot-files-to-load";
	static final String CFGKEY_CONFIDENCE = "confidence";
	static final String CFGKEY_RESULTTYPE = "results-selection";

    /** initial default count value. */
    private static final double DEFAULT_CONFIDENCE = 0.05;		// 95% CI
    private static final String DEFAULT_RESULTTYPE = "all";		// all hits for all spectra: one of ("all", "best" or "confident")

    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
    private final SettingsModelStringArray        m_files = new SettingsModelStringArray(CFGKEY_FILES, new String[] {});
    private final SettingsModelDoubleBounded m_confidence = (SettingsModelDoubleBounded) make(CFGKEY_CONFIDENCE);
    private final SettingsModelString        m_resulttype = make_as_string(CFGKEY_RESULTTYPE);
    

    /**
     * Constructor for the node model.
     */
    protected MascotReaderNodeModel() {
        // first output port is for spectra data, second is for search parameters
        super(0, 2);
    }

    protected static SettingsModel make(String k) {
    	if (k.equals(CFGKEY_FILES)) {
    		return new SettingsModelStringArray(CFGKEY_FILES, new String[] {} );
    	} else if (k.equals(CFGKEY_CONFIDENCE)) {
    		SettingsModel sm = new SettingsModelDoubleBounded(CFGKEY_CONFIDENCE, DEFAULT_CONFIDENCE, 0.0, 1.0);
    		sm.setEnabled(false); // only correct since the default result type is not confident
    		return sm;
    	} else if (k.equals(CFGKEY_RESULTTYPE)) {
    		return new SettingsModelString(CFGKEY_RESULTTYPE, DEFAULT_RESULTTYPE);
    	}
    	return null;
    }
    
    protected static SettingsModelString make_as_string(String k) {
    	return (SettingsModelString) make(k);
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
	@Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        logger.info("MascotReader: about to load "+m_files.getStringArrayValue().length+" .DAT files...");

        // the data table spec of the single output table, 
        // the table will have three columns:
        DataTableSpec[] outputs = make_output_spec(null);
        
        // the execution context will provide us with storage capacity, in this
        // case a data container to which we will add rows sequentially
        // Note, this container can also handle arbitrary big data tables, it
        // will buffer to disc if necessary.
        BufferedDataContainer container = exec.createDataContainer(outputs[0]);
        MyDataContainer c2        = new MyDataContainer(exec.createDataContainer(outputs[1]), "Param");
        
        // process all suitably named (regardless of case) files ie. dat files only
        List<File> dat_files = new ArrayList<File>();
        for (String s : m_files.getStringArrayValue()) {
        	File f = new File(s);
        	if (f.exists() && f.canRead() && !f.isDirectory()) {
        		dat_files.add(f);
        	}
        }
        int row_id = 0;
        int done = 0;
        int bad = 0;
        int good = 0;
        int total = dat_files.size();
        for (File f : dat_files) {
        	if (f.getName().toLowerCase().endsWith(".dat")) {
        		logger.info("Processing Mascot DAT file: "+f.getName());
        		
        		// if the .dat file has not been produced by mascot, we will get an exception quickly so we just catch and
        		// count problem files here...
        		MascotDatfileInf mascot_dat_file;
        		QueryToPeptideMapInf q2pm;
        		Vector good_hits;
        		
        		try {
        			mascot_dat_file = MascotDatfileFactory.create(f.getAbsolutePath(), MascotDatfileType.INDEX);
        			q2pm            = mascot_dat_file.getQueryToPeptideMap();
        			good++; // consider the .DAT file good if we get here without throw
        		
	        		for (int query=1; query<q2pm.getNumberOfQueries(); query++) {
		        			good_hits = q2pm.getAllPeptideHits(query);
		        			
		        			// no hits for the query?
		        			if (good_hits == null) {
		        				logger.debug("No hits available for query: "+query+" in "+f.getName()+" should be "+q2pm.getNumberOfPeptideHits(query));
		        			} else {
		        				// only output hits according to the chosen strategy:
		        				// 1. best hit only: only element 0 (which is always the best hit) is output
		        				// 2. all hits
		        				// 3. above user-chosen confidence level for the current FILE only
		        				Query q = mascot_dat_file.getQuery(query);
		        				String title = q.getTitle();
		        				boolean is_all        = m_resulttype.getStringValue().startsWith("all");
		        				boolean is_best       = m_resulttype.getStringValue().startsWith("best");
		        				boolean is_confidence = (!is_all && !is_best);
		        				int max = is_best ? 1 : good_hits.size();
		        				
				            	for (int i=0; i<max; i++) {
				            		PeptideHit    ph = (PeptideHit) good_hits.elementAt(i);
				            		
				            		if (is_confidence && !ph.scoresAboveIdentityThreshold(m_confidence.getDoubleValue())) {
				            			continue;
				            		}
				            		DataCell[] cells = new DataCell[N_COLS];
				            		cells[0]         = new StringCell(ph.getSequence());
				            		cells[1]         = new StringCell(ph.getModifiedSequence());
				            		cells[2]         = new DoubleCell(ph.getIonsScore());
				            		cells[3]         = new DoubleCell(ph.calculateIdentityThreshold());
				            		cells[4]         = new DoubleCell(ph.getDeltaMass());
				            		cells[5]         = CollectionCellFactory.createListCell(toProtAccsns(ph));
				                	cells[6]         = CollectionCellFactory.createListCell(toProtStart(ph));
				                	cells[7]         = CollectionCellFactory.createListCell(toProtEnd(ph));
				                	cells[8]         = new DoubleCell(ph.getExpectancy());
				                	cells[9]         = new StringCell(f.getName());
				                	cells[10]        = new IntCell(ph.getMissedCleavages());
				                	cells[11]        = new StringCell(title);
				                	cells[12]        = matchingIonsCell(ph, q, mascot_dat_file);
				                	cells[13]        = theoreticalIonsCell(ph, q, mascot_dat_file);
				                	cells[14]        = make_spectra(q);
				                	
				            		DataRow row = new DefaultRow("Hit"+row_id, cells);
				            		container.addRowToTable(row);
				            		row_id++;
				            	}
				            	good_hits.clear();
				            	good_hits = null;
		        			}
			            	
	        		}
	        		
	        		// now output search parameters to second output port
	        		Parameters p = mascot_dat_file.getParametersSection();
	        		save_parameters(c2, p, f);
        		} catch (Exception e) {
        			logger.warn("Cannot process "+f.getName()+" - file corrupt?");
        			bad++;
        			continue;
        		}
            	
            	exec.checkCanceled();
            	done++;
            	exec.setProgress(((double)done) / total, "Processed "+f.getName());
            	
            	// try to avoid heap space problems...
            	q2pm = null;
            	mascot_dat_file.finish();
            	mascot_dat_file = null;
        	} else {
        		logger.warn("Encountered non-.DAT file: "+f.getName()+ " -- ignored.");
        		done++;
        	}
        }
        
        // report overall activity
        if (bad > 0) {
        	logger.warn("File summary: "+(good+bad)+" total, "+bad+" bad, "+good+" good .DAT files.");
        } else {
        	logger.info("Processed "+good+" Mascot .DAT files");
        }
        // once we are done, we close the container and return its table
        container.close();
        c2.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out, c2.close()};
    }

    private void save_parameters(MyDataContainer c2, Parameters p, File f) {
		HashMap<String,String> params = new HashMap<String,String>();
		params.put("User Name", p.getUserName());
		params.put("Error Tolerant? ", p.getErrorTolerant());
		params.put("Database", p.getDatabase());
		params.put("Fixed Modifications", p.getFixedModifications());
		params.put("Variable modifications", p.getVariableModifications());
		params.put("Mascot License", p.getLicense());
		params.put("Search Title", p.getCom());
		params.put("Protein Mass Tolerance", p.getSEGT());
		params.put("Peptide Mass Tolerance", p.getTOL());
		params.put("Peptide Mass Tolerance Unit", p.getTOLU());
		params.put("Fragment Mass Tolerance", p.getITOL());
		params.put("Fragment Mass Tolerance Unit", p.getITOLU());
		params.put("Enzyme", p.getCleavage());
		params.put("Input file", p.getFile());
		params.put("Instrument", p.getInstrument());
		params.put("Frames", p.getFrames());
		params.put("Cutout", p.getCutout());
		params.put("Quantitation?", p.getQuantiation());
		params.put("Taxonomy search?", p.getTaxonomy());
		params.put("Taxonomy accession", p.getAccession());
		params.put("Taxonomy report type", p.getReportType());
		params.put("A Ion Weighting", p.getIATOL());
		params.put("A* Ion Weighting", p.getIASTOL());
		params.put("A++ Ion Weighting", p.getIA2TOL());
		params.put("B Ion Weighting", p.getIBTOL());
		params.put("B* Ion Weighting", p.getIBSTOL());
		params.put("B++ Ion Weighting", p.getIB2TOL());
		params.put("Y Ion Weighting", p.getIYTOL());
		params.put("Y* Ion Weighting", p.getIYSTOL());
		params.put("Y++ Ion Weighting", p.getIY2TOL());
		params.put("Monoisotopic or average mass?", p.getMass());
		params.put("Fragmentation rules", show_frag_rules(p.getRules()));
		
		
		for (String s : params.keySet()) {
			DataCell[] cells = new DataCell[3];
			cells[0] = new StringCell(f.getName());
			cells[1] = new StringCell(s);
			cells[2] = (params.get(s) != null) ? new StringCell(params.get(s)) : DataType.getMissingCell();
			c2.addRow(cells);
		}
	}

	private String show_frag_rules(int[] rules) {
		StringBuilder sb = new StringBuilder();
		for (int i : rules) {
			sb.append(i);
			sb.append(",");
		}
		String ret = sb.toString();
		if (ret.endsWith(","))
			return ret.substring(0, sb.length()-1);
		else
			return ret;
	}

	private DataTableSpec[] make_output_spec(DataTableSpec spec) {
    	  DataColumnSpec[] allColSpecs = new DataColumnSpec[N_COLS];
          allColSpecs[0] = 
              new DataColumnSpecCreator("Peptide Sequence", StringCell.TYPE).createSpec();
          allColSpecs[1] = 
              new DataColumnSpecCreator("Modified Peptide Sequence", StringCell.TYPE).createSpec();
          allColSpecs[2] = 
              new DataColumnSpecCreator("Ion Score", DoubleCell.TYPE).createSpec();
          allColSpecs[3] =
          	new DataColumnSpecCreator("Identity Threshold", DoubleCell.TYPE).createSpec();
          allColSpecs[4] =
          	new DataColumnSpecCreator("Mass Error", DoubleCell.TYPE).createSpec();
          allColSpecs[5] = new DataColumnSpecCreator("Protein Accessions", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
          allColSpecs[6] = new DataColumnSpecCreator("Protein Starts", ListCell.getCollectionType(IntCell.TYPE)).createSpec();
          allColSpecs[7] = new DataColumnSpecCreator("Protein Ends", ListCell.getCollectionType(IntCell.TYPE)).createSpec();
          allColSpecs[8] = new DataColumnSpecCreator("E-value", DoubleCell.TYPE).createSpec();
          allColSpecs[9] = new DataColumnSpecCreator("Reported in", StringCell.TYPE).createSpec();
          allColSpecs[10]= new DataColumnSpecCreator("Missed Cleavages", IntCell.TYPE).createSpec();
          allColSpecs[11]= new DataColumnSpecCreator("Spectrum Title", StringCell.TYPE).createSpec();
          allColSpecs[12]= new DataColumnSpecCreator("Matching Ions (list of ion=m/z pairs, B&Y only)", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
          allColSpecs[13]= new DataColumnSpecCreator("Theoretical Ions (list of ion=m/z pairs)", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
          allColSpecs[14]= new DataColumnSpecCreator("Spectra", AbstractSpectraCell.TYPE).createSpec();
          
          DataColumnSpec[] cols2 = new DataColumnSpec[3];
          cols2[0] = new DataColumnSpecCreator("DAT File", StringCell.TYPE).createSpec();
          cols2[1] = new DataColumnSpecCreator("Search Parameter", StringCell.TYPE).createSpec();
          cols2[2] = new DataColumnSpecCreator("Value", StringCell.TYPE).createSpec();
          
          return new DataTableSpec[] { new DataTableSpec(allColSpecs), new DataTableSpec(cols2) };
	}

	@SuppressWarnings("rawtypes")
	private DataCell theoreticalIonsCell(PeptideHit ph, Query q,
			MascotDatfileInf mascot_dat_file) {
    	PeptideHitAnnotation pha = new PeptideHitAnnotation(ph.getSequence(), ph.getModifications(), 
				mascot_dat_file.getMasses(), mascot_dat_file.getParametersSection(), 
				ph.getIonSeriesFound());
    	ArrayList<DataCell> theoretical_ions = new ArrayList<DataCell>();
    	Vector                          ions = pha.getAllTheoreticalFragmentions(); 
        if (ions.size() < 1) {
        	return DataType.getMissingCell();
        }
    	for (int i = 0; i < ions.size(); i++) {
             FragmentIon fm = (FragmentIon) ions.get(i);
             
             theoretical_ions.add(new StringCell(fm.getLabel()+"="+fm.getMZ()));
        }
    
		return CollectionCellFactory.createListCell(theoretical_ions);
	}

	@SuppressWarnings("rawtypes")
	private DataCell matchingIonsCell(PeptideHit ph, Query q, MascotDatfileInf mascot_dat_file) {
    	PeptideHitAnnotation pha = new PeptideHitAnnotation(ph.getSequence(), ph.getModifications(), 
    									mascot_dat_file.getMasses(), mascot_dat_file.getParametersSection(), 
    									ph.getIonSeriesFound());
    	ArrayList<DataCell> matching_ions = new ArrayList<DataCell>();
    	Vector                mascot_ions = pha.getMatchedBYions(q.getPeakList()); // BUG: only report matching B&Y ions for now
        if (mascot_ions.size() < 1) {
        	return DataType.getMissingCell();
        }
    	for (int i = 0; i < mascot_ions.size(); i++) {
             FragmentIon fm = (FragmentIon) mascot_ions.get(i);
          
             matching_ions.add(new StringCell(fm.getLabel()+"="+fm.getMZ()));
        }
		return CollectionCellFactory.createListCell(matching_ions);
	}

	protected Collection<StringCell> toProtAccsns(PeptideHit ph) {
    	ArrayList<StringCell> al = new ArrayList<StringCell>();
    	 
    	for (Object o : ph.getProteinHits()) {
    		ProteinHit prothit = (ProteinHit) o;
    		
    		al.add(new StringCell(prothit.getAccession()));
    	}
    	return al;
    }
    
    protected Collection<IntCell> toProtStartEnd(PeptideHit ph, boolean want_start) {
    	ArrayList<IntCell> al = new ArrayList<IntCell>();
   	 
    	for (Object o : ph.getProteinHits()) {
    		ProteinHit prothit = (ProteinHit) o;
    		al.add(new IntCell(want_start ? prothit.getStart() : prothit.getStop()));
    	}
    	return al;
    }
    
    protected Collection<IntCell> toProtStart(PeptideHit ph) {
    	return toProtStartEnd(ph, true);
    }
    
    protected Collection<IntCell> toProtEnd(PeptideHit ph) {
    	return toProtStartEnd(ph, false);
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
        return make_output_spec(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_files.saveSettingsTo(settings);
        m_confidence.saveSettingsTo(settings);
        m_resulttype.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_files.loadSettingsFrom(settings);
        m_confidence.loadSettingsFrom(settings);
        m_resulttype.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_files.validateSettings(settings);
        m_confidence.validateSettings(settings);
        m_resulttype.validateSettings(settings);
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
    
    protected DataCell make_spectra(Query q) {
    	if (q == null || q.getNumberOfPeaks() < 1) 
    		return DataType.getMissingCell();
    	
    	MyMGFPeakList mgf = new MyMGFPeakList();
    	mgf.setCharge(q.getChargeString());
    	mgf.setTitle(q.getTitle());
    	mgf.setPeaks(q.getMZArray(), q.getIntensityArray());
    	mgf.setPepMass(new Double(q.getPrecursorMZ()).toString());
    	return SpectraUtilityFactory.createCell(mgf);	
    }

}

