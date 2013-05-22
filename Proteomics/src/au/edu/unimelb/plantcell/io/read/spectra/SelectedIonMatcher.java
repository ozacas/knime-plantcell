package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.expasy.jpl.core.ms.spectrum.peak.PeakImpl;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;


/**
 * Matches the <selectedIon> element from the mzML (inside the <precursor> element)
 * 
 * @author andrew.cassin
 *
 */
public class SelectedIonMatcher extends AbstractXMLMatcher {
	private AbstractXMLMatcher parent;
	private String mz, charge_state, intensity;
	private List<String> possible_charge_states = new ArrayList<String>();
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<AbstractXMLMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
		parent       = getParent(scope_stack);
		mz           = null;
		charge_state = null;
		intensity    = null;
		possible_charge_states.clear();
	}

	@Override
	public boolean hasMinimalMatchData() {
		return (parent != null && parent instanceof PrecursorMatcher && mz != null);
	}
	
	@Override
	public void save(NodeLogger logger, MyDataContainer file_container,
			MyDataContainer scan_container, File xml_file) {
		if (hasMinimalMatchData()) {
			try {
				PrecursorMatcher pm = (PrecursorMatcher) parent;
				PeakImpl.Builder p = new PeakImpl.Builder(Double.valueOf(mz));
				if (intensity != null)
					p.intensity(Double.valueOf(intensity));
				if (charge_state != null) 
					p.charge(Integer.valueOf(charge_state));
				pm.addSelectedIon(p.build());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void addCVParam(String value, String name, String accession, String cvRef) throws Exception {
		if (accession.equals("MS:1000744")) 
			mz = value;
		else if (accession.equals("MS:1000041"))
			charge_state = value;
		else if (accession.equals("MS:1000042"))
			intensity = value;
		else if (accession.equals("MS:1000633"))
			possible_charge_states.add(value);
	}
}
