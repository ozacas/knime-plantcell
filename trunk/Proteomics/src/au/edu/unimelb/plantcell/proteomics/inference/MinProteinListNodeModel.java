package au.edu.unimelb.plantcell.proteomics.inference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.StringCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.CorePlugin;
import au.edu.unimelb.plantcell.core.ExternalProgram;
import au.edu.unimelb.plantcell.core.PreferenceConstants;


/**
 * This is the model implementation of MinProteinList.
 * Uses a greedy set cover algorithm to identify the minimal set of proteins which can explain the observed peptides
 *
 * @author Andrew Cassin
 */
public class MinProteinListNodeModel extends NodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger("Minimal protein list");
    
	static final String CFGKEY_PEPTIDES = "peptides";
	static final String CFGKEY_PROTEIN  = "protein";
	static final String CFGKEY_ALGO     = "algorithm";
	
	private final SettingsModelString m_peptide_column = new SettingsModelString(CFGKEY_PEPTIDES, "Peptides");
	private final SettingsModelString m_accsn_column   = new SettingsModelString(CFGKEY_PROTEIN, "Protein");
	private final SettingsModelString m_algorithm      = new SettingsModelString(CFGKEY_ALGO, "ILP: Minimum Set Cover");
	
    /**
     * Constructor for the node model.
     */
    protected MinProteinListNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	logger.info("Minimising protein set for "+inData[0].getRowCount()+" rows in input dataset.");
    	
    	int pep_idx  = inData[0].getDataTableSpec().findColumnIndex(m_peptide_column.getStringValue());
    	int accsn_idx= inData[0].getDataTableSpec().findColumnIndex(m_accsn_column.getStringValue());
    	if (pep_idx < 0 || accsn_idx < 0 || pep_idx == accsn_idx) {
    		throw new Exception("Illegal columns: "+m_peptide_column+" "+m_accsn_column+", re-configure the node!");
    	}
    	DataTableSpec newSpec = new DataTableSpec(inData[0].getDataTableSpec(), make_output_spec());
    	BufferedDataContainer container = exec.createDataContainer(newSpec);
    	
    	RowIterator it = inData[0].iterator();
    	
    	HashMap<String,String> prot2pep = new HashMap<String,String>();
    	HashMap<String,String> pep2lp   = new HashMap<String,String>();
    	HashMap<String,String> prot2lp  = new HashMap<String,String>();
    	HashMap<String,Set<String>> pep2protkeys = new HashMap<String,Set<String>>();
    	
    	int peptide_idx = 1;
    	int prot_idx = 1;
    	
    	logger.info("Processing raw input rows");
    	
    	while (it.hasNext()) {
    		DataRow r = it.next();
    		DataCell pep_cell = r.getCell(pep_idx);
    		DataCell accsn_cell= r.getCell(accsn_idx);
    		
    		// rows with missing cells cannot be processed (no missing values in ILP...)
    		if (pep_cell.isMissing() || accsn_cell.isMissing()) {
    			continue;
    		}
    		
    		String[] peptides;
    		String protein_accsn   = ((StringValue)accsn_cell).getStringValue();
    		String peptides_as_csv;
    		
    		if (!pep_cell.getType().isCollectionType()) {
    			peptides_as_csv = ((StringCell)pep_cell).getStringValue();
	    		
	    		if (peptides_as_csv.trim().length() < 1 || protein_accsn.trim().length() < 1) {
	    			throw new Exception("Must supply valid Protein ID (accession) and peptide list - no blank cells!");
	    		}
	    		peptides = peptides_as_csv.split(",\\s+");
    		} else {
    			// pep_cell is a collection eg. list or set cell
    			Iterator<DataCell> it2 = ((CollectionDataValue) pep_cell).iterator();
    			ArrayList<String> peps = new ArrayList<String>();
    			StringBuffer sb = new StringBuffer();
    			while (it2.hasNext()) {
    				DataCell c2 = it2.next();
    				if (c2 != null && !c2.isMissing() && c2.getType().isCompatible(StringValue.class)) {
    					peps.add(c2.toString());
    					if (peps.size() > 0) {
    						sb.append(", ");
    					}
    					sb.append(c2.toString());
    				}
    			}
    			peptides = peps.toArray(new String[0]);
    			peptides_as_csv = sb.toString();
    		}
    		
    		prot2pep.put(protein_accsn, peptides_as_csv);
    		for (String pep : peptides) {
    			if (!pep2lp.containsKey(pep)) {
    				String key = "_p"+peptide_idx+"_";
    				peptide_idx++;
    				pep2lp.put(pep, key );
    			}
    		}
    		
    		if (prot2lp.containsKey(protein_accsn)) {
    			throw new Exception("Error at row "+r.getKey().getString()+": already seen peptides for protein ID "+protein_accsn);
    		}
    		String key = "_x"+prot_idx+"_";
    		prot_idx++;
    		prot2lp.put(protein_accsn, key);
    		
    		for (String pep : peptides) {
    			if (pep2protkeys.containsKey(pep)) {
    				Set<String> s = pep2protkeys.get(pep);
    				s.add(key);
    				pep2protkeys.put(pep, s);
    			} else {
    				Set<String> s = new HashSet<String>();
    				s.add(key);
    				pep2protkeys.put(pep, s);
    			}
    		}
    	}
    	
    	logger.info("Computing non-equal costs");
    	
    	// non-equal costs?
    	HashMap<String,Double> costs = new HashMap<String,Double>();		// protein key -> weight (lower is better)
    	if (m_algorithm.getStringValue().toLowerCase().contains("unique")) {
    		for (String accsn : prot2lp.keySet()) {
    			String    pep_csv = prot2pep.get(accsn);
    			String[] peptides = pep_csv.split(",\\s+");
    			int unique_cnt = 0;
    			for (String pep : peptides) {
    				Set<String> s = pep2protkeys.get(pep);
    				if (s.size() == 1)
    					unique_cnt++;
    			}
    			
    			// NB: cost = 1 if there are no unique peptides
    			if (unique_cnt > 0) {
    				costs.put(prot2lp.get(accsn), new Double(1.0/(unique_cnt+1)));
    			}
    		}
    	}
    	
    	// create a cplex-style file with the necessary ILP formulation
    	logger.info("Computing ILP formulation for dataset");
    	File  tmp_file = File.createTempFile("minprotset", ".lp");
    	PrintWriter pw = new PrintWriter(new FileWriter(tmp_file));
    	pw.println("minimize");
    	pw.print(" cost: ");
    	Collection<String> c = prot2lp.values();
    	Iterator<String> it2 = c.iterator();
    	for (int i=0; i<c.size(); i++) {
    		String prot_key = it2.next();
    		Double cost = costs.get(prot_key);
    		if (cost != null) {
    			pw.print(cost);
    			pw.print(" ");
    		}
    		pw.print(prot_key);
    		if (i<c.size()-1) {
    			pw.print(" + ");
    		}
    	}
    	pw.println();
    	pw.println("subject to");
    	for (String peptide : pep2lp.keySet()) {
    		pw.print(pep2lp.get(peptide)+": ");
    		Set<String> prots = pep2protkeys.get(peptide);
    		if (prots == null || prots.size() < 1)
    			throw new Exception("No proteins for peptide "+peptide);
    		Iterator<String> it3 = prots.iterator();
    		for (int i=0; i<prots.size(); i++) {
    			pw.print(it3.next());
    			pw.print(" ");
    			if (i<prots.size()-1)
    				pw.print("+ ");
    		}
    		pw.println(">= 1");
    	}
    	pw.println("binary");
    	for (String prot_id : prot2lp.values()) {
    		pw.println(" "+prot_id);
    	}
    	pw.println("end");
    	pw.close();
    	
    	logger.info("Created LP solver file: "+tmp_file.getAbsolutePath());
    	
    	// run cplex to compute a solution... (hopefully!)
    	IPreferenceStore prefs = CorePlugin.getDefault().getPreferenceStore();
		String install_dir = prefs.getString(PreferenceConstants.PREFS_GLPK_FOLDER);
		logger.info("Got "+install_dir+" as GNU GLPK software location");
    	CommandLine cmdLine = new CommandLine(find_solver(install_dir));
    	final Map<String, File> map = new HashMap<String,File>();
    	File solution_file = File.createTempFile("minprotsol", ".txt");
    	solution_file.deleteOnExit();
    	map.put("input", tmp_file);
    	map.put("output", solution_file);
    	cmdLine.addArgument("--min");
    	cmdLine.addArgument("--lp");
    	cmdLine.addArgument("${input}");
    	cmdLine.addArgument("-o");
    	cmdLine.addArgument("${output}");
    	cmdLine.setSubstitutionMap(map);
    	final HashMap<String,Integer> results_pep2cnt = new HashMap<String,Integer>();
    	final HashMap<String,Integer> results_prot2cnt= new HashMap<String,Integer>();
    	
    	DefaultExecutor exe = new DefaultExecutor();
    	//exe.setExitValue(1);
    	exe.setWorkingDirectory(tmp_file.getParentFile());
    	DefaultExecuteResultHandler erh = new DefaultExecuteResultHandler() {

			@Override
			public void onProcessComplete(int arg0) {
				// process the results from the ILP solver (GLPK specific and probably version specific)
		    	logger.info("Solver ran ok (status "+arg0+"): processing solution...");
		    	File solution_file = map.get("output");
		    	logger.info("Processing solution file: "+solution_file.getAbsolutePath());
		    	try {
			    	BufferedReader soln_rdr  = new BufferedReader(new FileReader(solution_file));
			    	Pattern pep_line_pattern = Pattern.compile("^\\s*\\d+\\s*(_p\\d+_)\\s+(\\d+)\\s+\\d+");
			    	Pattern prot_line_pattern= Pattern.compile("^\\s*\\d+\\s*(_x\\d+_)\\s+\\S+\\s+(\\d+)\\s+");
			    	String line;
			    	while ((line = soln_rdr.readLine()) != null) {
			    		Matcher pep_matcher = pep_line_pattern.matcher(line);
			    		Matcher prot_matcher= prot_line_pattern.matcher(line);
			    		if (pep_matcher.find()) {
			    			String pep_key       = pep_matcher.group(1);
			    			String pep_usage_cnt = pep_matcher.group(2);
			    			results_pep2cnt.put(pep_key, new Integer(pep_usage_cnt));
			    		} else if (prot_matcher.find()) {
			    			String prot_key       = prot_matcher.group(1);
			    			String prot_usage_cnt = prot_matcher.group(2);
			    			results_prot2cnt.put(prot_key, new Integer(prot_usage_cnt));
			    		}
			    	}
			    	soln_rdr.close();
			    	logger.info("Processed results from solver for "+results_pep2cnt.size()+" peptides, "+results_prot2cnt.size()+" proteins.");
		    	} catch (Exception e) {
		    		logger.warn("Unable to process results from GLPSOL - no results available!");
		    		logger.warn(e.getMessage());
		    		e.printStackTrace();
		    		results_prot2cnt.clear();
		    		results_pep2cnt.clear();
		    	}
		    	
		    	// and let the superclass notify the any threads waiting that we are done...
				super.onProcessComplete(arg0);
			}

			@Override
			public void onProcessFailed(ExecuteException arg0) {
				logger.warn("Unable to run GLPSOL - is it installed correctly?");
				logger.warn(arg0.getMessage());
				arg0.printStackTrace();
				
				super.onProcessFailed(arg0);
			}
    		
    	};
    	
    	exe.execute(cmdLine, erh);
    	erh.waitFor();
    	
    	// 3. output TRUE for those rows which are part of the minimum set, FALSE otherwise
    	if (results_prot2cnt.size() > 0 && results_pep2cnt.size() > 0) {
	    	it = inData[0].iterator();
	    	while (it.hasNext()) {
	    		DataRow      r = it.next();
	    		String   accsn = ((StringCell)r.getCell(accsn_idx)).getStringValue();
	    		boolean is_min = (results_prot2cnt.get(prot2lp.get(accsn)).intValue() == 1);
	    		AppendedColumnRow new_r = new AppendedColumnRow(r, BooleanCell.get(is_min));
	    		container.addRowToTable(new_r);
	    	}
    	}
  
    	container.close();
    	return new BufferedDataTable[] { container.getTable() };
    }
	
    /**
     * Returns the File which corresponds to the GLPK solver executable within the distribution. Only
     * checks the folder specified by the <code>glpk_root</code> parameters for the software to use.
     * 
     * @param glpk_root
     * @return
     * @throws InvalidSettingsException
     */
	private File find_solver(String glpk_root) throws InvalidSettingsException {
		if (!new File(glpk_root).isDirectory()) {
			logger.warn("Cannot locate GNU GLPK software!");
		}
		File prog = ExternalProgram.find(glpk_root, "glpsol");
    	if (prog != null) 
    		return prog;
    	throw new InvalidSettingsException("Unable to locate glpsol.exe -- missing?");
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	// NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

     
        return new DataTableSpec[]{new DataTableSpec(inSpecs[0], make_output_spec())};
    }

    private DataTableSpec make_output_spec() {
    	DataColumnSpec cols[] = new DataColumnSpec[1];
    	
    	cols[0] = new DataColumnSpecCreator("Is in minimum set?", BooleanCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_peptide_column.saveSettingsTo(settings);
         m_accsn_column.saveSettingsTo(settings);
         m_algorithm.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_peptide_column.loadSettingsFrom(settings);
        m_accsn_column.loadSettingsFrom(settings);
        m_algorithm.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	  m_peptide_column.validateSettings(settings);
          m_accsn_column.validateSettings(settings);
          m_algorithm.validateSettings(settings);
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

