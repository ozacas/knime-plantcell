package au.edu.unimelb.plantcell.io.ws.pfam;

import java.io.IOException;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;


public interface XMLMatcher {

		/**
		 * Called when processing a start element, this method should process the attributes and setup the matcher
		 * state for whatever is required later on. The output cells are passed in the <code>cells</code> parameter for use.
		 * @param l
		 * @param parser
		 * @param scope_stack
		 * @param cells
		 * @throws IOException
		 * @throws XMLStreamException
		 * @throws InvalidSettingsException
		 */
		public void processElement(NodeLogger l, XMLStreamReader parser, Stack<XMLMatcher> scope_stack, DataCell[] cells)
				throws IOException, XMLStreamException, InvalidSettingsException;

		/**
		 * Called at the end element, this routine must save any text/cdata as required by the element matcher
		 */
		public void saveText(String string, DataCell[] cells);
}
