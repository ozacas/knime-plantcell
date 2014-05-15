package au.edu.unimelb.plantcell.io.ws.multialign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.LogOutputStream;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.muscle.AbstractAlignerNodeModel;
import au.edu.unimelb.plantcell.io.muscle.AppendAlignmentCellFactory;

/**
 * This is the model implementation of MuscleAligner.
 * 
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MultiAlignerNodeModel extends AbstractAlignerNodeModel {	  
    public static final String CFGKEY_EMAIL   = "email";
	public static final String CFGKEY_SEQ_COL = "sequence-column";
	public static final String CFGKEY_ALGO    = "alignment-algorithm";
	
	public static final String DEFAULT_EMAIL = "must.set.this@to.use.this.node";

	
	private SettingsModelString m_email   = new SettingsModelString(CFGKEY_EMAIL, DEFAULT_EMAIL);
	private SettingsModelString m_seq_col = new SettingsModelString(CFGKEY_SEQ_COL, "Sequence");
	private SettingsModelString m_algo    = new SettingsModelString(CFGKEY_ALGO, "MUSCLE");
	
	/* internal model state -- persisted */
    private final HashMap<String,AlignmentValue> m_alignment_map = new HashMap<String,AlignmentValue>();
    /* not persisted */
    private ExecutionContext m_exec;	// only valid during execute()
    private long last_call;
    
	/**
     * Constructor for the node model.
     */
    protected MultiAlignerNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {
      if (m_email.getStringValue().equals(DEFAULT_EMAIL)) {
              throw new InvalidSettingsException("You must set a valid E-Mail for EBI to contact you in the event of problems with the service!");
      }
      int seq_idx  = inData[0].getSpec().findColumnIndex(m_seq_col.getStringValue());
      if (seq_idx < 0) {
              throw new Exception("Cannot find sequence column... valid data?");
      }
    
      m_exec    = exec;
      last_call = -1;
      DataTableSpec outSpec = make_output_spec(inData[0].getSpec(), seq_idx);
  	
		// if the input sequences are groupby'ed then we do the calculation this way...
		if (isCollectionOfSequencesColumn(inData[0].getDataTableSpec().getColumnSpec(seq_idx))) {
			ColumnRearranger rearranger = new ColumnRearranger(inData[0].getSpec());    		
			rearranger.append(new AppendAlignmentCellFactory(outSpec, seq_idx, this));
			BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], rearranger, exec.createSubProgress(1.0));
			
		    return new BufferedDataTable[]{out};
		} else {
			// otherwise we do the groupby and then create a single-cell table as output from the alignment
			MyDataContainer c = new MyDataContainer(exec.createDataContainer(make_output_spec(inData[0].getSpec(), seq_idx)), "Aln");
			SequenceType   st = SequenceType.UNKNOWN;
				final Map<UniqueID,SequenceValue> seq_map = new HashMap<UniqueID,SequenceValue>();
				for (DataRow r : inData[0]) {
					DataCell cell = r.getCell(seq_idx);
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
			validateSequencesToBeAligned(seq_map);
			c.addRow(new DataCell[] { runAlignmentProgram(seq_map, rowid, st)} );
			return new BufferedDataTable[] {c.close()};
		}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	m_alignment_map.clear();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	final int seq_idx = inSpecs[0].findColumnIndex(m_seq_col.getStringValue());
    	DataTableSpec outSpec = make_output_spec(inSpecs[0], seq_idx);
    	return new DataTableSpec[] { outSpec };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_email.saveSettingsTo(settings);
    	m_seq_col.saveSettingsTo(settings);
    	m_algo.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_email.loadSettingsFrom(settings);
    	m_seq_col.loadSettingsFrom(settings);
    	m_algo.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_email.validateSettings(settings);
    	m_seq_col.validateSettings(settings); 
    	m_algo.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	  File file = new File(internDir, "muscle-internals.xml");
          FileInputStream fis = new FileInputStream(file);
          ModelContentRO modelContent = ModelContent.loadFromXML(fis);
          try {
       	   String[] keys = modelContent.getStringArray("internal-muscle-map-keys");
       	   m_alignment_map.clear();
       	   ModelContentRO subkey = modelContent.getModelContent("internal-muscle-map");
       	   for (String key : keys) {
       		   DataCell dc = subkey.getDataCell(key);
       		   if (dc instanceof MultiAlignmentCell) {
       			   m_alignment_map.put(key, (MultiAlignmentCell) dc);
       		   }
       	   }
       	   fis.close();
          } catch (InvalidSettingsException e) {
              throw new IOException(e.getMessage());
          }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    	     // save m_muscle_map... 
			 ModelContent modelContent = new ModelContent("muscle-internals.model");
			 String[] keys = m_alignment_map.keySet().toArray(new String[0]);
			 modelContent.addStringArray("internal-muscle-map-keys", keys);
			 ModelContentWO subkey = modelContent.addModelContent("internal-muscle-map");
			 for (String key : keys) {
				 // HACK BUG TODO FIXME: not all AlignmentValue's will be a DataCell...
				 subkey.addDataCell(key, (DataCell) m_alignment_map.get(key));
			 }
			 // create the XML file alongside the rest of the node data (same folder)
			 File file = new File(internDir, "muscle-internals.xml");
			 FileOutputStream fos = new FileOutputStream(file);
			 modelContent.saveToXML(fos);
    }
	
	public AlignmentValue getAlignment(String row_id) {
		return m_alignment_map.get(row_id);
	}

	@Override
	public List<String> getAlignmentRowIDs() {
		ArrayList<String> ret = new ArrayList<String>(m_alignment_map.size());
		ret.addAll(m_alignment_map.keySet());
		return ret;
	}

	@Override
	protected File getAlignmentProgram() {
		/* not a local executable so... */
		return null;
	}
	
	@Override
	public CommandLine makeCommandLineArguments(File fasta_file,
			SequenceType alignment_sequence_type) throws Exception {
		// not implemented: programs not run locally but at EBI...
		return null;
	}

	@Override
	protected String getAlignmentLogName() {
		String ret = m_algo.getStringValue();
		if (ret == null || ret.trim().length() < 1)
			return "Web Service Aligner";
		return ret;
	}

	@Override
	public DataCell makeAlignmentCellAndPopulateResultsMap(LogOutputStream tsv,
			SequenceType st, String row_id) throws IOException {
		return null;
	}
	
	@Override
	public void validateSequencesToBeAligned(final Map<UniqueID, SequenceValue> seqs) throws InvalidSettingsException {
		super.validateSequencesToBeAligned(seqs);
		
		/*
		 * Although this defends EBI's terms of service it probably prevents out-of-memory errors creating an in-memory fasta too....
		 */
		if (seqs.size() > 1000) {
			throw new InvalidSettingsException("Refusing to do an online alignment of more than 1000 sequences as per EBI terms of service!");
		}
		
		long cur = System.currentTimeMillis();
		/*
		 * Each successive call must be not less than 30secs. This is to prevent overloading EBI services and may not be configured for this reason.
		 */
		if (last_call > 0 && (cur - last_call) < 30 * 1000) {
			try {
				Thread.sleep(30 * 1000);
			} catch (InterruptedException e) {
				// be silent...
			}
		}
		last_call = cur;
	}

	/**
	 * May only be called during execute()
	 * @return
	 */
	private ExecutionContext getCurrentExecutionContext() {
		return m_exec;
	}
	
	@Override
	public DataCell runAlignmentProgram(final Map<UniqueID, SequenceValue> seqs, final String rowid, final SequenceType st) {
		try {
			 StringBuffer seq_as_fasta = new StringBuffer();
	         for (SequenceValue sv : seqs.values()) {
	              seq_as_fasta.append(">");
	              seq_as_fasta.append(sv.getID());
	              seq_as_fasta.append("\n");
	              seq_as_fasta.append(sv.getStringValue());
	              seq_as_fasta.append("\n");
	         }
	         
	         AbstractProxy proxy = AbstractProxy.makeProxy(logger, m_algo.getStringValue());
	         Properties props = new Properties();
	         props.put("id", rowid);
	         props.put("email", m_email.getStringValue());
	         props.put("sequences", seq_as_fasta.toString());
	          
	         logger.info("Submitting job to EBI using "+m_algo.getStringValue());
	         String jobid = proxy.run(props);
	         logger.info("Sent job to EBI for row "+rowid+ ", got job id: "+jobid);
	          
	         // 2. wait for completion (or failure)
	         if (!proxy.wait_for_completion(getCurrentExecutionContext(), jobid)) {
	        	  logger.warn("EBI job failed for row "+rowid+"... continuing with remaining rows.");
	        	  return DataType.getMissingCell();
	         }
	          
	          // 3. process the result into the output port
	         DataCell[] cells = proxy.get_results(getCurrentExecutionContext(), jobid);
	          
	         if (cells[1] instanceof AlignmentValue) {
	        	  m_alignment_map.put(rowid, (AlignmentValue) cells[1]);
	         }
	         return cells[1];
		} catch (Exception e) {
			e.printStackTrace();
			// fallthru
		}
		return DataType.getMissingCell();
	}
}

