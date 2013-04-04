package au.edu.unimelb.plantcell.proteomics.spectra.filtering;

import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.expasy.jpl.core.ms.spectrum.PeakListImpl;
import org.expasy.jpl.core.ms.spectrum.filter.AbstractPeakListFilter;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.io.read.spectra.AbstractSpectraCell;
import au.edu.unimelb.plantcell.io.read.spectra.SpectraUtilityFactory;
import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;

/**
 * CellFactory to perform filtering/normalisation of spectra
 * 
 * @author andrew.cassin
 *
 */
public class MyFilterCellFactory extends AbstractCellFactory {
		private int m_failed;
		private int m_idx;
		private final AbstractPeakListFilter m_filter;
		private final String m_method;
		
		public MyFilterCellFactory(int spectra_column_idx, final AbstractPeakListFilter filter, final String method) {
			assert(spectra_column_idx >= 0);
			m_idx = spectra_column_idx;
			m_filter = filter;
			m_method = method;
			m_failed = 0;
		}
		
		@Override
		public DataCell[] getCells(DataRow row) {
			DataCell c = row.getCell(m_idx);
			DataCell[] missing = new DataCell[] { DataType.getMissingCell(), DataType.getMissingCell(), DataType.getMissingCell() }; 
			if (c == null || c.isMissing() || ! (c instanceof SpectraValue)) {
				return missing;
			}
			
			try {
				SpectraValue sv = (SpectraValue) c;
				Peak precursor  = sv.getPrecursor();
				if (precursor == null) 
					throw new InvalidSettingsException("Cannot filter spectra without precursor peak!");
				PeakList pl = new PeakListImpl.Builder(sv.getMZ()).intensities(sv.getIntensity()).
										msLevel(sv.getMSLevel()).precursor(precursor).build();
				int before = pl.size();
				pl = m_filter.transform(pl);
				int after = pl.size();
				return new DataCell[] { new IntCell(before - after), new IntCell(after), SpectraUtilityFactory.createCell(pl) };
			} catch (Exception e) {
				e.printStackTrace();
				m_failed++;
				return missing;
			}
			
		}

		@Override
		public DataColumnSpec[] getColumnSpecs() {
			DataColumnSpec[] ret = new DataColumnSpec[3];
			
			ret[0] = new DataColumnSpecCreator("Number of peaks removed: "+m_method, IntCell.TYPE).createSpec();
			ret[1] = new DataColumnSpecCreator("Number of peaks remaining: "+m_method, IntCell.TYPE).createSpec();
			ret[2] = new DataColumnSpecCreator("Filtered Spectra: after "+m_method, AbstractSpectraCell.TYPE).createSpec();
			return ret;
		}

		@Override
		public void setProgress(int curRowNr, int rowCount, RowKey lastKey, ExecutionMonitor exec) {
			exec.setProgress(((double)curRowNr)/rowCount);
		}
		
		/**
		 * Called after all processing, this returns the number of spectra (rows) for which the filter failed
		 */
		public int getFailed() {
			return m_failed;
		}
}
