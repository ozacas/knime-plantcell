package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;
import java.util.Iterator;

import org.expasy.jpl.io.ms.MassSpectrum;
import org.expasy.jpl.io.ms.reader.DTAReader;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * A file format made popular by Sequest, since the file format has only
 * a single spectra per file we must be ready to load a lot of files...
 * Very simplistic support and incomplete. Anyone?
 * 
 * @author andrew.cassin
 *
 */
public class DTADataProcessor extends AbstractDataProcessor {
	private File m_file;
	private NodeLogger logger;
	
	public DTADataProcessor(NodeLogger l) {
		logger = l;
	}
	
	@Override
	public void setInput(String id) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean can(File f) throws Exception {
		m_file = f;
		String ext = f.getName().toLowerCase();
		return (ext.endsWith(".dta") || ext.endsWith(".dta.gz"));
	}

	@Override
	public void process(boolean load_spectra, ExecutionContext exec,
			MyDataContainer scan_container, MyDataContainer file_container)
			throws Exception {
		DTAReader rdr = DTAReader.newInstance();
		rdr.parse(m_file);
		Iterator<MassSpectrum> it = rdr.iterator();
		int no_precursor = 0;
		int ncols = scan_container.getTableSpec().getNumColumns();
		
		while (it.hasNext()) {
			MassSpectrum ms = it.next();
			
			BasicPeakList bpl = new BasicPeakList(ms);
			if (ms.getPeakList().getPrecursor() == null) {
				no_precursor++;
			}
			
			DataCell[] cells = missing_cells(ncols);
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
		
		if (no_precursor > 0) {
			logger.info("Precursor peak not specified in "+m_file.getName());
		}
	}

}
