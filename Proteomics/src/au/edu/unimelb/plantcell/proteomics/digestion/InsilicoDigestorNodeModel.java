package au.edu.unimelb.plantcell.proteomics.digestion;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.expasy.jpl.core.mol.chem.MassCalculator;
import org.expasy.jpl.core.mol.modif.Modification;
import org.expasy.jpl.core.mol.polymer.modif.unimod.UnimodManager;
import org.expasy.jpl.core.mol.polymer.modif.unimod.UnimodSpecificity;
import org.expasy.jpl.core.mol.polymer.pept.Peptide;
import org.expasy.jpl.core.mol.polymer.pept.cutter.DigestedPeptide;
import org.expasy.jpl.core.mol.polymer.pept.cutter.Digester;
import org.expasy.jpl.core.mol.polymer.pept.cutter.Peptidase;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;
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
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * This is the model implementation of InsilicoDigestor.
 * Often using javaprotlib, a few useful nodes for Proteomics Mass Spectrometry
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class InsilicoDigestorNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger("In silico digest");
        
  
    public static final String CFGKEY_ENZYME   = "protease";
    public static final String CFGKEY_MASSMONO = "monoisotopic-mass?";
    public static final String CFGKEY_MWFILTER = "molecular-weight-filter?";
    public static final String CFGKEY_MINMW    = "minimum-molecular-weight";
    public static final String CFGKEY_MAXMW    = "maximum-molecular-weight";
    public static final String CFGKEY_SEQUENCE = "sequence-column";
    public static final String CFGKEY_VMODS    = "variable-modifications";
    

    private SettingsModelString m_enzyme = new SettingsModelString(CFGKEY_ENZYME, "Trypsin");
    private SettingsModelString m_sequence = new SettingsModelString(CFGKEY_SEQUENCE, "Sequence");
    private SettingsModelBoolean m_monomass= new SettingsModelBoolean(CFGKEY_MASSMONO, true);
    private SettingsModelDouble  m_min     = new SettingsModelDouble(CFGKEY_MINMW, 122.0);
    private SettingsModelDouble  m_max     = new SettingsModelDouble(CFGKEY_MAXMW, 2000.0);
    private SettingsModelStringArray m_vmods=new SettingsModelStringArray(CFGKEY_VMODS, new String[] {});
    
    private int m_seq_idx = -1;	// not persisted
    private static List<String> m_unimods = null;
    
    /**
     * Constructor for the node model.
     */
    protected InsilicoDigestorNodeModel() {
        super(1, 1);
    }

    /**
     * Returns an alphabetically sorted list of all available enzymes from javaprotlib.
     * Used by the dialog to offer a list of enzymes to the user to configure.
     * 
     * @return
     */
    public static String[] getAvailableProteases() {
    	Set<Peptidase> all = Peptidase.getAllPooledPeptidases();
    	ArrayList<String> ret = new ArrayList<String>();
    	for (Peptidase p : all) {
    		ret.add(p.getName());
    	}
    	Collections.sort(ret);
    	return ret.toArray(new String[0]);
    }
    
    /**
     * Returns a list of unimod modifications loaded from javaprotlib internals
     * @return
     */
    public static synchronized List<String> getAvailableModifications() {
    	if (m_unimods != null) {
    		return m_unimods;
    	}
    	UnimodManager  um = UnimodManager.getInstance();
    	List<String> tmp = new ArrayList<String>();
    	tmp.addAll(um.getModificationNames());
    	Collections.sort(tmp);
    	m_unimods = tmp;
    	return m_unimods;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        DataTableSpec outputSpec = make_output_spec(inData[0].getDataTableSpec());
        if (m_seq_idx < 0) {
        	throw new InvalidSettingsException("Cannot find column: "+m_sequence.getStringValue()+"in input, re-configure?");
        }
        Peptidase enzyme = Peptidase.getInstance(m_enzyme.getStringValue());
        Digester digester= Digester.newInstance(enzyme);
        double min = m_min.getDoubleValue();
        double max = m_max.getDoubleValue();
        if (max < min) {
        	double tmp = min;
        	min = max;
        	max = tmp;
        }
        logger.info("Reporting peptides with masses beween "+min+" and "+max+ " digested with "+m_enzyme.getStringValue());
        int n_residues = 0;
        UnimodManager unimod = UnimodManager.getInstance();
        for (String mod : m_vmods.getStringArrayValue()) {
        	Modification m = unimod.getModif(mod);
        	if (m != null) {
        		Set<UnimodSpecificity> specs = unimod.getSpecificitiesFromModif(m);
        		for (UnimodSpecificity us : specs) {
        			@SuppressWarnings("rawtypes")
					Iterator it = us.getMatcher().iterator();
        			while (it.hasNext()) {
        				logger.info(it.next());
        			}
        		}
        	}
        }
        if (n_residues > 0) {
        	logger.info("Considering "+m_vmods.getStringArrayValue().length +" modifications possible on "+n_residues+" different amino acids.");
        }
        
        // the execution context will provide us with storage capacity, in this
        // case a data container to which we will add rows sequentially
        // Note, this container can also handle arbitrary big data tables, it
        // will buffer to disc if necessary.
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        RowIterator it = inData[0].iterator();
        int done = 0;
        int id = 1;
        MassCalculator mc = MassCalculator.getMonoAccuracyInstance();
        if (!m_monomass.getBooleanValue())
        	mc = MassCalculator.getAvgAccuracyInstance();
        
        while (it.hasNext()) {
        	DataRow  r = it.next();
        	DataCell c = r.getCell(m_seq_idx);
        	if (c == null || c.isMissing()) {
        		continue;
        	}
        	SequenceValue sv = (SequenceValue) c;
        	if (!sv.getSequenceType().isProtein()) {
        		logger.warn("Skipping non-protein sequence: "+sv.getID());
        		continue;
        	}
        	Peptide p = new Peptide.Builder(sv.getStringValue()).ambiguityEnabled().build();
        	digester.digest(p);
        	Set<DigestedPeptide> peptides = digester.getDigests();
        	for (DigestedPeptide dp : peptides) {
        		
        		double mass = mc.getMass(dp.getPeptide());
        		if (mass < min || mass > max)
        			continue;
        		DataCell[] cells = new DataCell[4];
        		String aa = dp.getPeptide().toAAString();
        		cells[0] = new StringCell(highlight_peptide(sv.getStringValue(), aa));
        		cells[1] = new StringCell(aa);
        		cells[2] = new DoubleCell(mass);
        		cells[3] = new StringCell(r.getKey().getString());
        		container.addRowToTable(new DefaultRow("Pept"+id++, cells));
        	}
        	
        	done++;
        	if (done % 100 == 0) {
        		exec.setProgress(((double)done) / inData[0].getRowCount());
        		exec.checkCanceled();
        	}
        }
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }

    private String highlight_peptide(String seq, String aa) {
    	StringBuilder sb = new StringBuilder(seq.length() + 1000);
    	String trimmed_seq = seq.trim().replaceAll("\\s+", "");
    	DenseBitVector bv = new DenseBitVector(trimmed_seq.length());
    	int start = 0;
    	while ((start = trimmed_seq.indexOf(aa, start)) >= 0) {
    		for (int j=start; j<start+aa.length(); j++) {
    			bv.set(j);
    		}
    		start++;
    	}
    	
    	sb.append("<html>");
    	int cnt = 0;
    	for (int i=0; i<bv.length(); i++) {
    		if (bv.get(i)) {
    			sb.append("<font color=\"red\"><b>");
    			sb.append(trimmed_seq.charAt(i));
    			sb.append("</b></font>");
    		} else {
    			sb.append(trimmed_seq.charAt(i));
    		}
    		cnt++;
    		if (cnt == 80) {
    			sb.append("<br/>");
    			cnt = 0;
    		}
    	}
    	return sb.toString();
	}

	private DataTableSpec make_output_spec(DataTableSpec dataTableSpec) {
    	DataColumnSpec[] cols = new DataColumnSpec[4];
    	String mass_label = "Peptide Mass (monoisotopic)";
    	if (!m_monomass.getBooleanValue()) {
    		mass_label = "Peptide Mass (average)";
    	}
    	cols[0] = new DataColumnSpecCreator("Highlighted protein sequence (HTML)", StringCell.TYPE).createSpec();
    	cols[1] = new DataColumnSpecCreator("Peptide", StringCell.TYPE).createSpec();
    	cols[2] = new DataColumnSpecCreator(mass_label, DoubleCell.TYPE).createSpec();
    	cols[3] = new DataColumnSpecCreator("Input RowID", StringCell.TYPE).createSpec();

		return new DataTableSpec(cols);
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
        m_seq_idx = inSpecs[0].findColumnIndex(m_sequence.getStringValue());
        if (m_seq_idx < 0) {
        	logger.warn("Unable to find sequence column: "+m_sequence.getStringValue());
        }
        return new DataTableSpec[]{make_output_spec(inSpecs[0])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_monomass.saveSettingsTo(settings);
    	m_enzyme.saveSettingsTo(settings);
    	m_sequence.saveSettingsTo(settings);
    	m_min.saveSettingsTo(settings);
    	m_max.saveSettingsTo(settings);
    	m_vmods.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_monomass.loadSettingsFrom(settings);
    	m_enzyme.loadSettingsFrom(settings);
    	m_sequence.loadSettingsFrom(settings);
    	m_min.loadSettingsFrom(settings);
    	m_max.loadSettingsFrom(settings);
    	m_vmods.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_monomass.validateSettings(settings);
    	m_enzyme.validateSettings(settings);
    	m_sequence.validateSettings(settings);
    	m_min.validateSettings(settings);
    	m_max.validateSettings(settings);
    	m_vmods.validateSettings(settings);
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

