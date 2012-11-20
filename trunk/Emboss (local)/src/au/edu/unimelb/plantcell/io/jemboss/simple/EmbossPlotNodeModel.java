package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.ExternalProgram;


/**
 * This is the model implementation of EmbossPredictor.
 * Runs EMBOSS tools which take sequence(s) as input and provide a GFF output for inclusion as a annotation track on the output sequences.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class EmbossPlotNodeModel extends EmbossPredictorNodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("EMBOSS Plot");
       
    // configuration settings
    public static final String CFGKEY_SEQUENCE = "sequence-column";
    public static final String CFGKEY_PROGRAM  = "emboss-program";
    public static final String CFGKEY_ARGS     = "command-line-args";
    public static final String CFGKEY_USER_FIELDS = UserSettingsPanel.CFGKEY_USER_FIELDS;

    
    private SettingsModelString m_sequence = new SettingsModelString(CFGKEY_SEQUENCE, "");
    private SettingsModelString m_program  = new SettingsModelString(CFGKEY_PROGRAM, "");
    private SettingsModelString m_args     = new SettingsModelString(CFGKEY_ARGS, "");
    private SettingsModelString m_user_fields = new SettingsModelString(CFGKEY_USER_FIELDS, "");

    /**
     * Constructor for the node model.
     */
    protected EmbossPlotNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	String       s = m_program.getStringValue();
    	int idx = s.indexOf(':');
    	if (idx < 0) 
    		throw new InvalidSettingsException("Please configure the node first!");
    	String program = s.substring(0, idx);
    	logger.info("Running EMBOSS plot program: "+program);
    	String emboss_dir = ACDApplication.getEmbossDir();
    	
    	logger.info("Running EMBOSS software using preference: "+emboss_dir);
    	File prog = ExternalProgram.find(emboss_dir, program);
    	if (prog == null)
    		throw new InvalidSettingsException("Unable to locate: "+program);
    	logger.info("Running: "+prog.getAbsolutePath());
    	   
    	int seq_idx = inData[0].getSpec().findColumnIndex(m_sequence.getStringValue());
    	if (seq_idx < 0)
    		throw new InvalidSettingsException("Cannot find column: "+m_sequence.getStringValue()+" - reconfigure?");

    	ACDApplication appl = ACDApplication.find(program);
    	
    	// instantiate the cell factory
        CellFactory cellFactory = new EmbossPlotCellFactory(
        		new DataColumnSpecCreator("Image from EMBOSS:"+prog, DataType.getType(PNGImageCell.class)).createSpec(),
                seq_idx, prog, m_args.getStringValue(), appl, logger);
        
        // create the column rearranger
        ColumnRearranger outputTable = new ColumnRearranger(inData[0].getDataTableSpec());
      
        // append the new column
        outputTable.append(cellFactory);
        
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], outputTable, exec);
        // return it
        return new BufferedDataTable[]{out};	
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return null;
    }
    
	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_sequence.saveSettingsTo(settings);
    	m_program.saveSettingsTo(settings);
    	m_args.saveSettingsTo(settings);
    	m_user_fields.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.loadSettingsFrom(settings);
    	m_program.loadSettingsFrom(settings);
    	m_args.loadSettingsFrom(settings);
    	m_user_fields.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.validateSettings(settings);
    	m_program.validateSettings(settings);
    	m_args.validateSettings(settings);
    	m_user_fields.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
    }

}

