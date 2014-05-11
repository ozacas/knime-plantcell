package au.edu.unimelb.plantcell.core.regions;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

import au.edu.unimelb.plantcell.core.cells.AnnotationType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackRendererInterface;

/**
 * An instance of this is created to represent (eg. BLAST) alignment results. The
 * annotation type permits many hits per region of sequence and will display accordingly.
 * 
 * @author andrew.cassin
 *
 */
public class AlignedRegionsAnnotation extends RegionsAnnotation implements TrackRendererInterface {
	
	@Override
	public AnnotationType getAnnotationType() {
		return AnnotationType.ALIGNED_REGIONS;
	}
	
	/**
	 * Co-ordinates of predictions start at 1 for phobius?o
	 */
	@Override
	public int getOffset() {
		return 1;
	}

	public void addRegions(List<? extends BlastHitRegion> tmp) {
		for (BlastHitRegion bhr : tmp) {
			addRegion(bhr);
		}
	}
	
	@Override
	public void serialize(DataCellDataOutput output) throws IOException {
		super.serialize(output);
	}
	
	@Override 
	public List<DataColumnSpec> asColumnSpec(String prefix) {
		List<DataColumnSpec> ret = super.asColumnSpec(prefix);
		ret.add(new DataColumnSpecCreator(prefix+": Query ID",       StringCell.TYPE).createSpec());
		ret.add(new DataColumnSpecCreator(prefix+": E-Value", 		 DoubleCell.TYPE).createSpec());
		ret.add(new DataColumnSpecCreator(prefix+": %Identity",      DoubleCell.TYPE).createSpec());
		ret.add(new DataColumnSpecCreator(prefix+": Alignment length",IntCell.TYPE).createSpec());
		ret.add(new DataColumnSpecCreator(prefix+": Frame",          StringCell.TYPE).createSpec());
		return ret;
	}

	private Color getBLASTColour(double score) {
		Color c = Color.RED;
		if (score >= 80 && score < 200) {
			c = Color.PINK;
		} else if (score >= 50 && score < 80) {
			c = Color.GREEN;
		} else if (score >= 40 && score < 50) {
			c = Color.MAGENTA;
		} else if (score < 40) {
			c = Color.BLACK;
		}
		
		return c;
	}
	
	private void render_blast_hit(Graphics g, int start, int end, int offset, double frac) {
		g.fillRect(200+(int)(start * frac), offset, 
				(int) ((end - start) * frac), 3);
	}
	
	@Override
	public Dimension paintTrack(Map<String, Integer> props, Graphics g,
			SequenceValue sv, Track t) {
		Integer offset = props.get("offset");
		int start_offset = offset;
		
		paintLabel(g, t.getName(), offset);
		
		List<RegionInterface> regions = t.getAnnotation().getRegions();
		Integer width = props.get("track.width");
		double frac = ((double)width) / sv.getLength();
		for (RegionInterface r : regions) {
			BlastHitRegion bhr = (BlastHitRegion) r;
			int start = r.getZStart();
			int end   = r.getZEnd();
			
			g.setColor(getBLASTColour(bhr.getScore()));
			render_blast_hit(g, start, end, offset, frac);
			offset  += 5;
		}
		int n_lines = (offset - start_offset) / 20;
		int height = offset - start_offset;

		if (n_lines >= 1) {
			int rank = 1;
			for (RegionInterface r : regions) {
				BlastHitRegion bhr = (BlastHitRegion) r;
				n_lines--;
				g.setColor(Color.DARK_GRAY);
				String label = rank +". "+ bhr.getSubject() + " eval=" + bhr.getEvalue() + " len="+bhr.getAlignmentLength();
				if (n_lines < 1) {
					label += " ...";
				}
				g.drawString(label, 605, 12+start_offset);
				start_offset += 20;
				if (n_lines <= 0)
					break;
				rank++;
			}
			g.setColor(Color.BLACK);
		}
		
		if (height < 20)
			height = 20;
		return new Dimension(0, height);
	}
}
