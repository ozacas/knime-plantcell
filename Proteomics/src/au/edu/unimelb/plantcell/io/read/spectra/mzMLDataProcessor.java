package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;


/**
 * Implements support for reading of mzML files using the controlled vocabulary. Only partial support
 * for the standard. But it should be adequate for most needs.
 * 
 * @author andrew.cassin
 *
 */
public class mzMLDataProcessor extends AbstractDataProcessor {
	private File m_file;
	private NodeLogger m_logger;
	private SpectrumListener m_sl;
	private boolean m_load_chromatogram;
	
	public mzMLDataProcessor(NodeLogger l) {
		assert(l != null);
		m_logger = l;
		m_sl = null;
		m_load_chromatogram = false;
	}
	
	public mzMLDataProcessor(NodeLogger l, SpectrumListener sl, boolean load_chromatogram) {
		this(l);
		assert(sl != null);
		m_sl = sl;
		m_load_chromatogram = load_chromatogram;
	}
	
	@Override
	public boolean can(File f) {
		m_file = f;
		String ext = f.getName().toLowerCase();
		
		if (ext.endsWith(".xml") || ext.endsWith(".mzml")) {
	        return true;
		}
		return false;
	}

	@Override
	public void process(boolean load_spectra, ExecutionContext exec, 
			MyDataContainer scan_container, MyDataContainer file_container) throws Exception {
		process_xml(exec, file_container, scan_container, m_file);
	}
	
	/**
	 * Parse the XML into the two output ports: one with per-file information, the other with the spectra.
	 * Here we parse using a fast stack-based XML approach to avoid instantiation of useless objects that have nothing to
	 * do with the final output. During parsing we handover to jmzml to create the necessary objects for decoding the input stream
	 * @throws XMLStreamException 
	 * @throws CanceledExecutionException 
	 * @throws IOException 
	 */
	public void process_xml(ExecutionContext exec, MyDataContainer o_f, MyDataContainer o_s, File input_file) throws Exception {
		Map<String,AbstractXMLMatcher> start_map = new HashMap<String,AbstractXMLMatcher>();
		start_map.put("spectrum", new SpectrumMatcher(m_sl));
		if (m_load_chromatogram)
			start_map.put("chromatogram", new ChromatogramMatcher(m_sl));
		BinaryMatcher bm = new BinaryMatcher();
		start_map.put("binary", bm);
		start_map.put("precursor", new PrecursorMatcher());
		start_map.put("selectedIon", new SelectedIonMatcher());
		start_map.put("binaryDataArray", new BinaryDataArrayMatcher());
		start_map.put("software", new SoftwareMatcher());
		start_map.put("source", new SourceMatcher());
		start_map.put("analyzer", new MassAnalyzerMatcher());
		start_map.put("detector", new MassDetectorMatcher());
		start_map.put("processingMethod", new ProcessingMethodMatcher());
		start_map.put("run", new RunMatcher());
		start_map.put("spectrumList", new SpectrumListMatcher());
		
		Map<String,AbstractXMLMatcher> end_map = new HashMap<String,AbstractXMLMatcher>();
		end_map.putAll(start_map);
		
		FileInputStream in = new FileInputStream(input_file);
		if (m_sl != null)
			m_sl.newFile(input_file);
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        XMLStreamReader parser = factory.createXMLStreamReader(in);
        Stack<AbstractXMLMatcher> object_stack = new Stack<AbstractXMLMatcher>();
        
        for (int event = parser.next();
                event != XMLStreamConstants.END_DOCUMENT;
                event = parser.next()) {
          /**
      	   * Cant call getLocalName() unless its an element so...
      	   */
      	  if (event != XMLStreamConstants.START_ELEMENT && event != XMLStreamConstants.END_ELEMENT) {
      		  // need to pass encoded binary data to binary matcher?
      		  if (event == XMLStreamConstants.CHARACTERS && !object_stack.isEmpty()) {
      			  object_stack.peek().addCharacters(parser.getText());
      		  }
      		  continue;
      	  }
      	 
      	  String localName = parser.getLocalName();
          if (event == XMLStreamConstants.START_ELEMENT) {
        	    if (start_map.containsKey(localName)) {
	        	    AbstractXMLMatcher xm = start_map.get(localName);
	        	    assert(xm != null);
	        		object_stack.push(xm);		// must be done BEFORE processElement()
	        		xm.processElement(m_logger, parser, object_stack);
        	    } else if (localName.equals("cvParam") && !object_stack.isEmpty()) {
        	    	XMLMatcher xm = object_stack.peek();
        	    	// TODO: check XML namespace?
        	    	String cvRef = parser.getAttributeValue(null, "cvRef");
        	    	String accession = parser.getAttributeValue(null, "accession");
        	    	String name = parser.getAttributeValue(null, "name");
        	    	String value= parser.getAttributeValue(null, "value");
        	    	String unitAccsn = parser.getAttributeValue(null, "unitAccession");
        	    	String unitName = parser.getAttributeValue(null, "unitName");
        	    	xm.addCVParam(value, name, accession, cvRef, unitAccsn, unitName);
        	    }
          } else if (event == XMLStreamConstants.END_ELEMENT && end_map.containsKey(localName)) {
        		XMLMatcher xm = object_stack.pop();
        		xm.save(m_logger, o_f, o_s, input_file);
        		if (xm instanceof SpectrumMatcher) {
        			exec.checkCanceled();
        		}
          }
        }
	}
	
	@Override
    public boolean finish() {
    	super.finish();
    	m_file = null;
    	return true;	
    }
    
	@Override
	public void setInput(String id) {
		// does nothing (m_filename is set by can() above)
	}

}
