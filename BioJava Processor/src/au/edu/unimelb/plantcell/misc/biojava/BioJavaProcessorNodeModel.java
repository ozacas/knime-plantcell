package au.edu.unimelb.plantcell.misc.biojava;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
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
				AlternateTranslationProcessor.getInstance(),
				FrameTranslationProcessor.getInstance(),
				HydrophobicityProcessor.getInstance(),
				LongestFrameProcessor.getInstance(),		// experimental (warning to all users)
				PositionByResidueProcessor.getInstance(),
				ResidueFrequencyProcessor.getInstance(),
				SequenceTranslationProcessor.getInstance(),
				SequenceCleanerProcessor.getInstance(),
				SNPAssistedFrameshiftDetector.getInstance(),
				//TrypticPeptideExtractor_v2.getInstance(),		// disabled pending a re-factor
				WeightedHomopolymerRateProcessor.getInstance(),
				SequenceFormattedCleanerProcessor.getInstance(),
				AlignmentSequenceExtractorTask.getInstance(),
				GCCalculatorTask.getInstance()
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
    		return HydrophobicityProcessor.getInstance();
    	} else if (task.startsWith("Six")) {
    		return FrameTranslationProcessor.getInstance();
    	} else if (task.startsWith("Convert")) {
    		return SequenceTranslationProcessor.getInstance();
    	} else if (task.startsWith("Alternate translation")) {
    		return AlternateTranslationProcessor.getInstance();
    	} else if (task.startsWith("Count")) {
    		return ResidueFrequencyProcessor.getInstance();
    	} else if (task.equals("Residue Frequency by Position")) {
    		return PositionByResidueProcessor.getInstance();
    	} else if (task.startsWith("Longest reading frame")) {
    		return LongestFrameProcessor.getInstance();
    	} else if (task.startsWith("Weighted")) {
    		return WeightedHomopolymerRateProcessor.getInstance();
    	} else if (task.startsWith("SNP")) {
    		return SNPAssistedFrameshiftDetector.getInstance();
    	} else if (task.startsWith("Tryptic")) {
    		return TrypticPeptideExtractor_v2.getInstance();
    	} else if (task.startsWith("Clean")) {
    		if (task.toLowerCase().indexOf(" format sequences") >= 0) 
    			return SequenceFormattedCleanerProcessor.getInstance();
    		return SequenceCleanerProcessor.getInstance();
    	} else if (task.startsWith("Alignment")) {
    		return AlignmentSequenceExtractorTask.getInstance();
    	} else if (task.startsWith("GC")) {
    		return GCCalculatorTask.getInstance();
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

        DataTableSpec outputSpec = make_output_spec(inData[0].getDataTableSpec(), bjpi);
        // make_output_spec() invokes init() on bjpi so we can assume the task is ready...
     
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
        bjpi.execute(new ColumnIterator(inData[0].iterator(), m_sequence_idx), exec, logger, inData, container);
        
        if (m_bad_char_count > 0) {
        	logger.warn("WARNING: encountered "+m_bad_char_count+" non-residue symbols during processing. Results may be incorrect!");
        }
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }

    private DataTableSpec make_output_spec(DataTableSpec inSpec, BioJavaProcessorTask bjpi) throws Exception {
    	 if (bjpi == null) 
    		 bjpi = make_biojava_processor(m_task.getStringValue());
    	 bjpi.init(this, m_task.getStringValue());
    	 
         DataTableSpec result_spec= bjpi.get_table_spec();
         
         DataTableSpec outputSpec;
         if (bjpi.isMerged())
         	outputSpec = new DataTableSpec("BioJava Processor Specification", inSpec, result_spec);
         else 
         	outputSpec = result_spec;
         return outputSpec;
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
    	DataTableSpec outspec = null;
    	
    	try {
    		outspec = make_output_spec(inSpecs[0], null);
    	} catch (NotConfigurableException e) {
    		logger.warn("Node must be configured first!");
    		outspec = null;
    	} catch (Exception e2) {
    		e2.printStackTrace();
    		throw new InvalidSettingsException("Unable to configure the node! Input table is different: error is "+e2.getMessage());
    	}
       
        return new DataTableSpec[]{outspec};
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

