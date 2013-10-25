package au.edu.unimelb.plantcell.io.read.alignments;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
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

import pal.alignment.Alignment;
import pal.alignment.AlignmentReaders;
import pal.datatype.AminoAcids;
import pal.datatype.Nucleotides;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentCellFactory;
import au.edu.unimelb.plantcell.io.ws.multialign.MultiAlignmentCell;

/**
 * Loads a alignment file using java PAL
 * 
 * @author andrew.cassin
 *
 */
public class AlignmentReaderNodeModel extends NodeModel {
    private final NodeLogger logger = NodeLogger.getLogger("Alignment Reader");
	
	public static final String CFGKEY_FILE    = "alignment-file";
	public static final String CFGKEY_FORMAT  = "alignment-file-format";
	public static final String CFGKEY_TYPE    = "sequence-type";		// AA or nucleotide
	public static final String[] FILE_FORMATS = new String[] { "Automatic (guess)", "Aligned FASTA", "Clustal", "Phylip (interleaved)", "Phylip (sequential)"};

	private SettingsModelString m_file   = new SettingsModelString(CFGKEY_FILE, "");
	private SettingsModelString m_format = new SettingsModelString(CFGKEY_FORMAT, "");
	private SettingsModelString m_type   = new SettingsModelString(CFGKEY_TYPE, SequenceType.AA.toString());
	
	
	protected AlignmentReaderNodeModel() {
		super(0,1);
	}
	
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
		MyDataContainer c = new MyDataContainer(exec.createDataContainer(new DataTableSpec(make_output_spec())), "Alignment");
	    
		File aln_file = new File(m_file.getStringValue());
		logger.info("Loading alignment(s) from "+aln_file.getAbsolutePath());
		List<String> plausible_formats = getPlausibleFormats(m_format.getStringValue(), aln_file);
		for (String format : plausible_formats) {
			boolean has_more = (!plausible_formats.get(plausible_formats.size()-1).equals(format));
			
			try {
				Alignment a = null;
				pal.datatype.DataType dt = m_type.getStringValue().equals("AA") ? new AminoAcids() : new Nucleotides();
				if (format.endsWith("FASTA")) {
					a = AlignmentReaders.readFastaSequences(new FileReader(aln_file), dt);
				} else if (format.startsWith("Clustal") || format.startsWith("Phylip")) {
					a = AlignmentReaders.readPhylipClustalAlignment(new FileReader(aln_file), dt);
				}
				
				DataCell[] cells = new DataCell[2];
				if (a != null) {
					cells[0] = AlignmentCellFactory.createCell(a);
				} else {
					cells[0] = DataType.getMissingCell();
				}
				cells[1] = new StringCell(aln_file.getAbsolutePath());
				c.addRow(cells);
				break; // if we get here then we succeeded so we dont need to try another format
			} catch (IOException e) {
				if (!has_more)
					throw e;
				logger.warn("Failed to load "+aln_file.getAbsolutePath()+" as format: "+format, e);
			}
		}
		return new BufferedDataTable[]{c.close()};
	}
	
	private List<String> getPlausibleFormats(String configured_format, File aln_file) {
		ArrayList<String> ret = new ArrayList<String>();
		if (configured_format.startsWith("Auto")) {
			for (int i=1; i<FILE_FORMATS.length; i++) {
				ret.add(FILE_FORMATS[i]);
			}
		} else {
			ret.add(configured_format);
		}
		
		return ret;
	}

	private DataColumnSpec[] make_output_spec() {
    	DataColumnSpec[] cols = new DataColumnSpec[2];
    	cols[0] = new DataColumnSpecCreator("Alignments", MultiAlignmentCell.TYPE).createSpec();
    	cols[1] = new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
    	
    	return cols;
	}
	
	 /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {

    	DataColumnSpec[]  cols= make_output_spec();
        return new DataTableSpec[]{new DataTableSpec(cols)};
    }
    
	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_file.saveSettingsTo(settings);
		m_format.saveSettingsTo(settings);
		m_type.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_file.validateSettings(settings);
		m_format.validateSettings(settings);
		m_type.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_file.loadSettingsFrom(settings);
		m_format.loadSettingsFrom(settings);
		m_type.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
	}

}
