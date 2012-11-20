package au.edu.unimelb.plantcell.misc.biojava;

import org.knime.core.data.*;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.biojava.bio.symbol.*;
import org.biojava.bio.seq.*;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;


public class SequenceTranslationProcessor extends BioJavaProcessorTask {
	private boolean m_convert_dna2prot;
	private boolean m_convert_rna2prot;
	private boolean m_convert_dna2rna;
	
	public SequenceTranslationProcessor() {
	}
	
	@Override
	public String getCategory() {
		return "Translation";
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new SequenceTranslationProcessor();
	}
	
	public void init(BioJavaProcessorNodeModel m, String task) {
		setOwner(m);
		m_convert_dna2prot = false;
		m_convert_rna2prot = false;
		m_convert_dna2rna  = false;
		task = task.toLowerCase().trim();
		if (task.endsWith("dna to protein sequence")) {
			m_convert_dna2prot = true;
		} else if (task.endsWith("rna to protein sequence")) {
			m_convert_rna2prot = true;
		} else {
			m_convert_dna2rna = true;
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public String[] getNames() {
		 return new String[] { "Convert DNA to RNA (Universal translation only)", 
	        "Convert RNA to Protein Sequence", 
	        "Convert DNA to Protein Sequence" };
	}
	
	/** {@inheritDoc} */
	@Override
	public String getHTMLDescription(String task) {
		return "<html>Computes a translation of sequence from DNA->RNA, RNA->AA or DNA->AA based "+
		"based on the node configuration. Currently, back translation from AA to DNA is not supported by this node.";
	}
	
	public void execute(ColumnIterator ci, ExecutionContext exec,
			NodeLogger l, BufferedDataTable[] inData, BufferedDataContainer c1)
			throws Exception {
		
		int done = 0;
		int n_rows = inData[0].getRowCount();
		while (ci.hasNext()) {
			DataCell c = ci.next();
			if (c == null || c.isMissing() || !(c instanceof SequenceValue))
				continue;
			SequenceValue sv = (SequenceValue) c;
			
			// skip missing sequences -- TODO: should we put into output table?
			if (sv.getLength() < 1)
				continue;
			SymbolList sl = asBioJava(sv);
			String seq;
			
			if (m_convert_dna2rna) {
				// convert DNA sequence to RNA
				// ensure multiple of 3 (trim excess)
				if (sl.length() % 3 != 0) {
					sl = sl.subList(1, sl.length() - (sl.length() % 3));
				}
				SymbolList rna = DNATools.toRNA(sl);
				seq = rna.seqString();
			} else if (m_convert_rna2prot){
				// convert RNA to protein
				// ensure multiple of 3 (trim excess)
				if (sl.length() % 3 != 0) {
					sl = sl.subList(1, sl.length() - (sl.length() % 3));
				}
				seq = RNATools.translate(sl).seqString();
			} else if (m_convert_dna2prot) {
				sl = DNATools.toRNA(sl);
				// ensure multiple of 3 (trim excess)
				if (sl.length() % 3 != 0) {
					sl = sl.subList(1, sl.length() - (sl.length() % 3));
				}
				SymbolList prot = RNATools.translate(sl);
				seq = prot.seqString();
			} else {
				throw new InvalidSettingsException("Unknown conversion -- implementation bug!");
			}
			DataCell[] cells = new DataCell[1];
			cells[0] = new StringCell(seq);
			c1.addRowToTable(new JoinedRow(ci.lastRow(), new DefaultRow(ci.lastRowID(), cells)));
			done++;
			if (done % 100 == 0) {
				exec.checkCanceled();
				exec.setProgress(((double)done)/n_rows, "Processed sequence "+sv.getID());
			}
		}
	}

	public DataTableSpec get_table_spec() {
		DataColumnSpec[] allColSpecs = new DataColumnSpec[1];
        allColSpecs[0] = 
            new DataColumnSpecCreator("Converted Sequence", StringCell.TYPE).createSpec();
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
		return outputSpec;
	}

	@Override
	public boolean isMerged() {
		return true;
	}

}
