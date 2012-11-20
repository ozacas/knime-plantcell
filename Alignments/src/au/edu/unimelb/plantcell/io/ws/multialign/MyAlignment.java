package au.edu.unimelb.plantcell.io.ws.multialign;

import pal.alignment.SimpleAlignment;
import pal.misc.Identifier;

public class MyAlignment extends SimpleAlignment {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5502564740524525330L;
	private double m_score;
	private String m_tag_line;
	
	public MyAlignment(Identifier[] ids, String[] seqs) {
		super(ids, seqs, null);
		m_score = 0.0;
		m_tag_line = "";
	}
	
	public MyAlignment(Identifier[] ids, String[] seqs, double score) {
		this(ids, seqs);
		m_score = score;
	}

	public double getScore() {
		return m_score;
	}
	
	public void setTagLine(String tl) {
		m_tag_line= tl;
	}
	public String getTagLine() {
		return m_tag_line;
	}
}
