package au.edu.unimelb.plantcell.misc.biojava;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
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
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * This is the model implementation of BioJavaProcessor.
 * Analyses the specified data using BioJava (see http://www.biojava.org) and produces the result at output
 *
 * @author Andrew Cassin
 */
public class BioJavaProcessorNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("BioJava Processor");
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_TASK         = "task";
	static final String CFGKEY_SEQUENCE_COL = "sequence-column";
	static final String CFGKEY_CATEGORY     = "category";		// currently selected task category in configure dialog
	
    /** initial default task */
    private static final String DEFAULT_TASK         = "";
    private static final String DEFAULT_SEQUENCE_COL = "Sequence";
    private static final String DEFAULT_CATEGORY     = "All";

    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
    private final SettingsModelString m_task            = make_as_string(CFGKEY_TASK);
    private final SettingsModelString m_column = make_as_string(CFGKEY_SEQUENCE_COL);	// this node may process columns other than String columns
    private final SettingsModelString m_category        = make_as_string(CFGKEY_CATEGORY);
    
    // state which is not persisted
    private int     m_sequence_idx;
    private boolean m_warned_bad_chars;		// a warning is logged if likely non-NA/AA letters are encountered during processing
    private int     m_bad_char_count;
    
    /**
     * Constructor for the node model.
     */
    protected BioJavaProcessorNodeModel() {
        super(1, 1);
        m_sequence_idx = -1;
        
    }

    public static SettingsModel make(String cfgkey) {
    	if (cfgkey.equals(CFGKEY_TASK)) {
    		return new SettingsModelString(CFGKEY_TASK, DEFAULT_TASK);
    	} else if (cfgkey.equals(CFGKEY_SEQUENCE_COL)) {
    		return new SettingsModelString(CFGKEY_SEQUENCE_COL, DEFAULT_SEQUENCE_COL);
    	} else if (cfgkey.equals(CFGKEY_CATEGORY)) {
    		return new SettingsModelString(CFGKEY_CATEGORY, DEFAULT_CATEGORY);
    	}
    	return null;
    }
    
    public static SettingsModelString make_as_string(String cfgkey) {
    	return (SettingsModelString) make(cfgkey);
    }
    
    public static BioJavaProcessorTask[] getTasks() {
    	return new BioJavaProcessorTask[] {
				new AlternateTranslationProcessor(),
				new FrameTranslationProcessor(),
				new HydrophobicityProcessor(),
				new LongestFrameProcessor(),		// experimental (warning to all users)
				//new PositionByResidueProcessor(),
				new ResidueFrequencyProcessor(),
				new SequenceTranslationProcessor(),
				new SequenceCleanerProcessor(),
				new SNPAssistedFrameshiftDetector(),
				//TrypticPeptideExtractor_v2.getInstance(),		// disabled pending a re-factor
				new WeightedHomopolymerRateProcessor(),
				new AlignmentSequenceExtractorTask(),
				new GCCalculatorTask()
    	};
    }
    
    public static String[] getTaskNames(Iterable<BioJavaProcessorTask> tasks) {
    	ArrayList<String> ret = new ArrayList<String>();
		for (BioJavaProcessorTask t : tasks) {
			for (String s : t.getNames()) {
				ret.add(s);
			}
		}
    	return ret.toArray(new String[0]);
    }
    
    public BioJavaProcessorTask make_biojava_processor(String task) throws NotConfigurableException,NotImplementedException {
    	if (task.startsWith("Hydrophobicity")) {
    		return new HydrophobicityProcessor();
    	} else if (task.startsWith("Six")) {
    		return new FrameTranslationProcessor();
    	} else if (task.startsWith("Convert")) {
    		return new SequenceTranslationProcessor();
    	} else if (task.startsWith("Alternate translation")) {
    		return new AlternateTranslationProcessor();
    	} else if (task.startsWith("Count")) {
    		return new ResidueFrequencyProcessor();
    	} else if (task.equals("Residue Frequency by Position")) {
    		return new PositionByResidueProcessor();
    	} else if (task.startsWith("Longest reading frame")) {
    		return new LongestFrameProcessor();
    	} else if (task.startsWith("Weighted")) {
    		return new WeightedHomopolymerRateProcessor();
    	} else if (task.startsWith("SNP")) {
    		return new SNPAssistedFrameshiftDetector();
    	} else if (task.startsWith("Tryptic")) {
    		return new TrypticPeptideExtractor_v2();
    	} else if (task.startsWith("Clean")) {
    		return new SequenceCleanerProcessor();
    	} else if (task.startsWith("Alignment")) {
    		return new AlignmentSequenceExtractorTask();
    	} else if (task.startsWith("GC")) {
    		return new GCCalculatorTask();
    	} else if (task.trim().length() >= 1) {
        	throw new NotImplementedException("Unknown BioJava task to perform! Probably a bug...");
    	} else {
    		throw new NotConfigurableException("NOTE: you must configure the node!");
    	}
    }
    
    /**
     *  Returns the user-selected cell for the specified data row.
     */
    public DataCell getUserSpecifiedCell(DataRow r) {
    	assert m_sequence_idx >= 0;
    	return r.getCell(m_sequence_idx);
    }
    
    /**
     * Retrieve the sequence as letters only in the user-configured cell. Other characters
     * are removed as this would upset biojava conversion (which would silently fail)
     * 
     * @param r
     * @return the codes for the 
     */
    public String getSequence(DataRow r) {
    	String val = getUserSpecifiedCell(r).toString();
    	StringBuffer sb = new StringBuffer(val.length());
    	int len = val.length();
    	for (int i=0; i<len; i++) {
    		char c = val.charAt(i);
    		if (Character.isLetter(c) || c == '-' || c == '*') {
    			sb.append(c);
    		} else {
    			m_bad_char_count++;
    			if (!m_warned_bad_chars) {
    				logger.warn("Encountered non-letter symbol: "+c+" in row "+r.getKey().getString()+": results may be incorrect (character ignored)");
    				m_warned_bad_chars = true;
    			} 
    		}
    	}
    	return sb.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        // the data table spec of the single output table, 
        // the table will have three columns:
    	BioJavaProcessorTask bjpi = make_biojava_processor(m_task.getStringValue());

        DataTableSpec outputSpec = new DataTableSpec(inData[0].getDataTableSpec(), 
        									new DataTableSpec(bjpi.getColumnSpecs()));
        
        // the execution context will provide us with storage capacity, in this
        // case a data container to which we will add rows sequentially
        // Note, this container can also handle arbitrary big data tables, it
        // will buffer to disc if necessary.
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        
        String colname = m_column.getStringValue();
        m_sequence_idx = inData[0].getDataTableSpec().findColumnIndex(colname);
        if (m_sequence_idx < 0) {
        	throw new Exception("Cannot find column: "+m_column.getStringValue());
        }
        logger.info("Processing column: "+colname+" at index "+m_sequence_idx);
        m_warned_bad_chars = false;
        m_bad_char_count   = 0;
        bjpi.init(this, m_task.getStringValue(), m_sequence_idx);
       
        // create the column rearranger
        ColumnRearranger outputTable = new ColumnRearranger(inData[0].getDataTableSpec());
      
        // append the new column
        outputTable.append(bjpi);
        
        BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], outputTable, exec);
      
        if (m_bad_char_count > 0) {
        	logger.warn("WARNING: encountered "+m_bad_char_count+" non-residue symbols during processing. Results may be incorrect!");
        }
        // once we are done, we close the container and return its table
        container.close();
        return new BufferedDataTable[]{out};
    }

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
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
        m_task.saveSettingsTo(settings);
        m_column.saveSettingsTo(settings);
        m_category.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
         
        m_task.loadSettingsFrom(settings);
        m_column.loadSettingsFrom(settings);
        if (settings.containsKey(CFGKEY_CATEGORY)) {
        	m_category.loadSettingsFrom(settings);
        } else {
        	m_category.setStringValue(DEFAULT_CATEGORY);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        m_task.validateSettings(settings);
        m_column.validateSettings(settings);
        if (settings.containsKey(CFGKEY_CATEGORY)) {
        	m_category.validateSettings(settings);
        }
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

