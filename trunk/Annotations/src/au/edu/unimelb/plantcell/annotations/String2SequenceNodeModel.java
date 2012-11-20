package au.edu.unimelb.plantcell.annotations;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
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
    
    private final SettingsModelColumnName m_accsn = new SettingsModelColumnName(CFGKEY_ACCSN_COL, "Accession");
    private final SettingsModelString m_sequence  = new SettingsModelString(CFGKEY_SEQUENCE_COL, "Sequence");
    private final SettingsModelString m_type      = new SettingsModelString(CFGKEY_TYPE, SequenceType.AA.toString());
    private final SettingsModelString m_descr     = new SettingsModelString(CFGKEY_DESCR, "None");
    
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
        
        boolean use_rowid = m_accsn.useRowID();
    	boolean add_description  = (m_descr.getStringValue() != null);
        int accsn_idx = inData[0].getSpec().findColumnIndex(m_accsn.getStringValue());
        int seq_idx   = inData[0].getSpec().findColumnIndex(m_sequence.getStringValue());
        int descr_idx = -1;
        if (add_description) {
        	descr_idx = inData[0].getSpec().findColumnIndex(m_descr.getStringValue());
        }
        if ((!use_rowid && accsn_idx < 0) || seq_idx < 0) {
        	throw new InvalidSettingsException("Cannot find sequence input - re-configure the node?");
        }
        
        DataTableSpec outputSpec = make_output_spec(inData[0].getSpec());
        BufferedDataContainer  c = exec.createDataContainer(outputSpec);
        RowIterator           it = inData[0].iterator();
    	SequenceType          st = SequenceType.getValue(m_type.getStringValue());
    	int n_rows = inData[0].getRowCount();
    	int frac   = n_rows / 100;
    	if (frac == 0)
    		frac = 1;
    	int done = 0;
        while (it.hasNext()) {
        	DataRow       r  = it.next();
        	DataCell c_seq   = r.getCell(seq_idx);
        	if (c_seq.isMissing()) 
        		continue;
        	String accsn = "";
        	if (use_rowid) {
        		accsn = r.getKey().getString();
        	} else {
        		DataCell c_accsn = r.getCell(accsn_idx);
        		if (c_accsn.isMissing()) 
        			continue;
        		accsn = c_accsn.toString();
        	}
        	
        	DataCell[] cells = new DataCell[1];
        	if (!add_description) {
        		cells[0] = SequenceUtilityFactory.createSequenceCell(accsn, c_seq.toString(), st);
        	} else {
        		cells[0] = SequenceUtilityFactory.createSequenceCell(accsn, r.getCell(descr_idx).toString(), c_seq.toString(), st);
        	}
        	c.addRowToTable(new JoinedRow(r, new DefaultRow(r.getKey().getString(), cells)));
        	if (done++ % frac == 0) {
        		exec.checkCanceled();
        		exec.setProgress(((double)done)/frac);
        	}
        }
        
        c.close();
        BufferedDataTable out = c.getTable();
        return new BufferedDataTable[]{out};
    }

	private DataTableSpec make_output_spec(DataTableSpec spec) {
    	DataColumnSpec[] cols = new DataColumnSpec[1];
    	cols[0] = new DataColumnSpecCreator("Biological Sequence", SequenceCell.TYPE).createSpec();
    
		return new DataTableSpec(spec, new DataTableSpec(cols));
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

