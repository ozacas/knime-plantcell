package au.edu.unimelb.plantcell.core.regions;

import java.io.IOException;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.TableMappable;
import au.edu.unimelb.plantcell.core.cells.Track;

/**
 * A class which implements this interface is capable of being compared region-by-region with another
 * RegionComparator instance. See {@link Region} for more details
 * @author andrew.cassin
 *
 */
public interface RegionInterface extends TableMappable {

	// for identifying the start position of the region (relative to zero)
	public int getZStart();
	
	// for identifying the end position of the region (relative to zero)
	public int getZEnd();
	
	/**
	 * Returns true if the region represents a single residue, false otherwise
	 */
	public boolean isSingleSite();
	
	/**
	 * Each region must be labelled (it may be empty if so desired, but not recommended)
	 */
	public String getLabel();
	
	/**
	 * Every region must have a unique ID with a prefix that indicates
	 * what runtime type it is - for correct serialisation. This method returns it.
	 * 
	 * @return
	 */
	public String getID();
	
	/**
	 * Returns the subset of the specified {@link SequenceValue} which is denoted
	 * by the specified region. May return null if the method decides that the specified
	 * feature is not available (eg. if only a partial sequence is available)
	 * 
	 * @param sv never null
	 */
	public SequenceValue getFeatureSequence(SequenceValue sv);
	
	/**
	 * returns a GFF v2 compliant representation of the region. The region will
	 * be skipped if the implementation returns <code>null</code>. The implementation is expected
	 * to return an entire feature line (using  sv and t) for inclusion in the output
	 */
	public String asGFF(SequenceValue sv, Track t);

	/**
	 * select a subset of the region for reporting. This current has no effect for some RegionInterface's
	 * as it is reserved for future implementation. Only numeric vectors can be currently constrained in this way.
	 * @Param roi the values of interest for reporting. Set bits only are reported. There is no requirement for contiguity, although this may confuse the user!
	 */
	public void setRegionOfInterest(DenseBitVector roi) throws InvalidSettingsException;
	
	/******************* SERIALIZATION OF REGIONS TO KNIME DATA SOURCE/SINKS *****************************/
	
	public void serialize(DataCellDataOutput output) throws IOException;

	public Object deserialize(DataCellDataInput input) throws IOException;



}
