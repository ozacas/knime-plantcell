package au.edu.unimelb.plantcell.core.cells;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.def.StringCell;

/**
 * A single feature comment of a sequence eg. PFAM, GO,...
 * A comment relates to the sequence as a whole, perhaps as part of a larger dataset.
 * CommentAnnotations use a controlled vocabulary for key nodes to operate.
 * 
 * @author andrew.cassin
 *
 */
public class CommentsAnnotation extends SequenceAnnotation {
	private List<Comment> m_comments = null;		// optimisation: dont create objects if there are no comments...
	
	public CommentsAnnotation() {
	}
	
	/**
	 * 
	 * @param text
	 * @param ct
	 */
	public CommentsAnnotation(Comment c) {
		addComment(c);
	}
	
	public void addComment(Comment c) {
		assert(c != null);
		addComment(c, true);
	}
	
	public void addComment(Comment c, boolean permit_duplicates) {
		if (!permit_duplicates && m_comments != null && m_comments.indexOf(c) >= 0)
			return;
		if (m_comments == null)
			m_comments = new ArrayList<Comment>();
		m_comments.add(c);
	}
	
	@Override
	public int countAnnotations() {
		return (m_comments == null) ? 0 : m_comments.size();
	}

	@Override
	public AnnotationType getAnnotationType() {
		return AnnotationType.COMMENTS;
	}

	@Override
	public void serialize(DataCellDataOutput output) throws IOException {
		output.writeUTF(getAnnotationType().name());
		int cnt = countAnnotations();
		output.writeInt(cnt);
		for (int i=0; i<cnt; i++) {
			m_comments.get(i).serialize(output);
		}
	}

	@Override
	public SequenceAnnotation deserialize(DataCellDataInput input)
			throws IOException {
		// NB: annotation type has already been read by the track deserializer
		int cnt = input.readInt();
		if (cnt == 0) {
			m_comments  = null;
			return this;
		}
		m_comments = new ArrayList<Comment>();
		for (int i=0; i<cnt; i++) {
			CommentType ct = CommentType.valueOf(input.readUTF());
			Comment c = make_comment(ct);
			c.deserialize(input);
			m_comments.add(c);
		}
		return this;
	}

	/**
	 * Correct a suitable subclass for the type of comment
	 */
	public Comment make_comment(CommentType ct) {
		// HACK TODO: for now, only one type of comment
		return new Comment(ct, "");
	}
	
	/**
	 * Returns the list of comments defined in the annotation
	 */
	public Iterable<Comment> getComments() {
		return m_comments;
	}
	
	@Override
	public String toString() {
		int n = m_comments.size();
		if (n > 1) {
			return "Total of "+n+" comments available.";
		} else if (n == 1) {
			for (Comment c : getComments()) {
				return c.getText();
			}
			return null;		// should not get here!
		} else {
			return "No comments available.";
		}
	}

	@Override
	public List<DataColumnSpec> asColumnSpec(String prefix) {
		ArrayList<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
		cols.add(new DataColumnSpecCreator(prefix+": Type",          StringCell.TYPE).createSpec());
		cols.add(new DataColumnSpecCreator(prefix+": Description", StringCell.TYPE).createSpec());
		
		return cols;
	}
}
