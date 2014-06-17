package au.edu.unimelb.plantcell.io.ws.mascot.config;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.activation.DataHandler;
import javax.xml.ws.Service;
import javax.xml.ws.soap.MTOMFeature;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.read.fasta.FastaIterator;
import au.edu.unimelb.plantcell.servers.mascotee.endpoints.ConfigService;

/**
 * Node to download mascot sequence databases
 * @author acassin
 *
 */
public class DownloadDatabaseNodeModel extends ShowConfigNodeModel {
	private NodeLogger logger = NodeLogger.getLogger("Download mascot database");
	
	public static final String CFGKEY_OUT       = "output-fasta";
	public static final String CFGKEY_MASCOT_DB = "mascot-db-to-download";

	private final SettingsModelString m_out_fasta = new SettingsModelString(CFGKEY_OUT, "");
	private final SettingsModelString m_db        = new SettingsModelString(CFGKEY_MASCOT_DB, "");
	
	public DownloadDatabaseNodeModel() {
		super();
	}
	
	@Override
	protected DataTableSpec make_output_spec() {
		DataColumnSpec[] cols = new DataColumnSpec[2];
		cols[0] = new DataColumnSpecCreator("Sequence", SequenceCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
		return new DataTableSpec(cols);
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	DataTableSpec outSpec = make_output_spec();
    	MyDataContainer     c = new MyDataContainer(exec.createDataContainer(outSpec), "Seq");
    	
    	// 1. download database via webservice and store in the user configured path (will throw if it already exists)
    	FileOutputStream fos = null;
    	File fasta = null;
    	boolean is_aa = true;
    	try {
	    	Service srv = ShowConfigNodeModel.getConfigService(getURL());
	    	ConfigService configService = srv.getPort(ConfigService.class, new MTOMFeature());
	        
	        logger.info("Downloading "+m_db.getStringValue()+" database from "+getURL()); 
	    	String url = configService.getDatabaseSequenceURL(m_db.getStringValue(), 0);
	    	is_aa = configService.isDatabaseAA(m_db.getStringValue());
	    	if (url == null) {
	    		throw new Exception("No mascot data available!");
	    	}
	    	logger.info("Downloading database using "+url);
	    	Path p = Paths.get(m_out_fasta.getStringValue());
	    	fasta = p.toFile();
	    	fos = new FileOutputStream(fasta);
	    	DataHandler dh = new DataHandler(new URL(url));
	    	dh.writeTo(fos);
	    	logger.info("Downloaded "+fasta.length()+" bytes from server.");
    	} catch (Exception e) {
    		e.printStackTrace();
    		throw e;
    	} finally {
    		if (fos != null) {
    			fos.close();
    		}
    	}
    	exec.setProgress(0.5);
    	
    	// 2. load downloaded file into output table
    	logger.info("Loading sequences into output table... ");
    	FastaIterator it = new FastaIterator(fasta, is_aa ? SequenceType.AA : SequenceType.DNA);
    	while (it.hasNext()) {
    		SequenceValue sv = it.next();
    		DataCell[] cells = new DataCell[2];
    		cells[0] = new SequenceCell(sv);
    		cells[1] = new StringCell(fasta.getAbsolutePath());
    		c.addRow(cells);
    	}
    	
    	// all done
    	return new BufferedDataTable[] { c.close() };
    }
    
	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		super.saveSettingsTo(settings);
		m_out_fasta.saveSettingsTo(settings);
		m_db.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.validateSettings(settings);
		m_out_fasta.validateSettings(settings);
		m_db.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		super.loadValidatedSettingsFrom(settings);
		m_out_fasta.loadSettingsFrom(settings);
		m_db.loadSettingsFrom(settings);
	}
}
