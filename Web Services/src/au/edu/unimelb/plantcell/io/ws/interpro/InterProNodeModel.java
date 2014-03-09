package au.edu.unimelb.plantcell.io.ws.interpro;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.data.xml.XMLValue;
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
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.osgi.framework.Bundle;

import uk.ac.ebi.interpro.resources.schemas.interproscan5.BlastProDomLocationType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.EntryType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.FingerPrintsLocationType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.FingerPrintsMatchType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.GoXrefType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.Hmmer2MatchType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.Hmmer3MatchType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.HmmerLocationType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.LocationType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.LocationsType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.MatchType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.MatchesType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.PantherMatchType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.PathwayXrefType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.ProfileScanLocationType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.ProteinMatchesType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.ProteinType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.SignalPLocationType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.SignatureType;
import uk.ac.ebi.interpro.resources.schemas.interproscan5.SuperFamilyHmmer3MatchType;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.ProteinSequenceRowIterator;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.cells.CoordinateSystem;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.io.ws.interproscan5.ArrayOfString;
import au.edu.unimelb.plantcell.io.ws.interproscan5.InputParameters;
import au.edu.unimelb.plantcell.io.ws.interproscan5.JDispatcherService;
import au.edu.unimelb.plantcell.io.ws.interproscan5.JDispatcherService_Service;
import au.edu.unimelb.plantcell.io.ws.interproscan5.ObjectFactory;
import au.edu.unimelb.plantcell.io.ws.interproscan5.WsParameterDetails;
import au.edu.unimelb.plantcell.io.ws.interproscan5.WsParameterValue;
import au.edu.unimelb.plantcore.core.regions.InterProRegion;
import au.edu.unimelb.plantcore.core.regions.InterProRegionsAnnotation;

/**
 * This is the model implementation of InterProScan.
 * Accesses the EBI webservice: interproscan with the user-specified settings
 *
 * @author Andrew Cassin
 */
public class InterProNodeModel extends NodeModel {
	/**
	 * Sequences shorter than 80aa are unlikely to match anything in InterProScan, so
	 * we skip them to avoid upsetting EBI with useless jobs
	 */
	public static final int MIN_LIKELY_INTERPROSCAN_DB = 80;
	
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("InterPro");
    
    static final String CFGKEY_EMAIL    = "email-address";
    static final String CFGKEY_SEQ      = "sequences-from";
    static final String CFGKEY_USE_CRC  = "use-crc?";
    static final String CFGKEY_USE_APPL = "algorithms-to-use";
  
    private static final String DEFAULT_EMAIL    = "who@what.ever.some.where";
    private static final String DEFAULT_SEQ      = "Sequence";

    /**
     * The number of seconds to wait if a running job has not yet completed. 
     * Scales linearly with the number of retries. Must be greater than zero
     */
	private static final int MANDATORY_DELAY = 20;


    
    // configure-dialog state which must be persistent
    private final SettingsModelString m_email = new SettingsModelString(CFGKEY_EMAIL, DEFAULT_EMAIL);
    private final SettingsModelString m_seq   = new SettingsModelString(CFGKEY_SEQ, DEFAULT_SEQ);
    private final SettingsModelStringArray m_vec = new SettingsModelStringArray(CFGKEY_USE_APPL, new String[] {});
    private int m_seq_idx = -1;
	
    /**
     * Constructor for the node model.
     */
    protected InterProNodeModel() {
            super(1, 3);
    }
    
    /**
     * Return the list of available algorithms for use within the node. Note that the 
     * value required by the web service is returned, rather than the label. Careful of backward compatibility
     * of user configure settings here, so we use the values....
     * 
     * @return
     */
    public static List<String> getAvailableAlgorithms() {
    	ArrayList<String> ret = new ArrayList<String>();
    	
    	WsParameterDetails resp = getClientProxy().getParameterDetails("appl");
    	for (WsParameterValue v : resp.getValues().getValue()) {
    		ret.add(v.getValue());
    	}
    	
    	if (ret.size() == 0) {
    		// make sure the user can choose something in the configure dialog... even if Internet is down for
    		// the above code
    		ret.add("PfamA");
    		ret.add("SMART");
    		logger.warn("Internet connection is likely not available... node cannot work without one!");
    	}
    	return ret;
    }
    
    public static JDispatcherService getClientProxy() {
		 // NB: need to use the local WSDL copy rather than go online for it... so...
		 try {
			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws");
			 URL u = FileLocator.find(bundle, new Path("/wsdl/interproscan5.wsdl"), null);
			 
			 // must not call default constructor for local WSDL... so...
			 JDispatcherService_Service cli = new JDispatcherService_Service(u,
					 new QName("http://soap.jdispatcher.ebi.ac.uk", "JDispatcherService"));
			 return cli.getJDispatcherServiceHttpPort();
		 } catch (Exception e) {
			 e.printStackTrace();
			 Logger.getAnonymousLogger().warning("Unable to initialise proxy: "+e.getMessage());
			 return null;
		 }
	}
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    
    	if (m_seq_idx < 0) {
    		throw new Exception("Invalid sequence column... re-configure the node?");
    	}
    	if (m_email.getStringValue().equals(DEFAULT_EMAIL) || m_email.getStringValue().trim().length() < 1) {
    		throw new Exception("You must provide a valid email address to use EBI. Re-configure the node.");
    	}
    	
    	
    	// traversal over input sequences
    	ProteinSequenceRowIterator it = new ProteinSequenceRowIterator(inData[0].iterator(), m_seq_idx, logger, new SequenceProcessor() {

			@Override
			public SequenceValue process(SequenceValue sv) {
				if (sv == null || sv.getLength() < MIN_LIKELY_INTERPROSCAN_DB) {
					logger.warn("Sequence too short for interpro... skipping "+sv.getID());
					return null;
				}
				
				
				if (!sv.getStringValue().matches("^[A-Za-z]+$")) {
					logger.warn("Punctuation symbols (gaps, stop codons etc.) not permitted by EBI InterPro in "+sv.getID());
					logger.warn("Please clean your sequences using 'Sequence to Columns' node first.");
					return null;
				}
				
				SequenceType  st = sv.getSequenceType();
				if (!st.isProtein()) {
					logger.warn("Skipping non-protein sequence "+sv.getID());
					return null;
				}
				// else ok..
				return sv;
			}
    		
    	});
    
    	// creator of objects for webservice
    	JDispatcherService cli = getClientProxy();
		ObjectFactory       of = new ObjectFactory();
    	
    	// create output containers
    	DataTableSpec[]      outputSpec = make_output_spec(inData[0].getSpec(), m_seq_idx);
        MyDataContainer c3 = new MyDataContainer(exec.createDataContainer(outputSpec[0]), "Site");
    	MyDataContainer c2 = new MyDataContainer(exec.createDataContainer(outputSpec[1]), "Hit");
    	MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(outputSpec[2]), "Seq");
        
    	// run the jobs...
        double n_rows = inData[0].getRowCount();
       
        exec.setProgress(0.0);
        HashMap<String,SequenceValue> outstanding_jobs = new HashMap<String,SequenceValue>();
    	
        int done = 0;
        int limit=20;
		while (it.hasNext()) {
			SequenceValue sv = it.nextSequence();
			DataRow r    = it.next();
        	String rowid = r.getKey().getString();
        	
        	outstanding_jobs.put(rowid, sv);
        	
			done++;
			if (outstanding_jobs.size() < limit && it.hasNext())
				continue;
			
			process_jobs(cli, outstanding_jobs, of, c3, c2, c1);
			outstanding_jobs.clear();
			
			if (it.hasNext()) {
				exec.setProgress(((double)done)/n_rows);
				exec.checkCanceled();
				logger.info("Waiting 30 sec. to be nice to EBI servers");
				Thread.sleep(30 * 1000);
			}
		}
		
		if (outstanding_jobs.size() > 0)
			throw new InvalidSettingsException("PROGRAMMER ERROR: jobs not run!");
    
    	return new BufferedDataTable[] {c3.close(), c2.close(), c1.close()};
    }

    /**
     * Processes XML from InterPro according to the schema at: http://www.ebi.ac.uk/interpro/resources/schemas/interproscan5/interproscan-model-1.1.xsd
     * 
     * @param prot
     * @param xml
     * @param c_site
     * @param c_prot
     * @param c_raw
     * @throws InvalidSettingsException
     */
    private void grokXML(SequenceValue prot, DataCell xml,
			MyDataContainer c_site, MyDataContainer c_prot, MyDataContainer c_raw) throws InvalidSettingsException {
    	DataCell[] cells;
    	SequenceCell sc  = new SequenceCell(prot);
    	InterProRegionsAnnotation ipra = new InterProRegionsAnnotation();
    	Track t = new Track(Track.INTERPRO_TRACK, ipra, CoordinateSystem.OFFSET_FROM_START);
    	sc.addTrack(t);
    	
		// ports 2 and 3
		if (xml instanceof XMLValue) {
			XMLValue  c_xml = (XMLValue) xml;
			try {
				JAXBContext ctx = JAXBContext.newInstance(ProteinMatchesType.class);
				ProteinMatchesType pmt = (ProteinMatchesType) 
						ctx.createUnmarshaller().unmarshal(new StringReader(c_xml.toString()));
				for (ProteinType hit : pmt.getProtein()) {
						MatchesType mt = hit.getMatches();
						String id = prot.getID();
						report_hits(c_prot, c_site, mt.getBlastprodomMatch(), id, ipra);
						report_hits(c_prot, c_site, mt.getCoilsMatch(), id, ipra);
						report_hits(c_prot, c_site, mt.getFingerprintsMatch(), id, ipra);
						report_hits(c_prot, c_site, mt.getHmmer2Match(), id, ipra);
						report_hits(c_prot, c_site, mt.getHmmer3Match(), id, ipra);
						report_hits(c_prot, c_site, mt.getPantherMatch(), id, ipra);
						report_hits(c_prot, c_site, mt.getPatternscanMatch(),  id, ipra);
						report_hits(c_prot, c_site, mt.getPhobiusMatch(),  id, ipra);
						report_hits(c_prot, c_site, mt.getProfilescanMatch(), id, ipra);
						report_hits(c_prot, c_site, mt.getSignalpMatch(),  id, ipra);
						report_hits(c_prot, c_site, mt.getSuperfamilyhmmer3Match(), id, ipra);
						report_hits(c_prot, c_site, mt.getTmhmmMatch(), id, ipra);
				}
				
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// raw output port (topmost)
    	cells            = new DataCell[2];
		cells[0]         = sc;
		cells[1] = (xml == null) ? DataType.getMissingCell() : xml;
		c_raw.addRow(cells);
	}

    private DataCell[] make_missing(int n_cells) {
    	assert(n_cells > 0);
    	DataCell[] cells = new DataCell[n_cells];
		for (int i=0; i<cells.length; i++) {
			cells[i] = DataType.getMissingCell();
		}
		return cells;
    }
  
	private void report_hits(final MyDataContainer hit_table, final MyDataContainer sites_table,
			List<? extends MatchType> matches, final String id, final InterProRegionsAnnotation ipra) {
		assert(id != null && id.length() > 0);
		
		if (matches == null || matches.size() < 1)
				return;
		
		for (MatchType mt : matches) {
			DataCell evalue = getEvalue(mt);
			DataCell score  = getScore(mt);
			
			report_signature(hit_table, mt.getSignature(), evalue, score, id);
			report_locations(sites_table, mt.getLocations(), evalue, score, id, mt.getSignature(), ipra);
		}
	}

	private void report_locations(final MyDataContainer sites, final LocationsType loc, 
			final DataCell evalue, final DataCell score, final String input_prot_id, 
			final SignatureType s, final InterProRegionsAnnotation ipra) {
		assert(sites != null && evalue != null && score != null && s != null);
		if (loc == null)
			return; 
		
		report_single_location(sites, loc.getBlastprodomLocation(), evalue, score, input_prot_id, s, ipra);
		report_single_location(sites, loc.getCoilsLocation(), evalue, score, input_prot_id, s, ipra);
		report_single_location(sites, loc.getFingerprintsLocation(), evalue, score, input_prot_id, s, ipra);
		report_single_location(sites, loc.getHmmer2Location(), evalue, score, input_prot_id, s, ipra);
		report_single_location(sites, loc.getHmmer3Location(), evalue, score, input_prot_id, s, ipra);
		report_single_location(sites, loc.getPantherLocation(), evalue, score, input_prot_id, s, ipra);
		report_single_location(sites, loc.getPatternscanLocation(), evalue, score, input_prot_id, s, ipra);
		report_single_location(sites, loc.getPhobiusLocation(), evalue, score, input_prot_id, s, ipra);
		report_single_location(sites, loc.getProfilescanLocation(), evalue, score, input_prot_id, s, ipra);
		report_single_location(sites, loc.getSignalpLocation(), evalue, score, input_prot_id, s, ipra);
	}
	
	private void report_single_location(final MyDataContainer sites, final List<? extends LocationType> l, 
			final DataCell evalue, final DataCell score, final String id, 
			final SignatureType s, final InterProRegionsAnnotation ipra) {
		if (l == null || l.size() < 1 || s == null)
			return;
	
		for (LocationType lt : l) {
			DataCell[] cells = make_missing(sites.getTableSpec().getNumColumns());
			cells[0]  = new StringCell(id);
			String dbname = s.getSignatureLibraryRelease().getLibrary().name();
			String db = dbname+" "+s.getSignatureLibraryRelease().getVersion();
			cells[1]  = asNameCell(db);
			cells[2]  = asNameCell(s.getAc());
			cells[3]  = asClassNameCell(lt, "LocationType");
			cells[5]  = getLocationScore(lt);
			cells[6]  = new IntCell(lt.getStart());
			cells[7]  = new IntCell(lt.getEnd());
			cells[8]  = getLocationEValue(lt);
			
			// add location to annotation of the sequence...
			InterProRegion ipr = new InterProRegion(dbname, s.getAc(), lt.getStart(), lt.getEnd());
            ipr.setInterProID("IPRxxxx");
			if (cells[5] instanceof DoubleCell) {
				 ipr.setScore(((DoubleCell)cells[5]).getDoubleValue());
			}
            
            ipr.setStatus(s.getName());
            ipr.setEvidence(cells[3].toString());
            ipr.setOffset(1);
            ipra.addRegion(ipr);

             
			// add row of cells to the table
			sites.addRow(cells);
		}
	}
	
	private DataCell getLocationEValue(LocationType lt) {
		if (lt instanceof BlastProDomLocationType) {
			return new DoubleCell(((BlastProDomLocationType)lt).getEvalue());
		} else if (lt instanceof HmmerLocationType) {
			return new DoubleCell(((HmmerLocationType)lt).getEvalue());
		}
		return DataType.getMissingCell();
	}
	
	public DataCell asClassNameCell(Object o, String suffix_to_remove) {
		String name = o.getClass().getName();
		int last_dot_position = name.lastIndexOf('.');
		if (last_dot_position >= 0) {
			String substr = name.substring(last_dot_position+1);
			if (substr.endsWith(suffix_to_remove)) {
				return new StringCell(substr.substring(0, substr.length() - suffix_to_remove.length()));
			} else {
				return new StringCell(substr);
			}
		}
		return DataType.getMissingCell();
	}
	
	/**
	 * Note that not every algorithm provides score details when access thru interproscan. Those that do, as of the time of writing, are here.
	 * @param lt must not be null
	 * @return
	 */
	private DataCell getLocationScore(final LocationType lt) {
		assert(lt != null);
		if (lt instanceof BlastProDomLocationType) {
			return new DoubleCell(((BlastProDomLocationType)lt).getScore());
		} else if (lt instanceof FingerPrintsLocationType) {
			return new DoubleCell(((FingerPrintsLocationType)lt).getScore());
		} else if (lt instanceof HmmerLocationType) {
			return new DoubleCell(((HmmerLocationType)lt).getScore());
		} else if (lt instanceof ProfileScanLocationType) {
			return new DoubleCell(((ProfileScanLocationType)lt).getScore());
		} else if (lt instanceof SignalPLocationType) {
			return new DoubleCell(((SignalPLocationType)lt).getScore());
		}
		
		return DataType.getMissingCell();
	}
	
	private void report_signature(final MyDataContainer hit_table, final SignatureType signature, 
			final DataCell evalue, final DataCell score, final String id) {
		assert(signature != null);
		DataCell accession = asNameCell(signature.getAc());
		DataCell name = asNameCell(signature.getName());
		DataCell description = asNameCell(signature.getDesc());
		EntryType entry = signature.getEntry();
		if (entry != null)
			report_entry(hit_table, entry, accession, name, description, evalue, score, id);
	}
	
	private void report_entry(final MyDataContainer hit_table, final EntryType entry, final DataCell accession,
			final DataCell name, final DataCell descr, final DataCell evalue, final DataCell score, final String id) {
		assert(entry != null);
		DataCell entry_type = asNameCell(entry.getType().name());
		
		/*
		 * DataColumnSpec[] c2 = new DataColumnSpec[8];
    	c2[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
    	c2[1] = new DataColumnSpecCreator("InterPro ID", StringCell.TYPE).createSpec();
    	c2[2] = new DataColumnSpecCreator("InterPro Name", StringCell.TYPE).createSpec();
    	c2[3] = new DataColumnSpecCreator("InterPro Type", StringCell.TYPE).createSpec();
    	c2[4] = new DataColumnSpecCreator("Classification ID", StringCell.TYPE).createSpec();
    	c2[5] = new DataColumnSpecCreator("Category", StringCell.TYPE).createSpec();
    	c2[6] = new DataColumnSpecCreator("Field", StringCell.TYPE).createSpec();
    	c2[7] = new DataColumnSpecCreator("Value", StringCell.TYPE).createSpec();
    	 *
		 */
		
		// 1. report each gene ontology entry (if any)
		int n = hit_table.getTableSpec().getNumColumns();
		for (GoXrefType go_record : entry.getGoXref()) {
			DataCell[] cells = make_missing(n);
			cells[0] = new StringCell(id);
			cells[1] = asNameCell(entry.getAc());
			cells[2] = name;
			cells[3] = entry_type;
			cells[6] = asNameCell(go_record.getId());
			cells[5] = asNameCell(go_record.getCategory().name());
			cells[4] = asNameCell(go_record.getDb());
			cells[7] = asNameCell(go_record.getName());
			hit_table.addRow(cells);
		}
		// 2. report each pathway entry (if any)
		for (PathwayXrefType pathway : entry.getPathwayXref()) {
			DataCell[] cells = make_missing(n);
			cells[0] = new StringCell(id);
			cells[1] = asNameCell(entry.getAc());
			cells[2] = name;
			cells[3] = entry_type;
			cells[6] = asNameCell(pathway.getId());
			// cells[5] is always missing for pathway assignments (for now)
			cells[4] = asNameCell(pathway.getDb());
			cells[7] = asNameCell(pathway.getName());
			hit_table.addRow(cells);
		}
		
	}

	private DataCell getEvalue(final MatchType mt) {
		assert(mt != null);
		if (mt instanceof Hmmer3MatchType) {
			return new DoubleCell(((Hmmer3MatchType)mt).getEvalue());
		} else if (mt instanceof Hmmer2MatchType) {
			return new DoubleCell(((Hmmer2MatchType)mt).getEvalue());
		} else if (mt instanceof FingerPrintsMatchType) {
			return new DoubleCell(((FingerPrintsMatchType)mt).getEvalue());
		} else if (mt instanceof PantherMatchType) {
			return new DoubleCell(((PantherMatchType)mt).getEvalue());
		} else if (mt instanceof SuperFamilyHmmer3MatchType) {
			return new DoubleCell(((SuperFamilyHmmer3MatchType)mt).getEvalue());
		}
		
		return DataType.getMissingCell();
	}
	
	private DataCell getScore(final MatchType mt) {
		assert(mt != null);
		if (mt instanceof Hmmer3MatchType) {
			return new DoubleCell(((Hmmer3MatchType)mt).getScore());
		} else if (mt instanceof Hmmer2MatchType) {
			return new DoubleCell(((Hmmer2MatchType)mt).getScore());
		} else if (mt instanceof PantherMatchType) {
			return new DoubleCell(((PantherMatchType)mt).getScore());
		} 
		
		return DataType.getMissingCell();
	}
	
    private DataCell asNameCell(String name) {
		if (name == null || name.trim().length() < 1 || name.toLowerCase().equals("no description")) {
			return DataType.getMissingCell();
		}
		return new StringCell(name);
	}

	private void process_jobs(JDispatcherService cli,
			HashMap<String, SequenceValue> batch, ObjectFactory of,
			final MyDataContainer c_raw, final MyDataContainer c_seq,
			final MyDataContainer c_site) throws Exception {
    	Map<String,SequenceValue>     prots = new HashMap<String,SequenceValue>();
    	Map<String,String> outstanding_jobs = new HashMap<String,String>();
    	
    	for (String key : batch.keySet()) {
    		SequenceValue prot = batch.get(key);
			String job_id  = submit_job_async(cli, m_email.getStringValue(), prot.getStringValue(), key, of);
			outstanding_jobs.put(key, job_id);
			prots.put(job_id, prot);
		}
		wait_for_completion(cli, outstanding_jobs.values());
		
		Collection<String> keys = new HashSet<String>();		
		keys.addAll(outstanding_jobs.values());	// NB: careful to deep copy! we are about to clear the map...
		batch.clear();
		for (String key : keys) {
			DataCell xml = getXMLResult(cli, key);
			SequenceValue  prot = prots.get(key);
			grokXML(prot, xml, c_raw, c_seq, c_site);
		}
	}

	private String submit_job_async(JDispatcherService cli, String email_address, String seq, String rkey, ObjectFactory of) throws Exception {
		for (int retry=0; retry < 4; retry++) { 
			try {
				InputParameters job_params = new InputParameters();
				// TODO BUG: why are the results from EBI the same despite m_vec changes?
	    		ArrayOfString aos = new ArrayOfString();
	    		for (String appl : m_vec.getStringArrayValue()) {
	    			aos.getString().add(appl);
	    		}
	    		job_params.setAppl(of.createInputParametersAppl(aos));
	    		job_params.setSequence(of.createInputParametersSequence(seq));
	    		job_params.setGoterms(of.createInputParametersGoterms(new Boolean(true)));
	    		job_params.setPathways(of.createInputParametersPathways(new Boolean(true)));
   	    		
				String jid = cli.run(m_email.getStringValue(), rkey, job_params);
				logger.info("Submitted job for row "+rkey+", got job-id: "+jid);
				return jid;
			} catch (SOAPFaultException soape) {
				throw soape;
			} catch (Exception e) {
				int delay = (retry+1)*500;	// seconds
				logger.warn("Problem when submitting job: "+e.getMessage()+ "... retrying in "+delay+" seconds");
				Thread.sleep(delay*1000);
			}
		}
		throw new FailedJobException("Cannot submit job after four attempts... giving up on "+rkey+"!");
	}

	private void wait_for_completion(JDispatcherService cli, Collection<String> keySet) 
    				throws ServiceException, InterruptedException, FailedJobException, IOException {
    	boolean wait = true;	// mandatory wait for first job in batch
    	for (String s : keySet) {
    		for (int idx=0; idx < 1000; idx++) {
    			if (wait) {
    				int delay = (MANDATORY_DELAY+(idx*MANDATORY_DELAY));		// seconds
    				logger.info("Pausing to meet EBI requirements: "+delay+" seconds.");
    				Thread.sleep(delay*1000);
    			}
        		String status = cli.getStatus(s).toLowerCase();
        		// completed or finished?
        		if (status.startsWith("complete") || status.startsWith("finish")) {
        			wait = false;	// check status without waiting for rest of batch
        			break;		// wait for next job
        		} else if (status.startsWith("fail") || status.startsWith("error")) {			// something go wrong?
        			throw new FailedJobException("Job "+s+" has failed at EBI. Aborting run.");
        		} else {
        			// incomplete so just go around again...
        			wait = true;
        		}
    		}
    	}
    	
    	// once we get here the entire batch is done
	}
	
	public DataCell getXMLResult(JDispatcherService cli, String jobID) throws Exception {
		byte[] ret = null;
		for (int retry=0; retry<4; retry++) {
			try {
				ret = cli.getResult(jobID, "xml", null);
				if (ret != null)
					break;
			} catch (Exception e) {
				logger.warn("Unable to fetch result for "+jobID+"!");
				int delay = 300+((retry+1)*200);
				logger.info("Delaying for "+delay+" seconds before retrying.");
				Thread.sleep(delay*1000);	
			}
		}
		if (ret == null) {
			return DataType.getMissingCell();
		}
		ByteArrayInputStream bis = new ByteArrayInputStream(ret);
		DataCell c = XMLCellFactory.create(bis);
		logger.info("Got XML result for job "+jobID);
		bis.close();
		return c;
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
    	m_seq_idx = inSpecs[0].findColumnIndex(m_seq.getStringValue());
        return make_output_spec(inSpecs[0], m_seq_idx);
    }

    private DataTableSpec[] make_output_spec(DataTableSpec inSpec, int seq_idx) {
		// columns for this node are dependant on what the user wants to see, so...
    	DataColumnSpec[] cols = new DataColumnSpec[2];
    	DataColumnSpecCreator my_annot_spec = new DataColumnSpecCreator("InterPro Annotated Sequence", SequenceCell.TYPE);
    	
		// we must keep the list of tracks as a property associated with the column. We must do this
		// so that annotation dialogs can correctly display the tracks for the user
    	DataColumnProperties isp = new DataColumnProperties();
    	if (seq_idx >= 0) {
    		isp = inSpec.getColumnSpec(seq_idx).getProperties();
    	}
    	TrackColumnPropertiesCreator tcpc = new TrackColumnPropertiesCreator(isp,
				new Track(Track.INTERPRO_TRACK, new InterProRegionsAnnotation(), CoordinateSystem.OFFSET_FROM_START)
);
		my_annot_spec.setProperties(tcpc.getProperties());
		cols[0] = my_annot_spec.createSpec();
    	cols[1] = new DataColumnSpecCreator("Raw XML result", XMLCell.TYPE).createSpec();
    	
    	DataColumnSpec[] c2 = new DataColumnSpec[8];
    	c2[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
    	c2[1] = new DataColumnSpecCreator("InterPro ID", StringCell.TYPE).createSpec();
    	c2[2] = new DataColumnSpecCreator("InterPro Name", StringCell.TYPE).createSpec();
    	c2[3] = new DataColumnSpecCreator("InterPro Type", StringCell.TYPE).createSpec();
    	c2[4] = new DataColumnSpecCreator("Classification ID", StringCell.TYPE).createSpec();
    	c2[5] = new DataColumnSpecCreator("Category", StringCell.TYPE).createSpec();
    	c2[6] = new DataColumnSpecCreator("Field", StringCell.TYPE).createSpec();
    	c2[7] = new DataColumnSpecCreator("Value", StringCell.TYPE).createSpec();
    	
    	DataColumnSpec[] c3 = new DataColumnSpec[9];
    	c3[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
    	c3[1] = new DataColumnSpecCreator("Database", StringCell.TYPE).createSpec();
    	c3[2] = new DataColumnSpecCreator("Database ID", StringCell.TYPE).createSpec();
    	c3[3] = new DataColumnSpecCreator("Evidence (algorithm)", StringCell.TYPE).createSpec();
    	c3[4] = new DataColumnSpecCreator("Status", StringCell.TYPE).createSpec();
    	c3[5] = new DataColumnSpecCreator("Score", DoubleCell.TYPE).createSpec();
    	c3[6] = new DataColumnSpecCreator("Start", IntCell.TYPE).createSpec();
    	c3[7] = new DataColumnSpecCreator("End", IntCell.TYPE).createSpec();
    	c3[8] = new DataColumnSpecCreator("E-Value", DoubleCell.TYPE).createSpec();
	
    	return new DataTableSpec[] { new DataTableSpec(c3),
    			new DataTableSpec(c2),
    			new DataTableSpec(cols)
    			 };
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_email.saveSettingsTo(settings);
    	m_seq.saveSettingsTo(settings);
    	m_vec.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_email.loadSettingsFrom(settings);
    	m_seq.loadSettingsFrom(settings);
    	m_vec.loadSettingsFrom(settings);
    	
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_email.validateSettings(settings);
    	m_seq.validateSettings(settings);
    	m_vec.validateSettings(settings);
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

