package au.edu.unimelb.plantcell.proteomics.fragmentation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.expasy.jpl.commons.base.builder.BuilderException;
import org.expasy.jpl.commons.base.cond.Condition;
import org.expasy.jpl.core.mol.polymer.pept.Peptide;
import org.expasy.jpl.core.mol.polymer.pept.PeptideTypeCondition;
import org.expasy.jpl.core.mol.polymer.pept.cutter.DigestedPeptide;
import org.expasy.jpl.core.mol.polymer.pept.cutter.Digester;
import org.expasy.jpl.core.mol.polymer.pept.cutter.Peptidase;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.FragmentationType;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.PeptideFragmentationException;
import org.expasy.jpl.core.mol.polymer.pept.fragmenter.PeptideFragmenter;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.core.ms.spectrum.peak.AnnotatedPeak;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.ProteinSequenceRowIterator;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * This is the model implementation the PeptideFragmenter
 * Often using javaprotlib, a few useful nodes for Proteomics Mass Spectrometry
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class PeptideFragmentationNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Peptide Fragmenter");
        
    public static final String CFGKEY_ENZYME   = "protease";
    public static final String CFGKEY_MWFILTER = "molecular-weight-filter?";
    public static final String CFGKEY_MINMW    = "minimum-molecular-weight";
    public static final String CFGKEY_MAXMW    = "maximum-molecular-weight";
    public static final String CFGKEY_SEQUENCE = "sequence-column";
    public static final String CFGKEY_CHARGES  = "charge-states";
    public static final String CFGKEY_PEAK_TYPES = "peak-types";
    

    private SettingsModelString  m_enzyme   = new SettingsModelString(CFGKEY_ENZYME, "Trypsin");
    private SettingsModelString  m_sequence = new SettingsModelString(CFGKEY_SEQUENCE, "Sequence");
    private SettingsModelDouble  m_min     = new SettingsModelDouble(CFGKEY_MINMW, 122.0);
    private SettingsModelDouble  m_max     = new SettingsModelDouble(CFGKEY_MAXMW, 2000.0);
    private SettingsModelStringArray m_charges = new SettingsModelStringArray(CFGKEY_CHARGES, new String[] { "1" });
    private SettingsModelStringArray m_peak_types = new SettingsModelStringArray(CFGKEY_PEAK_TYPES, new String[] { "b", "y", "precursor"
    });

    // internal state used during execute() -- not persisted
    private Digester m_digester;
    private Peptidase m_peptidase;
    private PeptideFragmenter m_fragmenter;
    private Condition<AnnotatedPeak> m_fragmentCondition;
    
    
    /**
     * Constructor for the node model.
     */
    protected PeptideFragmentationNodeModel() {
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
     * {@inheritDoc}
     */
    @SuppressWarnings("unused")
	@Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	int seq_idx = inData[0].getSpec().findColumnIndex(m_sequence.getStringValue());
    	if (seq_idx < 0) {
    		throw new InvalidSettingsException("Cannot find sequence column: "+m_sequence.getStringValue());
    	}
    	BufferedDataContainer container = exec.createDataContainer(make_output_spec(inData[0].getSpec()));
    	
    	// establish key conditions for the fragmentation as set by the user during configure
    	m_peptidase = Peptidase.getInstance(m_enzyme.getStringValue());
    	m_digester  = Digester.newInstance(m_peptidase);
    	
    	Set<FragmentationType> fragmentationTypes = new HashSet<FragmentationType>();
    	String fragments = make_fragment_string(fragmentationTypes, m_peak_types.getStringArrayValue());
    	m_fragmentCondition = new PeptideTypeCondition.Builder<AnnotatedPeak>(fragments).accessor(AnnotatedPeak.TO_PEAK_TYPE).build();
    	
    	m_fragmenter        = new PeptideFragmenter.Builder(fragmentationTypes).build();
    	
    	// iterate over the input rows...
    	final SequenceProcessor sp = new SequenceProcessor() {
 			private boolean warned = false;
 			
 			@Override
 			public SequenceValue process(SequenceValue sv) {
 				if (sv.getStringValue().toLowerCase().indexOf('x') >= 0) {
 					if (!warned) {
 						logger.warn("Cannot fragment peptide with sequence with ambiguous (X) residues!");
 						warned = true;
 					}
 					return null;	// skip over bad sequences after the warning
 				}
 				return sv;
 			}
 	
        };
        
        final ProteinSequenceRowIterator psi = new ProteinSequenceRowIterator(inData[0].iterator(), 
         		seq_idx, logger, sp);
        int done = 0;
        logger.info("Computing theoretical spectra of "+inData[0].getRowCount()+" rows with peptides.");
    	while (psi.hasNext()) {
    		SequenceValue peptide = psi.nextSequence();
    		
    	}
    	
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }

	private String make_fragment_string(Set<FragmentationType> frag_types, String[] vec) {
		StringBuilder sb = new StringBuilder(128);
		for (int i=0; i<vec.length; i++) {
			sb.append(vec[i]);
			String ion_type = vec[i].toLowerCase();
			if (ion_type.equals("a") || ion_type.equals("x")) {
					frag_types.add(FragmentationType.AX);
			} else if (ion_type.equals("b") || ion_type.equals("y")) {
					frag_types.add(FragmentationType.BY);
			} else if (ion_type.equals("c") || ion_type.equals("z")) {
					frag_types.add(FragmentationType.CZ);
			} else if (ion_type.equals("i")) {
					frag_types.add(FragmentationType.IMMONIUM);
			} else if (ion_type.equals("p")) {
					frag_types.add(FragmentationType.PRECURSOR);
			} else {
					logger.warn("not supported type " + vec[i]);
			}
			if (i<vec.length-1) {
				sb.append(',');
			}
		}
		return sb.toString();
	}

	private DataTableSpec make_output_spec(DataTableSpec dataTableSpec) {
    	DataColumnSpec[] cols = new DataColumnSpec[4];
    	String mass_label = "Peptide Mass (monoisotopic)";
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
        
        return new DataTableSpec[]{make_output_spec(inSpecs[0])};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_enzyme.saveSettingsTo(settings);
    	m_sequence.saveSettingsTo(settings);
    	m_min.saveSettingsTo(settings);
    	m_max.saveSettingsTo(settings);
    	m_charges.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_enzyme.loadSettingsFrom(settings);
    	m_sequence.loadSettingsFrom(settings);
    	m_min.loadSettingsFrom(settings);
    	m_max.loadSettingsFrom(settings);
    	m_charges.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_enzyme.validateSettings(settings);
    	m_sequence.validateSettings(settings);
    	m_min.validateSettings(settings);
    	m_max.validateSettings(settings);
    	m_charges.validateSettings(settings);
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
	
    private void addSpectra(String pep, Map<Peptide, PeakListImpl> peptide_fragments) {
    }
    
	@SuppressWarnings("unused")
	private void processNoDigestion(String seq) throws IOException, PeptideFragmentationException {
		try {
			Peptide peptide = new Peptide.Builder(seq).build();
			
			Map<Peptide, PeakListImpl> peptideFragments = makeFragmentation(peptide, m_charges.getStringArrayValue());
			
			if (!peptideFragments.isEmpty()) {
				addSpectra(peptide.toAAString(), peptideFragments);
			}
		} catch (BuilderException e) {
			logger.warn(e.getMessage()
			    + ": cannot make spectrum from ambiguous peptide " + seq + ".");
		}
	}
	
	@SuppressWarnings("unused")
	private void processWithDigestion(String seq) throws IOException, PeptideFragmentationException {
		Set<DigestedPeptide> peptides =  makeDigestion(seq);
		
		for (DigestedPeptide digest : peptides) {
			
			if (digest.isAmbiguous()) {
				logger.warn("# " + digest + ": ambigous.");
			} else if (!m_fragmenter.isFragmentable(digest)) {
				logger.warn("# " + digest + ": too short.");
			} else {
				
				Map<Peptide, PeakListImpl> peptideFragments = makeFragmentation(digest.getPeptide(), m_charges.getStringArrayValue());
				
				if (!peptideFragments.isEmpty()) {
					addSpectra(digest.toAAString(), peptideFragments);
				}
				
			}
		}
	}
	
	private Set<DigestedPeptide> makeDigestion(String sequence) {
		m_digester.digest(new Peptide.Builder(sequence).ambiguityEnabled()
		    .build());
		
		return m_digester.getDigests();
	}
	
	private Map<Peptide, PeakListImpl> makeFragmentation(Peptide seq, String[] charges)
	    throws NumberFormatException, PeptideFragmentationException {
		
		Map<Peptide, PeakListImpl> peptideFragments =
		    new HashMap<Peptide, PeakListImpl>();
		
		int maxCharge = 0;
		
		// get maximum charge
		Set<Integer> i_charges = new HashSet<Integer>();
		for (String cc_str : charges) {
			Integer i_cc = new Integer(cc_str);
			i_charges.add(i_cc);
			int currentCharge = i_cc.intValue();
			if (currentCharge > maxCharge) {
				maxCharge = currentCharge;
			}
		}
		
		m_fragmenter.setFragmentablePrecursor(new Peptide.Builder(seq).protons(maxCharge).build());
		m_fragmenter.setChargeSerie(i_charges);
		m_fragmenter.generateFragments();
		PeakListImpl pl = m_fragmenter.getPeakList(m_fragmentCondition);
		
		if (pl.size() == 0) {
			throw new PeptideFragmentationException("no peaks found for "
			    + seq);
		}
		
		// // add PEP/q to the map[title]
		peptideFragments.put(m_fragmenter.getFragmentablePrecursor(), pl);
		
		return peptideFragments;
	}
	
}

