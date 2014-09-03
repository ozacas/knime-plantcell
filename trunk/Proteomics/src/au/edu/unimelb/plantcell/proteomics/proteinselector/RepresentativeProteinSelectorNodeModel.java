package au.edu.unimelb.plantcell.proteomics.proteinselector;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.*;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.base.data.append.column.AppendedColumnRow;


/**
 * This is the model implementation of RepresentativeProteinSelector.
 * Selects, amongst proteins which share peptides, a representative sequence. In the future, this will provide multiple strategies for doing this: but only one for now. Designed to match the results from the ACPFG String Matcher
 *
 * @author Andrew Cassin
 */
public class RepresentativeProteinSelectorNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger("Heuristic Protein Selector");
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_PEPTIDE_MATCHES = "peptide-matches";
    static final String CFGKEY_STRATEGY        = "strategy";
    static final String CFGKEY_ACCSN           = "accsn";
	
    /** initial defaults */
    private static final String DEFAULT_PEPTIDE_MATCHES = "Matched Region";
    private static final String DEFAULT_ACCSN = "Accession"; // name chosen to match ACPFG String Matcher
    private static final String[] strategies = { "Elect sequence with most peptides incl. common peptides"} ;
    
    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
    private final SettingsModelColumnName m_peptide_matches = (SettingsModelColumnName) make(CFGKEY_PEPTIDE_MATCHES);
    private final SettingsModelStringArray m_strategy       = (SettingsModelStringArray) make(CFGKEY_STRATEGY);
    private final SettingsModelColumnName m_accsn           = (SettingsModelColumnName) make(CFGKEY_ACCSN);

    /**
     * Constructor for the node model.
     */
    protected RepresentativeProteinSelectorNodeModel() {
        super(1, 1);
    }

    public static SettingsModel make(String field_name) {
    	if (field_name.equals(CFGKEY_PEPTIDE_MATCHES)) {
    			return new SettingsModelColumnName(CFGKEY_PEPTIDE_MATCHES, DEFAULT_PEPTIDE_MATCHES);
    	} else if (field_name.equals(CFGKEY_STRATEGY)) {
    			return new SettingsModelStringArray(CFGKEY_STRATEGY, strategies);
    	} else if (field_name.equals(CFGKEY_ACCSN)) {
    			return new SettingsModelColumnName(CFGKEY_ACCSN, DEFAULT_ACCSN);
    	}
    	return null;
    }
    
    protected String cleanup_accsn(DataCell c) {
    	return c.toString().trim();
    }
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        // TODO do something here
        logger.info("Representative Protein Selector... beginning execution");

        
        // the data table spec of the single output table, 
        // the table will have three columns:
        DataColumnSpec[] allColSpecs = new DataColumnSpec[1];
        allColSpecs[0] = 
            new DataColumnSpecCreator("Representative?", StringCell.TYPE).createSpec();
        DataTableSpec outSpec = new DataTableSpec(allColSpecs);
        DataTableSpec newSpec = new DataTableSpec(inData[0].getDataTableSpec(), outSpec);
        // the execution context will provide us with storage capacity, in this
        // case a data container to which we will add rows sequentially
        // Note, this container can also handle arbitrary big data tables, it
        // will buffer to disc if necessary.
        BufferedDataContainer container = exec.createDataContainer(newSpec);
        int                   match_col = inData[0].getDataTableSpec().findColumnIndex(m_peptide_matches.getStringValue());
        int                   accsn_col = inData[0].getDataTableSpec().findColumnIndex(m_accsn.getStringValue());
        RowIterator it = inData[0].iterator();
        
        // pass 1: build up the necessary data structures
        Hashtable ht = new Hashtable();
        Hashtable accsn_cnt = new Hashtable();
        Hashtable peptides_by_accsn = new Hashtable();
        
        while (it.hasNext()) {
        	DataRow       row = it.next();
        	String[] peptides = row.getCell(match_col).toString().split(", ");
        	String   accsn    = cleanup_accsn(row.getCell(accsn_col));
        	
        	Set unique_peptides = new HashSet();
         	for (String peptide : peptides) {
         		unique_peptides.add(peptide);
        		if (!ht.containsKey(peptide)) {
        			List<String> l = new ArrayList<String>();
        			l.add(accsn);
        			ht.put(peptide,l);
        		} else {
        			List<String> l = (List<String>) ht.get(peptide);
        			l.add(accsn);
        		}
        	}
         	accsn_cnt.put(accsn, new Integer(unique_peptides.size()));
         	peptides_by_accsn.put(accsn, unique_peptides);
         	unique_peptides = null;
        }
        it = null;
        
        // pass 2: compute the representative sequences
        it = inData[0].iterator();
        int nrows = inData[0].getRowCount();
        int row_n = 0;
        Set<String> tied_winner_accsns = new HashSet<String>();
        Hashtable<String,String> results = new Hashtable<String, String>();
        
        while (it.hasNext()) {
        	DataRow row = it.next();
        	String[] peptides = row.getCell(match_col).toString().trim().split(", ");
        	String   accsn    = cleanup_accsn(row.getCell(accsn_col));
        	
        	//System.err.println(peptides[0]);
        	if (peptides.length == 1 && ((List<String>)ht.get(peptides[0])).size() == 1) {
        		List<String> l = (List<String>)ht.get(peptides[0]);
        		if (accsn.equals(l.get(0))) {
        			results.put(accsn, "yes");
        		}
        	} else {
        		// we need to find an accession which contains more peptides that include all of peptides[] and some others
        		// in the event of a tie we call both representative...
        		//System.err.println(peptides[0]+ " "+ peptides.length);
        		int need_to_beat = ((Integer)accsn_cnt.get(accsn)).intValue();
        		// go thru the list of all other accessions and see if we can find something better...
        		boolean found_better = false;
        		boolean tied = false;
        		Set<String> accsns = new HashSet<String>();
        		for (int i=0; i<peptides.length; i++) {
        			accsns.addAll((List<String>)ht.get(peptides[i]));
        		}
        		Set<String> tied_accsns = new HashSet<String>();
        		for (String a : accsns) {
        			int cnt = ((Integer)accsn_cnt.get(a)).intValue();
        			// only consider those accessions with at least as many peptides as the current row in the dataset
        			if (cnt > need_to_beat) {
        				if (has_all_peptides(peptides, (Set<String>) peptides_by_accsn.get(a))) {
        					// current sequence is not representative
        					found_better = true;
        					tied = false;
        					break;
        				}
        				// else try next accession...
        			} else if (cnt == need_to_beat && !a.equals(accsn)) {
        				// in this case we might have a tie... if we do then report the current row as representative anyway...
        				if (has_all_peptides(peptides, (Set<String>) peptides_by_accsn.get(a))) {
        					tied = true;
        					tied_accsns.add(a);
        					tied_accsns.add(accsn);
        				}
        				// keep trying to find something better...
        			}
        		}
        		
        		if (tied) {
        			if (tied_accsns.contains(accsn) && !already_reported(tied_accsns, tied_winner_accsns)) {
        				results.put(accsn, "tie winner");
        				tied_winner_accsns.add(accsn);
        			} 
        		} else if (!found_better) {
        			results.put(accsn, "yes");
        		}
        	}
        
        	if (row_n % 100 == 0) {
        		// check if the execution monitor was canceled
                exec.checkCanceled();
                // and update node progress "traffic light"
                exec.setProgress(((double) row_n) / nrows, "Processed " + row_n);
        	}
        	row_n++;
        }
        
        // 3. install results
        it = inData[0].iterator(); 
        while (it.hasNext()) {
        	DataRow row = it.next();
        	String   accsn    = cleanup_accsn(row.getCell(accsn_col));
        	String result = "";
        	
        	if (results.containsKey(accsn)) {
        		result = results.get(accsn);
        	}
        	AppendedColumnRow ar = new AppendedColumnRow(row, new StringCell(result));
        	container.addRowToTable(ar);
        }
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }

    /**
     * Returns true if all the peptides listed in p1 are contained within p2, false otherwise
     * @param p1 - first set of peptides
     * @param p2 - second set of peptides
     * @return true if all peptides in p1 are found in p2, false otherwise
     */
    protected boolean has_all_peptides(String[] p1, Set<String> p2) {
    	int matches = 0;
    	for (String peptide: p1) {
    		if (p2.contains(peptide)) {
    			matches++;
    		}
    	}
    	return (matches == p1.length);
    }
    
    /**
     *  If any is s1 are already present in in_here then true is returned, false otherwise
     * @param s1
     * @param in_here
     * @return
     */
    protected boolean already_reported(Set<String> s1, Set<String> in_here) {
    	for (String accsn : s1) {
    		if (in_here.contains(accsn)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Code executed on reset.
        // Models build during execute are cleared here.
        // Also data handled in load/saveInternals will be erased here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        // TODO: check if user settings are available, fit to the incoming
        // table structure, and the incoming types are feasible for the node
        // to execute. If the node can execute in its current state return
        // the spec of its output data table(s) (if you can, otherwise an array
        // with null elements), or throw an exception with a useful user message

        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

       m_peptide_matches.saveSettingsTo(settings);
       m_accsn.saveSettingsTo(settings);
       m_strategy.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
      m_peptide_matches.loadSettingsFrom(settings);
      m_accsn.loadSettingsFrom(settings);
      m_strategy.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        m_peptide_matches.validateSettings(settings);
        m_accsn.validateSettings(settings);
        m_strategy.validateSettings(settings);
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

