package au.edu.unimelb.plantcell.io.muscle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.LogOutputStream;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentCellFactory;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue.AlignmentType;


/**
 * This is the model implementation of LocalMuscle.
 * Supports running a local muscle executable and integration into the KNIME-PlantCell platform
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class LocalMuscleNodeModel extends AbstractAlignerNodeModel {	
	public static final String CFGKEY_EXE       = "muscle-exe";
	public static final String CFGKEY_SEQUENCES = "sequences-column";
	public static final String CFGKEY_LOG_STDERR= "log-stderr";
	public static final String CFGKEY_TRADEOFF  = "alignment-tradeoff";
	
	public static final String[] TRADEOFFS = new String[] { "Maximum accuracy", "Very fast", "Fastest possible" };
	
	/**
	 * user-configured node persisted state
	 */
	private SettingsModelString  m_exe         = new SettingsModelString(CFGKEY_EXE, "");
	private SettingsModelString  m_input_sequences = new SettingsModelString(CFGKEY_SEQUENCES, "");
	private SettingsModelBoolean m_log         = new SettingsModelBoolean(CFGKEY_LOG_STDERR, Boolean.FALSE);
	private SettingsModelString  m_performance = new SettingsModelString(CFGKEY_TRADEOFF, TRADEOFFS[0]);
	
	// not persisted (yet) but is intended for the node views
	private final Map<String,AlignmentValue> m_view_model = new HashMap<String,AlignmentValue>();
	private File alignment_exe;
	
    /**
     * Constructor for the node model.
     */
    protected LocalMuscleNodeModel() {
        super(1, 1);
        reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	logger.info("Performing alignments on "+inData[0].getRowCount()+" input rows.");
    	logger.info("Using "+m_exe.getStringValue()+" to align input sequences.");
    
    	final int seqs_idx = inData[0].getDataTableSpec().findColumnIndex(m_input_sequences.getStringValue());
    	if (seqs_idx < 0)
    		throw new InvalidSettingsException("Cannot find input sequences - re-configure?");
    	reset();
    	alignment_exe = validateAlignmentProgram(m_exe.getStringValue());
    	DataTableSpec outSpec = make_output_spec(inData[0].getSpec(), seqs_idx);
    	
    	// if the input sequences are groupby'ed then we do the calculation this way...
    	if (isCollectionOfSequencesColumn(inData[0].getDataTableSpec().getColumnSpec(seqs_idx))) {
    		ColumnRearranger rearranger = new ColumnRearranger(inData[0].getSpec());    		
    		rearranger.append(new AppendAlignmentCellFactory(outSpec, seqs_idx, this));
    		BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], rearranger, exec.createSubProgress(1.0));
    		
            return new BufferedDataTable[]{out};
    	} else {
    		// otherwise we do the groupby and then create a single-cell table as output from the alignment
    		MyDataContainer c = new MyDataContainer(exec.createDataContainer(make_output_spec(inData[0].getSpec(), seqs_idx)), "Aln");
    		SequenceType   st = SequenceType.UNKNOWN;
			final Map<UniqueID,SequenceValue> seq_map = new HashMap<UniqueID,SequenceValue>();
			for (DataRow r : inData[0]) {
				DataCell cell = r.getCell(seqs_idx);
				if (cell instanceof SequenceValue) {
					SequenceValue sv = (SequenceValue)cell;
					if (st != SequenceType.UNKNOWN && st != sv.getSequenceType()) {
						throw new InvalidSettingsException("Cannot mix sequence types (eg. AA versus NA) in sequence column on row: "+r.getKey().getString());
					} else {
						st = sv.getSequenceType();
					}
					seq_map.put(new UniqueID(), sv);
				}
			}
			
			final String rowid = "Alignment1";
			logWarningAboutAlignments(seq_map, st, rowid);
    		c.addRow(new DataCell[] { runAlignmentProgram(seq_map, rowid, st)} );
    		return new BufferedDataTable[] {c.close()};
    	}
    
    	
		
    }

    public File getAlignmentProgram() {
    	return alignment_exe;
    }
    
    @Override
    public CommandLine makeCommandLineArguments(final File f, final SequenceType st) {
    	CommandLine cmdLine = new CommandLine(getAlignmentProgram());
    	cmdLine.addArgument("-in");
    	cmdLine.addArgument(f.getName());
    	if (m_performance.getStringValue().equals(TRADEOFFS[0])) {
    		// no arguments to add
    	} else if (m_performance.getStringValue().equals(TRADEOFFS[1])) {
    		cmdLine.addArgument("-maxiters");
    		cmdLine.addArgument("2");
    	} else {
    		cmdLine.addArgument("-maxiters");
    		cmdLine.addArgument("1");
    		cmdLine.addArgument("-diags");
    		if (st.isProtein()) {
    			cmdLine.addArgument("-sv");
    			cmdLine.addArgument("-distance1");
    			cmdLine.addArgument("kbit20_3");
    		}
    	}
    	
    	return cmdLine;
    }
 
    @Override
	public DataCell makeAlignmentCellAndPopulateResultsMap(final LogOutputStream tsv, 
			final SequenceType st, final String row_id) throws IOException {
		assert(st != null && tsv != null && row_id != null);
		DataCell alignment_cell;
		if (st.isProtein())
               alignment_cell = AlignmentCellFactory.createCell(tsv.toString(), AlignmentType.AL_AA);
		else
               alignment_cell = AlignmentCellFactory.createCell(tsv.toString(), AlignmentType.AL_NA);
		if (alignment_cell != null && alignment_cell instanceof AlignmentValue) {
			// since a row_id must be unique, something is very wrong if the row_id is already in the map
			assert(!m_view_model.containsKey(row_id));
			m_view_model.put(row_id, (AlignmentValue) alignment_cell);
		}
		return alignment_cell;
	}
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	super.reset();
    	m_view_model.clear();
    	alignment_exe = null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

    	final int seq_idx = inSpecs[0].findColumnIndex(m_input_sequences.getStringValue());
    	DataTableSpec  outSpec = make_output_spec(inSpecs[0], seq_idx);
        return new DataTableSpec[]{outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
         m_exe.saveSettingsTo(settings);
         m_input_sequences.saveSettingsTo(settings);
         m_log.saveSettingsTo(settings);
         m_performance.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
       m_exe.loadSettingsFrom(settings);
       m_input_sequences.loadSettingsFrom(settings);
       m_log.loadSettingsFrom(settings);
       m_performance.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
       m_exe.validateSettings(settings);
       m_input_sequences.validateSettings(settings);
       m_log.validateSettings(settings);
       m_performance.validateSettings(settings);
    }
  
	@Override
	public List<String> getAlignmentRowIDs() {
		ArrayList<String> ret = new ArrayList<String>();
		ret.addAll(m_view_model.keySet());
		return ret;
	}

	@Override
	public AlignmentValue getAlignment(final String row_id) {
		return m_view_model.get(row_id);
	}

	@Override
	protected String getAlignmentLogName() {
		return "MUSCLE";
	}

}

