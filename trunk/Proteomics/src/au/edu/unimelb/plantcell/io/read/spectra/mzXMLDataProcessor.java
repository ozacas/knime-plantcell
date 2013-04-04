package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;

import org.expasy.jpl.io.ms.jrap.DataProcessingInfo;
import org.expasy.jpl.io.ms.jrap.MSInstrumentInfo;
import org.expasy.jpl.io.ms.jrap.MSOperator;
import org.expasy.jpl.io.ms.jrap.MSXMLParser;
import org.expasy.jpl.io.ms.jrap.MZXMLFileInfo;
import org.expasy.jpl.io.ms.jrap.Scan;
import org.expasy.jpl.io.ms.jrap.ScanHeader;
import org.expasy.jpl.io.ms.jrap.SoftwareInfo;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.MyDataContainer;

/**
 * mzXML support from the JRAP library, we dont use the same library for mzML as it doesnt
 * have as good a feature set as other mzML libraries for java.
 * 
 * @author andrew.cassin
 *
 */
public class mzXMLDataProcessor extends AbstractDataProcessor {
	private File m_file;
	private MSXMLParser m_p;
	private NodeLogger logger;
	
	public mzXMLDataProcessor(NodeLogger l) {
		assert(l != null);
		logger = l;
	}
	
	@Override
	public void setInput(String id) throws Exception {
		// ignored: handled by can() instead
	}

	@Override
	public boolean can(File f) throws Exception {
		m_file = f;
		String ext = f.getName().toLowerCase();
		
		if (ext.endsWith(".xml") || ext.endsWith(".mzxml")) {
	        return true;
		}
		return false;
	}

	@Override
	public void process(boolean load_spectra, ExecutionContext exec,
			MyDataContainer scan_container, MyDataContainer file_container)
			throws Exception {

		// first output the file port
		process_file(exec, file_container, m_p.getHeaderInfo());
		
		// now output the scan port
		process_scans(exec, scan_container);
	}

	protected void process_scans(ExecutionContext exec, MyDataContainer scan_container) throws Exception {
		int ncols = scan_container.getTableSpec().getNumColumns();
		for (int i=1; i<=m_p.getScanCount(); i++) {
			ScanHeader sh = m_p.rapHeader(i);
			
			DataCell[] cells = new DataCell[ncols];
			
			DataCell scan_type = (sh.getScanType() != null) ? 
									new StringCell(sh.getScanType()) : DataType.getMissingCell();
			
			cells[0] = scan_type;
			cells[1] = new StringCell(sh.getPolarity());
			cells[2] = new StringCell(sh.getRetentionTime());
			cells[3] = new DoubleCell(sh.getBasePeakIntensity());
			cells[4] = new DoubleCell(sh.getBasePeakMz());
			cells[5] = new IntCell(sh.getCentroided());
			cells[6] = new IntCell(sh.getDeisotoped());
			cells[7] = new IntCell(sh.getChargeDeconvoluted());
			cells[8] = new IntCell(sh.getMsLevel());
			cells[9] = DataType.getMissingCell();
			cells[10]= new IntCell(sh.getPrecursorCharge());
			cells[11]= new IntCell(sh.getPrecursorScanNum());
			cells[12]= DataType.getMissingCell();
			cells[13]= new DoubleCell(sh.getPrecursorMz());
			cells[14]= new DoubleCell(sh.getTotIonCurrent());
			cells[15]= new DoubleCell(sh.getCollisionEnergy());
			cells[16]= new DoubleCell(sh.getIonisationEnergy());
			cells[17]= new DoubleCell(sh.getStartMz());
			cells[18]= new DoubleCell(sh.getEndMz());
			cells[19]= new DoubleCell(sh.getLowMz());
			cells[20]= new DoubleCell(sh.getHighMz());
			cells[21]= new StringCell(m_file.getAbsolutePath());
			cells[22]= new IntCell(sh.getPeaksCount());
			
			// load spectra?
			if (ncols > 23) {
					Scan s = m_p.rap(sh.getNum());
					cells[23] = SpectraUtilityFactory.createCell(s, scan_type.toString());
			}
			scan_container.addRow(cells);
			
			if (i % 300 == 0) {
				exec.checkCanceled();
			}
		}
	}
	
	/**
	 * Report the file summary
	 * @param exec
	 * @param fc
	 * @param file_seq
	 * @param fh
	 * @throws Exception
	 */
	protected void process_file(ExecutionContext exec, MyDataContainer fc, MZXMLFileInfo fh) throws Exception {
    	MSInstrumentInfo   ii= fh.getInstrumentInfo();
	    DataProcessingInfo dp=fh.getDataProcessing();
	    int ncols = fc.getTableSpec().getNumColumns();
	    DataCell[] cells = new DataCell[ncols];
	    assert(ncols == 9);
	    	
	    cells[0] = safe_cell(ii.getManufacturer());
	    cells[1] = safe_cell(ii.getModel());
	    SoftwareInfo si = ii.getSoftwareInfo();
	    cells[2] = safe_cell(si != null ? si.toString() : null);
	   
	    MSOperator mso = ii.getOperator();
	    cells[3] = safe_cell(mso != null ? mso.toString() : null);
	  
	    cells[4] = safe_cell(ii.getMassAnalyzer());
	    cells[5] = safe_cell(ii.getIonization());
	    cells[6] = safe_cell(ii.getDetector());
	    cells[7] = safe_cell(dp.toString());
	    cells[8] = safe_cell(m_file.getAbsolutePath());
	    
	    fc.addRow(cells);
    }

}
