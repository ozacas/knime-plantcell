package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.expasy.jpl.io.ms.MSScan;
import org.expasy.jpl.io.ms.reader.MGFReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;



/**
 * Implements support for .mgf and .mgf.gz files using JavaProtLib. The
 * only data loaded into the table (in the spectra column!) is as follows:
 * BEGIN IONS
   TITLE=The first peptide - dodgy peak detection, so extra wide tolerance
   PEPMASS=896.05 25674.3
   CHARGE=3+
   TOL=3
   TOLU=Da
   SEQ=n-AC[DHK]s
   COMP=2[H]0[M]3[DE]*[K]
   240.1 3
   242.1 12
   245.2 32
   ...
 * query parameters, in particular are not currently supported.
 * 
 * @author andrew.cassin
 *
 */
public class MGFDataProcessor extends AbstractDataProcessor {
	private File m_file;
	private NodeLogger logger;
	
	public MGFDataProcessor(NodeLogger l) {
		m_file = null;
		logger = l;
	}

	@Override
	public boolean can(File f) throws Exception {
		m_file = f;
		String ext = f.getName().toLowerCase();
		return (ext.endsWith(".mgf") || ext.endsWith(".mgf.gz"));
	}

	@Override
	public void process(boolean load_spectra, ExecutionContext exec,
			MyDataContainer scan_container, MyDataContainer file_container)
			throws Exception {
		
		// examine first peaklist to determine what the internal state of the MGFReader should be for this file
		// HACK BUG FIXME TODO: should we look harder?
		
		// MGFReader assumes m/z are sorted in increasing m/z so we do that here
		MGFReader rdr = MGFReader.newInstance();
		File tmp_file = copy_and_sort_peak_list(m_file, rdr);
		
		// load peaklists from input file
		rdr.parse(tmp_file);
		
		Iterator<MSScan> it = rdr.iterator();
		int ncols = scan_container.getTableSpec().getNumColumns();
		int no_precursor = 0;
		
		try {
			while (it.hasNext()) {
				MSScan ms = it.next();
				
				Peak precursor = ms.getPrecursor();
				BasicPeakList bpl;
				if (precursor == null) {
					no_precursor++;
					bpl = new BasicPeakList(ms, 0.0d, 1);
				} else {
					bpl = new BasicPeakList(ms, precursor.getMz(), precursor.getCharge());
				}
				
				DataCell[] cells = missing_cells(ncols);
				cells[2]  = new StringCell(bpl.getRT_safe());
				cells[21] = new StringCell(m_file.getAbsolutePath());
				if (ncols > 23) {
				         cells[23] = SpectraUtilityFactory.createCell(bpl);
				}
				cells[22] = new IntCell(bpl.getNumPeaks());
				
				String pepmass = bpl.getPepmass_safe();
				if (pepmass != null)
				         cells[13] = new DoubleCell(Double.parseDouble(pepmass));
				else
				         cells[13] = DataType.getMissingCell();
				String charge = bpl.getCharge_safe();
				if (charge != null) {
				         charge    = charge.trim().replaceAll("\\+", "");
				         if (charge.length() > 0)
				                 cells[10] = new IntCell(Integer.parseInt(charge));
				         else
				                 cells[10] = DataType.getMissingCell();
				}
				cells[0]  = new StringCell(bpl.getTitle_safe());
				
				scan_container.addRow(cells);
			}
		} finally {
			// TODO BUG FIXME: anyone know how to get file closed by javaprotlib? 
			if (!tmp_file.delete()) {
				tmp_file.deleteOnExit();
				logger.debug("Could not delete temp file (delete on exit instead): "+tmp_file.getName());
			}
		}
		
		if (no_precursor > 0)
			NodeLogger.getLogger("MGF Data Reader").warn("Cannot find precursor peaks in "+no_precursor+" spectra, some data will be missing.");
	}
	
	/**
	 * We must copy MGF files before loading them due to a (bug) in the MGFReader class which refuses to load
	 * mgf's where the peaks are not sorted in increasing m/z... why no flag to sort in-memory?
	 * Also arranges to automatically number each scan if the MGF does not provide a suitable SCAN=... record for each peak list
	 * 
	 * @param in
	 * @param rdr alters the configuration of the reader as required to support the MGF data encountered
	 * @return a temporary file which must be deleted by the caller when no longer required. 
	 * @throws InvalidSettingsException 
	 * @throws IOException
	 */
	private File copy_and_sort_peak_list(File in, MGFReader rdr) throws IOException, InvalidSettingsException {
		assert(in != null && rdr != null);
		
		File tmp = File.createTempFile("temp_spectra", ".mgf");
		String line;
		BufferedReader br = null;
		PrintWriter pw = null;
		
		int n_scans = 0;
		int saw_scans = 0;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(in)));
			pw = new PrintWriter(new FileWriter(tmp));
			boolean in_peaklist = false;
			HashMap<Double,String> peaks = new HashMap<Double,String>();
			while ((line = br.readLine()) != null) {
				if (!in_peaklist) {
					pw.println(line);
					if (line.startsWith("BEGIN IONS")) {
						in_peaklist = true;
					} 
					continue;
				}
				
				if (in_peaklist && line.startsWith("END IONS")) {
					// sort current peak list by increasing m/z and then write to output (tmp) file
					ArrayList<Double> mz_order = new ArrayList<Double>(peaks.size());
					mz_order.addAll(peaks.keySet());
					Collections.sort(mz_order);
					// HACK TODO: how safe is it to rely on equality of doubles... hmmmm....?
					int wrote = 0;
					for (Double d : mz_order) {
						pw.println(peaks.get(d));
						wrote++;
					}
					
					if (wrote != mz_order.size()) 
						throw new InvalidSettingsException("Did not save all peaks: m/z duplicates?");
					peaks.clear();
					pw.println(line);
					in_peaklist = false;
					n_scans++;
					continue;
				}
				
				if (in_peaklist && line.startsWith("SCANS")) {
					saw_scans++;
				}
				
				if (line.matches("^[-+\\d].*$")) {
					line = line.trim();
					
					String mz = line.split("\\s+")[0];
					if (mz.startsWith("+"))
						mz = mz.substring(1);
					Double d_mz = Double.valueOf(mz);
					peaks.put(d_mz, line);
				} else {
					pw.println(line);
				}
			}
		} finally {
			if (pw != null)
				pw.close();
			if (br != null)
				br.close();
		}
		
		logger.info("Sorted (by increasing m/z) "+n_scans+" peak lists for "+in.getName()+", "+saw_scans+" had SCANS headers.");
		if (saw_scans != n_scans) {
			logger.warn("Automatically numbering peak lists in "+in.getName()+" as MGF data did not provide scan numbers (for every peak list)");
			rdr.enableAutoScanNum(true);
		} else {
			rdr.enableAutoScanNum(false);
		}
		return tmp;
	}

	@Override
	public void setInput(String filename) throws Exception {
		// nothing to do: handled by can()
	}

}
