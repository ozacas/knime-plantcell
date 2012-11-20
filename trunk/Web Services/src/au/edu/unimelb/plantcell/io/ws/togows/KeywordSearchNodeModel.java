package au.edu.unimelb.plantcell.io.ws.togows;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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

public class KeywordSearchNodeModel extends NodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(KeywordSearchNodeModel.class);
    
	public final static String CFGKEY_URL          = "jabaws-url";
	public static final String CFGKEY_DATABASE     = "database";
	public static final String CFGKEY_FIELD        = "search-terms";
	public static final String CFGKEY_DB_LIST      = "database-list";
	
	private final SettingsModelString m_url      = new SettingsModelString(CFGKEY_URL, "http://togows.dbcls.jp/search");
	private final SettingsModelString m_database = new SettingsModelString(CFGKEY_DATABASE, "uniprot");
	private final SettingsModelString m_field    = new SettingsModelString(CFGKEY_FIELD, "keywords to search for");
	private final SettingsModelStringArray m_dbs = new SettingsModelStringArray(CFGKEY_DB_LIST, new String[] {""} );
	
	public KeywordSearchNodeModel() {
		this(0, 1);
	}
	
	protected KeywordSearchNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}
	
	public DataTableSpec make_output_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[3];
		cols[0] = new DataColumnSpecCreator("Database", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Search Term(s)", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Hit from "+m_database.getStringValue(), StringCell.TYPE).createSpec();
		
		return new DataTableSpec(cols);
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
		
		DataTableSpec        outputSpec = make_output_spec();
    	BufferedDataContainer container = exec.createDataContainer(outputSpec);
    	
    	String     base = m_url.getStringValue() + "/" + m_database.getStringValue();
    	StringBuffer sb = new StringBuffer();
    	for (int i=0; i<m_field.getStringValue().length(); i++) {
    		char c = m_field.getStringValue().charAt(i);
    		if (Character.isWhitespace(c)) {
    			sb.append('+');
    		} else {
    			sb.append(c);
    		}
    	}
    	
    	String count_url = base + "/" + sb.toString() + "/count";
    	logger.info("Opening connection to "+count_url);
		URL                  u = new URL(count_url);
		URLConnection     conn = u.openConnection();
		BufferedInputStream is = new BufferedInputStream(conn.getInputStream());
		BufferedReader    brdr = new BufferedReader(new InputStreamReader(is));
		String line;
		int n_rows = 0;
		while ((line = brdr.readLine()) != null) {
			Integer i = new Integer(line.trim());
			if (i > 1000 * 1000) {
				throw new InvalidSettingsException("Refusing to download more than one million rows of data!");
			}
			logger.info("Found "+i.intValue()+" result rows");
			n_rows = i.intValue();
			break;
		}
		brdr.close();
		
		int limit=100;
		int id = 1;

		for (int k=1; k<= n_rows; k += limit) {
			logger.info("Fetching results... from "+k+" to "+(limit+k));
			u = new URL(base + "/" + sb.toString()+"/"+k+","+limit);
			conn = u.openConnection();
			brdr = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while ((line = brdr.readLine()) != null) {
				DataCell[] cells = new DataCell[outputSpec.getNumColumns()];
				for (int i=0; i<cells.length; i++) {
					cells[i] = DataType.getMissingCell();
				}
				cells[0] = new StringCell(m_database.getStringValue());
				cells[1] = new StringCell(sb.toString());
				cells[2] = new StringCell(line.trim());
				container.addRowToTable(new DefaultRow("Hit"+id++, cells));
			}
			brdr.close();
			Thread.sleep(5 * 1000);		// 5s sleep to be nice to provider
			exec.checkCanceled();
			exec.setProgress(((double)k)/n_rows);
		}
		if (id == 1) {
			logger.warn("No hits found for search query: "+m_field.getStringValue()+" to database: "+m_database.getStringValue());
		}
		container.close();
		BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out };
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
		m_dbs.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.validateSettings(settings);
		m_database.validateSettings(settings);
		m_field.validateSettings(settings);
		m_dbs.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.loadSettingsFrom(settings);
		m_database.loadSettingsFrom(settings);
		m_field.loadSettingsFrom(settings);
		m_dbs.loadSettingsFrom(settings);
		
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
