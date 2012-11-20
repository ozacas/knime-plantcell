package au.edu.unimelb.plantcell.proteomics.ppgroupxml;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * Matches the &lt;PROTEIN2MATCH&gt; element 
 * 
 * @author andrew.cassin
 *
 */
public class P2MMatcher implements ProteinPilotMatcher {

	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<ProteinPilotMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
	}

	@Override
	public void summary(NodeLogger logger) {
	}

	@Override
	public boolean hasMinimalMatchData() {
		return false;
	}

	@Override
	public void save(NodeLogger logger, MyDataContainer my_peptides,
			MyDataContainer my_proteins, MyDataContainer my_quant, File xml_file) {
		
	}

}
