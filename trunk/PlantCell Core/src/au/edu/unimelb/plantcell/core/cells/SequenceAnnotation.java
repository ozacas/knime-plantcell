package au.edu.unimelb.plantcell.core.cells;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.axis.encoding.Base64;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;

import au.edu.unimelb.plantcell.core.regions.AlignedRegionsAnnotation;
import au.edu.unimelb.plantcell.core.regions.GeneRegionsAnnotation;
import au.edu.unimelb.plantcell.core.regions.InterProRegionsAnnotation;
import au.edu.unimelb.plantcell.core.regions.PFAMRegionsAnnotation;
import au.edu.unimelb.plantcell.core.regions.PhobiusRegionsAnnotation;
import au.edu.unimelb.plantcell.core.regions.PredGPIRegionsAnnotation;
import au.edu.unimelb.plantcell.core.regions.RegionInterface;
import au.edu.unimelb.plantcell.core.regions.RegionsAnnotation;
import au.edu.unimelb.plantcell.core.regions.TMHMMRegionsAnnotation;

/**
 * Represents a single annotation object. A single object can represent annotations
 * for an entire track or a single part of one track as determined by the implementation.
 * This abstract base class must be implemented that wish to be persisted or it will
 * not work within the cell framework. All subclasses must implement a default constructor
 * 
 * @author andrew.cassin
 *
 */
public abstract class SequenceAnnotation {
	
	/**
	 * how many annotations have been defined in this instance?
	 */
	public abstract int countAnnotations();
	
	/**
	 * the type of data specified by the annotation
	 */
	public abstract AnnotationType getAnnotationType();

	/**
	 * Persist the annotation to the specified KNIME output stream. All implementations
	 * MUST serialise the {@link AnnotationType} first to the output stream for correct
	 * deserialization.
	 * 
	 * @param output
	 * @throws IOException
	 */
	public abstract void serialize(DataCellDataOutput output) throws IOException;

	public abstract SequenceAnnotation deserialize(DataCellDataInput input) throws IOException;


	/**
	 * Convenience wrapper around <code>SequenceAnnotation.make(AnnotationType)</code> when you have
	 * a track name and only a column properties for the column containing the sequences with the track.
	 * This code must match the data model established by <code>Track.asText()</code>
	 * 
	 * @param tcp
	 * @param name
	 * @return may be null if the specified track name is not found in the specified properties
	 */
	public static SequenceAnnotation make(DataColumnProperties tcp, String name) {
		AnnotationType at = AnnotationType.USER_DEFINED;
		Enumeration<String> props = tcp.properties();
		while (props.hasMoreElements()) {
			String propName = props.nextElement();
			if (propName.equals(Track.PLANTCELL_TRACK_PREFIX+name)) {
				Pattern p = Pattern.compile("\\bannotation_type=(\\S+)\\s");
				String str= new String(Base64.decode(tcp.getProperty(propName)));
				Matcher m = p.matcher(str);
				if (m.find()) {
					at = AnnotationType.valueOf(m.group(1));
					// fallthru...
				} else {
					// throw? not for now...
				}
				return make(at);
			}
		}
		return null;
	}
	
	/**
	 * Constructs an initially empty annotation of the specified type. Called during
	 * deserialization once we know the type of annotation to restore
	 * @param valueOf
	 * @return
	 */
	public static SequenceAnnotation make(AnnotationType at) {
		switch (at) {
			case TMHMM_REGIONS:
				return new TMHMMRegionsAnnotation();
			case LABELLED_REGIONS:
				return new RegionsAnnotation();	// offset is zero by default ie. zero relative
			case COMMENTS:
				return new CommentsAnnotation();
			case ALIGNED_REGIONS:				// for blast hits
				return new AlignedRegionsAnnotation();
			case PHOBIUS_REGIONS:
				return new PhobiusRegionsAnnotation();
			case INTERPRO_REGIONS:
				return new InterProRegionsAnnotation();
			case NUMERIC:
				return new NumericAnnotation();
			case GENE_PREDICTION_REGIONS:
				return new GeneRegionsAnnotation();
			case PREDGPI_REGIONS:
				return new PredGPIRegionsAnnotation();
			case PFAM_REGIONS:
				return new PFAMRegionsAnnotation();
			default:
				return new UserDefinedAnnotation();
		}
	}

	/**
	 * Returns the columns required to support this annotation. If the annotation is
	 * polymorphic, then enough columns to represent that will need to be returned here.
	 * Each returned column name should have the specified prefix (guaranteed non-null).
	 * 
	 * @param prefix
	 * @return
	 */
	public abstract List<DataColumnSpec> asColumnSpec(String prefix);

	/**
	 * Returns a list of regions (virtual or sub-objects) as appropriate to the annotation
	 * type. May return null to denote no regions available.
	 * 
	 * @return
	 */
	public List<RegionInterface> getRegions() {
		return null;
	}

	/**
	 * Return a graphical renderer for the track (which is able to paint it onto
	 * a canvas)
	 * @return null since subclasses will override
	 */
	public TrackRendererInterface getRenderer() {
		return null;
	}

	
}
