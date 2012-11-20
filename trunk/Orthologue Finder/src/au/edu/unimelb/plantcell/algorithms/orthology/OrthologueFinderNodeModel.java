package au.edu.unimelb.plantcell.algorithms.orthology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * This is the model implementation of OrthologueFinder.
 * Given two blast results (sequence A against database B and sequence B against database A) this node computes orthologous genes using Reciprocal Blast Hit (RBH) and other algorithms
 *
 * @author Andrew Cassin
 */
public class OrthologueFinderNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(OrthologueFinderNodeModel.class);
        
    public static final String CFGKEY_RESULTS_DB1 = "results-db1";
    public static final String CFGKEY_RESULTS_DB2 = "results-db2";
    public static final String CFGKEY_ALGORITHM   = "algorithm";
	public static final String CFGKEY_EPSILON     = "epsilon";
    
    private final SettingsModelString m_hits_db1 = new SettingsModelString(CFGKEY_RESULTS_DB1, "");
    private final SettingsModelString m_hits_db2 = new SettingsModelString(CFGKEY_RESULTS_DB2, "");
    private final SettingsModelString m_algo     = new SettingsModelString(CFGKEY_ALGORITHM, "RBH");
    private final SettingsModelString m_epsilon  = new SettingsModelString(CFGKEY_EPSILON, "1e-50");
    
    // internal state (not persisted)
    private final Map<String,BLASTBucket> m_hits = new HashMap<String,BLASTBucket>();
    
    /**
     * Constructor for the node model.
     */
    protected OrthologueFinderNodeModel() {
       super(0, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        logger.info("Extracting putative orthologs from BLAST results: epsilon "+m_epsilon.getStringValue());

        DataTableSpec[] tables = make_output_specs();
        File h1 = new File(m_hits_db1.getStringValue());
        File h2 = new File(m_hits_db2.getStringValue());
        BufferedReader rdr1 = new BufferedReader(new InputStreamReader(new FileInputStream(h1)));
        BufferedReader rdr2 = new BufferedReader(new InputStreamReader(new FileInputStream(h2)));
        BufferedDataContainer c = exec.createDataContainer(tables[0]);
        BufferedDataContainer c2= exec.createDataContainer(tables[1]);
        
        // load blast hits into internal structure
        m_hits.clear();
        process_blast_hits(rdr1, c2, true);
        process_blast_hits(rdr2, c2, false);
        
        
        // check best hit reciprocally and output to port where suitable...
        HashSet<PutativeOrthologue> done = new HashSet<PutativeOrthologue>();
        int id = 1;
        for (String key : m_hits.keySet()) {
        	BLASTBucket               b1 = m_hits.get(key);
        	OrthologueFilterInterface ofi = new BestHitOnly();
        	List<BLASTBucket>  possibles = b1.getBest(m_hits);
        	
        	for (BLASTBucket b2 : possibles) {
        		PutativeOrthologue[] orthos = b1.getOrthologues(ofi, b2, m_hits);
        		
        		for (PutativeOrthologue o : orthos) {
	        		if (!done.contains(o)) {
	        			DataCell[] accsn = new DataCell[2];
	        			accsn = new DataCell[] { new StringCell(o.getKey(true)), new StringCell(o.getKey(false)) };
	        			
	        			c.addRowToTable(new DefaultRow(new RowKey("r"+id++), accsn));
	        			// never again report an ortholog for the pair denoted by o
	        			done.add(o);
	        		}
        		}
        	}
        }
        
        // once we are done, we close the container and return its table
        c.close();
        c2.close();
        rdr1.close();
        rdr2.close();
        
        BufferedDataTable out = c.getTable();
        BufferedDataTable out2= c2.getTable();
        return new BufferedDataTable[]{out,out2};
    }

    /**
     * Reads each line from the specified <code>Reader</code> and populates the internal member
     * with the key data structure for examination of the blast results
     * 
     * @param rdr
     * @throws IOException
     */
    private void process_blast_hits(BufferedReader rdr, BufferedDataContainer raw_port, boolean is_A) throws IOException {
    	String line = null;
    	while ((line = rdr.readLine()) != null) {
    		if (line == null || line.trim().length() < 1 || line.startsWith("#")) {
    			continue;
    		}
    		String[] fields = line.split("\\t");
    		if (fields.length != 12) {
    			throw new IOException("Wrong number of BLAST fields: expected 12 got "+fields.length);
    		}
    		
    		BLASTRecord br = new BLASTRecord(fields, is_A);
    		raw_port.addRowToTable(br.asRow());
    		String k = br.getQueryAccsn();
    		BLASTBucket l = m_hits.get(k);
    		if (l == null) {
    			BLASTBucket al = new BLASTBucket(new BigDecimal(m_epsilon.getStringValue()), br);
    			m_hits.put(k, al);
    		} else {
    			l.add(br);
    		}
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

        return make_output_specs();
    }

    protected DataTableSpec[] make_output_specs() {
    	DataColumnSpec[] cols1 = new DataColumnSpec[2];
    	cols1[0] = new DataColumnSpecCreator("Accession from A", StringCell.TYPE).createSpec();
    	cols1[1] = new DataColumnSpecCreator("Accession from B", StringCell.TYPE).createSpec();
    	DataTableSpec port1 = new DataTableSpec(cols1);

    	DataColumnSpec[] cols2 = new DataColumnSpec[12];
    	// fields[0] is query accsn (string)
    	cols2[0] = new DataColumnSpecCreator("Query Accession", StringCell.TYPE).createSpec();
    	
		// fields[1] is subject accsn (string)
    	cols2[1] = new DataColumnSpecCreator("Subject Accession", StringCell.TYPE).createSpec();
    	
		// fields[2] is %identity (double)
    	cols2[2] = new DataColumnSpecCreator("% Identity", DoubleCell.TYPE).createSpec();
    	
		// fields[3] is alignment length (int)
    	cols2[3] = new DataColumnSpecCreator("Alignment Length", IntCell.TYPE).createSpec();
    	
		// fields[4] is mismatches (int)
    	cols2[4] = new DataColumnSpecCreator("Mismatches", IntCell.TYPE).createSpec();
    	
		// fields[5] is gap opens (int)
    	cols2[5] = new DataColumnSpecCreator("Gap Opens", IntCell.TYPE).createSpec();
    	
		// fields[6] is query start (int)
    	cols2[6] = new DataColumnSpecCreator("Query Start", IntCell.TYPE).createSpec();
    	
		// fields[7] is query end (int)
    	cols2[7] = new DataColumnSpecCreator("Query End", IntCell.TYPE).createSpec();
    	
		// fields[8] is subject start (int)
    	cols2[8] = new DataColumnSpecCreator("Subject Start", IntCell.TYPE).createSpec();
    	
		// fields[9] is subject end (int)
    	cols2[9] = new DataColumnSpecCreator("Subject End", IntCell.TYPE).createSpec();
    	
		// fields[10] is e-value (double)
    	cols2[10]= new DataColumnSpecCreator("E-Value", DoubleCell.TYPE).createSpec();
    	
		// fields[11] is bit score (double)
    	cols2[11]= new DataColumnSpecCreator("Bit Score", DoubleCell.TYPE).createSpec();
    	
    	DataTableSpec port2 = new DataTableSpec(cols2);
    	
    	return new DataTableSpec[] { port1, port2 };
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_hits_db1.saveSettingsTo(settings);
    	m_hits_db2.saveSettingsTo(settings);
    	m_algo.saveSettingsTo(settings);
    	m_epsilon.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_hits_db1.loadSettingsFrom(settings);
    	m_hits_db2.loadSettingsFrom(settings);
    	m_epsilon.loadSettingsFrom(settings);

    	if (settings.containsKey(CFGKEY_ALGORITHM))
    		m_algo.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_hits_db1.validateSettings(settings);
    	m_hits_db2.validateSettings(settings);
    	m_epsilon.validateSettings(settings);

    	if (settings.containsKey(CFGKEY_ALGORITHM))
    		m_algo.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
        // TODO load internal data. 
        // Everything handed to output ports is loaded automatically (data
        // returned by the execute method, models loaded in loadModelContent,
        // and user settings set through loadSettingsFrom - is all taken care 
        // of). Load here only the other internals that need to be restored
        // (e.g. data used by the views).

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
        // TODO save internal models. 
        // Everything written to output ports is saved automatically (data
        // returned by the execute method, models saved in the saveModelContent,
        // and user settings saved through saveSettingsTo - is all taken care 
        // of). Save here only the other internals that need to be preserved
        // (e.g. data used by the views).

    }

}

