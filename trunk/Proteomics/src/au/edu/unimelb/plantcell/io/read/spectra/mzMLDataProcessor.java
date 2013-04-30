package au.edu.unimelb.plantcell.io.read.spectra;

import java.io.File;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;

import uk.ac.ebi.jmzml.model.mzml.AnalyzerComponent;
import uk.ac.ebi.jmzml.model.mzml.CVParam;
import uk.ac.ebi.jmzml.model.mzml.DetectorComponent;
import uk.ac.ebi.jmzml.model.mzml.ParamGroup;
import uk.ac.ebi.jmzml.model.mzml.ReferenceableParamGroup;
import uk.ac.ebi.jmzml.model.mzml.Software;
import uk.ac.ebi.jmzml.model.mzml.SourceComponent;
import uk.ac.ebi.jmzml.model.mzml.Spectrum;
import uk.ac.ebi.jmzml.model.mzml.UserParam;
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
	private NodeLogger m_logger;
	
	// xpaths for required data from the mzml
	private final static String SOURCE_XPATH = "/mzML/instrumentConfigurationList/instrumentConfiguration/componentList/source";
	private final static String ANALYZER_XPATH="/mzML/instrumentConfigurationList/instrumentConfiguration/componentList/analyzer";
	private final static String DETECTOR_XPATH="/mzML/instrumentConfigurationList/instrumentConfiguration/componentList/detector";
	private final static String DP_LIST_XPATH="/mzML/run/spectrumList/defaultDataProcessing";
	
	public mzMLDataProcessor(NodeLogger l) {
		assert(l != null);
		m_logger = l;
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
		process_file(exec, file_container, m_file);
		
		// now output the scan port
		process_scans(exec, scan_container, new MzMLUnmarshaller(m_file));
	}
	
	/**
	 * Load spectra from file or just the header information as desired by the user
	 * 
	 * @param exec
	 * @param scan_container
	 * @param rdr
	 * @throws Exception should only throw in case of serious error (scans cannot be loaded successfully)
	 */
	protected void process_scans(ExecutionContext exec, MyDataContainer scan_container, MzMLUnmarshaller rdr) throws Exception {
		int n_cols = scan_container.getTableSpec().getNumColumns();
		
		MzMLObjectIterator<Spectrum> it = rdr.unmarshalCollectionFromXpath("/run/spectrumList/spectrum", Spectrum.class);
		int n_spectra = 0;
		while (it.hasNext()) {
			DataCell[] cells = missing_cells(n_cols);
			
			SpectrumAdapter sa = new SpectrumAdapter(it.next(), true);
			BasicPeakList bpl = new BasicPeakList(sa);

			cells[0]  = safe_cell(sa.getTitle());
			cells[1]  = sa.isPositiveScan() ? new StringCell("+") : new StringCell("-");
			cells[2]  = new StringCell(String.valueOf(sa.getRetentionTime()));
			cells[3]  = (!Double.isNaN(sa.getBasePeakMZ())) ? new DoubleCell(sa.getBasePeakMZ()) : DataType.getMissingCell();
			cells[4]  = (!Double.isNaN(sa.getBasePeakIntensity())) ? new DoubleCell(sa.getBasePeakIntensity()) : DataType.getMissingCell();
			cells[5]  = (sa.isCentroided() != null) ? BooleanCell.get(sa.isCentroided().booleanValue()) : DataType.getMissingCell();
			cells[8]  = sa.getMSLevel() < 1 ? DataType.getMissingCell() : new IntCell(sa.getMSLevel());
			cells[9]  = (sa.getScanID() != null) ? new StringCell(sa.getScanID()) : DataType.getMissingCell();
			cells[10] = new IntCell(sa.getParentCharge());
			cells[12] = new DoubleCell(sa.getParentIntensity());
			cells[13] = new DoubleCell(sa.getParentMZ());
			cells[14] = (!Double.isNaN(sa.getTIC())) ? new DoubleCell(sa.getTIC()) : DataType.getMissingCell();
			cells[19] = (!Double.isNaN(sa.getLowMZ())) ? new DoubleCell(sa.getLowMZ()) : DataType.getMissingCell();
			cells[20] = (!Double.isNaN(sa.getHighMZ())) ? new DoubleCell(sa.getHighMZ()) : DataType.getMissingCell();
			cells[21] = new StringCell(m_file.getAbsolutePath());
			cells[22] = new IntCell(bpl.getNumPeaks());
			if (n_cols > 23) {
				 // empty peaklist ? using KNIME missing cell rather than empty spectra instance 
		         cells[23] = (bpl.getNumPeaks() > 0) ? SpectraUtilityFactory.createCell(bpl) : DataType.getMissingCell();
			}
			String charge = bpl.getCharge_safe();
			if (charge != null) {
			         charge    = charge.trim().replaceAll("\\+", "");
			         if (charge.length() > 0)
			                 cells[10] = new IntCell(Integer.valueOf(charge));
			         else
			                 cells[10] = DataType.getMissingCell();
			}
			
			scan_container.addRow(cells);
			n_spectra++;
		}
		m_logger.info("Processed "+n_spectra+" spectra from "+m_file.getName());
	}
	
	/**
	 * Report the file summary - note this is not complete (yet) -- TODO FIXME
	 * @param exec
	 * @param fc
	 * @param file_seq
	 * @param fh
	 * @throws Exception method should only throw if processing of the file should be aborted (ie. no scans should be processed)
	 */
	protected void process_file(ExecutionContext exec, MyDataContainer fc, File f) throws Exception {
		DataCell[] cells = missing_cells(fc.getTableSpec().getNumColumns());
		cells[8]         = new StringCell(m_file.getAbsolutePath());
		MzMLUnmarshaller rdr = new MzMLUnmarshaller(f);
		int sourceCnt    = rdr.getObjectCountForXpath(SOURCE_XPATH);
		int analyzerCnt  = rdr.getObjectCountForXpath(ANALYZER_XPATH);
		int detectorCnt  = rdr.getObjectCountForXpath(DETECTOR_XPATH);
		
		if (sourceCnt > 0) {
			try {
				MzMLObjectIterator<SourceComponent> it1 = rdr.unmarshalCollectionFromXpath(SOURCE_XPATH, SourceComponent.class);
				cells[5] = safe_cell(make_paramgroup_string(it1.next()));
			} catch (Exception e) {
				e.printStackTrace();
				// do nothing: the cell is already missing
			}
		}
		
		if (analyzerCnt > 0) {
			try {
				MzMLObjectIterator<AnalyzerComponent> it2 = rdr.unmarshalCollectionFromXpath(ANALYZER_XPATH, AnalyzerComponent.class);			
				cells[4] = safe_cell(make_paramgroup_string(it2.next()));
			} catch (Exception e) {
				e.printStackTrace();
				// do nothing: the cell is already missing
			}
		}
		
		if (detectorCnt > 0) {
			try {
				MzMLObjectIterator<DetectorComponent> it3 = rdr.unmarshalCollectionFromXpath(DETECTOR_XPATH, DetectorComponent.class);
				cells[6] = safe_cell(make_paramgroup_string(it3.next()));
			} catch (Exception e) {
				e.printStackTrace();
				// do nothing: the cell is already missing
			}
		}
		
		try {
			MzMLObjectIterator<Software> softlist = rdr.unmarshalCollectionFromXpath("/mzML/softwareList/software", Software.class);
			if (softlist.hasNext()) {
				StringBuilder sb = new StringBuilder();
				while (softlist.hasNext()) {
					Software s = softlist.next();
					sb.append(s.getId() + " " + s.getVersion()+ "\n");
				}
				cells[2] = new StringCell(sb.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			// do nothing: the cell is already missing
		}
		
		try {
			MzMLObjectIterator<ReferenceableParamGroup> grp_list = rdr.unmarshalCollectionFromXpath("/mzML/referenceableParamGroupList/referenceableParamGroup", ReferenceableParamGroup.class);
			if (grp_list.hasNext()) {
				StringBuilder sb = new StringBuilder();
				while (grp_list.hasNext()) {
					ReferenceableParamGroup rpg = grp_list.next();
					for (CVParam cv : rpg.getCvParam()) {
						String val = cv.getValue();
						if (val != null && val.trim().length() > 0) {
							sb.append(cv.getName() + " " + val);
						} else {
							sb.append(cv.getName()+"\n");
						}
					}
				}
				cells[1] = new StringCell(sb.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			// do nothing: the cell is already missing
		}
		
		fc.addRow(cells);
    }

    private String make_paramgroup_string(ParamGroup pg) {
		StringBuilder sb = new StringBuilder(10 * 1024);
		if (pg != null) {
			for (CVParam cv : pg.getCvParam()) {
				String val = cv.getValue();
				if (val != null && val.trim().length() > 0)
					sb.append(cv.getName()+" = " + val + "\n");
				else 
					sb.append(cv.getName()+"\n");
			}
			for (UserParam up : pg.getUserParam()) {
				String val = up.getValue();
				if (val != null && val.trim().length() > 0)
					sb.append(up.getName() + " = " + up.getValue() + "\n");
				else 
					sb.append(up.getName()+"\n");
			}
		}
		return sb.toString();
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
