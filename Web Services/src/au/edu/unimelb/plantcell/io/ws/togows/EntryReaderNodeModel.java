package au.edu.unimelb.plantcell.io.ws.togows;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

public class EntryReaderNodeModel extends NodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(KeywordSearchNodeModel.class);
    
	public final static String CFGKEY_URL          = "jabaws-url";
	public static final String CFGKEY_DATABASE     = "database";
	public static final String CFGKEY_FIELD        = "field";
	public static final String CFGKEY_FIELD_LIST   = "field-list";
	public static final String CFGKEY_DB_LIST      = "database-list";
	public static final String CFGKEY_ID_COLUMN    = "id-column";

	
	private final SettingsModelString m_url      = new SettingsModelString(CFGKEY_URL, "http://togows.dbcls.jp/entry/");
	private final SettingsModelString m_database = new SettingsModelString(CFGKEY_DATABASE, "uniprot");
	private final SettingsModelString m_field    = new SettingsModelString(CFGKEY_FIELD, "Retrieve ... field(s)");
	private final SettingsModelStringArray m_dbs = new SettingsModelStringArray(CFGKEY_DB_LIST, new String[] {""} );
	private final SettingsModelStringArray m_fields = new SettingsModelStringArray(CFGKEY_FIELD_LIST, new String[] { "All" });
	private final SettingsModelString m_input_col= new SettingsModelString(CFGKEY_ID_COLUMN, "Accession");
	
	public EntryReaderNodeModel() {
		this(1, 1);
	}
	
	protected EntryReaderNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}
	
	public DataTableSpec make_output_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[3];
		cols[0] = new DataColumnSpecCreator("Database", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Entry ID", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Field: "+m_field.getStringValue(), StringCell.TYPE).createSpec();
		
		return new DataTableSpec(cols);
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
		DataTableSpec        outputSpec = make_output_spec();
    	BufferedDataContainer container = exec.createDataContainer(outputSpec);
        	
		@SuppressWarnings("unused")
		int limit  = 10;		// fetch no more than ten records at once
		int id     = 1;
		int n_rows = inData[0].getRowCount();
		RowIterator it = inData[0].iterator();
		int    col_idx = inData[0].getDataTableSpec().findColumnIndex(m_input_col.getStringValue());
		if (col_idx < 0) {
			throw new InvalidSettingsException("Cannot find column (re-configure the node?): "+m_input_col.getStringValue());
		}
		
		while (it.hasNext()) {
			DataRow r = it.next();
			DataCell c = r.getCell(col_idx);
			if (c == null || c.isMissing()) {
				logger.warn("Skipping missing value in row: "+r.getKey().getString());
				continue;
			}
			
			String val = c.toString();
			String field_spec = "/" + m_field.getStringValue();
			if (field_spec.equals("/All")) {
				field_spec = "";
			}
			
			try {
				DataCell result = get_url(m_url.getStringValue() + "/" + m_database.getStringValue() + "/" + val + field_spec);
			
				DataCell[] cells = new DataCell[3];
				for (int i=0; i<cells.length; i++) {
					cells[i] = DataType.getMissingCell();
				}
	
				cells[0] = new StringCell(m_database.getStringValue());
				cells[1] = new StringCell(val);
				cells[2] = result;
				container.addRowToTable(new DefaultRow("Entry"+id++, cells));
			} catch (Exception e) {
				logger.warn("Unable to fetch result for row "+r.getKey().getString()+ ", message: "+e.getMessage());
			}
			exec.checkCanceled();
			exec.setProgress(((double)id)/n_rows);
			Thread.sleep(5 * 1000);		// be nice to provider
		}
		container.close();
		logger.info("Read "+(id-1)+ " entries ("+m_field.getStringValue() + ") from "+ m_database.getStringValue());
		BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out };
	}
	

	private DataCell get_url(String url) throws MalformedURLException, IOException, InterruptedException  {
		StringBuffer sb = new StringBuffer(10 * 1024);
		logger.info("Fetching "+url+".");
		
		int delay = 0;
		while (delay < 500) {
			HttpURLConnection u = (HttpURLConnection) new URL(url).openConnection();
			u.setReadTimeout(20 * 1000);
			u.setInstanceFollowRedirects(true);	
			u.connect();
			String line;
		
			try {
				BufferedReader rdr = new BufferedReader(new InputStreamReader(u.getInputStream()));
				while ((line = rdr.readLine()) != null) {
					sb.append(line);
					sb.append('\n');
				}
				u.disconnect();
				rdr.close();
				return new StringCell(sb.toString());
	
			} catch (IOException ioe) {
				int status = u.getResponseCode();
				// handle permanent failures first...
				if (status == 500) {
					BufferedReader err_rdr = new BufferedReader(new InputStreamReader(u.getErrorStream()));
					while ((line = err_rdr.readLine()) != null) {
						logger.info(line);
					}
					return DataType.getMissingCell();
				}
				
				// perhaps a retryable connect-exception?
				if (status < 0) {
					delay += 60;
					logger.warn("Unable to contact TogoWS: delaying for "+delay+" seconds");
					Thread.sleep(delay * 1000);
					continue;
				}
				// else
				
				throw ioe;		// abort execution... something is pretty wrong
			}
		}
		
		return DataType.getMissingCell();
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
	            throws InvalidSettingsException {
		return new DataTableSpec[] { make_output_spec() };
	}
	
	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// NO-OP
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// NO-OP
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_url.saveSettingsTo(settings);
		m_database.saveSettingsTo(settings);
		m_field.saveSettingsTo(settings);
		m_fields.saveSettingsTo(settings);
		m_dbs.saveSettingsTo(settings);
		m_input_col.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.validateSettings(settings);
		m_database.validateSettings(settings);
		m_field.validateSettings(settings);
		m_fields.validateSettings(settings);
		m_dbs.validateSettings(settings);
		m_input_col.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.loadSettingsFrom(settings);
		m_database.loadSettingsFrom(settings);
		m_field.loadSettingsFrom(settings);
		m_fields.loadSettingsFrom(settings);
		m_dbs.loadSettingsFrom(settings);
		m_input_col.loadSettingsFrom(settings);

		if (m_dbs.getStringArrayValue().length < 1 || m_dbs.getStringArrayValue()[0].length() < 1) {
			try {
				new DatabaseLoader().reload(new URL(m_url.getStringValue()), logger, m_dbs);
			} catch (MalformedURLException e) {
				logger.warn("Invalid URL to TogoWS: cannot load databases!");
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void reset() {
	}

}
