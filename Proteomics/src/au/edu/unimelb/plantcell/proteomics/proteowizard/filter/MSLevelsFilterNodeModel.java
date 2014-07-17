package au.edu.unimelb.plantcell.proteomics.proteowizard.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.ws.Service;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.servers.core.jaxb.results.DataFileType;
import au.edu.unimelb.plantcell.servers.core.jaxb.results.ListOfDataFile;
import au.edu.unimelb.plantcell.servers.msconvertee.endpoints.MSConvert;
import au.edu.unimelb.plantcell.servers.msconvertee.endpoints.ProteowizardJob;
import au.edu.unimelb.plantcell.servers.msconvertee.jaxb.FilterParametersType;
import au.edu.unimelb.plantcell.servers.msconvertee.jaxb.MsLevelType;
import au.edu.unimelb.plantcell.servers.msconvertee.jaxb.ObjectFactory;

/**
 * Filters the chosen data files (from input table) using the Proteowizard msconvert utility via
 * the MSConvertEE web service. This service enables centralised data conversion and vendor-support.
 * 
 * @author acassin
 *
 */
public class MSLevelsFilterNodeModel extends NodeModel {
	private final static NodeLogger logger = NodeLogger.getLogger("MS Levels Filter");
	
	public final static String CFGKEY_URL = "msconvertee-url";
	public final static String CFGKEY_USERNAME = "username";
	public final static String CFGKEY_PASSWORD = "password";
	public final static String CFGKEY_INPUT_FILE_COLUMN = "input-data-url-column";
	public final static String CFGKEY_OUTPUT_DATA_FORMAT= "output-format";
	public final static String CFGKEY_ACCEPTED_MSLEVELS = "ms-levels-to-accept";
	public final static String CFGKEY_SAVETO = "save-to-folder";
	public final static String CFGKEY_TABLE_DESIRED = "table-output-desired";
	public final static String CFGKEY_OVERWRITE_RESULTS = "overwrite-existing-output";
	
	public final static String[] TABLE_OUTPUT_DESIRED = new String[] { "File summary (incl. stdout & stderr)",
		"MS/MS (and higher) spectra only", "All spectra (slow)" };
	
	private final SettingsModelString m_url           = new SettingsModelString(CFGKEY_URL, "");
	private final SettingsModelString m_username      = new SettingsModelString(CFGKEY_USERNAME, "");
	private final SettingsModelString m_password      = new SettingsModelString(CFGKEY_PASSWORD, "");
	private final SettingsModelString m_output_format = new SettingsModelString(CFGKEY_OUTPUT_DATA_FORMAT, "");
	private final SettingsModelBoolean m_overwrite    = new SettingsModelBoolean(CFGKEY_OVERWRITE_RESULTS, Boolean.FALSE);
	private final SettingsModelString m_save_to       = new SettingsModelString(CFGKEY_SAVETO, "");
	private final SettingsModelString m_input_column  = new SettingsModelString(CFGKEY_INPUT_FILE_COLUMN, "");
	private final SettingsModelStringArray m_mslevels = new SettingsModelStringArray(CFGKEY_ACCEPTED_MSLEVELS, new String[] {});
	private final SettingsModelString m_table_desired = new SettingsModelString(CFGKEY_TABLE_DESIRED, TABLE_OUTPUT_DESIRED[0]);
	
	protected static final QName MSCONVERTEE_QNAME = new QName("http://impl.msconvertee.servers.plantcell.unimelb.edu.au/", "MSConvertImplService");
	
	public MSLevelsFilterNodeModel() {
		this(1,1);
	}
	
	protected MSLevelsFilterNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
	}
	
	protected MSConvert makeServiceProxy(String url) throws Exception {
		getNodeLogger().info("Connecting to "+url);
		Service         s = Service.create(new URL(url), MSCONVERTEE_QNAME);
		if (s == null) {
			throw new IOException("Cannot create/connect to MSConvertEE service: "+url);
		}
		return s.getPort(MSConvert.class);
	}
	
	protected NodeLogger getNodeLogger() {
		return logger;
	}
	
	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	// NO-OP
    }
    
	protected String getServiceEndpoint() throws MalformedURLException {
		String u = m_url.getStringValue().trim();
		if (u == null || u.length() < 1) {
			throw new MalformedURLException("MSConvertEE URL cannot be missing!");
		}
		if (u.endsWith("?wsdl")) {
			return u;
		} else if (u.endsWith("MSConvertImpl")) {
			return u+"?wsdl";
		} else if (u.endsWith("webservices/") || u.endsWith("webservices")) {
			if (!u.endsWith("/")) {
				u += "/";
			}
			return u+"MSConvertImpl?wsdl";
		} else if (u.endsWith("msconvertee") || u.endsWith("msconvertee/")) {
			if (!u.endsWith("/")) {
				u += "/";
			}
			return u+"webservices/MSConvertImpl?wsdl";
		}
		// oh well... we just try what was entered and hope for the best...
		return u;
	}
	
	protected String getOutputFormat() {
		return m_output_format.getStringValue();
	}
	
	protected String getInputFileColumnName() {
		return m_input_column.getStringValue();
	}
	
	protected int getInputFileColumnIndex(DataTableSpec spec) {
		if (spec == null) {
			return -1;
		}
		return spec.findColumnIndex(getInputFileColumnName());
	}

	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
		int file_idx = getInputFileColumnIndex(inData[0].getSpec());
		if (file_idx < 0) {
			throw new InvalidSettingsException("No such column: "+m_input_column.getStringValue()+" - reconfigure?");
		}
		int done = 0;
		ObjectFactory of = new ObjectFactory();
		MsLevelType mslevels = of.createMsLevelType();
		for (String s : getMSLevelsFromUser()) {
			mslevels.getMsLevel().add(Integer.valueOf(s.trim()));
		}
		MSConvert msc = makeServiceProxy(getServiceEndpoint());
		MyDataContainer c = new MyDataContainer(exec.createDataContainer(
				make_output_spec(getDesiredTable(), inData[0].getSpec())), "Row");
		
		// iterate each file...
		for (DataRow r : inData[0]) {
			DataCell input_file_cell = r.getCell(file_idx);
			if (input_file_cell == null || input_file_cell.isMissing()) {
				continue;
			}
			exec.checkCanceled();
			ProteowizardJob j = new ProteowizardJob();
			j.setOutputFormat(getOutputFormat());
			FilterParametersType fpt = of.createFilterParametersType();
			fpt.setMsLevelFilter(mslevels);
			j.setFilterParameters(fpt);
			DataHandler[] dh = setupDataFiles(j, input_file_cell.toString());
			String jID = msc.convert(j, dh);
			waitForCompletion(msc, jID, new String[] { "FINISH", "COMPLETE" }, exec);
			// if we get here, we can download the results and save them and then make the output table desired
			ListOfDataFile results = msc.getResults(jID);
			if (results == null || results.getDataFile() == null || results.getDataFile().size() < 1) {
				throw new IOException("No results from msconvert for job: "+jID);
			}
			exec.checkCanceled();
			makeDesiredTable(c, results, exec);
			done++; 
		}
		getNodeLogger().info("Filtered and converted: "+done+" files.");
		return new BufferedDataTable[] {c.close()};
	}
	
	protected String getDesiredTable() {
		return m_table_desired.getStringValue();
	}

	protected String[] getMSLevelsFromUser() {
		return m_mslevels.getStringArrayValue();
	}

	/**
	 * Returns a list of DataHandlers for each data file represented by url. Modifies the specified {@link ProteowizardJob}
	 * to specify the input list of data files.
	 * 
	 * @param j	modified by this call (must not be null
	 * @param url input data file (must not be null)
	 * @return
	 */
	protected DataHandler[] setupDataFiles(ProteowizardJob j, String url) throws IOException {
		assert(j != null && url != null);
		URL u = toURL(url);
		DataHandler dh = new DataHandler(u);
		j.setInputDataFormat(guessInputDataFormat(u));
		j.getInputDataNames().clear();
		j.getInputDataNames().add(extractLastPathComponent(u.getPath()));
		return new DataHandler[] {dh};
	}

	protected String extractLastPathComponent(String path) throws IOException {
		Pattern p = Pattern.compile("^.*[/\\\\]([^/\\\\]+?)$");
		Matcher m = p.matcher(path);
		if (m.matches()) {
			String grp1 = m.group(1);
			return grp1;
		} else {
			throw new IOException("No last path component of filename!");
		}
	}

	protected String guessInputDataFormat(URL url) throws IOException {
		assert(url != null);
		String path = url.getPath().trim().toLowerCase();
		if (path.endsWith(".raw")) {
			return "RAW";
		} else if (path.endsWith(".mgf")) {
			return "MGF";
		} else if (path.endsWith(".mzml")) {
			return "MzML";
		} else if (path.endsWith(".mzxml") || path.endsWith(".xml")) {
			return "MzXML";
		} else if (path.endsWith(".wiff")) {
			return "WIFF";
		} else {
			throw new IOException("Unsupported file type: "+path);
		}
	}
	
	protected File getOutputFolder() {
		return new File(m_save_to.getStringValue());
	}

	protected void makeDesiredTable(MyDataContainer c, ListOfDataFile results, ExecutionContext exec) throws IOException { 
		assert(exec != null && results != null && c != null);
		
		String desired_table = getDesiredTable();
		
		// 1. save results
		List<File> savedFiles = saveResults(results, getOutputFolder());
		
		// 2. process results into desired table
		if (desired_table.equals(TABLE_OUTPUT_DESIRED[0]))  {
			List<File> stdout = findFile(savedFiles, new FilenameFilter() {

				@Override
				public boolean accept(File arg0, String arg1) {
					return arg1.equals("stdout");
				}
				
			});
			List<File> stderr = findFile(savedFiles, new FilenameFilter() {

				@Override
				public boolean accept(File arg0, String arg1) {
					return arg1.equals("stderr");
				}
				
			});
			List<File> convertedFiles = findFile(savedFiles, new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return !(name.equals("stdout") || name.equals("stderr"));
				}
				
			});
			for (File f : convertedFiles) {
				DataCell[] cells = new DataCell[4];
				cells[0] = new StringCell(f.toURI().toURL().toExternalForm());
				cells[1] = DataType.getMissingCell();
				cells[2] = (stdout.size() > 0) ? new StringCell(fileContentsAsString(stdout.get(0))) : DataType.getMissingCell();
				cells[3] = (stdout.size() > 0) ? new StringCell(fileContentsAsString(stderr.get(0))) : DataType.getMissingCell();
				c.addRow(cells);
			}
		} else {
			throw new IOException("Unknown output table: "+desired_table);
		}
	}

	private String fileContentsAsString(File file) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(file));
			String line;
			while ((line = rdr.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}
		} finally {
			if (rdr != null) {
				rdr.close();
			}
		}
		return sb.toString();
	}

	private List<File> findFile(List<File> savedFiles, FilenameFilter filter) {
		ArrayList<File> ret = new ArrayList<File>();
		for (File f : savedFiles) {
			if (filter.accept(f.getParentFile(), f.getName())) {
				ret.add(f);
			}
		}
		return ret;
	}

	protected List<File> saveResults(final ListOfDataFile results, final File output_folder) throws IOException {
		assert(results != null && output_folder != null);
		if (!output_folder.exists()) {
			throw new IOException("No such folder: "+output_folder);
		}
		ArrayList<File> ret = new ArrayList<File>();
		for (DataFileType dft : results.getDataFile()) {
			String name = safeName(dft.getSuggestedName());
			File out = new File(output_folder, name);
			if (out.exists() && !shouldOverwriteExistingFiles()) {
				throw new IOException("Will not overwrite existing: "+out.getAbsolutePath());
			}
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(out);
				dft.getData().writeTo(fos);
				fos.close();
				ret.add(out);
			} finally {
				if (fos != null) {
					fos.close();
				}
			}
			// check that the file has the same length as the server
			if (out.length() != dft.getRequiredLength()) {
				throw new IOException("Server copy has "+dft.getRequiredLength()+" bytes, but only got "+out.length());
			}
		}
		return ret;
	}

	private String safeName(String suggestedName) throws IOException {
		if (suggestedName == null) {
			throw new IOException("No server filename to save!");
		}
		// TODO FIXME BUG: do something to sanity check the server's suggested filename
		return "results_"+suggestedName;
	}

	protected DataTableSpec make_output_spec(final String desired_table, final DataTableSpec spec) {
		DataColumnSpec[] cols;
		
		if (desired_table.equals(TABLE_OUTPUT_DESIRED[0])) {
			cols = new DataColumnSpec[4];
			cols[0] = new DataColumnSpecCreator("File", StringCell.TYPE).createSpec();
			cols[1] = new DataColumnSpecCreator("Output format", StringCell.TYPE).createSpec();
			cols[2] = new DataColumnSpecCreator("Stdout (log messages)", StringCell.TYPE).createSpec();
			cols[3] = new DataColumnSpecCreator("Stderr (error messages)", StringCell.TYPE).createSpec();
			return new DataTableSpec(cols);
		}
		
		return null;
	}

	protected void waitForCompletion(final MSConvert msc, String jID, String[] expected_states, final ExecutionContext exec) throws IOException, CanceledExecutionException {
		String status;
		do {
			try {
				for (int i=0; i<6; i++) {
					Thread.sleep(5 * 1000);
					exec.checkCanceled();
				}
				status = msc.getStatus(jID);
			} catch (SOAPException|InterruptedException e) {
				throw new IOException(e);
			}
		} while (status.startsWith("QUEUE") || status.startsWith("PEND") || status.startsWith("RUN"));
		
		for (String s : expected_states) {
			if (status.startsWith(s)) {
				return;
			}
		}
		throw new IOException("Job "+jID+" did not finish successfully, got status: "+status);
	}

	protected URL toURL(final String uri) throws MalformedURLException {
		if (uri.startsWith("file:")) {
			return new URL(uri);
		} else {
			// assume its a URL, but it we get an exception then assume its a File
			try {
				URL u = new URL(uri);
				return u;
			} catch (MalformedURLException mfe) {
				File f = new File(uri);
				return f.toURI().toURL();
			}
		}
	}

	@Override
	public DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		String out_table = getDesiredTable();
		if (out_table == null || out_table.equals("")) {
			out_table = TABLE_OUTPUT_DESIRED[0];
		}
		DataTableSpec outSpec = make_output_spec(out_table, inSpecs[0]);
		return new DataTableSpec[] { outSpec };
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

	protected boolean shouldOverwriteExistingFiles() {
		return m_overwrite.getBooleanValue();
	}
	
	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_url.saveSettingsTo(settings);
		m_input_column.saveSettingsTo(settings);
		m_output_format.saveSettingsTo(settings);
		m_mslevels.saveSettingsTo(settings);
		m_table_desired.saveSettingsTo(settings);
		m_username.saveSettingsTo(settings);
		m_password.saveSettingsTo(settings);
		m_save_to.saveSettingsTo(settings);
		m_overwrite.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.validateSettings(settings);
		m_input_column.validateSettings(settings);
		m_output_format.validateSettings(settings);
		m_mslevels.validateSettings(settings);
		m_table_desired.validateSettings(settings);
		m_username.validateSettings(settings);
		m_password.validateSettings(settings);
		m_save_to.validateSettings(settings);
		m_overwrite.validateSettings(settings);

	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.loadSettingsFrom(settings);
		m_input_column.loadSettingsFrom(settings);
		m_output_format.loadSettingsFrom(settings);
		m_mslevels.loadSettingsFrom(settings);
		m_table_desired.loadSettingsFrom(settings);
		m_username.loadSettingsFrom(settings);
		m_password.loadSettingsFrom(settings);
		m_save_to.loadSettingsFrom(settings);
		m_overwrite.loadSettingsFrom(settings);
	}
}
