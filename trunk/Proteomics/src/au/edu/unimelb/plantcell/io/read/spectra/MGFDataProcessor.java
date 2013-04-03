package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.BufferedReader;
import java.io.File;
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
	private BufferedReader m_is;
	private File m_file;
	
	public MGFDataProcessor() {
		m_is = null;
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
		
		// load peaklists from input file
		MGFReader rdr = MGFReader.newInstance();
		rdr.parse(m_file);
		
		Iterator<MSScan> it = rdr.iterator();
		int ncols = scan_container.getTableSpec().getNumColumns();
		int no_precursor = 0;
		
		while (it.hasNext()) {
			BasicPeakList mgf = new BasicPeakList(it.next());
			Peak precursor = mgf.getPrecursor();
			if (precursor == null) {
				no_precursor++;
			}
			DataCell[] cells = missing_cells(ncols);
			cells[21] = new StringCell(m_file.getAbsolutePath());
			if (ncols > 23) {
			         cells[23] = SpectraUtilityFactory.createCell(mgf);
			}
			cells[22] = new IntCell(mgf.getNumPeaks());
			
			String pepmass = mgf.getPepmass_safe();
			if (pepmass != null)
			         cells[13] = new DoubleCell(Double.parseDouble(pepmass));
			else
			         cells[13] = DataType.getMissingCell();
			String charge = mgf.getCharge_safe();
			if (charge != null) {
			         charge    = charge.trim().replaceAll("\\+", "");
			         if (charge.length() > 0)
			                 cells[10] = new IntCell(Integer.parseInt(charge));
			         else
			                 cells[10] = DataType.getMissingCell();
			}
			cells[0]  = new StringCell(mgf.getTitle_safe());
			
			scan_container.addRow(cells);
		}
		
		if (no_precursor > 0)
			NodeLogger.getLogger("MGF Data Reader").warn("Cannot find precursor peaks in "+no_precursor+" spectra, some processing will be disabled.");
		// file_container is mostly blank as MGF doesnt provide necessary data
		DataCell[] file_cells = missing_cells(file_container.getTableSpec().getNumColumns());
	}
	
	@Override
	public void setInput(String filename) throws Exception {
		// nothing to do: handled by can()
	}

}
