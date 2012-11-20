package au.edu.unimelb.plantcell.io.read.xtandem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import de.proteinms.xtandemparser.xtandem.Domain;
import de.proteinms.xtandemparser.xtandem.Peptide;
import de.proteinms.xtandemparser.xtandem.PeptideMap;
import de.proteinms.xtandemparser.xtandem.Protein;
import de.proteinms.xtandemparser.xtandem.ProteinMap;
import de.proteinms.xtandemparser.xtandem.Spectrum;
import de.proteinms.xtandemparser.xtandem.XTandemFile;


/**
 * This is the model implementation of XTandemReader.
 * Uses the xtandem reader codebase, http://code.google.com/p/xtandem-parser/, to load identified spectra from XTandem and scores into KNIME.
 *
 * @author Andrew Cassin
 */
public class XTandemReaderNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(XTandemReaderNodeModel.class);
    
    
	static final String CFGKEY_FILES     = "xtandem-files-to-load";

    private final SettingsModelStringArray        m_files = new SettingsModelStringArray(CFGKEY_FILES, new String[] {});

    

    /**
     * Constructor for the node model.
     */
    protected XTandemReaderNodeModel() {
        super(0, 3);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	DataTableSpec[] outSpec = make_output_spec();
    	
    	BufferedDataContainer c_spectra = exec.createDataContainer(outSpec[0]);
    	BufferedDataContainer c_peptide = exec.createDataContainer(outSpec[1]);
    	BufferedDataContainer c_protein = exec.createDataContainer(outSpec[2]);
    	
    	for (String fname : m_files.getStringArrayValue()) {
    		File f = new File(fname);
    		if (!f.exists() || !f.canRead() || !f.isFile()) {
    			logger.warn("Cannot open "+f.getAbsolutePath()+" for reading... skipping!");
    			continue;
    		}
    		
	    	// first output port (spectra)
    		logger.info("Processing file: "+f.getAbsolutePath());
	    	XTandemFile xtf = new XTandemFile(f.getAbsolutePath());
	    	List<Spectrum> spectra =  xtf.getSpectraList();
	    	HashMap<Integer,String> spectra_numbers = new HashMap<Integer,String>();
	    	int id=1;
	    	for (Spectrum s : spectra) {
	    		DataCell[] cells = new DataCell[7];
	    		for (int i=0; i<cells.length; i++) {
	    			cells[i] = DataType.getMissingCell();
	    		}
	    		String spectra_key = "Spectra"+id++;
	    		spectra_numbers.put(s.getSpectrumNumber(), spectra_key);
	    		cells[0] = new IntCell(s.getSpectrumNumber());
	    		cells[1] = new StringCell(s.getLabel());
	    		cells[2] = new DoubleCell(s.getExpectValue());
	    		cells[3] = new StringCell(s.getPrecursorRetentionTime());
	    		cells[4] = new IntCell(s.getPrecursorCharge());
	    		cells[5] = new DoubleCell(s.getPrecursorMh());
	    		// cells[6] is the spectra itself
	    		cells[6] = new StringCell(f.getAbsolutePath());
	    		
	    		c_spectra.addRowToTable(new DefaultRow(spectra_key, cells));
	    	}
	    	
	    	// second output port: peptide identifications for reported spectra
	    	PeptideMap pm = xtf.getPeptideMap();
	    	id = 1;
	    	for (Integer spectra_number : spectra_numbers.keySet()) {
	    		for (Peptide p : pm.getAllPeptides(spectra_number)) {
	    			for (Domain d : p.getDomains()) {
	    				DataCell[] cells = new DataCell[9];
	        			cells[0] = new StringCell(spectra_numbers.get(p.getSpectrumNumber()));
	        			cells[1] = new StringCell(p.getFastaFilePath());
	        			cells[2] = new StringCell(p.getSequence());
	        			cells[3] = new StringCell(p.getPeptideID());
	        			cells[4] = new StringCell(d.getDomainSequence());
	        			cells[5] = new DoubleCell(d.getDomainDeltaMh());
	        			cells[6] = new DoubleCell(d.getDomainHyperScore());
	        			cells[7] = new IntCell(d.getMissedCleavages());
	        			cells[8] = new DoubleCell(d.getDomainMh());
	        			c_peptide.addRowToTable(new DefaultRow("Peptide"+id++, cells));
	    			}
	    		}
	    	}
	    	
	    	// third output port: protein inferencing from identified peptides
	    	ProteinMap prot_map = xtf.getProteinMap();
	    	Iterator it = prot_map.getProteinIDIterator();
	    	id=1;
	    	while (it.hasNext()) {
	    		Protein p = prot_map.getProtein(it.next().toString());
	    	
	        	DataCell[] cells = new DataCell[6];
	        	for (int i=0; i<cells.length; i++) {
	        		cells[i] = DataType.getMissingCell();
	        	}
	        	cells[0] = new StringCell(p.getID());
	        	cells[1] = new StringCell(p.getLabel());
	        	cells[2] = new DoubleCell(p.getSummedScore());
	        	cells[3] = new DoubleCell(p.getExpectValue());
	        	cells[4] = new StringCell(p.getUID());
	        	c_protein.addRowToTable(new DefaultRow("Protein"+id++, cells));
	    	}
    	}
    	c_spectra.close();
    	c_peptide.close();
    	c_protein.close();
    	
    	return new BufferedDataTable[]{c_spectra.getTable(), 
    			c_peptide.getTable(), c_protein.getTable()};
    }

    private DataTableSpec[] make_output_spec() {
    	DataColumnSpec[] spectra_cols = new DataColumnSpec[7];
    	spectra_cols[0] = new DataColumnSpecCreator("Spectra Number", IntCell.TYPE).createSpec();
    	spectra_cols[1] = new DataColumnSpecCreator("Spectra Label", StringCell.TYPE).createSpec();
    	spectra_cols[2] = new DataColumnSpecCreator("Expect Value", DoubleCell.TYPE).createSpec();
    	spectra_cols[3] = new DataColumnSpecCreator("Precursor Retention Time", StringCell.TYPE).createSpec();
    	spectra_cols[4] = new DataColumnSpecCreator("Precursor Charge state", IntCell.TYPE).createSpec();
    	spectra_cols[5] = new DataColumnSpecCreator("Precursor Mh", DoubleCell.TYPE).createSpec();
		spectra_cols[6] = new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
		
    	DataColumnSpec[] peptide_cols = new DataColumnSpec[9];
    	peptide_cols[0] = new DataColumnSpecCreator("Spectra", StringCell.TYPE).createSpec();
    	peptide_cols[1] = new DataColumnSpecCreator("FASTA file path", StringCell.TYPE).createSpec();
    	peptide_cols[2] = new DataColumnSpecCreator("Peptide", StringCell.TYPE).createSpec();
    	peptide_cols[3] = new DataColumnSpecCreator("Peptide ID", StringCell.TYPE).createSpec();
    	peptide_cols[4] = new DataColumnSpecCreator("Domain sequence", StringCell.TYPE).createSpec();
    	peptide_cols[5] = new DataColumnSpecCreator("Domain Delta Mh", DoubleCell.TYPE).createSpec();
    	peptide_cols[6] = new DataColumnSpecCreator("Domain Hyperscore", DoubleCell.TYPE).createSpec();
    	peptide_cols[7] = new DataColumnSpecCreator("Missed Cleavages", IntCell.TYPE).createSpec();
    	peptide_cols[8] = new DataColumnSpecCreator("Domain Mh", DoubleCell.TYPE).createSpec();
		
    	DataColumnSpec[] protein_cols = new DataColumnSpec[6];
    	protein_cols[0] = new DataColumnSpecCreator("Protein ID", StringCell.TYPE).createSpec();
    	protein_cols[1] = new DataColumnSpecCreator("Protein Label", StringCell.TYPE).createSpec();
    	protein_cols[2] = new DataColumnSpecCreator("Summed Score", DoubleCell.TYPE).createSpec();
    	protein_cols[3] = new DataColumnSpecCreator("E-Value", DoubleCell.TYPE).createSpec();
    	protein_cols[4] = new DataColumnSpecCreator("Protein UID", StringCell.TYPE).createSpec();
    	protein_cols[5] = new DataColumnSpecCreator("Identified Peptides (list)", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
    	
		return new DataTableSpec[] { new DataTableSpec(spectra_cols),
									 new DataTableSpec(peptide_cols),
									 new DataTableSpec(protein_cols) };
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
       return make_output_spec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_files.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_files.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_files.validateSettings(settings);
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

