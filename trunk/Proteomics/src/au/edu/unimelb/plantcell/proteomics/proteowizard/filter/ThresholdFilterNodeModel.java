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
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.servers.core.jaxb.results.ListOfDataFile;
import au.edu.unimelb.plantcell.servers.msconvertee.endpoints.MSConvert;
import au.edu.unimelb.plantcell.servers.msconvertee.endpoints.ProteowizardJob;
import au.edu.unimelb.plantcell.servers.msconvertee.jaxb.FilterParametersType;
import au.edu.unimelb.plantcell.servers.msconvertee.jaxb.ObjectFactory;
import au.edu.unimelb.plantcell.servers.msconvertee.jaxb.ThresholdParametersType;

/**
 * Filters the chosen data files (from input table) using the Proteowizard msconvert utility via
 * the MSConvertEE web service. This service enables centralised data conversion and vendor-support.
 * 
 * @author acassin
 *
 */
public class ThresholdFilterNodeModel extends MSLevelsFilterNodeModel {
	private final static NodeLogger logger = NodeLogger.getLogger("Threshold Filter");
	
	public static final String CFGKEY_ORIENTATION = "threshold-orientation";
	public static final String CFGKEY_THRESHOLD   = "threshold-value";
	public static final String CFGKEY_METHOD      = "thresholding-method";

	public static final String[] THRESHOLD_ORIENTATIONS = new String[] {"Above the specified threshold", "Below the specified threshold" };
	public static final String[] METHODS = new String[] { "Keep N peaks", "Keep N peaks (and ties)", "Only those peaks which satisfy the absolute threshold", 
		"Only those peaks which meet the base peak threshold", "Only those peaks which mean the percentage of the TIC", 
		"Retain the most/least intense peaks up to the chosen percentage of the TIC"
	};
	
	private final SettingsModelString m_orientation = new SettingsModelString(CFGKEY_ORIENTATION, "");
	private final SettingsModelDouble m_threshold   = new SettingsModelDouble(CFGKEY_THRESHOLD, 0.0d);
	private final SettingsModelString m_method      = new SettingsModelString(CFGKEY_METHOD, METHODS[0]);
	
	public ThresholdFilterNodeModel() {
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
			setThresholdParameters(j);
		
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

	private void setThresholdParameters(final ProteowizardJob j) {
		assert(j != null);
		ObjectFactory         of = new ObjectFactory();
		FilterParametersType fpt = of.createFilterParametersType();
		ThresholdParametersType thres = of.createThresholdParametersType();
		fpt.setIntensityFilter(thres);
		j.setFilterParameters(fpt);
	}

	@Override
	public DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		return super.configure(inSpecs);
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		super.saveSettingsTo(settings);
		m_method.saveSettingsTo(settings);
		m_threshold.saveSettingsTo(settings);
		m_orientation.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.validateSettings(settings);
		m_method.validateSettings(settings);
		m_threshold.validateSettings(settings);
		m_orientation.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.loadValidatedSettingsFrom(settings);
		m_method.validateSettings(settings);
		m_threshold.validateSettings(settings);
		m_orientation.validateSettings(settings);
	}
}
