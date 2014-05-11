package au.edu.unimelb.plantcell.io.ws.pfam;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.httpclient.methods.PostMethod;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyHttpClient;
import au.edu.unimelb.plantcell.core.cells.CoordinateSystem;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.core.cells.TrackCreator;
import au.edu.unimelb.plantcell.core.regions.PFAMHitRegion;
import au.edu.unimelb.plantcell.core.regions.PFAMRegionsAnnotation;
import au.edu.unimelb.plantcell.io.ws.tmhmm.AbstractWebServiceNodeModel;

/**
 * given a column of {@link SequenceCell}'s this task iterates thru them on the public server.
 * It is deliberately slow to avoid a site being banned.
 * 
 * @author andrew.cassin
 *
 */
public class PFAMSequenceSearchTask extends PFAMTask {
	private MyHttpClient m_http;
	private String m_result_url;
	private int m_significant;
	private DataTableSpec m_inspec;
	
	@Override
	public void init(final NodeLogger l, int user_configured_column_index, final URL pfam_base_url, DataTableSpec inSpec) { 
		super.init(l, user_configured_column_index, pfam_base_url, inSpec);
		m_http = new MyHttpClient(); // rate limited to one query per 5 seconds (MAX.)
		m_inspec = inSpec;
	}
	
	@Override
	public DataTableSpec getTableSpec() {
		DataColumnSpec[] cols = new DataColumnSpec[3];
		// here we must use the input table spec to add the necessary column properties for the annotation
		DataColumnSpecCreator dcsc = new DataColumnSpecCreator("PFAM Annotated Sequence", SequenceCell.TYPE);
	    if (m_col_idx >= 0 && m_inspec != null) {		// column configured yet?
	    	try {
		        DataColumnProperties isp = m_inspec.getColumnSpec(m_col_idx).getProperties();
				TrackColumnPropertiesCreator tcpc = new TrackColumnPropertiesCreator(isp,
												new Track(Track.PFAM_TRACK, getTrackCreator())
											);
				dcsc.setProperties(tcpc.getProperties());
	    	} catch (InvalidSettingsException ise) {
	    		// be silent... fallthru
	    	}
	    }
	    cols[0] = dcsc.createSpec();
		cols[1] = new DataColumnSpecCreator("PFAM families hit (set)", SetCell.getCollectionType(StringCell.TYPE)).createSpec();
		cols[2] = new DataColumnSpecCreator("Number of significant match locations", IntCell.TYPE).createSpec();
		
		return new DataTableSpec(cols);
	}

	private TrackCreator getTrackCreator() {
		return new TrackCreator() {

			@Override
			public Track createTrack(String name)
					throws InvalidSettingsException {
				return new Track(name, new PFAMRegionsAnnotation(), CoordinateSystem.OFFSET_FROM_START);
			}
			
		};
	}
	
	/**
	 * Submits the sequence to the rest-ful API of PFAM and then parses the XML result to find the result URL
	 * 
	 * @return URL to retrieve results from or null on error
	 */
	private String post(SequenceValue sv) throws Exception {
		PostMethod pm = new PostMethod(m_url+"/search/sequence");
	
		pm.addParameter("ga", "1");		// use gathering threshold as per http://pfam.janelia.org/help#tabview=tab10
		pm.addParameter("searchBs", "0");
		pm.addParameter("skipAs", "0");
		pm.addParameter("seq", sv.getStringValue());
		pm.addParameter("output", "xml");
		
		String resp = get(m_http, pm);
		
		// parse the XML to get the result_url...
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
		StringReader sr = new StringReader(resp);
		//m_logger.info(resp);
		XMLStreamReader parser = factory.createXMLStreamReader(sr);
		m_result_url = null;
		
		/*
		 * which XML elements are inside each other's scope?
		 */
		HashMap<String,XMLMatcher> start_map = new HashMap<String,XMLMatcher>();
		XMLMatcher xm = new XMLMatcher() {
			
			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				// NO-OP
			}

			@Override
			public void saveText(String string, DataCell[] cells) {
				if (string != null && string.length() > 0)
					m_result_url = string.trim();
			}
			
		};
			
		start_map.put("result_url", xm);
		parse_xml(parser, start_map, new DataCell[1]);
		
		// wait a few seconds to ensure the result is there...
		Thread.sleep(4 * 1000);
		parser.close();
		sr.close();
		
		if (m_result_url != null) {
			m_logger.debug("Fetching results from: "+m_result_url);
		}
		return m_result_url;
	}
	
	@Override
	public DataCell[] get(DataRow input_row) throws Exception {
		DataCell c = input_row.getCell(m_col_idx);
		if (c == null || c.isMissing() || !(c instanceof SequenceValue))
			return missing_cells();
		// protein sequence only can be queried with PFAM
		SequenceValue sv = (SequenceValue) c;
		DataCell[] out = missing_cells();
		if (!sv.getSequenceType().isProtein())
			return out;
	
		String resp = "";
		String destination_url = "";
		for (int i=0; i<AbstractWebServiceNodeModel.MAX_RETRIES; i++) {
			try {
				// submit rest-ful query to PFAM
				destination_url = post(sv);
				if (destination_url == null)
					return out;
				
				resp = get(m_http, new URL(destination_url));
				break;
			} catch (Exception e) {
				if (i == AbstractWebServiceNodeModel.MAX_RETRIES)
					throw e;
				m_logger.warn("Failed to fetch from "+destination_url);
				int delay = 30 + (30+(i*60));
				m_logger.warn("Delaying for "+delay+"secs. before retry");
				Thread.sleep(delay * 1000);
			}
		}
		 
		
		/** EG:
		 * <pfam xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns="http://pfam.sanger.ac.uk/"
      xsi:schemaLocation="http://pfam.sanger.ac.uk/
                          http://pfam.sanger.ac.uk/static/documents/schemas/results.xsd"
      release="26.0" 
      release_date="2011-11-17">
  <results job_id="371b9f09-2f49-4dd5-8c5e-619d10627660">
    <matches>
      <protein length="121">
        <database id="pfam" release="26.0" release_date="2011-11-17">
          <match accession="PF13659.1" id="Methyltransf_26" type="Pfam-A" class="Domain">
            <location start="2" end="121" ali_start="4" ali_end="121" hmm_start="3" hmm_end="116" evalue="3.5e-19" bitscore="68.8" evidence="hmmer v3.0" significant="1">
              <hmm>
                <![CDATA[rvldpgcGsGtfllaalell.aalllgvEldpraaalaarnlaraglaervrvrqgdlrdlaellragsfdlvvtnpPygpragaraalrd......lydafleaaarllkpgGvlvlivp]]>
              </hmm>
              <match_string>
                <![CDATA[ +l++g+G+G ++l+++++  ++++++++++p aa+laa+n+ + +  +r+r  ++d++++    ++++fdl+++npPy ++ +++++ ++        + ++ + a+ l+  G++++i p]]>
              </match_string>
              <pp>
                <![CDATA[69*****************999************************************9998...89************98866555544445566677799***************9975]]>
              </pp>
              <seq>
                <![CDATA[NILEVGTGTGLVALMTAQRNpTSNITAIDVNPVAAELAAKNFLESHFGHRMRAMHCDYKTFG---TQKKFDLIISNPPYFETNPSEKDATArqqrelSFKTLISKTAEILATEGRFCVIIP]]>
              </seq>
              <raw>
                <![CDATA[
#HMM       rvldpgcGsGtfllaalell.aalllgvEldpraaalaarnlaraglaervrvrqgdlrdlaellragsfdlvvtnpPygpragaraalrd......lydafleaaarllkpgGvlvlivp
#MATCH      +l++g+G+G ++l+++++  ++++++++++p aa+laa+n+ + +  +r+r  ++d++++    ++++fdl+++npPy ++ +++++ ++        + ++ + a+ l+  G++++i p
#PP        69*****************999************************************9998...89************98866555544445566677799***************9975
#SEQ       NILEVGTGTGLVALMTAQRNpTSNITAIDVNPVAAELAAKNFLESHFGHRMRAMHCDYKTFG---TQKKFDLIISNPPYFETNPSEKDATArqqrelSFKTLISKTAEILATEGRFCVIIP
                ]]>
              </raw>
            </location>
          </match>
        </database>
      </protein>
    </matches>
  </results>
</pfam>
		 */
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
		StringReader sr = new StringReader(resp);
		//m_logger.info(resp);
		XMLStreamReader parser = factory.createXMLStreamReader(sr);
		 
		/*
		 * which XML elements are inside each other's scope?
		 */
		HashMap<String,XMLMatcher> start_map = new HashMap<String,XMLMatcher>();
		final ArrayList<PFAMHitRegion> pfam_hits = new ArrayList<PFAMHitRegion>();
		
		m_significant = 0;
		MatchMatcher mm = new MatchMatcher();
		mm.clear();
		start_map.put("match", mm);
		start_map.put("location", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				String sig = parser.getAttributeValue(null, "significant");
				if (sig != null && sig.equals("1")) {
					m_significant++;
				}
				String ev = parser.getAttributeValue(null, "evidence");
				String hmm_start = parser.getAttributeValue(null, "hmm_start");
				String hmm_end   = parser.getAttributeValue(null, "hmm_end");
				XMLMatcher xmlm = scope_stack.peek();
				if (xmlm instanceof MatchMatcher) {
					MatchMatcher   mm = (MatchMatcher) xmlm;
					HashMap<String,String> fields = new HashMap<String,String>();
					fields.put("pfam-hmm-start", hmm_start);
					fields.put("pfam-hmm-end", hmm_end);
					fields.put("pfam-hmm-significant", sig);
					fields.put("pfam-class", mm.getLastClass());
					fields.put("pfam-type", mm.getLastType());
					fields.put("pfam-evidence", ev);
					fields.put("bit score", parser.getAttributeValue(null, "bitscore"));
					fields.put("evalue", parser.getAttributeValue(null, "evalue"));
					fields.put("q. start", parser.getAttributeValue(null, "start"));
					fields.put("q. end", parser.getAttributeValue(null, "end"));
					fields.put("query id", mm.getLastAccession());
					fields.put("label", mm.getLastID());
					PFAMHitRegion hit = new PFAMHitRegion(fields);
					pfam_hits.add(hit);
				}
			}

			@Override
			public void saveText(String string, DataCell[] cells) {
				// NO-OP
			}
			
		});
		
		try {
			parse_xml(parser, start_map, out);
		} catch (Exception e) {
			// only done during debugging (dont want to fill the console with crapml)
			//m_logger.info(resp);
			throw e;
		}
		SequenceCell sc = new SequenceCell(sv);
		Track t = sc.addTrack(Track.PFAM_TRACK, getTrackCreator());
		PFAMRegionsAnnotation pra = new PFAMRegionsAnnotation();
		pra.addRegions(pfam_hits);
		t.addAnnotation(pra);
		out[0] = sc;
		out[1] = CollectionCellFactory.createSetCell(mm.getAccessions());
		out[2] = new IntCell(m_significant);
		
		return out;
	}

}
