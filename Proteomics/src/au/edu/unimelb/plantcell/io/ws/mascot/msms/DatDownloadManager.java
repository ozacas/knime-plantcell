package au.edu.unimelb.plantcell.io.ws.mascot.msms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.servers.mascotee.endpoints.DatFileService;

/**
 * Responsible for fetching DAT files from the MascotEE server
 * @author acassin
 *
 */
public class DatDownloadManager {
	public final static QName DATFILE_NAMESPACE=
			new QName("http://www.plantcell.unimelb.edu.au/bioinformatics/wsdl", "DatFileService");
	
	private NodeLogger logger;
	private String url;
	private String output_folder;
	private final ExecutionContext exec;
	
	public DatDownloadManager(final NodeLogger logger, final String url, final String output_folder, final ExecutionContext exec) {
		this.logger = logger;
		this.url = url;
		this.output_folder = output_folder;
		this.exec = exec;
	}
	
	public List<File> downloadDatFiles(final Collection<String> results_files) throws Exception {
		DatFileService dat_downloader = makeDatFileService();
		ArrayList<File> downloaded_files = new ArrayList<File>();
		for (String dat_file : results_files) {
			exec.checkCanceled();
			logger.info("Obtaining date for creation of "+dat_file);
			String dated_dat_file = dat_downloader.getDatedDatFilePath(dat_file);
			logger.info("Obtained full DAT path: "+dated_dat_file);
			if (dated_dat_file == null) {
				throw new Exception("Unable to download "+dat_file+" ... continuing anyway.");
			}
			
			DataHandler  dh = dat_downloader.getDatFile(dated_dat_file);
			OutputStream os = null;
			try {
				File output_file = new File(output_folder, dat_file);
				if (output_file.exists()) {
					throw new Exception("Will not overwrite existing: "+output_file.getAbsolutePath());
				}
				os = new FileOutputStream(output_file);
				dh.writeTo(os);
				logger.info("Saved to "+output_file.getAbsolutePath());
				downloaded_files.add(output_file);
			} finally {
				if (os != null) {
					os.close();
				}
			}
		}
		return downloaded_files;
	}
	
    private DatFileService makeDatFileService() throws MalformedURLException {
    	String u = this.url;
		if (u.endsWith("/")) {
			u += "DatFileService?wsdl";
		}
		Service      srv = Service.create(new URL(u), DATFILE_NAMESPACE);
		// MTOM will make an attachment greater than 1MB otherwise inline
		DatFileService dat = srv.getPort(DatFileService.class, new MTOMFeature(1024 * 1024));
		BindingProvider bp = (BindingProvider) dat;
        SOAPBinding binding = (SOAPBinding) bp.getBinding();
        if (!binding.isMTOMEnabled()) {
        	logger.warn("MTOM support is unavailable: may run out of java heap space");
        }
       
		return dat;
	}


}
