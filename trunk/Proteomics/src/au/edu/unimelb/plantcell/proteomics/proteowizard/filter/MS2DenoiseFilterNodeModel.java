package au.edu.unimelb.plantcell.proteomics.proteowizard.filter;

import java.io.IOException;

import javax.activation.DataHandler;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.servers.core.jaxb.results.ListOfDataFile;
import au.edu.unimelb.plantcell.servers.msconvertee.endpoints.MSConvert;
import au.edu.unimelb.plantcell.servers.msconvertee.endpoints.ProteowizardJob;
import au.edu.unimelb.plantcell.servers.msconvertee.jaxb.FilterParametersType;
import au.edu.unimelb.plantcell.servers.msconvertee.jaxb.MS2DenoiseType;
import au.edu.unimelb.plantcell.servers.msconvertee.jaxb.ObjectFactory;

/**
 * Filters the chosen data files (from input table) using the Proteowizard msconvert utility via
 * the MSConvertEE web service. This service enables centralised data conversion and vendor-support.
 * 
 * @author acassin
 *
 */
public class MS2DenoiseFilterNodeModel extends MSLevelsFilterNodeModel {
	private final static NodeLogger logger = NodeLogger.getLogger("MS/MS Denoise Filter");
	
	public static final String CFGKEY_N = "number-of-peaks-to-retain-per-window";
	public static final String CFGKEY_WINDOW   = "window-size-in-daltons";
	public static final String CFGKEY_RELAX      = "multicharge-fragment-relaxation";
	
	private SettingsModelIntegerBounded     m_n = new SettingsModelIntegerBounded(CFGKEY_N, 6, 1, 1000);
	private SettingsModelDoubleBounded m_window = new SettingsModelDoubleBounded(CFGKEY_WINDOW, 30.0d, 1.0d, 10000.0d);
	private SettingsModelBoolean        m_relax = new SettingsModelBoolean(CFGKEY_RELAX, Boolean.TRUE);
	
	public MS2DenoiseFilterNodeModel() {
		super(1,1);
	}

	protected NodeLogger getNodeLogger() {
		return logger;
	}
   
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
		int file_idx = getInputFileColumnIndex(inData[0].getSpec());
		if (file_idx < 0) {
			throw new InvalidSettingsException("No such column: "+getInputFileColumnName()+" - reconfigure?");
		}
		int done = 0;
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
			addDenoiseFilterSettings(j);
		
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

	private void addDenoiseFilterSettings(final ProteowizardJob j) {
		assert(j != null);
		ObjectFactory         of = new ObjectFactory();
		FilterParametersType fpt = of.createFilterParametersType();
		MS2DenoiseType      ms2f = of.createMS2DenoiseType();
		ms2f.setPeaksInWindow(m_n.getIntValue());
		ms2f.setWindowWidth(m_window.getDoubleValue());
		ms2f.setMultichargeFragmentRelaxation(m_relax.getBooleanValue());
		fpt.setMs2Denoise(ms2f);
		j.setFilterParameters(fpt);
	}

	@Override
	public DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		return super.configure(inSpecs);
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		super.saveSettingsTo(settings);
		m_n.saveSettingsTo(settings);
		m_window.saveSettingsTo(settings);
		m_relax.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.validateSettings(settings);
		m_n.validateSettings(settings);
		m_window.validateSettings(settings);
		m_relax.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.loadValidatedSettingsFrom(settings);
		m_n.loadSettingsFrom(settings);
		m_window.loadSettingsFrom(settings);
		m_relax.loadSettingsFrom(settings);
	}
}
