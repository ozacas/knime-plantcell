package au.edu.unimelb.plantcell.proteomics.itraq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import au.edu.unimelb.plantcell.core.ExternalProgram;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.TempDirectory;

/**
 * This is the model implementation of ITraqAnalyzer.
 * Given a (set of) proteomics runs, with identified peptides, proteins and iTRAQ quantitation values this nodes performs an analysis and provides normalised results for the user in easy-to-read format. Based on method published in the scientific literature.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatic
 */
public class ITraqAnalyzerNodeModel extends NodeModel {
    private final static NodeLogger logger = NodeLogger.getLogger("iTRaQ Analyzer");
    
    // location of root of R installation (needed for finding the R executable)
    public static final String CFGKEY_R_FOLDER_ROOT = "r-root-folder";
    
    /**
     * Constructor for the node model
     */
    protected ITraqAnalyzerNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	// 1. temporary directory where the R scripts to run and plots & data matrices are stored for execution by R
    	TempDirectory td       = new TempDirectory();
    	if (!td.asFile().exists())
    		throw new InvalidSettingsException("Unable to create temp directory: "+td);
    	
    	File Rexe = locateR();
    	
    	// 2. create output containers
    	DataTableSpec[] outSpec = make_output_specs(new DataTableSpec[] { inData[0].getSpec() });
    	MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(outSpec[0]), "QuantitatedProtein");
    	MyDataContainer c2 = new MyDataContainer(exec.createDataContainer(outSpec[1]), "Plot");
    	
    	// 2. run algorithm as per paper requirements, logging as we go to keep the user informed about data operations
    	try {
    		validateInputData(inData);
    		exec.checkCanceled();
    		exec.setProgress(0.2);
    		checkHMiscAvailable(Rexe, td);
    		exec.checkCanceled();
    		exec.setProgress(0.4);
    		normaliseInputData(inData[0], td);
    		exec.checkCanceled();
    		exec.setProgress(0.6);
    		trainWeights(td);
    		exec.checkCanceled();
    		exec.setProgress(0.8);
    		computeProteinQuantitation(td);
    	} finally {
    		if (td != null)
    			td.deleteRecursive();
    	}

        return new BufferedDataTable[]{c1.close(), c2.close()};
    }

    private File locateR() throws InvalidSettingsException {
    	File usr_folder        = new File("/usr");
    	File usr_local_folder  = new File("/usr/local");
    	File r_pref_folder     = null;
    	File R = ExternalProgram.find(new String[] { "R", "Rscript" }, new File[] {r_pref_folder, usr_folder, usr_local_folder});
    	if (R == null)
    		throw new InvalidSettingsException("Cannot find R -- is it installed and node correctly configured?");
    	logger.info("Using R executable at: "+R.getAbsolutePath());
    	return R;
	}

	private void computeProteinQuantitation(final TempDirectory td) {
    	assert(td != null);
	}

	private void trainWeights(final TempDirectory td) {
    	assert(td != null);
    	
	}

	private void normaliseInputData(final BufferedDataTable inData, final TempDirectory td) {
		assert(inData != null && td != null);
		// perform median normalisation so that all reporter ion channels have the same median quantitation value
		// we dont do checking of the data here, we assume validateInputData() has done all that for us
		
		File normalised_data_file = saveDataMatrixToFile(inData, td);
		medianNormalise(normalised_data_file);		// done by R
	}

	private void medianNormalise(final File normalised_data_file) {
		assert(normalised_data_file != null);
		// TODO...
	}

	private File saveDataMatrixToFile(final BufferedDataTable inData, final TempDirectory td) {
		File out = new File(td.asFile(), "raw_data_before_normalisation.tsv");
		// TODO...
		return out;
	}

	private void checkHMiscAvailable(final File r, final TempDirectory td) throws IOException,InvalidSettingsException {
		assert(r != null);
		File script = new File(td.asFile(), "check_hmisc.r");
		PrintWriter pw = new PrintWriter(new FileWriter(script));
		pw.write("if (\"hMisc\" %in% installed.packages()) quit(save=\"no\", status=11) else quit(save=\"no\", status=10)");
		pw.close();
		
		int exit_status = run_r(r, script);
		if (exit_status != 11)
			throw new InvalidSettingsException("You must install HMisc into your R installation for this node to work! (see node documentation) : exit status "+exit_status);
	}

	/**
	 * invokes the specified R executable with the chosen script and returns the exit status
     * of the script to the caller (or if R crashes, the exit status of the R process)
	 * @param r
	 * @param script
	 * @return exit status of R script or R process (eg. if crash occurs)
	 * @throws IOException 
	 * @throws ExecuteException 
	 */
	private int run_r(final File r, final File script) throws ExecuteException, IOException {
		assert(r != null && script != null);
		boolean is_rscript = (r.getName().toLowerCase().indexOf("rscript") >= 0);
		CommandLine cl = new CommandLine(r);
		if (! is_rscript) {
			cl.addArgument("--file="+script.getAbsolutePath());
		} else {
			cl.addArgument(script.getAbsolutePath());
		}
		DefaultExecutor exe = new DefaultExecutor();
		return exe.execute(cl);
	}

	private void validateInputData(BufferedDataTable[] inData) {
		// TODO Auto-generated method stub
		
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
    	return make_output_specs(inSpecs);
    }

    private DataTableSpec[] make_output_specs(DataTableSpec[] inSpecs) {
		DataColumnSpec[] col1 = new DataColumnSpec[10];
		col1[0] = new DataColumnSpecCreator("Protein ID", StringCell.TYPE).createSpec();
		col1[1] = new DataColumnSpecCreator("Peptides (incl. weights)", StringCell.TYPE).createSpec();
		col1[2] = new DataColumnSpecCreator("113", DoubleCell.TYPE).createSpec();
		col1[3] = new DataColumnSpecCreator("114", DoubleCell.TYPE).createSpec();
		col1[4] = new DataColumnSpecCreator("115", DoubleCell.TYPE).createSpec();
		col1[5] = new DataColumnSpecCreator("116", DoubleCell.TYPE).createSpec();

		col1[6] = new DataColumnSpecCreator("117", DoubleCell.TYPE).createSpec();
		col1[7] = new DataColumnSpecCreator("118", DoubleCell.TYPE).createSpec();
		col1[8] = new DataColumnSpecCreator("119", DoubleCell.TYPE).createSpec();
		col1[9] = new DataColumnSpecCreator("121", DoubleCell.TYPE).createSpec();
		
		DataColumnSpec[] col2 = new DataColumnSpec[2];
		col2[0] = new DataColumnSpecCreator("Plot caption", StringCell.TYPE).createSpec();
		col2[1] = new DataColumnSpecCreator("QA Plot", DataType.getType(PNGImageCell.class)).createSpec();
		
		return new DataTableSpec[] { new DataTableSpec(col1), new DataTableSpec(col2) };
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // TODO: generated method stub
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // TODO: generated method stub
    }

}

