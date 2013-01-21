package au.edu.unimelb.plantcell.ensembl;

import org.biojava3.core.sequence.ProteinSequence;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import uk.ac.roslin.ensembl.dao.database.DBSpecies;
import uk.ac.roslin.ensembl.datasourceaware.core.DAGene;
import uk.ac.roslin.ensembl.datasourceaware.core.DATranscript;
import uk.ac.roslin.ensembl.datasourceaware.core.DATranslation;
import uk.ac.roslin.ensembl.exception.DAOException;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;


/**
 * This is the model implementation of EnsembleAddHomologue.
 * Adds homologues for the input data to the output table
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AddProteinNodeModel extends AddHomologueNodeModel {
  
	final static NodeLogger   logger    = NodeLogger.getLogger("Add Proteins");
	
	
    /**
     * Constructor for the node model.
     */
    protected AddProteinNodeModel() {
        super(1,1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
		DBSpecies sp =  load_species(logger, m_db_props.getStringValue(), m_species.getStringValue());
		DataTableSpec[] outSpecs = make_output_spec(inData[0].getSpec());
		MyDataContainer c = new MyDataContainer(exec.createDataContainer(outSpecs[0]), "Protein"); 
		int seq_idx = -1;
		if (! m_id.useRowID()) {
			seq_idx = inData[0].getSpec().findColumnIndex(m_id.getStringValue());
			if (seq_idx < 0)
				throw new InvalidSettingsException("Unable to find column: "+m_id.getStringValue()+" - reconfigure?");
		}
		RowIterator it = inData[0].iterator();
 
		int done = 0;
		boolean use_row_id = m_id.useRowID();
		while (it.hasNext()) {
			 DataRow r = it.next();
			 String id = r.getKey().getString();
			 if (!use_row_id) {
				 DataCell id_cell = r.getCell(seq_idx);
				 if (id_cell == null || id_cell.isMissing())
					 continue;
				 
				 id = get_id(id_cell);
			 }
			 
			 try {
				 report_proteins(c, sp, id);
			 } catch (DAOException dao) {
				 logger.warn("Error getting record for gene "+id+": ignored.");
				 dao.printStackTrace();
			 }
			 
			 if (done++ % 100 == 0) {
				 exec.checkCanceled();
				 exec.setProgress(((double)done) / inData[0].getRowCount());
			 }
		}
		
		return new BufferedDataTable[]{c.close()};
    }

   
    private void report_proteins(MyDataContainer c, DBSpecies sp, String id) throws DAOException, InvalidSettingsException {
		DAGene g = sp.getGeneByStableID(id);
		if (g == null) {
			logger.warn("Unable to load gene: "+id+"... ignored.");
			return;
		}
		DataCell[] cells = new DataCell[c.getTableSpec().getNumColumns()];
		cells[0]         = safe_string_cell(g.getStableID());
		
		// report canonical proteins translations for all transcripts of the given gene
		int idx = 1;
		for (DATranscript t : g.getTranscripts() ) {
			// not translated to protein? ok, skip it for now
			if (!t.isTranslated())
				continue;
			DATranslation mrna = t.getCanonicalTranslation();
			if (mrna == null) 
				continue;
			ProteinSequence ps = mrna.getProteinSequence();
			cells[1] = safe_string_cell(t.getStableID());
			String tmp = id+"_"+idx++;
			cells[2] = safe_string_cell(tmp);
			cells[3] = new SequenceCell(SequenceType.AA, tmp, ps.getSequenceAsString());
		
			c.addRow(cells);
		}
	}
  
    @Override
	protected DataTableSpec[] make_output_spec(DataTableSpec spec) {
		DataColumnSpec[] cols = new DataColumnSpec[4];
		cols[0] = new DataColumnSpecCreator("Gene Stable ID", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Transcript stable ID", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Protein stable ID", StringCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Protein Sequence", SequenceCell.TYPE).createSpec();
	
		return new DataTableSpec[] { new DataTableSpec(cols) };
	}

}

