package au.edu.unimelb.plantcell.proteomics.ppgroupxml;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * Save the peaklist to a spectra cell? Not currently implemented...
 * @author andrew.cassin
 *
 */
public class MSMSPeakListMatcher implements ProteinPilotMatcher {
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<ProteinPilotMatcher> scope_stack) throws IOException,
			XMLStreamException {
		// TODO Auto-generated method stub

	}

	@Override
	public void summary(NodeLogger logger) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hasMinimalMatchData() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void save(NodeLogger logger, MyDataContainer my_peptides,
			MyDataContainer my_proteins, MyDataContainer my_quant, File xml_file) {
	}

}
