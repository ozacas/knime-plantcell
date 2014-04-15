package au.edu.unimelb.plantcell.io.read.alignments;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentViewDataModel;
import au.edu.unimelb.plantcell.io.ws.multialign.MultiAlignmentCell;

/**
 * Loads a alignment file using java PAL
 * 
 * @author andrew.cassin
 *
 */
public class AlignmentReaderNodeModel extends NodeModel implements AlignmentViewDataModel {
    private final NodeLogger logger = NodeLogger.getLogger("Alignment Reader");
	
	public static final String CFGKEY_FILE    = "alignment-file";		// backward compatibility only... not used anymore
	
	public static final String CFGKEY_URL     = "alignment-url";		// now supports reading from http://... etc.
	public static final String CFGKEY_FORMAT  = "alignment-file-format";
	public static final String CFGKEY_TYPE    = "sequence-type";		// AA or nucleotide
	public static final String[] FILE_FORMATS = new String[] { "Automatic (guess)", "Aligned FASTA", "Clustal", "Phylip (interleaved)", "Phylip (sequential)"};

	public static final String[] SEQTYPES = new String[] { SequenceType.AA.toString(), SequenceType.Nucleotide.toString() };
	
	private SettingsModelString m_url    = new SettingsModelString(CFGKEY_URL, "");
	private SettingsModelString m_format = new SettingsModelString(CFGKEY_FORMAT, FILE_FORMATS[0]);
	private SettingsModelString m_type   = new SettingsModelString(CFGKEY_TYPE, SEQTYPES[0]);
	
	
	// not persisted (yet)
	private Map<String,AlignmentValue> m_alignments = new HashMap<String,AlignmentValue>();
	
	
	protected AlignmentReaderNodeModel() {
		super(0,1);
	}
	
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
		MyDataContainer c = new MyDataContainer(exec.createDataContainer(new DataTableSpec(make_output_spec())), "Alignment");
	    
		URL u = new URL(m_url.getStringValue());
		logger.info("Loading alignment(s) from "+u.toString());
		List<String> plausible_formats = getPlausibleFormats(m_format.getStringValue(), u);
		pal.datatype.DataType       dt = getSequenceType(m_type.getStringValue());
		logger.info("Treating sequences as "+dt.toString()+ " (configured "+m_type.getStringValue()+")");
		for (String format : plausible_formats) {
			boolean has_more = (!plausible_formats.get(plausible_formats.size()-1).equals(format));
			
			try {
				Alignment a = null;
				BufferedReader rdr = new BufferedReader(new InputStreamReader(u.openStream()));
				
				if (format.endsWith("FASTA")) {
					logger.info("Trying to load "+u.toString()+ " as Fasta (aligned).");
					a = AlignmentReaders.readFastaSequences(rdr, dt);
				} else if (format.startsWith("Clustal") || format.startsWith("Phylip")) {
					logger.info("Trying to load "+u.toString()+" as Phylip/Clustal.");
					a = AlignmentReaders.readPhylipClustalAlignment(rdr, dt);
				}
				
				DataCell[] cells = new DataCell[2];
				if (a != null) {
					cells[0] = AlignmentCellFactory.createCell(a);
				} else {
					cells[0] = DataType.getMissingCell();
				}
				cells[1] = new StringCell(u.toString());
				c.addRow(cells);
				if (cells[0] instanceof AlignmentValue) {
					m_alignments.put(c.lastRowID(), (AlignmentValue) cells[0]);
				}
				rdr.close();
				break; // if we get here then we succeeded so we dont need to try another format
			} catch (IOException e) {
				if (!has_more)
					throw e;
				logger.warn("Failed to load "+u.toString()+" as format: "+format, e);
			}
		}
		return new BufferedDataTable[]{c.close()};
	}
	
	/**
	 * Is the specified sequence type indicative of a nucleotide alignment or protein sequence (amino acids)?
	 * @param seqtype
	 * @return never null, but an instance from the PAL codebase reflecting the user configuration
	 */
	private pal.datatype.DataType getSequenceType(final String seqtype) {
		if (seqtype != null && (seqtype.equals("NA") || seqtype.toLowerCase().startsWith("Nucl"))) {
			return new Nucleotides();
		}
		return new AminoAcids();
	}

	/**
	 * Returns a list of likely file formats to try for the specified URL. 
	 * @param configured_format
	 * @param u
	 * @return each entry will have a value as prescribed by <code>FILE_FORMATS</code>
	 */
	private List<String> getPlausibleFormats(final String configured_format, final URL u) {
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
		m_url.saveSettingsTo(settings);
		m_format.saveSettingsTo(settings);
		m_type.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		if (settings.containsKey(CFGKEY_URL)) {
			m_url.validateSettings(settings);
		}
		m_format.validateSettings(settings);
		m_type.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		// backward compatibility? if so, convert to URL...
		if (settings.containsKey(CFGKEY_FILE)) {
			loadUrlFromFile(m_url, settings.getString(CFGKEY_FILE));
		} else {
			m_url.loadSettingsFrom(settings);
		}
		m_format.loadSettingsFrom(settings);
		m_type.loadSettingsFrom(settings);
	}
	
	/**
	 * Previous versions of KNIME PlantCell only supported reading from files. Now we use URL's for user convenience,
	 * but this means converting old File's to URL syntax for the node to use. This is the purpose of this method. The
	 * string'ified URL is stored in <code>dest</code>.
	 * 
	 * @param dest
	 * @param file_path
	 * @throws InvalidSettingsException
	 */
	private void loadUrlFromFile(final SettingsModelString dest, final String file_path) throws InvalidSettingsException {
		if (file_path == null || file_path.length() < 1)
			throw new InvalidSettingsException("Bogus file path for alignment!");
		
		try {
			File f = new File(file_path);
			dest.setStringValue(f.toURI().toURL().toString());
		} catch (MalformedURLException e) {
			throw new InvalidSettingsException(e.getMessage());
		}
	}

	@Override
	protected void reset() {
	}

	@Override
	public List<String> getAlignmentRowIDs() {
		ArrayList<String> ret = new ArrayList<String>();
		ret.addAll(m_alignments.keySet());
		return ret;
	}

	@Override
	public AlignmentValue getAlignment(final String row_id) {
		AlignmentValue av = m_alignments.get(row_id);
		return av;
	}

}
