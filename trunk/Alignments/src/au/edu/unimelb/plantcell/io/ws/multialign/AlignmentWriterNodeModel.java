package au.edu.unimelb.plantcell.io.ws.multialign;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
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

import au.edu.unimelb.plantcell.io.ws.multialign.FormattedRenderer.FormatType;



/**
 * This is the model implementation of AlignmentWriter.
 * Saves one or more alignments to disk (ie. AlignmentCell's). The filename comes from the RowID
 * with a suitable extension for the desired format (.aln, .clustalw etc.)
 *
 * @author Andrew Cassin
 */
public class AlignmentWriterNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Alignment Writer");
        
    static final String CFGKEY_FORMAT = "alignment-format";
    static final String CFGKEY_COLUMN = "alignment-column";
    static final String CFGKEY_FOLDER = "destination-folder";
    
    private final SettingsModelString m_format = new SettingsModelString(CFGKEY_FORMAT, "Clustal");
    private final SettingsModelString m_column = new SettingsModelString(CFGKEY_COLUMN, "Alignment");
    private final SettingsModelString m_folder = new SettingsModelString(CFGKEY_FOLDER, "c:/temp");

    /**
     * Constructor for the node model.
     */
    protected AlignmentWriterNodeModel() {
        super(1, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	RowIterator it = inData[0].iterator();
    	int col_idx = inData[0].getDataTableSpec().findColumnIndex(m_column.getStringValue());
    	if (col_idx < 0) {
    		throw new Exception("Cannot find column: "+m_column.getStringValue()+", re-configure the node?");
    	}
    	while (it.hasNext()) {
    		DataRow         r = it.next();
    		DataCell aln_cell = r.getCell(col_idx);
    		String basename   = r.getKey().getString();
    		
    		if (aln_cell == null || aln_cell.isMissing()) {
    			logger.warn("Skipping missing alignment for row: "+r.getKey().getString());
    			continue;
    		}
    		
    		if (aln_cell instanceof MultiAlignmentCell) {
    			MultiAlignmentCell ac = (MultiAlignmentCell) aln_cell;
    			String format = m_format.getStringValue().toLowerCase();
    			save_alignment(new File(m_folder.getStringValue(), getFileName(basename, format)), ac, format);
    		}
    	}
    	return null;
    }

    /**
     * Responsible for choosing an appropriate filename to save the alignment to based on the alignment format chosen
     * @param basename
     * @param format
     * @return
     */
    private String getFileName(String basename, String format) {
		if (format.startsWith("fasta")) {
			return basename+".fasta";
		} else {
			return basename+".aln";
		}
	}

	/**
     * Saves the specified alignment cell (instance of MultiAlignmentCell) in the specified file, with the specified format.
     * An error is thrown if the alignment is not valid for the chosen format or cannot be saved for any reason
     * 
     * @param file			   file to save to 
     * @param ac			   cell to save
     * @param alignment_format must be in all lowercase eg. clustal, nexus etc...
     * @throws IOException	   thrown upon exception eg. disk full
     */
    private void save_alignment(final File file, final MultiAlignmentCell ac, final String alignment_format) throws IOException, UnsupportedAlignmentException  {
    	FormatType ft = null;
    	if (alignment_format.startsWith("clustal")) {
    		ft = FormattedRenderer.FormatType.F_CLUSTALW;
    	}
    	if (alignment_format.startsWith("fasta")) {
    		ft = FormattedRenderer.FormatType.F_FASTA;
    	}
    	if (ft != null) {
    		String txt = ac.getFormattedAlignment(ft);
    		PrintWriter pw = new PrintWriter(new FileOutputStream(file));
    		pw.print(txt);
    		pw.close();
    	} else {
    		throw new UnsupportedAlignmentException("Unsupported alignment format "+alignment_format);
    	}
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
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_format.saveSettingsTo(settings);
    	m_column.saveSettingsTo(settings);
    	m_folder.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_format.loadSettingsFrom(settings);
    	m_column.loadSettingsFrom(settings);
    	m_folder.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_format.validateSettings(settings);
    	m_column.validateSettings(settings);
    	m_folder.validateSettings(settings);
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

