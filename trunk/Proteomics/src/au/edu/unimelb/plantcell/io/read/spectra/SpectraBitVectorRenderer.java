package au.edu.unimelb.plantcell.io.read.spectra;

import org.apache.log4j.Logger;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.knime.core.data.renderer.BitVectorValuePixelRenderer;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;

import au.edu.unimelb.plantcell.core.CorePlugin;
import au.edu.unimelb.plantcell.core.Preferences;

/**
 * Provides a simple-minded 1D peak density map for MS/MS spectra with bins and window
 * as established by user preference settings.
 * 
 * @author andrew.cassin
 *
 */
public class SpectraBitVectorRenderer extends BitVectorValuePixelRenderer
		implements DataValueRenderer, IPropertyChangeListener {

	/**
	 *  distinguishes renderer data when serialized from all others
	 */
	private static final long serialVersionUID = 1143519816230556260L;
	
	private double m_threshold;
	private double m_window_left;		
	private double m_window_right;		
	private double m_bin_size = 0.1;
	private String m_title;
	

	/**
	 * Uses the current preference setting (at time of construction) to ensure that the right
	 * values are displayed in the 1D peak density render
	 * 
	 * @param title	as displayed in the column heading context menu
	 */
	public SpectraBitVectorRenderer(String title) {
		m_title       = title;
		
		// establish defaults from the preferences, but the SpectraUtilityFactory arranges for this class to
		// listen to preference changes to get updated values as the user wishes
		IPreferenceStore prefs = CorePlugin.getDefault().getPreferenceStore();
		m_threshold   = asDouble(prefs.getString(Preferences.PREFS_SPECTRA_THRESHOLD), 0.0);
		m_window_left = asDouble(prefs.getString(Preferences.PREFS_SPECTRA_MIN_MZ), 0.0);
		m_window_right= asDouble(prefs.getString(Preferences.PREFS_SPECTRA_MAX_MZ), 2000.0);
		m_bin_size    = asDouble(prefs.getString(Preferences.PREFS_SPECTRA_BIN_SIZE), 0.1);
	}
	
	
	
	private double asDouble(String val, double default_on_exception) {
		try {
			return Double.valueOf(val);
		} catch (Exception nfe) {
			return default_on_exception;
		}
	}

	

	/** {@inheritDoc} */
    @Override
    protected void setValue(final Object val) {
    	//Logger.getLogger("1D peak density").info("Threshold: "+m_threshold+", left="+m_window_left+", right="+m_window_right+", bin size="+m_bin_size);
    	
    	if (val instanceof AbstractSpectraCell) {
    		if (m_bin_size <= 0.0) {
    			Logger.getLogger("Peak Density 1D Map").warn("Cannot use zero width bins: change your preferences! Using 0.1");
    			m_bin_size = 0.1;
    		}
    		// compute the bit vector for the super class to render
    		AbstractSpectraCell spectra = (AbstractSpectraCell) val;
    		double[] mz = spectra.getMZ();
    		
    		double max_mz = m_window_right - m_window_left;
    		int n_bits = (int)(max_mz / m_bin_size)+1;
    		DenseBitVectorCellFactory mybits = new DenseBitVectorCellFactory(n_bits);
    		double[] intensity = spectra.getIntensity();
    		
    		for (int i=0; i<mz.length; i++) {
    			if (intensity[i] >= m_threshold && mz[i] >= m_window_left && mz[i] <= m_window_right) {
    				mybits.set(n_bits - 1 - (int) ((mz[i] - m_window_left) / m_bin_size));
    			}
    		}
    		super.setValue(mybits.createDataCell());
    	} else {
    		super.setValue(val);
    	}
    }
	
	/** {@inheritDoc} */
    @Override
	public String getDescription() {
		return m_title;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		String new_value= event.getNewValue().toString();
		
		if (property.equals(Preferences.PREFS_SPECTRA_THRESHOLD))
			m_threshold   = asDouble(new_value, 0.0);
		else if (property.equals(Preferences.PREFS_SPECTRA_MIN_MZ))
			m_window_left = asDouble(new_value, 0.0);
		else if (property.equals(Preferences.PREFS_SPECTRA_MAX_MZ))
			m_window_right = asDouble(new_value, 0.0);
		else if (property.equals(Preferences.PREFS_SPECTRA_BIN_SIZE))
			m_bin_size    = asDouble(new_value, 0.1);
		else {
			// ignore it since it is some other preference settings unrelated to this renderer
		}
	}
}
