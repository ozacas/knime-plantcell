package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Stack;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.xmlbeans.impl.util.Base64;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import uk.ac.ebi.jmzml.model.mzml.BinaryDataArray;
import uk.ac.ebi.jmzml.model.mzml.BinaryDataArray.Precision;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * Responsible for decoding binary data. Code shamelessly borrowed from jMZML
 * 
 * @author andrew.cassin
 *
 */
public class BinaryMatcher extends AbstractXMLMatcher {
	private StringBuilder sb = new StringBuilder(100 * 1024);
	private AbstractXMLMatcher parent;
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<AbstractXMLMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
		parent = getParent(scope_stack);
		
	}
	
	@Override
	public boolean hasMinimalMatchData() {
		return (sb.length() > 0 && parent != null);
	}
	
	@Override
	public void save(NodeLogger logger, MyDataContainer file_container,
			MyDataContainer scan_container, File xml_file) {
		if (hasMinimalMatchData() && parent instanceof BinaryDataArrayMatcher) {
			BinaryDataArrayMatcher bdam = (BinaryDataArrayMatcher) parent;
			try {
				double[] decoded = convertData(bdam.getPrecision(), bdam.needsUncompressing(), sb.toString());
				bdam.setValues(decoded);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		sb = new StringBuilder(100 * 1024);
	}
	
	private double[] convertData(BinaryDataArray.Precision prec, boolean decompress, String encoded_text) throws IOException,DataFormatException,UnsupportedEncodingException {
		byte[] bytes = Base64.decode(encoded_text.getBytes("ASCII"));
		if (decompress) {
			bytes = decompress(bytes);
		}
		return convertData(prec, bytes);
	}
	
	private byte[] decompress(byte[] bytes) throws DataFormatException,IOException {
		ByteArrayOutputStream bos = null;
		try {
			Inflater i = new Inflater();
			i.setInput(bytes);
			bos = new ByteArrayOutputStream(bytes.length);
			byte[] buf = new byte[10*1024];
			while (!i.finished()) {
				int count = i.inflate(buf);
				bos.write(buf, 0, count);
			}
			return bos.toByteArray();
		} finally {
			if (bos != null)
				bos.close();
		}
	}

	/**
	 * Copies data on the way out so as not to present any problems holding onto a reference to the bytebuffer
	 * @param prec
	 * @param bytes
	 * @return
	 */
	private double[] convertData(Precision prec, byte[] bytes) {
		ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		double[] ret;
		
		switch (prec) {
		case FLOAT64BIT:
			DoubleBuffer db = bb.asDoubleBuffer().asReadOnlyBuffer();
			ret = new double[db.limit()];
			db.get(ret);
			return ret;
			
		case FLOAT32BIT:
			FloatBuffer fb = bb.asFloatBuffer();
			float[]    arr = new float[fb.limit()];
			fb.get(arr);
			ret = new double[arr.length];
			for (int i=0; i<arr.length; i++) {
				ret[i] = arr[i];
			}
			return ret;
			
		case INT32BIT:
			IntBuffer ib = bb.asIntBuffer();
			int[] ia = new int[ib.limit()];
			ret = new double[ia.length];
			for (int i=0; i<ia.length; i++) {
				ret[i] = ia[i];
			}
			return ret;
			
		case INT64BIT:
			LongBuffer lb = bb.asLongBuffer();
			long[] la = new long[lb.limit()];
			ret = new double[la.length];
			for (int i=0; i<la.length; i++) {
				ret[i] = la[i];
			}
			return ret;
		
		default:
			return null;
		}
	}

	@Override
	public void addCharacters(String text) {
		sb.append(text);
	}
}
