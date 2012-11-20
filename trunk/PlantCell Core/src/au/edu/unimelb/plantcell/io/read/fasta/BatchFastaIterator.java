package au.edu.unimelb.plantcell.io.read.fasta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Retrieves sequences using {@link FastaIterator} with the chosen number of sequences
 * at a time. Uses the {@link SequenceValue} interface to record the details associated
 * with each sequence and for convenient downstream analysis.
 * 
 * @author andrew.cassin
 *
 */
public class BatchFastaIterator implements Iterator<List<SequenceValue>> {
	public final static int DEFAULT_TOTAL_MB_LIMIT = 20 * 1024 * 1024;	// at most 20MB per batch (unless one sequence exceeds this!)
	
	private FastaIterator m_it;
	private int m_seq_cnt_limit;
	private int m_seq_size_limit;
	
	/**
	 * Read sequences from a fasta file with no more than <code>batch_size</code> 
	 * sequences in it. Each sequence is given the <code>SequenceType</code> specified
	 * and the data is obtained from the specified <code>fasta_file</code>. An exception
	 * is thrown if the file cannot be processed. The other constructor is recommended
	 * as it also specifies a limit on the total sequence per batch.
	 * 
	 * @param fasta_file
	 * @param st
	 * @param batch_size
	 * @throws Exception
	 */
	public BatchFastaIterator(File fasta_file, SequenceType st, int batch_size) throws Exception {
		this(fasta_file, st, batch_size, 0);	// 0 means no size limit
	}

	/**
	 * Preferred constructor which imposes a total sequence length limit per batch. This
	 * limit may be exceeded if the first sequence exceeds it.
	 * 
	 * @param f
	 * @param st
	 * @param seq_limit
	 * @param size_limit maximum (soft) amount of sequence data per batch (in bytes)
	 * @throws Exception
	 */
	public BatchFastaIterator(File f, SequenceType st, int seq_limit, int size_limit) throws Exception {
		m_it = new FastaIterator(f, st);
		m_seq_cnt_limit = seq_limit;
		m_seq_size_limit= size_limit;
	}
	
	@Override
	public boolean hasNext() {
		return m_it.hasNext();
	}

	@Override
	public List<SequenceValue> next() {
		List<SequenceValue> batch = new ArrayList<SequenceValue>();
		int total_size_bytes = 0;
		do {
			SequenceValue sv = m_it.next();
			total_size_bytes += sv.getLength();
			batch.add(sv);
			if (m_seq_size_limit > 0 && total_size_bytes > m_seq_size_limit)
				break;
		} while (batch.size() < m_seq_cnt_limit && hasNext());
		
		// user convenience: return null if batch is empty at EOF
		if (batch.size() < 1) 
			return null;
		
		return batch;
	}

	@Override
	public void remove() {
		// NO-OP
	}

	public HashMap<UniqueID, SequenceValue> nextAsMap() {
		List<SequenceValue> batch = next();
		if (batch == null)
			return null;
		HashMap<UniqueID,SequenceValue> ret = new HashMap<UniqueID,SequenceValue>();
		boolean first = true;
		for (SequenceValue sv : batch) {
			ret.put(new UniqueID(first), sv);
			first = false;
		}
		return ret;
	}

	
	
	
}
