package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * Responsible for parsing the <precursor> element from the mzML
 * 
 * @author andrew.cassin
 *
 */
public class PrecursorMatcher extends AbstractXMLMatcher {
	private static final HashSet<String> ok_methods = new HashSet<String>();
	static {
		ok_methods.add("MS:1000133");
		ok_methods.add("MS:1000134");
		ok_methods.add("MS:1000135");
		ok_methods.add("MS:1000136");
		ok_methods.add("MS:1000242");
		ok_methods.add("MS:1000250");
		ok_methods.add("MS:1000262");
		ok_methods.add("MS:1000282");
		ok_methods.add("MS:1000422");
		ok_methods.add("MS:1000433");
	};
	private final List<String> dissociation_methods = new ArrayList<String>();
	private final List<Peak> selected_ions = new ArrayList<Peak>();
	private AbstractXMLMatcher parent;
	
	public PrecursorMatcher() {
	}
	
	@Override
	public void processElement(NodeLogger l, XMLStreamReader parser,
			Stack<AbstractXMLMatcher> scope_stack) throws IOException,
			XMLStreamException, InvalidSettingsException {
		dissociation_methods.clear();
		parent = getParent(scope_stack);
	}
	
	public void addSelectedIon(Peak p) {
		assert(p != null);
		selected_ions.add(p);
	}
	
	@Override
	public boolean hasMinimalMatchData() {
		return (parent != null && parent instanceof SpectrumMatcher && dissociation_methods.size() > 0);
	}
	
	@Override
	public void save(NodeLogger logger, MyDataContainer file_container,
			MyDataContainer scan_container, File xml_file) {
		// give fully formed precursor peak to spectrum to work with
		if (hasMinimalMatchData()) {
			SpectrumMatcher sm = (SpectrumMatcher) parent;
			for (Peak p : selected_ions) {
				sm.addPrecursor(p);
			}
		}
	}
	
	@Override
	public void addCVParam(String value, String name, String accession, String cvRef) throws Exception {
		if (ok_methods.contains(accession))
			dissociation_methods.add(name);
	}
}
