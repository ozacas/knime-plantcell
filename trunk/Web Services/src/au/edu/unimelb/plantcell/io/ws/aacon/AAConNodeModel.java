package au.edu.unimelb.plantcell.io.ws.aacon;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
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

import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue;

import compbio.data.msa._01._12._2010.AAConWS;
import compbio.data.msa._01._12._2010.Analize;
import compbio.data.msa._01._12._2010.FastaSequence;
import compbio.data.msa._01._12._2010.JobStatus;
import compbio.data.msa._01._12._2010.ObjectFactory;
import compbio.data.msa._01._12._2010.Score;
import compbio.data.msa._01._12._2010.ScoreHolder;
import compbio.data.msa._01._12._2010.ScoreManager;
import compbio.data.msa._01._12._2010.SequenceAnnotation;

public class AAConNodeModel extends NodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(AAConNodeModel.class);
    
	public final static String CFGKEY_URL          = "jabaws-url";
	public static final String CFGKEY_SEQUENCE_COL = "sequence-column";
	
	private final SettingsModelString m_url = new SettingsModelString(CFGKEY_URL, "http://proteomics.bio21.unimelb.edu.au/jabaws/");
	private final SettingsModelString m_sequence_column = new SettingsModelString(CFGKEY_SEQUENCE_COL, "");
	
	public AAConNodeModel() {
		this(1, 1);
	}
	
	protected AAConNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(1, 1);
	}
	
	public DataTableSpec make_output_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[4];
		cols[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Method", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Site", IntCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Score", DoubleCell.TYPE).createSpec();
		
		return new DataTableSpec(cols);
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
		int seq_idx = inData[0].getDataTableSpec().findColumnIndex(m_sequence_column.getStringValue());
		if (seq_idx < 0) {
			throw new InvalidSettingsException("Unable to locate column (re-configure?): "+m_sequence_column.getStringValue());
		}
		ObjectFactory of = new ObjectFactory();
		Analize a = of.createAnalize();
		AAConWS ws = new AAConWS();
		SequenceAnnotation annot = ws.getAAConWSPort();
		BufferedDataContainer container = exec.createDataContainer(make_output_spec());
		RowIterator it = inData[0].iterator();
		int done = 1;
		while (it.hasNext()) {
			DataRow r = it.next();
			DataCell seq_cell = r.getCell(seq_idx);
			if (seq_cell == null || seq_cell.isMissing())
				continue;
			
			AlignmentValue av = (AlignmentValue) seq_cell;
			List<FastaSequence> l_fa = a.getFastaSequences();
			for (int i=0; i<av.getSequenceCount(); i++) {
				String id = r.getKey().getString()+"_"+i;
				//logger.debug(id);
				FastaSequence f = of.createFastaSequence();
				f.setId(id);
				f.setSequence(av.getAlignedSequenceString(i));
				l_fa.add(f);
			}
			
			String jobid = annot.analize(l_fa);
			wait_for_completion(annot, jobid);
			ScoreManager sm = annot.getAnnotation(jobid);
			DataCell[] cells = new DataCell[4];
			cells[0] = new StringCell(r.getKey().getString());
			
			for (ScoreHolder sh : sm.getSeqScores()) {
				for (Score s : sh.getScores()) {
					cells[1] = new StringCell(s.getMethod());
					int site = 1;
					for (Float f : s.getScores()) {
						cells[2] = new IntCell(site++);
						cells[3] = new DoubleCell(f.doubleValue());
						container.addRowToTable(new DefaultRow("Row"+done++, cells));
					}
				}
			}
		}
		
		container.close();
		BufferedDataTable out = container.getTable();
		return new BufferedDataTable[] { out };
	}
	
	private void wait_for_completion(SequenceAnnotation annot, String jobid) throws Exception {
		JobStatus status = annot.getJobStatus(jobid);
		int delay = 0;
		while (!status.equals(JobStatus.FINISHED)) {
			if (status.equals(JobStatus.CANCELLED))
				throw new Exception("AACon job cancelled: "+jobid);
			if (status.equals(JobStatus.UNDEFINED) || status.equals(JobStatus.FAILED))
				throw new InvalidSettingsException("invalid job status: "+status.value());
			logger.info("Waiting "+(delay+2)+" seconds for job "+jobid+" to finish.");
			Thread.sleep((delay + 2) * 1000);
			delay += 2;
			if (delay > 300) {
				throw new Exception("Very long delay... are you sure AACon is working?");
			}
			status = annot.getJobStatus(jobid);
			logger.info("After delay, got status from AACon "+status.value());
		}
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
	            throws InvalidSettingsException {
		return new DataTableSpec[] { make_output_spec() };
	}
	
	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// NO-OP
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// NO-OP
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_url.saveSettingsTo(settings);
		m_sequence_column.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.validateSettings(settings);
		m_sequence_column.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_url.loadSettingsFrom(settings);
		m_sequence_column.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
	}

}
