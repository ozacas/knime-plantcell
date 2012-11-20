package au.edu.unimelb.plantcell.core.cells;

import java.io.IOException;

import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;

public class Comment implements SerializableInterface<Comment> {
	private CommentType m_type;
	private String      m_text;
	
	public Comment() {
		this("");
	}
	
	public Comment(String text) {
		this(CommentType.Description, text);
	}
	
	public Comment(CommentType ct, String text) {
		m_type = ct;
		m_text = text;
	}

	public CommentType getType() {
		return m_type;
	}
	
	public String getText() {
		return m_text;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Comment) {
			Comment c2 = (Comment) o;
			return (m_type.equals(c2.m_type) && m_text.equals(c2.m_text));
		}
		return false;
	}
	
	@Override
	public void serialize(DataCellDataOutput output) throws IOException {
		output.writeUTF(m_type.name());
		output.writeUTF(m_text);
	}

	@Override
	public Comment deserialize(DataCellDataInput input) throws IOException {
		// NB: correct comment subclass is passed is as this (m_type has already been read to do that) so...
		m_text = input.readUTF();
		return this;
	}

	public boolean hasType(CommentType ct) {
		return getType().equals(ct);
	}
}
