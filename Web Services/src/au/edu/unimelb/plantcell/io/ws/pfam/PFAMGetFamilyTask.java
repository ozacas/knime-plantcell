package au.edu.unimelb.plantcell.io.ws.pfam;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyHttpClient;


/**
 * Given an input column with PFAM family ID's eg. PF000171.1, this task fetches the family
 * description and returns key results as extra columns via <code>get()</code> in the order
 * specified by <code>getColumnSpecs()</code>
 * 
 * @author andrew.cassin
 *
 */
public class PFAMGetFamilyTask extends PFAMTask {
	private static Pattern pfam_accsn_re    = Pattern.compile("^(PF\\d+)");
	private static Pattern pfam_family_name = Pattern.compile("^(\\w+)$");
	private MyHttpClient m_http;		
	
	
	@Override
	public void init(final NodeLogger l, int user_configured_column_index, final URL pfam_base_url, DataTableSpec inSpec) { 
		super.init(l, user_configured_column_index, pfam_base_url, inSpec);
		m_http = new MyHttpClient(); // rate limited to one query per 5 seconds (MAX.)
	}
	
	@Override
	public DataTableSpec getTableSpec() {
		DataColumnSpec[] cols = new DataColumnSpec[11];
		cols[0] = new DataColumnSpecCreator("PFAM Release", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("PFAM Entry type", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("PFAM Accession", StringCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("PFAM ID", StringCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("PFAM Description", StringCell.TYPE).createSpec();
		cols[5] = new DataColumnSpecCreator("PFAM Comment", StringCell.TYPE).createSpec();
		cols[6] = new DataColumnSpecCreator("PFAM Curation Status", StringCell.TYPE).createSpec();
		cols[7] = new DataColumnSpecCreator("PFAM Number of full sequences", IntCell.TYPE).createSpec();
		cols[8] = new DataColumnSpecCreator("PFAM Number of species", IntCell.TYPE).createSpec();
		cols[9] = new DataColumnSpecCreator("PFAM Number of structures", IntCell.TYPE).createSpec();
		cols[10]= new DataColumnSpecCreator("PFAM Curation type", StringCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}

	private String check_family(String input) {
		Matcher m = pfam_accsn_re.matcher(input);
		if (m.find()) {
			return m.group(1)+"?output=xml";
		} else {
			m = pfam_family_name.matcher(input);
			if (m.matches()) {
				return m.group(1)+"?output=xml";
			} else {
				m_logger.warn("Unable to identify '"+input+"' - did you choose the right column?");
				return null;
			}
		}
	}
	
	@Override
	public DataCell[] get(DataRow input_row) throws Exception {
		DataCell c = input_row.getCell(m_col_idx);
		if (c == null || c.isMissing())
			return missing_cells();
		
		String pfam_family = check_family(c.toString().trim());
		if (pfam_family == null)
			return missing_cells();
		
		DataCell[] out = missing_cells();
		URL u = new URL(m_url, "/family/"+pfam_family);
		m_logger.info("Fetching: "+u);
	
		String resp = get(m_http, u);
		
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
		StringReader sr = new StringReader(resp);
		//m_logger.info(resp);
		XMLStreamReader parser = factory.createXMLStreamReader(sr);
		 
		/*
		 * which XML elements are inside each other's scope?
		 */
		HashMap<String,XMLMatcher> start_map = new HashMap<String,XMLMatcher>();
		start_map.put("pfam", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				String rel = parser.getAttributeValue(null, "release");
				if (rel.length() > 0) {
					cells[0] = new StringCell(rel);
				}
			}

			@Override
			public void saveText(String s, DataCell[] cells) {
			}
			
		});
		start_map.put("entry", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				String type = parser.getAttributeValue(null, "entry_type");
				String accession = parser.getAttributeValue(null, "accession");
				String id = parser.getAttributeValue(null, "id");
				if (type != null && type.length() > 0) {
					cells[1] = new StringCell(type);
				}
				if (accession != null && accession.length() > 0) {
					cells[2] = new StringCell(accession);
				}
				if (id != null && id.length() > 0) {
					cells[3] = new StringCell(id);
				}
			}

			@Override
			public void saveText(String s, DataCell[] cols) {				
			}
			
		});
		
		start_map.put("description", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				// NO-OP: just want to capture its text
			}

			@Override
			public void saveText(String s, DataCell[] cols) {
				if (s != null && s.length() > 0) {
					s = s.replaceAll("\\s\\s+", " ");
					cols[4] = new StringCell(s.trim());
				}
			}
			
		});
		start_map.put("comment", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				// NO-OP: just saving the text
				
			}

			@Override
			public void saveText(String s, DataCell[] cells) {
				if (s != null && s.length() > 0) {
					s = s.replaceAll("\\s\\s+", " ");
					cells[5] = new StringCell(s.trim());
				}
			}
			
		});
		start_map.put("status", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				// no-op
			}

			@Override
			public void saveText(String s, DataCell[] cells) {
				if (s != null && s.length() > 0) {
					cells[6] = new StringCell(s);
				}
			}
			
		});
		start_map.put("full", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				// NO-OP
			}

			@Override
			public void saveText(String s, DataCell[] cells) {
				if (s != null && s.length() > 0) {
					cells[7] = new IntCell(Integer.valueOf(s));
				}
			}
			
		});
		start_map.put("num_species", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				// NO-OP: just save the text of the element
			}

			@Override
			public void saveText(String s, DataCell[] cells) {
				if (s != null && s.length() > 0) {
					cells[8] = new IntCell(Integer.valueOf(s));
				}
			}
			
		});
		start_map.put("num_structures", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				// NO-OP: just save the text of the element
			}

			@Override
			public void saveText(String s, DataCell[] cells) {
				if (s != null && s.length() > 0) {
					cells[9] = new IntCell(Integer.valueOf(s));
				}
			}
			
		});
		start_map.put("type", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				// NO-OP: just save the text of the element
			}

			@Override
			public void saveText(String s, DataCell[] cells) {
				if (s != null && s.length() > 0) {
					cells[10] = new StringCell(s.trim());
				}
			}
			
		});
		
		parse_xml(parser, start_map, out);
		parser.close();
		sr.close();
		return out;
	}

}
