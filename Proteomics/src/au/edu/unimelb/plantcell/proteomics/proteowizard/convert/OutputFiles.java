package au.edu.unimelb.plantcell.proteomics.proteowizard.convert;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.io.read.spectra.AbstractDataProcessor;
import au.edu.unimelb.plantcell.io.read.spectra.MGFDataProcessor;
import au.edu.unimelb.plantcell.io.read.spectra.mzMLDataProcessor;
import au.edu.unimelb.plantcell.io.read.spectra.mzXMLDataProcessor;

/**
 * An abstract to capture result files from a Mass Spec data conversion. Contains useful support methods to
 * enable nodes working with this data
 * 
 * @author acassin
 *
 */
public class OutputFiles implements Iterable<File> {
	private Map<File,OutputFileFormat> m_files = new HashMap<File,OutputFileFormat>();
	
	public OutputFiles() {
	}
	
	public OutputFiles(List<File> savedFiles) {
		addAll(savedFiles);
	}

	@Override
	public Iterator<File> iterator() {
		return m_files.keySet().iterator();
	}
	
	public int size() {
		return m_files.size();
	}
	
	public void clear() {
		m_files.clear();
	}
	
	public void addAll(Collection<File> files) {
		for (File f : files) {
			add(f);
		}
	}
	
	public boolean has(OutputFileFormat format) {
		assert(format != null);
		for (File f : m_files.keySet()) {
			if (format.equals(m_files.get(f))) {
				return true;
			}
		}
		return false;
	}
	
	public void add(File f) {
		if (f != null) {
			m_files.put(f, guessFileFormat(f));
		}
	}

	/**
	 * Filters the current files in this using the specified {@link OUtputFileFilter}. A new instance with only
	 * the accepted files is returned.
	 * 
	 */
	public OutputFiles filter(final OutputFileFilter filter) {
		assert(filter != null);
		OutputFiles ret = new OutputFiles();
		filterAndAccumulateTo(ret, filter);
		return ret;
	}
	
	/**
	 * Attempts to guess the file format, currently only based on the file extension (but may change in future)
	 * @param f
	 * @return a non-null value from OutputFileFormat
	 */
	private OutputFileFormat guessFileFormat(File f) {
		if (f == null) {
			return OutputFileFormat.UNKNOWN;
		}
		String lc = f.getName().toLowerCase().trim();
		if (lc.endsWith(".mgf")) {
			return OutputFileFormat.MGF;
		} else if (lc.endsWith(".mzml")) {
			return OutputFileFormat.MZML;
		} else if (lc.endsWith(".xml") || lc.endsWith(".mzxml")) {
			return OutputFileFormat.MZXML;
		} else if (lc.endsWith(".raw")) {
			return OutputFileFormat.THERMO_RAW;
		} else if (lc.endsWith(".wiff") || lc.endsWith(".wiff.scan")) {
			return OutputFileFormat.ANALYST_WIFF;
		} else {
			return OutputFileFormat.UNKNOWN;
		}
	}

	/**
	 * Similar to {@link #filter(OutputFileFilter)} but this method adds the accepted files to <code>entries</code>
	 * rather than returning a new instance
	 * 
	 * @param entries
	 * @param outputFileFilter
	 */
	public void filterAndAccumulateTo(final OutputFiles entries, final OutputFileFilter filter) {
		assert(entries != null && filter != null);
		ArrayList<File> files_accepted = new ArrayList<File>();
		for (File f : m_files.keySet()) {
			if (filter.accept(f, m_files.get(f))) {
				files_accepted.add(f);
			}
		}
		entries.addAll(files_accepted);
	}

	public OutputFiles filterByDataProcessor(final NodeLogger logger, 
			final OutputFileFormat[] permitted_formats) throws InvalidSettingsException {
		OutputFiles ret = new OutputFiles();
		
	    if (permitted_formats.length == 0) {
        	throw new InvalidSettingsException("No file formats permitted -- check configuration?");
        }
		filterAndAccumulateTo(ret, new OutputFileFilter() {

			@Override
			public boolean accept(final File f, final OutputFileFormat expected_data_format_for_f) {
				List<AbstractDataProcessor> dp_list = getDataProcessors(f, logger, permitted_formats);

				for (AbstractDataProcessor dp : dp_list) {
					try {
						if (dp.can(f)) {
							return true;
						}
					} catch (Exception e) {
						// be silent since we will return false in the worst case
					}
				}
				return false;
			}
			
		});
		return ret;
	}

	public List<AbstractDataProcessor> getDataProcessors(File f, NodeLogger logger) {
		try {
			return getDataProcessors(f, logger, OutputFileFormat.supportedFormats());
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<AbstractDataProcessor>();		// something's gone wrong we assume file cant be processed at all
		}
	}
	
	/**
	 * Return a list of data processors which can handle the specified file format. Usually 1 but may be more
	 * than one if the file format has different data processors depending on the version or some other technical challenge.
	 * 
	 * @param f
	 * @param logger
	 * @param permittedFormats
	 * @return
	 * @throws Exception usually if a file is in a weird state eg. empty or corrupted and the file handlers cannot tell what is wrong
	 */
	public List<AbstractDataProcessor> getDataProcessors(File f, NodeLogger logger,
			OutputFileFormat[] permittedFormats) {
		List<AbstractDataProcessor> ret = new ArrayList<AbstractDataProcessor>();
		HashSet<OutputFileFormat> allowed = new HashSet<OutputFileFormat>();
		for (OutputFileFormat format : permittedFormats) {
			allowed.add(format);
		}
		
		// data processors are only returned if they can operate on the specified file
		try {
			if (allowed.contains(OutputFileFormat.MGF)) {
				addIfCapableProcessor(ret, new MGFDataProcessor(logger), f);
			}
			if (allowed.contains(OutputFileFormat.MZML)) {
				addIfCapableProcessor(ret, new mzMLDataProcessor(logger, null, false), f);
			}
			if (allowed.contains(OutputFileFormat.MZXML)) {
				addIfCapableProcessor(ret, new mzXMLDataProcessor(logger), f);
			}
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<AbstractDataProcessor>();
		}
	}

	private void addIfCapableProcessor(final List<AbstractDataProcessor> ret, final AbstractDataProcessor dp, final File f) throws Exception {
		assert(ret != null && dp != null);
		
		if (dp.can(f)) {
			ret.add(dp);
		}
	}
}
