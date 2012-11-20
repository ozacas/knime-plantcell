package au.edu.unimelb.plantcell.io.ws.multialign;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.MultiLineStringValueRenderer;

/*
 * Displays an alignment in CLUSTALW format inside a KNIME table cell (HTML pre-formatted) 
 */
public class FormattedRenderer extends MultiLineStringValueRenderer {
	/**
	 * not used (avoids java warning)
	 */
	private static final long serialVersionUID = 6888195260349272796L;

	public enum FormatType { F_CLUSTALW, F_PLAIN, F_PHYLIP_INTERLEAVED, F_PHYLIP_SEQUENTIAL};
	
	private FormatType m_format;
	
	
	public FormattedRenderer(FormatType format) {
		super(null);
		m_format = format;
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (!(value instanceof MultiAlignmentCell)) {
			return new JLabel();
		}
		MultiAlignmentCell mac = (MultiAlignmentCell) value;
		String alignment = mac.getFormattedAlignment(m_format);
		return super.getTableCellRendererComponent(table, alignment, 
											isSelected, hasFocus, row, column);
	}

	@Override
	public String getDescription() {
		switch (m_format) {
		case F_CLUSTALW:
			return "CLUSTALW";
		case F_PHYLIP_INTERLEAVED:
			return "Phylip (Interleaved)";
		case F_PHYLIP_SEQUENTIAL:
			return "Phylip (Sequential)"; 
		default:
			return "Plain";
		}
	}

	@Override
	public boolean accepts(DataColumnSpec spec) {
		return (spec != null && spec.getType().isCompatible(AlignmentValue.class));
	}

}
