package au.edu.unimelb.plantcell.io.ws.pfam;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * Abstract base class for PFAM REST queries. Supports KNIME CellFactory implementation and catches
 * exceptions from the subclass where it would interfere with KNIME's processing of the output data.
 * 
 * @author andrew.cassin
 *
 */
public abstract class PFAMTask implements CellFactory {
	public final static int MAX_RETRIES = 3;
	
	protected int m_col_idx = -1;
	protected NodeLogger m_logger;
	protected URL m_url;
	
	
	public void init(final NodeLogger l, int user_configured_column_index, final URL pfam_base_url, DataTableSpec inSpec) {
		assert(l != null && user_configured_column_index >= 0 && pfam_base_url != null);
		m_logger  = l;
		m_col_idx = user_configured_column_index;
		m_url     = pfam_base_url;
	}
	
	public DataCell[] missing_cells() {
		DataCell[] cells = new DataCell[getTableSpec().getNumColumns()];
		for (int i=0; i<cells.length; i++) {
			cells[i] = DataType.getMissingCell();
		}
		return cells;
	}
	
	public abstract DataTableSpec getTableSpec();
	
	@Override
	public final DataColumnSpec[] getColumnSpecs() {
		DataTableSpec spec = getTableSpec();
		DataColumnSpec[] cols = new DataColumnSpec[spec.getNumColumns()];
		for (int i=0; i<cols.length; i++) {
			cols[i] = spec.getColumnSpec(i);
		}
		return cols;
	}
	
	@Override
	public final DataCell[] getCells(DataRow in) {
		try {
			DataCell[] ret = get(in);
			return ret;
		} catch (Exception e) {
			if (m_logger != null) {
				m_logger.warn("Unable to get or process PFAM data for "+in.getCell(m_col_idx)+" - ignored.");
			}
			e.printStackTrace();
			return missing_cells();
		}
	}
	
	@Override
	public final void setProgress(int curRowNr, int rowCount, RowKey lastKey, ExecutionMonitor exec) {
		exec.setProgress(((double)curRowNr)/rowCount);
	}
	
	public abstract DataCell[] get(DataRow input_row) throws Exception;
	
	protected String get(HttpClient client, HttpMethod m) throws Exception {
		for (int i=0; i<MAX_RETRIES; i++) {
			try {
				int httpStatus = client.executeMethod(m);
				if (httpStatus >= 200 && httpStatus < 300) {
					return m.getResponseBodyAsString();
				} else {
					m_logger.warn("Got http status "+httpStatus+" ... retrying in 90sec.");
					Thread.sleep(90 * 1000);
				}
			} catch (Exception e) {
				if (i<MAX_RETRIES-1) {
					int delay = (300 + (300*i));
					m_logger.warn("Unable to fetch "+m.getPath()+", retrying in "+delay+ " seconds.");
					Thread.sleep(delay * 1000);
				} else {
					throw e;
				}
			}
		}
		return null;
	}
	
	protected String get(HttpClient client, URL u) throws Exception {
		GetMethod gm = new GetMethod(u.toString());
		return get(client, gm);
	}

	/**
	 * the assumption with this method is that the XML is well formed, otherwise its very likely to produce an exception
	 * 
	 * @param parser 
	 * @param start_map
	 * @param out
	 * @throws IOException
	 * @throws XMLStreamException
	 * @throws InvalidSettingsException
	 */
	protected void parse_xml(XMLStreamReader parser, HashMap<String,XMLMatcher> start_map, DataCell[] out) throws IOException, XMLStreamException, InvalidSettingsException {
		Stack<XMLMatcher> object_stack = new Stack<XMLMatcher>();
		
		StringBuilder sb = new StringBuilder();		// keeps track of the current element's text (in case a matcher wants to save it)
		for (int event = parser.next();
		         event != XMLStreamConstants.END_DOCUMENT;
		         event = parser.next()) {
		  
			  if ((event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) && object_stack.size() > 0) {
				  sb.append(parser.getText());
			  }
			  
			  /**
			   * Cant call getLocalName() unless its an element so...
			   */
			  if (event != XMLStreamConstants.START_ELEMENT && event != XMLStreamConstants.END_ELEMENT)
				  continue;
			 
			  String localName = parser.getLocalName();
			  if (event == XMLStreamConstants.START_ELEMENT && start_map.containsKey(localName)) {
				  XMLMatcher x = start_map.get(localName);
				  assert(x != null);
				  sb = new StringBuilder();
				  x.processElement(m_logger, parser, object_stack, out);
				  object_stack.push(x);
			  } 
			  if (event == XMLStreamConstants.END_ELEMENT && start_map.containsKey(localName)) {
				  XMLMatcher x = object_stack.pop();
				  x.saveText(sb.toString(), out);
			  }
		}
	}
}
