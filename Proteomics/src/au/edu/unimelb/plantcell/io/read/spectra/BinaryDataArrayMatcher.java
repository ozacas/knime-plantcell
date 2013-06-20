package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import uk.ac.ebi.jmzml.model.mzml.BinaryDataArray;
import uk.ac.ebi.jmzml.model.mzml.BinaryDataArray.Precision;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * Loads an encoded binary data list into the parent (ie. spectrum) state. Code shamelessly borrowed from
 * jMZML's {@link http://code.google.com/p/jmzml/source/browse/trunk/src/main/java/uk/ac/ebi/jmzml/model/mzml/BinaryDataArray.java} class
 * 
 * @author andrew.cassin
 *
 */
public class BinaryDataArrayMatcher extends AbstractXMLMatcher {
	private final HashMap<String,String> m_accsn2name = new HashMap<String,String>();
	private final HashMap<String,String> m_name2value = new HashMap<String,String>();
	private AbstractXMLMatcher parent;
	private double[] values;
	private String m_encoded_length;
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<AbstractXMLMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
		m_accsn2name.clear();
		m_name2value.clear();
		parent = getParent(scope_stack);
		m_encoded_length = parser.getAttributeValue(null, "encodedLength");
	}
	
	/**
	 * Returns the encoded data format of the soon-to-be-processed <code>&lt;binary&gt;</code> XML element
	 * @return
	 */
	public BinaryDataArray.Precision getPrecision() {
		if (find(new String[] {"64-bit float", "MS:1000523"}) != null) {
			return Precision.FLOAT64BIT;
		} else if (find(new String[] {"32-bit float", "MS:1000521" }) != null) {
			return Precision.FLOAT32BIT;
		} else if (find(new String[] { "32-bit integer", "MS:1000519" }) != null) {
			return Precision.INT32BIT;
		} else if (find(new String[] { "64-bit integer", "MS:1000522" }) != null) {
			return Precision.INT64BIT;
		} else {
			return Precision.NTSTRING;
		}
	}
	
	/**
	 * Set values
	 */
	public void setValues(double[] val) {
		values = val;
	}
	
	/**
	 * Returns the encoded length attribute (-1 on invalid encoded length)
	 */
	public int getEncodedLength() {
		try {
			return Integer.valueOf(m_encoded_length);
		} catch (Exception e) {
			return -1;
		}
	}
	
	/**
	 * Encoded binary data need uncompressing?
	 */
	public boolean needsUncompressing() {
		if (find(new String[] { "zlib compression", "MS:1000574"}) != null)
			return true;
		else if (find(new String[] { "no compression", "MS:1000576"}) != null)
			return false;
		
		// HACK TODO FIXME: assume false? or throw for missing required parameter?
		return false;
	}
	
	@Override
	public boolean hasMinimalMatchData() {
		return (parent != null && parent instanceof SpectrumMatcher && values != null);
	}
	
	protected DataCell find(String[] items) {
		for (String s : items) {
			if (m_accsn2name.containsKey(s) && m_name2value.containsKey(m_accsn2name.get(s))) {
				return new StringCell(m_name2value.get(m_accsn2name.get(s)));
			} else if (m_name2value.containsKey(s)) {
				return new StringCell(m_name2value.get(s));
			}
		}
		return null;
	}
	
	/**
	 * For now, this method only supports m/z, intensity and time (ie. retention time) vectors
	 * 
	 * @return true if array of m/z values, otherwise intensity values
	 */
	private BinaryDataType getDataType() {
		if (find(new String[] { "MS:1000514", "m/z array" }) != null)
			return BinaryDataType.MZ_TYPE;
		else if (find(new String[] { "MS:1000515", "intensity array" }) != null) {
			return BinaryDataType.INTENSITY_TYPE;
		} else if (find(new String[] { "MS:1000595", "time array" }) != null) {
			return BinaryDataType.TIME_TYPE;
		} else {
			return BinaryDataType.UNKNOWN_TYPE;
		}
	}
	
	@Override
	public void save(NodeLogger logger, MyDataContainer file_container,
			MyDataContainer scan_container, File xml_file) {
		if (hasMinimalMatchData()) {
			((SpectrumMatcher)parent).setBinaryData(getDataType(), values);
		}
	}
	
	@Override
	public void addCVParam(final String value, final String name, final String accession, final String cvRef, final String unitAccsn, final String unitName) throws Exception {
		if (m_accsn2name.containsKey(accession))
			throw new Exception("Duplicate accession: "+accession);
		m_accsn2name.put(accession, name);
		m_name2value.put(name, value);
	}
}
