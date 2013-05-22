package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

public abstract class AbstractXMLMatcher implements XMLMatcher {

	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<AbstractXMLMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
	}

	@Override
	public void summary(NodeLogger logger) {
	}

	@Override
	public boolean hasMinimalMatchData() {
		return true;
	}

	/**
	 * Invoked when the end of the element is reached to save relevant state to the output container(s)
	 */
	@Override
	public void save(NodeLogger logger, MyDataContainer file_container,
			MyDataContainer scan_container, File xml_file) {
	}

	@Override
	public void addCVParam(String value, String name, String accession, String cvRef) throws Exception {
		// NO-OP in base class, subclasses to override
	}
	
	protected DataCell[] missing(DataTableSpec inSpec) {
		DataCell[] ret = new DataCell[inSpec.getNumColumns()];
		Arrays.fill(ret, DataType.getMissingCell());
		return ret;
	}
	
	/**
	 * Return the parent of <code>this</code> within the context of the specified stack of matching scopes
	 * 
	 * @param scope_stack
	 * @return
	 */
	protected AbstractXMLMatcher getParent(Stack<AbstractXMLMatcher> scope_stack) {
		AbstractXMLMatcher xm = scope_stack.peek();
		if (xm == this) {
			int n = scope_stack.size();
			return (n>1) ? scope_stack.get(n-2) : null;
		} else {
			return xm;
		}
	}

	/**
	 * Implementations must not hold a reference to <code>text</code> after the call completes
	 * @param text
	 */
	public void addCharacters(String text) {
		// NO-OP
	}
}
