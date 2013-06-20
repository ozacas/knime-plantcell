package au.edu.unimelb.plantcell.io.read.spectra;

/**
 * When a binary data array is encountered in the input, we create an instance of this type to reflect
 * the type of array during processing. 
 * 
 * @author andrew.cassin
 *
 */
public enum BinaryDataType {
	MZ_TYPE, INTENSITY_TYPE, TIME_TYPE, CHARGE_TYPE, S2N_TYPE, WAVELENGTH_TYPE, UNKNOWN_TYPE;
	
	public boolean isMZ() {
		return (this == MZ_TYPE);
	}
	
	public boolean isIntensity() {
		return (this == INTENSITY_TYPE);
	}
	
	public boolean isTime() {
		return (this == TIME_TYPE);
	}
}
