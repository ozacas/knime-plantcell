package au.edu.unimelb.plantcell.algorithms.StringFinder;

import java.util.List;

import org.knime.core.data.DataCell;

import au.edu.unimelb.plantcell.core.cells.AnnotationType;
import au.edu.unimelb.plantcell.core.cells.CoordinateSystem;
import au.edu.unimelb.plantcell.core.cells.SequenceAnnotation;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.regions.Region;
import au.edu.unimelb.plantcell.core.regions.RegionsAnnotation;


public class SequenceAnnotatorReporter implements MatchReporter {
	public final static String TRACK_STRING_FINDER = "Find:matches";
	
	public final Track getTrack() {
		// by default it is 0 relative, which suits StringFinder
		return new Track(TRACK_STRING_FINDER, SequenceAnnotation.make(AnnotationType.LABELLED_REGIONS), 
									CoordinateSystem.OFFSET_FROM_START);
	}
	
	@Override
	public DataCell report(FindGlobalNodeModel m, DataCell str_cell) throws Exception {
		// clone the cell as appropriate to the subclass of StringValue
		SequenceCell ret = new SequenceCell(str_cell);
		
		// remove results from previous search and make new track
		ret.removeTrackByName(TRACK_STRING_FINDER);
		Track t = getTrack();
		ret.addTrack(t);
		RegionsAnnotation ra = (RegionsAnnotation) t.getAnnotation();
		
		// add matches
		List<Extent> match_pos = m.getMatchPos();
		for (Extent e : match_pos) {
			try {
				Region r = new Region(e.m_start, e.m_end, str_cell.toString().substring(e.m_start, e.m_end));
				ra.addRegion(r);
			} catch (IndexOutOfBoundsException ie) {
				// oops... sequence has been truncated... probably a bug of some sort???
				ie.printStackTrace();
				ra.addRegion(new Region(e.m_start, e.m_end, "?"));
			}
		}
		return ret;
	}

}
