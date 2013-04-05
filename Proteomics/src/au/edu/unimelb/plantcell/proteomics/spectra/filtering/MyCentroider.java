package au.edu.unimelb.plantcell.proteomics.spectra.filtering;

import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.expasy.jpl.core.ms.spectrum.editor.PeakCentroider;

/**
 * Need a class which wraps the PeakCentroider depending on whether it is adaptive or fixed for the KNIME platform.
 * 
 * @author andrew.cassin
 *
 */
public class MyCentroider extends PeakCentroider {
	private boolean m_adaptive = false;
	private PeakCentroider m_pc = null;
	
	public MyCentroider(boolean is_adaptive) {
		m_adaptive = is_adaptive;
		if (!is_adaptive) {
			m_pc = this;
		}
	}
	
	public MyCentroider(double mz_max, double mz_min) {
		m_adaptive = false;
		m_pc = this;
		m_pc.setMzMaxDiff(mz_max);
		m_pc.setMzSamplingDist(mz_min);
	}
	
	@Override
	public PeakList transform(PeakList input) {
		if (m_adaptive) {
			m_pc = adaptedCentroider(input);
			return m_pc.transform(input);
		} else {
			return super.transform(input);
		}
	}
}
