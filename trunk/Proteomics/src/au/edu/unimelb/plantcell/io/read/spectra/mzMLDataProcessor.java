package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;

import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

import uk.ac.ebi.jmzml.model.mzml.Spectrum;
import uk.ac.ebi.jmzml.xml.io.MzMLObjectIterator;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;
import au.edu.unimelb.plantcell.core.MyDataContainer;


/**
 * Implements support for mzML spectra and scan/file statistics, based on the 
 * jMZML library: http://code.google.com/p/jmzml/. This library supports visual display 
 * of spectra/chromatograms which this node will soon offer (for mzML files only) 
 * 
 * @author andrew.cassin
 *
 */
public class mzMLDataProcessor extends AbstractDataProcessor {
	private File m_file;
	
	public mzMLDataProcessor(NodeLogger l) {
		assert(l != null);
	}
	
	@Override
	public boolean can(File f) {
		m_file = f;
		String ext = f.getName().toLowerCase();
		
		if (ext.endsWith(".xml") || ext.endsWith(".mzml")) {
	        return true;
		}
		return false;
	}

	@Override
	public void process(boolean load_spectra, ExecutionContext exec, 
			MyDataContainer scan_container, MyDataContainer file_container) throws Exception {
	
		// first output the file port
		process_file(exec, file_container, new MzMLUnmarshaller(m_file));
		
		// now output the scan port
		process_scans(exec, scan_container, new MzMLUnmarshaller(m_file));
	}
	
	protected void process_scans(ExecutionContext exec, MyDataContainer scan_container, MzMLUnmarshaller rdr) throws Exception {
		
		int no_precursor = 0;
		int n_cols = scan_container.getTableSpec().getNumColumns();
		
		MzMLObjectIterator<Spectrum> it = rdr.unmarshalCollectionFromXpath("/run/spectrumList/spectrum", Spectrum.class);
		while (it.hasNext()) {
			BasicPeakList bpl = new BasicPeakList(it.next());
			Peak precursor = bpl.getPrecursor();
			if (precursor == null) {
				no_precursor++;
			}
			DataCell[] cells = missing_cells(n_cols);
			cells[21] = new StringCell(m_file.getAbsolutePath());
			if (n_cols > 23) {
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
		
		if (no_precursor > 0)
			NodeLogger.getLogger("mzML Data Reader").warn("Cannot find precursor peaks in "+no_precursor+" spectra, some processing will be disabled.");
	}
	
	/**
	 * Report the file summary
	 * @param exec
	 * @param fc
	 * @param file_seq
	 * @param fh
	 * @throws Exception
	 */
	protected void process_file(ExecutionContext exec, MyDataContainer fc, MzMLUnmarshaller rdr) throws Exception {
    	
    }

    @Override
    public boolean finish() {
    	super.finish();
    	m_file = null;
    	return true;	
    }
    
	@Override
	public void setInput(String id) {
		// does nothing (m_filename is set by can() above)
	}

}
