package au.edu.unimelb.plantcell.core.cells;

import javax.swing.Icon;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;
import org.knime.core.data.renderer.StringValueRenderer;
import org.knime.core.node.InvalidSettingsException;

public class SequenceUtilityFactory extends UtilityFactory {
	  /** Singleton icon to be used to display this cell type. */
    private static final Icon ICON = loadIcon(SequenceCell.class, "sequence.png");

    private static final SequenceValueComparator COMPARATOR = new SequenceValueComparator();
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Icon getIcon() {
        return ICON;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataValueComparator getComparator() {
        return COMPARATOR;
    }
    
    /** {@inheritDoc} */
    @Override
    protected DataValueRendererFamily getRendererFamily(
            final DataColumnSpec spec) {
        return new DefaultDataValueRendererFamily(StringValueRenderer.INSTANCE, 
        		SummarySequenceRenderer.INSTANCE,
        		GraphicalTrackRenderer.INSTANCE,
        		ThreeLetterSequenceRenderer.INSTANCE
        		);
    }

    /**
     * 
     * @param accsn
     * @param seq  string value to use. Whitespace is removed by this call.
     * @param st
     * @return the newly created cell
     * @throws InvalidSettingsException if the sequence is invalid with respect to its sequence type
     */
	public static SequenceCell createSequenceCell(String accsn, String seq, SequenceType st) throws InvalidSettingsException {
		String no_ws_seq = seq.replaceAll("\\s+", "");
		return new SequenceCell(st, accsn, no_ws_seq);
	}

	public static DataCell createSequenceCell(String accsn, String descr, String seq, SequenceType st) 
		throws InvalidSettingsException {
		SequenceCell sc = createSequenceCell(accsn, seq, st);
		if (descr.length() > 0) {
			sc.addComment(new Comment(CommentType.Description, descr));
		}
		return sc;
	}
}
