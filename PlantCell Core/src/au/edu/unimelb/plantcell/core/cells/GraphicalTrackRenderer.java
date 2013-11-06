package au.edu.unimelb.plantcell.core.cells;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.DataValueRenderer;

/**
 * This code is as ugly as sin. Abandon all hope... perhaps a design pattern or two? needs some
 * flexibility for the user...
 * 
 * @author andrew.cassin
 *
 */
public class GraphicalTrackRenderer extends Canvas implements DataValueRenderer {
	protected final static Font LABEL_FONT = new Font ("Sanserif", Font.BOLD, 12);
	protected final static Font MONO_FONT  = new Font(Font.MONOSPACED, Font.PLAIN, 14);
	
	/**
	 * not used
	 */
	private static final long serialVersionUID = 7152594053688193342L;
	
	// key properties shared by all renderers
	public static final Map<String,Integer> m_props = new HashMap<String,Integer>();
	static {
		m_props.put("label.width", 200);
		m_props.put("label.x", 0);
		m_props.put("label.y", 5);
		
		m_props.put("track.width", 400);
		m_props.put("track.x", 200);
		m_props.put("track.y", 5);
		m_props.put("track.height.minimum", 20);
		
		m_props.put("offset", 0);		    // each track adds to this during paintTrack()
		
		m_props.put("legend.width", 0);		// 0 denotes unlimited
		m_props.put("legend.x", m_props.get("track.x") + m_props.get("track.width"));
		m_props.put("legend.height", m_props.get("track.height.minimum"));
	};
	

	// label in column heading
	private static final String DESCR = "Graphical tracks";
	private SequenceValue m_seq;
	
	public GraphicalTrackRenderer(String description) {
		super();
		m_seq = null;
	}

	protected boolean hasSequence() {
		return (m_seq != null && m_seq.getLength() > 0);
	}
	
	protected SequenceValue getSequence() {
		return m_seq;
	}
	
	protected void setSequence(SequenceValue sv) {
		m_seq = sv;
	}
	
	/**
	 * used by the {@link SequenceUtilityFactory} to render {@link SequenceValue}'s
	 */
	public static final GraphicalTrackRenderer INSTANCE = new GraphicalTrackRenderer(DESCR);

	
	@Override
	public String getDescription() {
		return DESCR;
	}
	
	@Override
	public Component getRendererComponent(Object o) {
		if (o != null && o instanceof SequenceValue) {
			SequenceValue sv = (SequenceValue) o;
			boolean has_regions = false;
			if (sv.countTracks() > 0) {
				for (Track t : sv.getTracks()) {
					if (t.hasRegions()) {
						has_regions = true; 
						break;
					}
						
				} 
				if (has_regions) {
					this.setSequence(sv);
					return this;
				}
			}
			// else fallthru...
		} 
		
		return new JLabel("No visual tracks for display!");
	}
	

	@Override
	public boolean accepts(DataColumnSpec cs) {
		return (cs != null && cs.getType().isCompatible(SequenceValue.class));
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable arg0, Object arg1,
			boolean arg2, boolean arg3, int arg4, int arg5) {
		return getRendererComponent(arg1);
	}

	@Override
	public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") JList arg0, Object arg1,
			int arg2, boolean arg3, boolean arg4) {
		return getRendererComponent(arg1);
	}
	
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(600,40);
	}
	
	@Override
	public Dimension getPreferredSize() {
		return getMinimumSize();
	}
	
	@Override 
	public Dimension getMaximumSize() {
		return getMinimumSize();
	}

	@Override
	public void paint(Graphics g) {
		if (hasSequence())
			return;
		Font oldFont = g.getFont();
		g.setColor(Color.BLACK);
		StringBuilder sb = new StringBuilder();
		sb.append(m_seq.getID());
		sb.append(" length="+m_seq.getLength()+" ");
		sb.append((m_seq.getDescription() != null) ? m_seq.getDescription() : "");
		g.setFont(LABEL_FONT);
		g.drawString(sb.toString(), 5, 16);
		g.setFont(oldFont);
		m_props.put("offset", new Integer(20));
		for (Track t : m_seq.getTracks()) {
			TrackRendererInterface r = t.getRenderer();
			if (r != null) {
				Dimension d = r.paintTrack(m_props, g, m_seq, t);
				Integer offset = (m_props.get("offset") + (int)d.getHeight() + 5);
				m_props.put("offset", offset);
			}
		}
	}
	
}
