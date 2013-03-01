package au.edu.unimelb.plantcell.io.ws.pfam;

import java.net.URL;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 * Abstract base class for PFAM REST queries
 * @author andrew.cassin
 *
 */
public abstract class PFAMTask implements CellFactory {
	public final static int MAX_RETRIES = 3;
	
	protected int m_col_idx = -1;
	protected NodeLogger m_logger;
	protected URL m_url;
	
	
	public void init(final NodeLogger l, int user_configured_column_index, final URL pfam_base_url) {
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
			e.printStackTrace();
			return missing_cells();
		}
		
	}
	
	@Override
	public final void setProgress(int curRowNr, int rowCount, RowKey lastKey, ExecutionMonitor exec) {
		exec.setProgress(((double)curRowNr)/rowCount);
	}
	
	public abstract DataCell[] get(DataRow input_row) throws Exception;
	
	protected String get(HttpClient client, URL u) throws Exception {
		for (int i=0; i<MAX_RETRIES; i++) {
			try {
				GetMethod gm = new GetMethod(u.toString());
				int httpStatus = client.executeMethod(gm);
				if (httpStatus >= 200 && httpStatus < 300) {
					return gm.getResponseBodyAsString();
				} else {
					m_logger.warn("Got http status "+httpStatus+" ... retrying in 90sec.");
					Thread.sleep(90 * 1000);
				}
			} catch (Exception e) {
				if (i<MAX_RETRIES-1) {
					int delay = (300 + (300*i));
					m_logger.warn("Unable to fetch "+u+", retrying in "+delay+ " seconds.");
					Thread.sleep(delay * 1000);
				} else {
					throw e;
				}
			}
		}
		return null;
	}

}
