package au.edu.unimelb.plantcell.servers.proteowizard;

import java.io.File;
import java.io.IOException;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.ws.soap.MTOM;



/**
 * Bean class to represent a RawFile as supplied from the vendor's instrument or
 * a file containing the converted (mzML) data. This is used in the webservice interface,
 * so be careful of changes here
 * 
 * @author andrew.cassin
 *
 */
@XmlAccessorType
@MTOM
public class RawFile {
	private String      m_name;
	private long m_length = -1;
	@XmlMimeType("application/octet-stream")
    private DataHandler m_dh;
	
	public RawFile() {
		setName("");
		setLength(-1);
		m_dh = null;
	}
	
	/**
	 * Convenience constructor which permits people to create a fully initialised rawfile instance from a File
	 * @param f must not be null
	 */
	public RawFile(File f) throws IOException {
		setName(f.getName());
		setLength(f.length());
		if (!f.exists())
			throw new IOException("No such file: "+f.getAbsolutePath());
		if (!f.canRead())
			throw new IOException("No permission to read: "+f.getAbsolutePath());
		setDH(new DataHandler(new FileDataSource(f)));
	}
	
	/**
	 * Name for the file (eg. mysample.raw)
	 * @return
	 */
	public String getName() {
		return m_name;
	}
	
	public void setName(String new_name) {
		m_name = new_name;
	}
	
	/**
	 * Length of the data stream (in bytes) or -1 if not known
	 * @return
	 */
	public long getLength() {
		return m_length;
	}
	
	public void setLength(long new_len) {
		m_length = new_len;
	}
	
	public void setDH(DataHandler dh) {
		m_dh = dh;
	}
	
	public DataHandler getDH() {
		return m_dh;
	}
}
