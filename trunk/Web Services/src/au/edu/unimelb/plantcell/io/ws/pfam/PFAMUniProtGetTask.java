package au.edu.unimelb.plantcell.io.ws.pfam;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
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
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyHttpClient;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;

/**
 * Given a column of UniProt ID's, this task retrieves the PFAM matches and sequence data for the user
 * @author andrew.cassin
 *
 */
public class PFAMUniProtGetTask extends PFAMTask {
	private final Pattern uniprot_re = Pattern.compile("^([A-Z]\\w+)(\\.\\d+)?$");
	private MyHttpClient m_http;
	
	@Override
	public void init(final NodeLogger l, int user_configured_column_index, final URL pfam_base_url, DataTableSpec inSpec) {
		super.init(l, user_configured_column_index, pfam_base_url, inSpec);
		m_http = new MyHttpClient();
	}
	
	@Override
	public DataTableSpec getTableSpec() {
		DataColumnSpec[] cols = new DataColumnSpec[10];
		cols[0] = new DataColumnSpecCreator("UniProt Release", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("UniProt Protein ID", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Description", StringCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Taxonomy ID", IntCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("Taxonomy Species", StringCell.TYPE).createSpec();
		cols[5] = new DataColumnSpecCreator("Taxonomy", StringCell.TYPE).createSpec();
		cols[6] = new DataColumnSpecCreator("UniProt Sequence (from PFAM)", SequenceCell.TYPE).createSpec();
		cols[7] = new DataColumnSpecCreator("PFAM matches (Accession list)", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
		cols[8] = new DataColumnSpecCreator("PFAM matches (ID list)", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
		cols[9] = new DataColumnSpecCreator("PFAM match evalues (list)", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec();
		
		return new DataTableSpec(cols);
	}

	private String check_uniprot(String input) {
		if (input == null)
			return null;
		
		Matcher m = uniprot_re.matcher(input);
		if (m.matches()) {
			return m.group(1);
		}
		return null;
	}
	
	@Override
	public DataCell[] get(DataRow input_row) throws Exception {
		final DataCell c = input_row.getCell(m_col_idx);
		if (c == null || c.isMissing())
			return missing_cells();
		
		String uniprot_id = check_uniprot(c.toString().trim());
		DataCell[] out = missing_cells();
		if (uniprot_id == null)
			return out;
		
		URL u = new URL(m_url, "/protein/"+uniprot_id+"?output=xml");
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
		final ArrayList<StringCell> accsn_matches = new ArrayList<StringCell>();
		final ArrayList<StringCell> id_matches = new ArrayList<StringCell>();
		final ArrayList<DoubleCell> evalue_matches = new ArrayList<DoubleCell>();

		start_map.put("entry", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				String rel = parser.getAttributeValue(null, "db_release");
				if (rel != null && rel.length() > 0)
					cells[0] = new StringCell(rel);
				String id = parser.getAttributeValue(null, "id");
				if (id != null && id.length() > 0)
					cells[1] = new StringCell(id);
			}

			@Override
			public void saveText(String string, DataCell[] cells) {
				// NO-OP
			}
			
		});
		
		start_map.put("description", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				// NO-OP: only want text
			}

			@Override
			public void saveText(String s, DataCell[] cells) {
				if (s != null && s.length() > 0) {
					s = s.replaceAll("\\s+", " ");
					cells[2] = new StringCell(s.trim());
				}
			}
			
		});
		start_map.put("taxonomy", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				String species = parser.getAttributeValue(null, "species_name");
				if (species != null) {
					cells[4] = new StringCell(species.trim());
				}
				String tid = parser.getAttributeValue(null, "tax_id");
				if (tid != null) {
					cells[3] = new IntCell(Integer.valueOf(tid));
				}
			}

			@Override
			public void saveText(String s, DataCell[] cells) {
				if (s != null && s.length() > 0) {
					s = s.replaceAll("\\s+", " ");
					cells[5] = new StringCell(s.trim());
				}
			}
			
		});
		start_map.put("sequence", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				// NO-OP: no need for the attributes at this time
			}

			@Override
			public void saveText(String string, DataCell[] cells) {
				if (string != null && string.length() > 0) {
					try {
						cells[6] = new SequenceCell(SequenceType.AA, c.toString(), string);
					} catch (InvalidSettingsException e) {
						// be-silent: bad sequences shouldn't happen very often and
						// the cell has already been set to missing
					}
				}
			}
		});
		start_map.put("match", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				String accsn = parser.getAttributeValue(null, "accession");
				String id    = parser.getAttributeValue(null, "id");
				if (accsn != null && id != null && accsn.length() > 0 && id.length() > 0) {
					accsn_matches.add(new StringCell(accsn.trim()));
					id_matches.add(new StringCell(id.trim()));
				}
			}

			@Override
			public void saveText(String string, DataCell[] cells) {
				// NO-OP
			}
			
		});
		start_map.put("location", new XMLMatcher() {

			@Override
			public void processElement(NodeLogger l, XMLStreamReader parser,
					Stack<XMLMatcher> scope_stack, DataCell[] cells)
					throws IOException, XMLStreamException,
					InvalidSettingsException {
				String eval = parser.getAttributeValue(null, "evalue");
				if (eval != null && eval.matches("^[\\deE\\.-]+$")) {
					evalue_matches.add(new DoubleCell(Double.valueOf(eval)));
				}
			}

			@Override
			public void saveText(String string, DataCell[] cells) {
				// NO-OP
			}
			
		});
		
		parse_xml(parser, start_map, out);
		parser.close();	// NB: does NOT close the input stream
		sr.close();
		
		// the final 3 columns must be saved after parsing is complete, not during
		if (accsn_matches.size() > 0)
			out[7] = CollectionCellFactory.createListCell(accsn_matches);
		if (id_matches.size() > 0) 
			out[8] = CollectionCellFactory.createListCell(id_matches);
		if (evalue_matches.size() > 0) 
			out[9] = CollectionCellFactory.createListCell(evalue_matches);
		
		return out;
	}

}
