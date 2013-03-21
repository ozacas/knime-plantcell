package au.edu.unimelb.plantcell.annotations;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
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
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceUtilityFactory;



/**
 * This is the model implementation of RegionAnalyzer.
 * Various nodes for analysis of sequence regions * n
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class String2SequenceNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Columns 2 Sequence");
        
   
    public final static String CFGKEY_ACCSN_COL    = "accession-column";
    public final static String CFGKEY_SEQUENCE_COL = "sequence-column";
    public final static String CFGKEY_TYPE         = "sequence-type";
    public static final String CFGKEY_DESCR        = "sequence-description";
    public static final String CFGKEY_REPLACE_COLUMN = "replace-sequence-column?";
    
    private final SettingsModelColumnName m_accsn = new SettingsModelColumnName(CFGKEY_ACCSN_COL, "Accession");
    private final SettingsModelString m_sequence  = new SettingsModelString(CFGKEY_SEQUENCE_COL, "Sequence");
    private final SettingsModelString m_type      = new SettingsModelString(CFGKEY_TYPE, SequenceType.AA.toString());
    private final SettingsModelString m_descr     = new SettingsModelString(CFGKEY_DESCR, "None");
    private final SettingsModelBoolean m_replace_seq = new SettingsModelBoolean(CFGKEY_REPLACE_COLUMN, false);
    
    private final static String DEFAULT_COL_NAME = "New sequence";
    
    /**
     * Constructor for the node model.
     */
    protected String2SequenceNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        logger.info("Converting strings to sequences");
        logger.info("Accession column is:   "+m_accsn.getStringValue());
        logger.info("Description column is: "+m_descr.getStringValue());
        logger.info("Sequence column is:    "+m_sequence.getStringValue());
        logger.info("Replacing input column? "+m_replace_seq.getBooleanValue());
        
        final boolean use_rowid = m_accsn.useRowID();
    	final boolean add_description  = (m_descr.getStringValue() != null);
        final int accsn_idx = inData[0].getSpec().findColumnIndex(m_accsn.getStringValue());
        final int seq_idx   = inData[0].getSpec().findColumnIndex(m_sequence.getStringValue());
        int didx = -1;
        if (add_description) {
        	didx = inData[0].getSpec().findColumnIndex(m_descr.getStringValue());
        }
        final int descr_idx = didx;
        if ((!use_rowid && accsn_idx < 0) || seq_idx < 0) {
        	throw new InvalidSettingsException("Cannot find sequence input - re-configure the node?");
        }
        
    
    	final SequenceType          st = SequenceType.getValue(m_type.getStringValue());
    	
    	ColumnRearranger c = new ColumnRearranger(inData[0].getSpec());
    	String name = m_sequence.getStringValue();
    	if (!m_replace_seq.getBooleanValue()) {
    		name = DEFAULT_COL_NAME;
    	}
    	DataColumnSpec seq_col = new DataColumnSpecCreator(name, SequenceCell.TYPE).createSpec();
    	CellFactory factory = new SingleCellFactory(seq_col) {

			@Override
			public DataCell getCell(DataRow row) {
				DataCell c_seq = row.getCell(seq_idx);
				if (c_seq == null || c_seq.isMissing())
					return DataType.getMissingCell();
				String accsn = "";
				if (use_rowid) {
	        		accsn = row.getKey().getString();
	        	} else {
	        		DataCell c_accsn = row.getCell(accsn_idx);
	        		if (c_accsn.isMissing()) 
	        			return DataType.getMissingCell();
	        		accsn = c_accsn.toString();
	        	}
	        	
				try {
		        	if (!add_description) {
		        		return SequenceUtilityFactory.createSequenceCell(accsn, c_seq.toString(), st);
		        	} else {
		        		return SequenceUtilityFactory.createSequenceCell(accsn, row.getCell(descr_idx).toString(), c_seq.toString(), st);
		        	}
				} catch (InvalidSettingsException ise) {
					ise.printStackTrace();
					return DataType.getMissingCell();
				}
			}
    		
    	};
    	
    	if (m_replace_seq.getBooleanValue()) {
    		c.replace(factory, name);
    	} else {
    		c.append(factory);
    	}
    	
    	BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], c, exec);
        return new BufferedDataTable[]{out};
    }

	private DataTableSpec make_output_spec(DataTableSpec spec) {
		if (!m_replace_seq.getBooleanValue()) {
	    	DataColumnSpec[] cols = new DataColumnSpec[1];
	    	cols[0] = new DataColumnSpecCreator(DEFAULT_COL_NAME, SequenceCell.TYPE).createSpec();
	    
			return new DataTableSpec(spec, new DataTableSpec(cols));
		} else {
			DataColumnSpec[] out_cols = new DataColumnSpec[spec.getNumColumns()];
			for (int i=0; i<spec.getNumColumns(); i++) {
				DataColumnSpec colSpec = spec.getColumnSpec(i);
				if (colSpec.getName().equals(m_sequence.getStringValue())) {
					colSpec = new DataColumnSpecCreator(m_sequence.getStringValue(), SequenceCell.TYPE).createSpec();
				}
				out_cols[i] = colSpec;
			}
			return new DataTableSpec(out_cols);
		}
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
        return new DataTableSpec[]{ make_output_spec(inSpecs[0]) };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_accsn.saveSettingsTo(settings);
    	m_sequence.saveSettingsTo(settings);
    	m_type.saveSettingsTo(settings);
    	m_descr.saveSettingsTo(settings);
    	m_replace_seq.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_accsn.loadSettingsFrom(settings);
    	m_sequence.loadSettingsFrom(settings);
    	m_type.loadSettingsFrom(settings);
    	m_descr.loadSettingsFrom(settings);
    	if (settings.containsKey(CFGKEY_REPLACE_COLUMN)) {		// backward compatibility
    		m_replace_seq.loadSettingsFrom(settings);
    	}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_accsn.validateSettings(settings);
    	m_sequence.validateSettings(settings);
    	m_type.validateSettings(settings);
    	m_descr.validateSettings(settings);
    	if (settings.containsKey(CFGKEY_REPLACE_COLUMN)) { // backward compatibility
    		m_replace_seq.validateSettings(settings);
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

