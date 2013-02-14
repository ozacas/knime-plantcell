package au.edu.unimelb.plantcell.core.biojava.tasks;


import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.SymbolList;
import org.biojava.bio.symbol.SymbolListViews;
import org.biojava.bio.symbol.TranslationTable;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.StringCell;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;


public class AlternateTranslationTask extends BioJavaProcessorTask {
	private TranslationTable[] m_tables;
	private String[]           m_table_names;
	
	public AlternateTranslationTask() {
		super();
	}
	
	@Override
	public String getCategory() {
		return "Translation";
	}
	
	public void init(String task_name, int col) throws Exception {
		super.init(task_name, col);
		
		// load translation tables
		m_tables = new TranslationTable[16];
		m_table_names = new String[m_tables.length];
		m_tables[0] = RNATools.getGeneticCode(TranslationTable.ALT_YEAST_NUC);
		m_table_names[0] = TranslationTable.ALT_YEAST_NUC;
		m_tables[1] = RNATools.getGeneticCode(TranslationTable.ASCID_MITO);
		m_table_names[1] = TranslationTable.ASCID_MITO;
		m_tables[2] = RNATools.getGeneticCode(TranslationTable.BACTERIAL);
		m_table_names[2] = TranslationTable.BACTERIAL;
		m_tables[3] = RNATools.getGeneticCode(TranslationTable.BLEPH_MNUC);
		m_table_names[3] = TranslationTable.BLEPH_MNUC;
		m_tables[4] = RNATools.getGeneticCode(TranslationTable.CHLORO_MITO);
		m_table_names[4] = TranslationTable.CHLORO_MITO;
		m_tables[5] = RNATools.getGeneticCode(TranslationTable.CILIATE_NUC);
		m_table_names[5] = TranslationTable.CILIATE_NUC;
		m_tables[6] = RNATools.getGeneticCode(TranslationTable.ECHIN_MITO);
		m_table_names[6] = TranslationTable.ECHIN_MITO;
		m_tables[7] = RNATools.getGeneticCode(TranslationTable.EUPL_NUC);
		m_table_names[7] = TranslationTable.EUPL_NUC;
		m_tables[8] = RNATools.getGeneticCode(TranslationTable.FWORM_MITO);
		m_table_names[8] = TranslationTable.FWORM_MITO;
		m_tables[9] = RNATools.getGeneticCode(TranslationTable.INVERT_MITO);
		m_table_names[9] = TranslationTable.INVERT_MITO;
		m_tables[10] = RNATools.getGeneticCode(TranslationTable.MOLD_MITO);
		m_table_names[10] = TranslationTable.MOLD_MITO;
		m_tables[11] = RNATools.getGeneticCode(TranslationTable.SCENE_MITO);
		m_table_names[11] = TranslationTable.SCENE_MITO;
		m_tables[12] = RNATools.getGeneticCode(TranslationTable.TREMA_MITO);
		m_table_names[12] = TranslationTable.TREMA_MITO;
		m_tables[13] = RNATools.getGeneticCode(TranslationTable.UNIVERSAL);
		m_table_names[13] = TranslationTable.UNIVERSAL;
		m_tables[14] = RNATools.getGeneticCode(TranslationTable.VERT_MITO);
		m_table_names[14] = TranslationTable.VERT_MITO;
		m_tables[15] = RNATools.getGeneticCode(TranslationTable.YEAST_MITO);
		m_table_names[15] = TranslationTable.YEAST_MITO;
	}
	
	/** {@inheritDoc} */
	@Override
	public String[] getNames() {
        return new String[] { "Alternate translation of DNA to Protein (all built-in tables)" };
	}
	
	/** {@inheritDoc} */
	@Override
	public String getHTMLDescription(String task_name) {
		return "<html>Computes all translations from DNA to Protein sequence, "+
		"using all available codon tables from BioJava. Each translation is in a"+
		" separate column. <b>No</b> frame translation is performed.";
	}
	
	@Override
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] allColSpecs = new DataColumnSpec[m_tables.length];
		for (int i=0; i<m_tables.length; i++) {
			allColSpecs[i] = 
				new DataColumnSpecCreator("Translation "+m_table_names[i], StringCell.TYPE).createSpec();
		}
		return allColSpecs;
	}

	@Override
	public DataCell[] getCells(DataRow row) {
		SequenceValue sv = getSequenceForRow(row);
		if (sv == null || !sv.getSequenceType().isNucleotides())
			return missing_cells(m_tables.length);
		
		try {
			SymbolList   dna = asBioJava(sv);
			DataCell[] cells = new DataCell[m_tables.length];
			for (int i=0; i<m_tables.length; i++) {
				// trim if not multiple of 3
				if (dna.length() %3 != 0) {
					dna = dna.subList(1, dna.length() - (dna.length() % 3));
				}
				SymbolList rna  = DNATools.toRNA(dna);
				SymbolList syms = SymbolListViews.windowedSymbolList(rna, 3);
				SymbolList prot = SymbolListViews.translate(syms, m_tables[i]);
				cells[i]        = new StringCell(prot.seqString());
			}
			return cells;
		} catch (Exception e) {
			e.printStackTrace();
			return missing_cells(m_tables.length);
		}
	}
}
