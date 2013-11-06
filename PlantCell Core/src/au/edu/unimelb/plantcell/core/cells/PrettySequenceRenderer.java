package au.edu.unimelb.plantcell.core.cells;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JLabel;

import org.knime.core.data.renderer.DataValueRenderer;

/**
 * Pretty format a sequence for the user. Implements 60 characters from the sequence per line
 * organised as six blocks of 10. Displays metadata about the sequence too and offsets for user
 * convenience.
 * 
 * @author andrew.cassin
 *
 */
public class PrettySequenceRenderer extends GraphicalTrackRenderer {
	/**
	 * not used internally
	 */
	private static final long serialVersionUID = 1207152861297781179L;
	/**
	 * used by the {@link SequenceUtilityFactory} to render {@link SequenceValue}'s
	 */
	public static final DataValueRenderer INSTANCE = new PrettySequenceRenderer();
	
	public PrettySequenceRenderer() {
		super("");
	}
	

	@Override
	public String getDescription() {
		return "Pretty formatted sequence";
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(440,200);
	}
	
	@Override
	public void paint(Graphics g) {
		if (hasSequence()) {
			g.setColor(Color.DARK_GRAY);
			StringBuilder sb = new StringBuilder();
			SequenceValue sv = getSequence();
			sb.append(sv.getID());
			sb.append(" length="+sv.getLength()+" ");
			sb.append(sv.hasDescription() ? sv.getDescription() : "");
			g.setFont(LABEL_FONT);
			g.drawString(sb.toString(), 5, 16);
						
			int n_blocks = sv.getLength() / 10;
			if (n_blocks % 10 > 0)
					n_blocks++;
			String seq = sv.getStringValue();
			
			
			int n = 6;
			if (n_blocks < n) {
				n = n_blocks;
			}
			
			// horizontal offsets into sequence
			g.setColor(Color.GRAY);
			g.setFont(Font.getFont("Arial 9"));
			for (int i=0; i<n; i++) {
				g.drawString(""+(i*10+1), 100 + i * 120, 40);
			}
			
			
			// residues
			g.setColor(Color.black);
			g.setFont(MONO_FONT);
			int line = 0;
			for (int i=0; i<n_blocks; i++) {
				int start = i * 10;
				int end = start + 10;
				if (end > sv.getLength())
					end = sv.getLength();
				if (end > start) {
					String block = seq.substring(start, end);
					int pos = (i % 6);
					
					if (pos == 0 && i>0)
						line++;
					g.drawString(block, 100 + pos * 120, 60 + line*22);
				}
			}
			
			// vertical offsets into sequence (decimal for biologists ;)
			g.setColor(Color.GRAY);
			g.setFont(Font.getFont("Arial 9"));
			for (int i=0; i<=line; i++) {
				g.drawString(""+(60 * i+1), 10, 60 + i * 22);
			}
		}
	}

	@Override
	public Component getRendererComponent(Object in) {
		if (in != null && in instanceof SequenceValue) {
			SequenceValue sv = (SequenceValue) in;
			this.setSequence(sv);
			return this;
		}
		
		return new JLabel("No sequence for display!");
	}


}
