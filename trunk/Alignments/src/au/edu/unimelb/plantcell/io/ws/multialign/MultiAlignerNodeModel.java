package au.edu.unimelb.plantcell.io.ws.multialign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * This is the model implementation of MuscleAligner.
 * 
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MultiAlignerNodeModel extends NodeModel {
	private static final NodeLogger logger = NodeLogger.getLogger(MultiAlignerNodeModel.class);
	  
    public static final String CFGKEY_EMAIL = "email";
	public static final String CFGKEY_SEQ_COL = "sequence-column";
	public static final String CFGKEY_ALGO = "alignment-algorithm";
	
	public static final String DEFAULT_EMAIL = "must.set.this@to.use.this.node";

	
	private SettingsModelString m_email = new SettingsModelString(CFGKEY_EMAIL, DEFAULT_EMAIL);
	private SettingsModelString m_seq_col = new SettingsModelString(CFGKEY_SEQ_COL, "Sequence");
	private SettingsModelString m_algo = new SettingsModelString(CFGKEY_ALGO, "MUSCLE");
	
	/* internal model state -- persisted */
    private final HashMap<String,AlignmentValue> m_alignment_map = new HashMap<String,AlignmentValue>();
    
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
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	  if (m_email.getStringValue().equals(DEFAULT_EMAIL)) {
              throw new Exception("You must set a valid E-Mail for EBI to contact you in the event of problems with the service!");
      }
      int n_rows   = inData[0].getRowCount();
      int seq_idx  = inData[0].getSpec().findColumnIndex(((SettingsModelString)m_seq_col).getStringValue());
      if (seq_idx < 0) {
              throw new Exception("Cannot find columns... valid data?");
      }
      int done = 0;

      // create the output columns (raw format for use with R)
      DataTableSpec outputSpec = new DataTableSpec(inData[0].getDataTableSpec(), make_output_spec());
      BufferedDataContainer container = exec.createDataContainer(outputSpec, false, 0);

      // each row is a separate MUSCLE job, the sequences are in one collection cell, the accessions (IDs) in the other
      RowIterator it = inData[0].iterator();
      m_alignment_map.clear();
      while (it.hasNext()) {
    	  DataRow r = it.next();
    	  	
          exec.setProgress(((double)done) / n_rows);

          List<SequenceValue> seqs   = new ArrayList<SequenceValue>();

          if (!grok_sequences(r.getCell(seq_idx), seqs)) {
                  logger.warn("Skipping invalid sequence data in row: "+r.getKey().getString());
                  done++;
                  continue;
          }

          if (seqs.size() < 1) {
                  throw new Exception("Cannot MUSCLE zero sequences: error at row "+r.getKey().getString());
          }
          if (seqs.size() > 1000) {
                  throw new Exception("Too many sequences in row "+r.getKey().getString());
          }
          // ensure no two sequences have the same ID
          HashSet<String> dup_accsns = new HashSet<String>();
          boolean skip = false;
          SequenceType must_be = seqs.get(0).getSequenceType();
          for (SequenceValue sv : seqs) {
        	  if (dup_accsns.contains(sv.getID())) {
        		  logger.warn("Skipping row: "+r.getKey().getString()+" as it contains the same accession multiple times!");
        		  skip = true;
        		  break;
        	  }
        	  if (!must_be.equals(sv.getSequenceType())) {
        		  logger.warn("Skipping row: "+r.getKey().getString()+" as not all sequences are of the same type: "+must_be);
        		  skip = true;
        		  break;
        	  }
        	  dup_accsns.add(sv.getID());
          }
          if (skip)
        	  continue;
          
          // dummy a fake "FASTA" file (in memory) and then submit that to EBI along with other necessary parameters
          StringBuffer seq_as_fasta = new StringBuffer();
          for (SequenceValue sv : seqs) {
                  seq_as_fasta.append(">");
                  seq_as_fasta.append(sv.getID());
                  seq_as_fasta.append("\n");
                  seq_as_fasta.append(sv.getStringValue());
                  seq_as_fasta.append("\n");
          }
          
          // 1. submit job for current row
          AbstractProxy proxy = AbstractProxy.makeProxy(logger, m_algo.getStringValue());
        
          Properties props = new Properties();
          props.put("id", r.getKey().getString());
          props.put("email", m_email.getStringValue());
          props.put("sequences", seq_as_fasta.toString());
          
          logger.info("Submitting job to EBI using "+m_algo.getStringValue());
          String jobid = proxy.run(props);
          logger.info("Sent job to EBI for row "+r.getKey().getString()+ ", got job id: "+jobid);
          
          // 2. wait for completion (or failure)
          if (!proxy.wait_for_completion(exec, jobid)) {
        	  logger.warn("EBI job failed for row "+r.getKey().getString()+"... continuing with remaining rows.");
        	  done++;
        	  continue;
          }
          
          // 3. process the result into the output port
          DataCell[] cells = proxy.get_results(exec, jobid);
          container.addRowToTable(new JoinedRow(r, new DefaultRow(r.getKey().getString(), cells)));
          
          if (cells[1] instanceof AlignmentValue) {
        	  m_alignment_map.put(r.getKey().getString(), (AlignmentValue) cells[1]);
          }
          
          // 4.
          if (it.hasNext()) {
	          logger.info("Delaying to 30sec. to be nice to EBI servers");
	          for (int i=0; i<10; i++) {
	        	  Thread.sleep(3 * 1000);
	        	  exec.checkCanceled();
	          }
          }
          done++;
      }
      container.close();
      
      return new BufferedDataTable[]{container.getTable()};
    }
    
  
	private boolean grok_sequences(DataCell seq_cell, List<SequenceValue> seqs) throws InvalidSettingsException {
        assert(seqs != null);

        if (seq_cell == null || seq_cell.isMissing()) {
                return false;
        }

        add_seqs(seq_cell, seqs);

        return (seqs.size() > 0);
	}

	private void add_seqs(DataCell cell, List<SequenceValue> l) {
        assert(cell != null && l != null);

        // if its not a collection of strings, not much we can do
        if (!cell.getType().isCollectionType() || !cell.getType().getCollectionElementType().isCompatible(StringValue.class))
                return;

        Iterator<DataCell> i = null;
        if (cell instanceof SetCell) {
                SetCell sc = (SetCell) cell;
                i = sc.iterator();
        } else if (cell instanceof ListCell) {
                ListCell lc = (ListCell) cell;
                i = lc.iterator();
        }

        if (i != null) {
                while (i.hasNext()) {
                        DataCell dc = i.next();
                        if (dc.getType().isCompatible(SequenceValue.class) && !dc.isMissing()) {
                                l.add(((SequenceValue)dc));
                        }
                }
        }
	}


    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    	m_alignment_map.clear();
    }

    
    protected DataTableSpec make_output_spec() {   
    	DataColumnSpec[] cols = new DataColumnSpec[3];
		cols[0] = new DataColumnSpecCreator("EBI JobID", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Aligned Sequences", MultiAlignmentCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Aligner debug output", StringCell.TYPE).createSpec();
		return new DataTableSpec(cols);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

        return new DataTableSpec[] { new DataTableSpec(inSpecs[0], make_output_spec()) };
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

	public String[] getRowIDs() {
		return m_alignment_map.keySet().toArray(new String[0]);
	}
	
	public AlignmentValue getAlignment(String row_id) {
		return m_alignment_map.get(row_id);
	}

}

