package au.edu.unimelb.plantcell.proteomics.gravy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import org.expasy.jpl.core.mol.polymer.BioPolymerUtils;
import org.expasy.jpl.core.mol.polymer.pept.Peptide;
import org.expasy.jpl.core.mol.polymer.pept.cutter.Peptidase;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
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
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.ProteinSequenceRowIterator;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * This is the model implementation of InsilicoDigestor.
 * Often using javaprotlib, a few useful nodes for Proteomics Mass Spectrometry
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class GravyScorerNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Gravy Scorer");
        
  
    public static final String CFGKEY_SEQUENCE = "sequence-column";
    public static final String CFGKEY_WINDOW_SIZE = "gravy-window-size";

    private SettingsModelString m_sequence = new SettingsModelString(CFGKEY_SEQUENCE, "Sequence");
    private SettingsModelInteger m_window  = new SettingsModelInteger(CFGKEY_WINDOW_SIZE, 9);
    
    /**
     * Constructor for the node model.
     */
    protected GravyScorerNodeModel() {
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
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        DataTableSpec outputSpec = make_output_spec(inData[0].getDataTableSpec());
        int seq_idx = inData[0].getDataTableSpec().findColumnIndex(m_sequence.getStringValue());
        if (seq_idx < 0) {
        	throw new InvalidSettingsException("Cannot find column: "+m_sequence.getStringValue()+"in input, re-configure?");
        }
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        final SequenceProcessor sp = new SequenceProcessor() {
			private boolean warned = false;
			
			@Override
			public SequenceValue process(SequenceValue sv) {
				if (sv.getStringValue().toLowerCase().indexOf('x') >= 0) {
					if (!warned) {
						logger.warn("Cannot compute score with sequence with ambiguous (X) sequence!");
						warned = true;
					}
					return null;	// skip over bad sequences after the warning
				}
				return sv;
			}
	
        };
        final ProteinSequenceRowIterator psi = new ProteinSequenceRowIterator(inData[0].iterator(), 
        		seq_idx, logger, sp);
        
        while (psi.hasNext()) {
        	SequenceValue prot = psi.nextSequence();
        	DataRow r   = psi.next();
        	
        	Peptide pep = new Peptide.Builder(prot.getStringValue()).build();
        	DataCell[] cells = new DataCell[4];
        	cells[0] = new DoubleCell(BioPolymerUtils.getGravyScore(pep));
        	float[] gscores = BioPolymerUtils.getGravyScores(pep, m_window.getIntValue());
        	ArrayList<DoubleCell> gscore_cells = new ArrayList<DoubleCell>();
        	float min = Float.MAX_VALUE;
        	float max = Float.MIN_VALUE;
        	for (float gscore : gscores) {
        		gscore_cells.add(new DoubleCell(gscore));
        		if (min > gscore) {
        			min = gscore;
        		}
        		if (max < gscore) {
        			max = gscore;
        		}
        	}
        	cells[1] = CollectionCellFactory.createListCell(gscore_cells);
        	cells[2] = new DoubleCell(min);
        	cells[3] = new DoubleCell(max);
        	container.addRowToTable(new JoinedRow(r, 
        			new DefaultRow(r.getKey().getString(), cells)));
        }
        
        if (psi.getRejectedRowCount() > 0) {
        	logger.warn(psi.getRejectedRowCount()+" sequences could not be processed.");
        }
        
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }


	private DataTableSpec make_output_spec(DataTableSpec inSpec) throws IllegalArgumentException {
    	DataColumnSpec[] cols = new DataColumnSpec[4];
    	cols[0] = new DataColumnSpecCreator("GRAVY score [-4.6, 4.6]", DoubleCell.TYPE).createSpec();
    	cols[1] = new DataColumnSpecCreator("Window scores along sequence", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec();
    	cols[2] = new DataColumnSpecCreator("Min. window score", DoubleCell.TYPE).createSpec();
    	cols[3] = new DataColumnSpecCreator("Max. window score", DoubleCell.TYPE).createSpec();
		return new DataTableSpec(inSpec, new DataTableSpec(cols));
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
    	m_sequence.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.loadSettingsFrom(settings);
   
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.validateSettings(settings);
    	
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

