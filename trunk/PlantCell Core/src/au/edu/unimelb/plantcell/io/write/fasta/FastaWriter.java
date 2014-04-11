package au.edu.unimelb.plantcell.io.write.fasta;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * A class to support writing FASTA files
 * @author andrew.cassin
 *
 */
public class FastaWriter {
	private File m_file;
	private Map<UniqueID, SequenceValue> m_seqs;
	
	public FastaWriter(File fasta_file_to_create, Iterator<SequenceValue> seqs) {
		m_file = fasta_file_to_create;
		m_seqs = new HashMap<UniqueID,SequenceValue>();
		boolean first = true;
		while (seqs.hasNext()) {
			SequenceValue sv = seqs.next();
			m_seqs.put(new UniqueID(first), sv);
			first = false;
		}
	}
	
	public FastaWriter(File tmp_fasta, Map<UniqueID, SequenceValue> batch_map) {
		m_file = tmp_fasta;
		m_seqs = batch_map;
	}

	/**
	 * Deletes the specified file refered to in the constructor and then writes each sequence
	 * in FASTA format to the specified file. 
	 * @throws IOException if any error occurs
	 */
	public void write() throws IOException {
		m_file.delete();
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(m_file)));
		for (UniqueID id : m_seqs.keySet()) {
			SequenceValue sv = m_seqs.get(id);
			pw.println(">"+id);
			pw.println(sv.getStringValue());
		}
		pw.close();
	}
}
