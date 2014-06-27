package au.edu.unimelb.plantcell.servers.msconvertee.endpoints;

public enum MSConvertFeature {
	// output file formats from conversion
	OUTPUT_MGF, OUTPUT_MZ5, OUTPUT_MS1, OUTPUT_CMS1, OUTPUT_CMS2, OUTPUT_MS2, 
	OUTPUT_MZXML, OUTPUT_MZML, 
	
	// are filtering plugins supported by this msconvert version?
	FILTERS_ARE_SUPPORTED, 
	
	// is merging different data files together supported?
	MERGE_IS_SUPPORTED,
	
	// filtering types found by scanning the msconvert command line usage
	FILTER_BY_SCAN, FILTER_BY_MSLEVEL, FILTER_BY_ETD, FILTER_BY_ZERO_INTENSITY, FILTER_BY_CHARGE_STATE,
	FILTER_BY_ACTIVATION, FILTER_BY_INTENSITY_THRESHOLD, FILTER_BY_MZ, FILTER_BY_PRECURSOR, FILTER_BY_ANALYZER, FILTER_BY_POLARITY,
	
	// other options available 
	PEAK_PICKING, PRECURSOR_REFINEMENT, SORT_BY_SCAN_TIME, REMOVE_ION_TRAP_MS1_SCANS, MS2_DENOISE, MS2_DEISOTOPE, 
	
	// 32 versus 64-bit precision for data in spectra
	USE_64_BIT_MZ_PRECISION, USE_32_BIT_MZ_PRECISION, USE_64_BIT_INTENSITY_PRECISION, USE_32_BIT_INTENSITY_PRECISION,
	
	// data compression
	COMPRESS_BINARY_DATA, COMPRESS_ENTIRE_FILE, 
	
	// this feature is NEVER available and is used by test cases to test the negative result
	UNSUPPORTED_FEATURE, 
}
