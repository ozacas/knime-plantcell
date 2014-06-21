package au.edu.unimelb.plantcell.proteomics.spectra.filtering;

import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.core.ms.spectrum.editor.AbstractPeakListEditor;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.IntCell;

import au.edu.unimelb.plantcell.io.read.spectra.AbstractSpectraCell;
import au.edu.unimelb.plantcell.io.read.spectra.SpectraUtilityFactory;
import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;

/**
 * javaprotlib uses "editors" for some things rather than filters... so methods which are not handled by {@link MyFilterCellFactory}
 * come here instead.
 * 
 * @author andrew.cassin
 *
 */
public class MyEditorCellFactory extends AbstractCellFactory {
	private int m_column_idx;
	private final AbstractPeakListEditor m_editor;
	private String m_method;
	
	public MyEditorCellFactory(int idx, final AbstractPeakListEditor editor, String method) {
		assert(editor != null && idx >= 0);
		m_column_idx = idx;
		m_editor = editor;
		m_method = method;
	}

	@Override
	public DataCell[] getCells(DataRow row) {
		DataCell c = row.getCell(m_column_idx);
		DataCell[] ret = new DataCell[getColumnSpecs().length];
		for (int i=0; i<ret.length; i++) {
			ret[i] = DataType.getMissingCell();
		}
		if (c == null || c.isMissing() || !(c instanceof SpectraValue)) {
			return ret;
		}
		SpectraValue sv = (SpectraValue) c;
		Peak precursor  = sv.getPrecursor();
		
		PeakList pl = new PeakListImpl.Builder(sv.getMZ()).intensities(sv.getIntensity()).
				msLevel(sv.getMSLevel()).precursor(precursor).build();
		int size = pl.size();
		try {
			pl = m_editor.transform(pl);
			int n_transformed = pl.size();	// HACK TODO BUG FIXME: not how many peaks are altered....
			ret[0] = new IntCell(n_transformed);
			ret[1] = new IntCell(size);
			ret[2] = SpectraUtilityFactory.createCell(pl);
			return ret;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] cols = new DataColumnSpec[3];
		cols[0] = new DataColumnSpecCreator("Number of peaks transformed: "+m_method, IntCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Number of peaks: "+m_method, IntCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Altered Spectra: after "+m_method, AbstractSpectraCell.TYPE).createSpec();
		return cols;
	}
}
