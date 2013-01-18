package au.edu.unimelb.plantcell.ensembl;

import java.util.Calendar;

import org.biojava3.core.sequence.RNASequence;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

import uk.ac.roslin.ensembl.dao.database.DBSpecies;
import uk.ac.roslin.ensembl.datasourceaware.core.DAExon;
import uk.ac.roslin.ensembl.datasourceaware.core.DAGene;
import uk.ac.roslin.ensembl.datasourceaware.core.DATranscript;
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
public class AddTranscriptNodeModel extends AddHomologueNodeModel {
  
	private final static NodeLogger   logger    = NodeLogger.getLogger("Add Transcripts");

	static final String CFGKEY_REPORT_EXONS = "report-exons?";
	
	private SettingsModelBoolean m_report_exons = new SettingsModelBoolean(CFGKEY_REPORT_EXONS, false);
	
    /**
     * Constructor for the node model.
     */
    protected AddTranscriptNodeModel() {
        this(1,2);
    }

    public AddTranscriptNodeModel(int i, int j) {
		super(i, j);
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
		DBSpecies sp =  load_species(logger, m_db_props.getStringValue(), null);
		DataTableSpec[] outSpecs = make_output_spec(inData[0].getSpec());
		MyDataContainer c = new MyDataContainer(exec.createDataContainer(outSpecs[0]), "Transcript"); 
		MyDataContainer c2= new MyDataContainer(exec.createDataContainer(outSpecs[1]), "Exon");
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
				 report_transcripts(c, c2, sp, id);
			 } catch (DAOException dao) {
				 logger.warn("Error getting record for gene "+id+": ignored.");
				 dao.printStackTrace();
			 }
			 
			 if (done++ % 100 == 0) {
				 exec.checkCanceled();
				 exec.setProgress(((double)done) / inData[0].getRowCount());
			 }
		}
		
		return new BufferedDataTable[]{c.close(), c2.close()};
    }

    private void report_transcripts(MyDataContainer c, MyDataContainer c2, DBSpecies sp, String gene_id) throws DAOException, InvalidSettingsException {
		assert(sp != null && gene_id != null && c != null && c2 != null);
		
		DAGene g = sp.getGeneByStableID(gene_id);
		if (g == null) {
   		 	logger.warn("Unable to locate gene by stable ID: "+gene_id+"... ignoring!");
   		 	return;
   	 	}
		for (DATranscript t : g.getTranscripts()) {
			DataCell[] cells = new DataCell[c.getTableSpec().getNumColumns()];
			
			cells[0] = safe_string_cell(g.getStableID());
			cells[1] = safe_string_cell(t.getStableID());
			cells[2] = safe_string_cell(t.getDisplayName());
			cells[3] = safe_string_cell(t.getStatus());
			
			RNASequence rnaseq = t.getPrimaryTranscriptRNASequence();
			if (rnaseq != null && rnaseq.getLength() > 0) {
				cells[4] = new SequenceCell(SequenceType.RNA, t.getStableID(), rnaseq.getSequenceAsString());
			} else {
				cells[4] = DataType.getMissingCell();
			}
			
			cells[5] = safe_string_cell(t.getDescription());
			cells[6] = safe_string_cell(t.getAssembly());

			Calendar cal = Calendar.getInstance();
			if (t.getCreationDate() != null) {
				cal.setTime(t.getCreationDate());
				cells[7] = make_date_cell(cal);
			} else {
				cells[7] = DataType.getMissingCell();
			}
			if (t.getModificationDate() != null) {
				cal.setTime(t.getModificationDate());
				cells[8] = make_date_cell(cal);
			} else {
				cells[8] = DataType.getMissingCell();
			}
			
			c.addRow(cells);
			
			if (m_report_exons.getBooleanValue()) {
				report_exons(c2, t);
			}
		}
	}

    private void report_exons(MyDataContainer c2, DATranscript t) {
		DataCell[] cells = new DataCell[c2.getTableSpec().getNumColumns()];
		cells[0] = new StringCell(t.getStableID());
		for (DAExon exon : t.getExons()) {
			cells[1] = new StringCell(exon.getRNASequence().getSequenceAsString());
			cells[2] = new IntCell(exon.getRank());
			c2.addRow(cells);
		}
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_report_exons.saveSettingsTo(settings);
    	super.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_report_exons.loadSettingsFrom(settings);
    	super.loadValidatedSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_report_exons.validateSettings(settings);
    	super.validateSettings(settings);
    }
    
  
    @Override
	protected DataTableSpec[] make_output_spec(DataTableSpec spec) {
		DataColumnSpec[] cols = new DataColumnSpec[9];
		cols[0] = new DataColumnSpecCreator("Gene Stable ID", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Transcript stable ID", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Display name", StringCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Transcript status", StringCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("Primary RNA Sequence", SequenceCell.TYPE).createSpec();
		cols[5] = new DataColumnSpecCreator("Transcript Description", StringCell.TYPE).createSpec();
		cols[6] = new DataColumnSpecCreator("Assembly", StringCell.TYPE).createSpec();
		cols[7] = new DataColumnSpecCreator("Creation date", DateAndTimeCell.TYPE).createSpec();
		cols[8] = new DataColumnSpecCreator("Modification date", DateAndTimeCell.TYPE).createSpec();
		
		DataColumnSpec[] exon_cols = new DataColumnSpec[3];
		exon_cols[0] = new DataColumnSpecCreator("Transcript stable ID", StringCell.TYPE).createSpec();
		exon_cols[1] = new DataColumnSpecCreator("Exon Sequence", StringCell.TYPE).createSpec();
		exon_cols[2] = new DataColumnSpecCreator("Exon rank", IntCell.TYPE).createSpec();
		
		return new DataTableSpec[] { new DataTableSpec(cols), new DataTableSpec(exon_cols) };
	}

}

