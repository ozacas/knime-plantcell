package au.edu.unimelb.plantcell.algorithms.StringFinder;

import java.awt.Color;
import java.util.List;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.DenseBitVector;

/**
 * Reports matches using the colour for the table row the pattern came from which did the match. The table
 * of 
 * 
 * @author andrew.cassin
 *
 */
public class HighlightMatchReporter implements MatchReporter {
	
	public HighlightMatchReporter() {
	}
	
	@Override
	public DataCell report(FindGlobalNodeModel m, DataCell str_cell)
			throws Exception {
		String      str = str_cell.toString();
		StringBuffer sb = new StringBuffer(str.length());
		sb.append("<html><pre>");		// HACK TODO FIXME: cell will display as HTML (partial standards compliance)
		DenseBitVector              bv = m.getResultsBitVector();
		List<Extent>                 e = m.getMatchPos();
		
		// be defensive against zero length search string (ok, they should never match... except stupid patterns)
		int len = (int) bv.length();
		if (len < 1)
			len = 1;
		int[] colors = new int[len];
		for (int i=0; i<colors.length; i++) {
			colors[i] = -1;
		}
		for (int i=0; i<e.size(); i++) {
			Extent ex = e.get(i);
			for (int j=ex.m_start; j<ex.m_end; j++) {
				if (j < colors.length)
					colors[j] = i;
			}
		}
		
		// traverse the input string finding matches and colour-ise them as appropriate
		assert(str.length() == bv.length());
		for (int i=0; i<bv.length(); i++) {
			if (bv.get(i)) {
				Color c = Color.red;
				if (colors[i] >= 0) {
					c = e.get(colors[i]).getColour();
				}
				String rgb = Integer.toHexString(c.getRGB());
			
				sb.append("<font color=\"#" + rgb.substring(2) + "\">");
				sb.append(str.charAt(i));
				sb.append("</font>");
			} else {
				sb.append(str.charAt(i));
			}
		}
		return new StringCell(sb.toString());
	}

}
