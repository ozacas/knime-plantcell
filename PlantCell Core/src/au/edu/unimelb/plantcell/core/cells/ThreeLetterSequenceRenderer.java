package au.edu.unimelb.plantcell.core.cells;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.ProteinTools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.seq.Sequence;
import org.knime.core.data.renderer.MultiLineStringValueRenderer;

/**
 * If, like me, you struggle to remember all those pesky amino acid's you'll appreciate this ;-)
 * 
 * @author andrew.cassin
 *
 */
public class ThreeLetterSequenceRenderer extends MultiLineStringValueRenderer {
	public final static ThreeLetterSequenceRenderer INSTANCE = new ThreeLetterSequenceRenderer("Three-letter sequence residues");
	
	public ThreeLetterSequenceRenderer(String description) {
		super(description);
	}

	/**
	 * not used
	 */
	private static final long serialVersionUID = 8024197741862648009L;

	@Override
	public void setValue(Object val) {
		if (val instanceof SequenceValue) {
			SequenceValue sv = (SequenceValue) val;
			SequenceType st = sv.getSequenceType();
			String id = sv.getID();
			Sequence seq = make_sequence(st, sv.getStringValue(), id);
			if (seq == null) {
				super.setValue(val);
				return;
			}
			
			try {
				StringBuilder sb = new StringBuilder(10 * 1024);
				int cnt = 0;
				for (int i=0; i<seq.length(); i++) {
					//Logger.getAnonymousLogger().info(seq.length()+ " "+ i);

					sb.append(seq.symbolAt(i+1).getName());
					if (cnt++ < 40) {
						sb.append(' ');
					} else {
						sb.append('\n');
						cnt = 0;
					}
				}
				super.setValue(sb.toString());
			} catch (Exception e) {
				super.setValue(val);
			}
		} else {
			super.setValue(val);
		}
	}

	private Sequence make_sequence(SequenceType st, String seq, String name) {
		Sequence ret = null;
		try {
			if (st == SequenceType.DNA) {
				ret = DNATools.createDNASequence(seq, name);
			} else if (st == SequenceType.RNA) {
				ret = RNATools.createRNASequence(seq, name);
			} else if (st == SequenceType.AA || st == SequenceType.UNKNOWN) {		// HACK: handle unknown as protein?
				ret = ProteinTools.createProteinSequence(seq, name);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return ret;
	}
}
