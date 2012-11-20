package au.edu.unimelb.plantcell.io.ws.suba;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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

public class SubaNodeModel extends NodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Suba Accessor");
    
    public final static int    MAX_RETRIES        = 4;
	public final static String CFGKEY_URL         = "golgip-url";
	public static final String CFGKEY_ACCSN       = "accsn-column";
	public static final String DEFAULT_SUBA_URL   = "http://suba.plantenergy.uwa.edu.au/services/byAGI.php?agi=";
	
	private final SettingsModelString m_url   = new SettingsModelString(CFGKEY_URL, DEFAULT_SUBA_URL);
	private final SettingsModelString m_accsn = new SettingsModelString(CFGKEY_ACCSN, "Sequence");
	
	public SubaNodeModel() {
		this(1, 1);
	}
	
	protected SubaNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}
	
	public DataTableSpec[] make_output_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[5];
		cols[0] = new DataColumnSpecCreator("AGI",               StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Predicted or observed?", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Type",                  StringCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Localisation",           StringCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("Publication URL (if any)", StringCell.TYPE).createSpec();
		
		
		return new DataTableSpec[] { new DataTableSpec(cols) };
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
		DataTableSpec[]        outputSpecs = make_output_spec();
    	final BufferedDataContainer container = exec.createDataContainer(outputSpecs[0]);
        
    	int accsn_idx  = inData[0].getDataTableSpec().findColumnIndex(m_accsn.getStringValue());
    	if (accsn_idx < 0) {
    		throw new InvalidSettingsException("Cannot find AGI column: "+m_accsn.getStringValue()+", re-configure?");
    	}
		int n_rows     = inData[0].getRowCount();
		int id         = 1;
		RowIterator it = inData[0].iterator();
	
		HttpClient client = new HttpClient();
		Pattern agi_match = Pattern.compile("^[Aa][Tt]\\d+[Gg]\\d+\\.\\d+$");
		while (it.hasNext()) {
			DataRow r = it.next();
			DataCell c= r.getCell(accsn_idx);
			if (c == null || c.isMissing())
				continue;
			String agi = c.toString();
			Matcher m = agi_match.matcher(agi);
			if (!m.matches()) {
				logger.warn(agi + " does not match expected TAIRv8 identifier - skipped!");
				continue;
			}
			
			String response = get(client, m_url.getStringValue()+agi);
			try {
					JSONObject jo = new JSONObject(response);
					@SuppressWarnings("rawtypes")
					Iterator j = jo.keys();
					while (j.hasNext()) {
						String key = (String) j.next();
						Object o = jo.get(key);
						DataCell[] cells = new DataCell[5];
						cells[0] = new StringCell(agi);
						if (key.equals("predicted")) {
							// 0 is prediction, 1 is the prediction program
							JSONArray arr = (JSONArray) o;
							for (int i=0; i<arr.length(); i++) {
								JSONArray a = (JSONArray) arr.get(i);
								cells[1] = new StringCell(key);
								cells[2] = new StringCell(a.get(1).toString());
								cells[3] = new StringCell(a.get(0).toString());
								cells[4] = DataType.getMissingCell();
								container.addRowToTable(new DefaultRow("Row"+id++, cells));
							}
						} else if (key.equals("observed")) {
							// 0 is reported localisation, 1 is manuscript, 2 is type of evidence eg. ms/ms
							JSONArray arr = (JSONArray) o;
							for (int i=0; i<arr.length(); i++) {
								JSONArray a = (JSONArray) arr.get(i);
								cells[1] = new StringCell(key);
								cells[2] = new StringCell(a.get(2).toString());
								cells[3] = new StringCell(a.get(0).toString());
								cells[4] = new StringCell("http://www.ncbi.nlm.nih.gov/pubmed/"+a.get(1).toString());
								container.addRowToTable(new DefaultRow("Row"+id++, cells));
							}
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
					logger.warn("Unable to process results from SUBA for AGI: "+agi+", skipping row.");
				}
		
			
			logger.info("Waiting for 10sec to be nice to SUBA server");
			exec.checkCanceled();
			exec.setProgress(((double)id)/n_rows);
			Thread.sleep(10 * 1000);
		}
		container.close();
		
		BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out };
	}
	
	private String get(HttpClient client, String url) throws Exception {
		for (int i=0; i<MAX_RETRIES; i++) {
			try {
				GetMethod gm = new GetMethod(url);
				int httpStatus = client.executeMethod(gm);
				if (httpStatus >= 200 && httpStatus < 300) {
					return gm.getResponseBodyAsString();
				} else {
					logger.warn("Got http status "+httpStatus+" ... retrying in 90sec.");
					Thread.sleep(90 * 1000);
				}
			} catch (Exception e) {
				if (i<MAX_RETRIES-1) {
					int delay = (300 + (300*i));
					logger.warn("Unable to fetch "+url+", retrying in "+delay+ " seconds.");
					Thread.sleep(delay * 1000);
				} else {
					throw e;
				}
			}
		}
		return null;
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
	            throws InvalidSettingsException {
		return make_output_spec();
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
		m_accsn.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.validateSettings(settings);
		m_accsn.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.loadSettingsFrom(settings);
		m_accsn.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
	}

}
