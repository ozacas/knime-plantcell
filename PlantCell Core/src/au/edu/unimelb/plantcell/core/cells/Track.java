package au.edu.unimelb.plantcell.core.cells;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.axis.encoding.Base64;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcore.core.regions.RegionInterface;

/**
 * Represents a set of annotations sharing a key property eg. from same source (eg. TMHMM)
 * @author andrew.cassin
 *
 */
public class Track extends StandardTrackRenderer implements SerializableInterface<Track> {
	// all PlantCell extension track names have this as a prefix
    public static final String PLANTCELL_TRACK_PREFIX   = "PlantCell:Tracks:";

	// predefined labels for builtin tracks
	public final static String[] TMHMM_LABELS   = new String[] { "TMhelix", "inside", "outside" };
	public final static String[] NO_LABELS      = new String[] { };
	public final static String[] PHOBIUS_LABELS = NO_LABELS;
	
	 // track related stuff
    public static final String TMHMM_TRACK              = "TMHMM";			// track names for Web Services nodes 
	public static final String PHOBIUS_TRACK            = "EBI:Phobius";
	public static final String INTERPRO_TRACK           = "EBI:InterPro";
	public static final String BLAST_LONGEST_TRACK      = "NCBI:BLAST+:longest";
	public static final String BLAST_BEST_EVAL_TRACK    = "NCBI:BLAST+:best";
	public static final String BLAST_TOP20_TRACK        = "NCBI:BLAST+:top20";
	public static final String RPSBLAST_TRACK           = "NCBI:RPSBLAST:all";
	public static final String WUBLAST_TRACK            = "EBI:WU-BLAST:all";
	public static final String NETSURFP_ALPHA           = "CBS:NetSurfP:Pr(Alpha Helix)";
	public static final String NETSURFP_BETA            = "CBS:NetSurfP:Pr(Beta Strand)";
	public static final String NETSURFP_COIL            = "CBS:NetSurfP:Pr(Coil)";
	public static final String NETPHOS_TRACK            = "CBS:NetPhos";
	public static final String EMBOSS_TRACKS            = "EMBOSS:";
	public static final String GENE_PREDICTION_AUGUSTUS = "GenePrediction:Augustus";
	public static final String PREDGPI_TRACK            = "PredGPI";
	public static final String SIGNALP_TRACK = "CBS:SignalP";

	
	private String             m_name;
	private SequenceAnnotation m_annot;
	private CoordinateSystem   m_coords;
	
	public Track(String name) {
		this(name, null, CoordinateSystem.OFFSET_FROM_START);
	}
	
	public Track(Track t) {
		this(new String(t.getName()), t.m_annot, t.m_coords);
	}

	public Track(String name, TrackCreator tc) throws InvalidSettingsException {
		this(tc.createTrack(name));
	}
	
	public Track(String name, SequenceAnnotation sa, CoordinateSystem cs) {
		assert(name != null && name.length() > 0);
		setName(name);
		addAnnotation(sa);
		setCoordinateSystem(cs);
	}
	
	/**
	 * Constructs a track from the specified map (which has typically been created via 
	 * the column properties). See
	 * @param props
	 */
	public Track(Map<String,String> props) {
		assert(props != null && props.size() > 0);
		setName(props.get("name"));
		setCoordinateSystem(CoordinateSystem.valueOf(props.get("coords")));
		if (props.get("annotated").equals("yes")) {
			addAnnotation(SequenceAnnotation.make(
					AnnotationType.valueOf(props.get("annotation_type"))));
		} else {
			addAnnotation(null);
		}
	}
	
	public void setName(String new_name) {
		m_name = new_name;
	}
	
	public void setCoordinateSystem(CoordinateSystem cs) {
		m_coords = cs;
	}

	/**
	 * Has any annotation been made on this track? If yes, <code>true</code> is returned
	 */
	public final boolean hasAnnotation() {
		return (countAnnotations() > 0);
	}
	
	public int countAnnotations() {
		if (m_annot == null)
			return 0;
		return m_annot.countAnnotations();
	}
	
	/**
	 * set the annotation object associated with the track. To remove an existing annotation,
	 * pass <code>null</code>
	 * @return
	 */
	public void addAnnotation(SequenceAnnotation sa) {
		m_annot = sa;
	}
	
	public String getName() {
		return m_name;
	}
	
	@Override
	public void serialize(DataCellDataOutput output) throws IOException {
		output.writeUTF(getName());
		output.writeUTF(m_coords.name());
		boolean has_annot = hasAnnotation();
		output.writeBoolean(has_annot);
		if (has_annot) {
			m_annot.serialize(output);
		}
	}

	@Override
	public Track deserialize(DataCellDataInput input) throws IOException {
		String name = input.readUTF();
		m_coords    = CoordinateSystem.valueOf(input.readUTF());
		m_annot     = null;
		setName(name);
		boolean has_annot = input.readBoolean();
		if (has_annot) {
			String annot_type = input.readUTF();
			//Logger.getAnonymousLogger().info(annot_type);
			m_annot = SequenceAnnotation.make(AnnotationType.valueOf(annot_type)).deserialize(input);
		}
		return this;
	}

	public boolean hasName(String name) {
		return name.equals(getName());
	}

	public boolean hasType(AnnotationType at) {
		if (!hasAnnotation()) 
			return false;
		return at.equals(m_annot.getAnnotationType());
	}

	public SequenceAnnotation getAnnotation() {
		return m_annot;
	}
	
	/**
	 * Returns a single line of text describing each track. Use in node dialogs for annotations. This state
	 * is stored in a {@link DataColumnProperties} instance associated with the column
	 * @return
	 */
	public String asText() {
		StringBuilder sb = new StringBuilder(1024);
		sb.append("name=");
		sb.append(getName());
		sb.append('\n');
		String annot = "annotated=yes";
		if (m_annot == null) {
			annot = "annotated=no";
			sb.append(annot);
			sb.append('\n');
		} else {
			sb.append(annot);
			sb.append('\n');
			sb.append("annotation_type=");
			sb.append(m_annot.getAnnotationType().name());
			sb.append('\n');
		}
		sb.append("coords=");
		sb.append(m_coords.name());
		return Base64.encode(sb.toString().getBytes());
	}
	
	/**
	 * Construct a properties map from the column properties data which can be used
	 * to re-construct a track from the column properties.
	 * @param s
	 * @return
	 */
	public static Map<String,String> fromText(String s) {
		Map<String,String> map = new HashMap<String,String>();
		String lines = new String(Base64.decode(s));
		Pattern p = Pattern.compile("^([^=]+?)=(.*)$");
		for (String line : lines.split("\\n")) {
			Matcher m = p.matcher(line.trim());
			if (m.matches()) {
				map.put(m.group(1), m.group(2));
			}
		}
		return map;
	}
	
	@Override
	public String toString() {
		if (m_annot == null) {
			return getName();
		} else {
			return getName() + ": " + m_annot.toString();
		}
	}

	/**
	 * Returns true if the annotation defines region(s) of interest within the sequence. Otherwise false.
	 * @return
	 */
	public boolean isRegionAnnotation() {
		if (!hasAnnotation())
			return false;
		return getAnnotation().getAnnotationType().isRegion();
	}

	public boolean isNumericAnnotation() {
		if (!hasAnnotation()) 
			return false;
		return getAnnotation().getAnnotationType().isNumeric();
	}

	/**
	 * An annotation track can supports region operations if it implements RegionOpInterface or it
	 * contains objects which do...
	 * 
	 * @return
	 */
	public boolean hasRegions() {
		// some annotations will implement regions directly...
		if (getAnnotation() instanceof RegionInterface)
			return true;
		// whilst others will create sub-objects which do...
		if (isRegionAnnotation() && this.countAnnotations() > 0)
			return true;
		// else...
		return false;
	}

	public TrackRendererInterface getRenderer() {
		if (hasRegions()) {
			TrackRendererInterface r = getAnnotation().getRenderer();
			if (r != null)
				return r;
			return this;
		}
		
		return null;
	}
}
