package au.edu.unimelb.plantcell.core.biojava.tasks;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

public class ResidueAnalysisTask extends BioJavaProcessorTask {
	private boolean m_want_percent = false;
	
	@Override
	public String getCategory() {
		return "Statistics";
	}
	
	@Override
	public void init(String task_name, int col) throws Exception {
		super.init(task_name, col);
		m_want_percent = (task_name.indexOf("%") >= 0);
	}
	
	@Override
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] cols = new DataColumnSpec[13];
		DataType dt = IntCell.TYPE;
		String s = "";
		if (m_want_percent) {
			dt = DoubleCell.TYPE;
			s = " %";
		}
		cols[0] = new DataColumnSpecCreator("Neutral residues"+s, dt).createSpec();
		cols[1] = new DataColumnSpecCreator("Non-polar residues"+s, dt).createSpec();
		cols[2] = new DataColumnSpecCreator("Slightly polar residues (C&W)"+s, dt).createSpec();
		cols[3] = new DataColumnSpecCreator("Polar residues"+s, dt).createSpec();
		cols[4] = new DataColumnSpecCreator("Basic residues"+s, dt).createSpec();
		cols[5] = new DataColumnSpecCreator("Acidic residues"+s, dt).createSpec();
		cols[6] = new DataColumnSpecCreator("Other"+s, dt).createSpec();
		cols[7] = new DataColumnSpecCreator("Non-polar and hydrophobic residues"+s, dt).createSpec();
		cols[8] = new DataColumnSpecCreator("Polar and hydrophilic residues"+s, dt).createSpec();
		cols[9] = new DataColumnSpecCreator("Uncharged polar hydrophilic residues"+s, dt).createSpec();
		cols[10]= new DataColumnSpecCreator("Charged polar hydrophilic residues"+s, dt).createSpec();
		cols[11]= new DataColumnSpecCreator("Positively charged polar hydrophilic residues"+s, dt).createSpec();
		cols[12]= new DataColumnSpecCreator("Negatively charged polar hydrophilic residues"+s, dt).createSpec();
		return cols;
	}

	@Override
	public String[] getNames() { 
		// NB: new tasks must be appended for backward compatibility
		return new String[] {"Add counts of polar, non-polar, acid and basic residues",
				"Add % amino acids for polar, non-polar, acid and basic classifications"}; 
	}
	
	@Override
	public String getHTMLDescription(String task) {
		return "<html>Computes and adds separate columns for the number of amino acids which are polar, neutral," +
					" acidic and non-polar. Hydrophobic and hydrophilic residues are also separately counted.";
	}
	
	@Override
	public DataCell[] getCells(DataRow row) {
		SequenceValue sv = getSequenceForRow(row);
		if (sv == null || !sv.getSequenceType().isProtein())
			return missing_cells(getColumnSpecs().length);
		
		String prot = sv.getStringValue().toUpperCase();
		int neutrals = 0;
		int non_polar= 0;
		int polar    = 0;
		int slightly_polar=0;
		int basic    = 0;
		int acidic   = 0;
		int other    = 0;
		int non_polar_hydrophobic=0;		// non polar AND hydrophobic
		int polar_hydrophilic=0;			// polar AND hydrophilic
		int uncharged_polar_hydrophilic=0;
		int charged_polar_hydrophilic = 0;
		int pos_charge_polar_hydrophilic=0;
		int neg_charge_polar_hydrophilic=0;
		for (int i=0; i<prot.length(); i++) {
			char c = prot.charAt(i);
			if (is_neutral(c)) {
				neutrals++;
			} else if (is_basic(c)) {
				basic++;
			} else if (is_acidic(c)) {
				acidic++;
			} else {	// stop codon or gap or something?
				other++;
				continue;		// make no further assessment of this 'residue'
			}
			
			if (is_non_polar(c)) {
					non_polar++;
					if (is_hydrophobic(c)) 
						non_polar_hydrophobic++;
			} else if (is_polar(c)) {
					polar++;
					if (is_hydrophilic(c)) {
						polar_hydrophilic++;
						if (is_uncharged_polar_hydrophilic(c)) {
							uncharged_polar_hydrophilic++;
						} else if (is_charged_polar_hydrophilic(c)) {
							charged_polar_hydrophilic++;
							if (is_basic(c)) 
								pos_charge_polar_hydrophilic++;
							else 
								neg_charge_polar_hydrophilic++;
						}
					}
			} else if (is_slightly_polar(c)) {
					slightly_polar++;
					if (is_hydrophilic(c)) {
						polar_hydrophilic++;
						if (is_uncharged_polar_hydrophilic(c)) {
							uncharged_polar_hydrophilic++;
						} else if (is_charged_polar_hydrophilic(c)) {
							charged_polar_hydrophilic++;
							if (is_basic(c)) 
								pos_charge_polar_hydrophilic++;
							else 
								neg_charge_polar_hydrophilic++;
						}
					}
			} 
		}
		
		DataCell[] cells = new DataCell[13];
		if (m_want_percent) {
			double len = sv.getLength();
			cells[0] = new DoubleCell(((double)neutrals)*100.0/len);
			cells[1] = new DoubleCell(((double)non_polar)*100.0/len);
			cells[2] = new DoubleCell(((double)slightly_polar)*100.0/len);
			cells[3] = new DoubleCell(((double)polar)*100.0/len);
			cells[4] = new DoubleCell(((double)basic)*100.0/len);
			cells[5] = new DoubleCell(((double)acidic)*100.0/len);
			cells[6] = new DoubleCell(((double)other)*100.0/len);
			cells[7] = new DoubleCell(((double)non_polar_hydrophobic)*100.0/len);
			cells[8] = new DoubleCell(((double)polar_hydrophilic)*100.0/len);
			cells[9] = new DoubleCell(((double)uncharged_polar_hydrophilic)*100.0/len);
			cells[10]= new DoubleCell(((double)charged_polar_hydrophilic)*100.0/len);
			cells[11]= new DoubleCell(((double)pos_charge_polar_hydrophilic)*100.0/len);
			cells[12]= new DoubleCell(((double)neg_charge_polar_hydrophilic)*100.0/len);
		} else {
			cells[0] = new IntCell(neutrals);
			cells[1] = new IntCell(non_polar);
			cells[2] = new IntCell(slightly_polar);
			cells[3] = new IntCell(polar);
			cells[4] = new IntCell(basic);
			cells[5] = new IntCell(acidic);
			cells[6] = new IntCell(other);
			cells[7] = new IntCell(non_polar_hydrophobic);
			cells[8] = new IntCell(polar_hydrophilic);
			cells[9] = new IntCell(uncharged_polar_hydrophilic);
			cells[10]= new IntCell(charged_polar_hydrophilic);
			cells[11]= new IntCell(pos_charge_polar_hydrophilic);
			cells[12]= new IntCell(neg_charge_polar_hydrophilic);
		}
		return cells;
	}

	
	private boolean is_charged_polar_hydrophilic(char c) {
		return (c == 'D' || c == 'E' || c == 'K' || c == 'R' || c == 'H');
	}

	/**************************************************************************************************
	 * These methods are based on data at http://chemistry.gravitywaves.com/CHE450/04_Amino%20AcidsProteins.htm
	 * ************************************************************************************************
	 */
	private boolean is_uncharged_polar_hydrophilic(char c) {
		return (c == 'N' || c == 'Q' || c == 'C' || c == 'S' || c == 'T' || c == 'Y');
	}

	/**************************************************************************************************
	 * These methods are based on data at http://www.elmhurst.edu/~chm/vchembook/561aminostructure.html
	 * ************************************************************************************************
	 */
	
	private boolean is_neutral(char c) {
		return (c == 'A' || c == 'N' || c == 'C' || c == 'Q' || c == 'G' || c == 'I' || c == 'L' || c == 'M' ||
				c == 'F' || c == 'P' || c == 'S' || c == 'T' || c == 'W' || c == 'Y' || c == 'V');
	}
	
	private boolean is_basic(char c) {
		return (c == 'R' || c == 'H' || c == 'K');
	}
	
	private boolean is_acidic(char c) {
		return (c == 'E' || c == 'D');
	}
	
	private boolean is_non_polar(char c) {
		return (c == 'A' || c == 'G' || c == 'I' || c == 'L' || c == 'M' || c == 'F' || c == 'P' || c == 'V');
	}
	
	private boolean is_polar(char c) {
		return (c == 'Y' || c == 'T' || c == 'S' || c == 'K' || c == 'H' || c == 'Q' || 
				c == 'E' || c == 'D' || c == 'N' || c == 'R');
	}
	
	private boolean is_slightly_polar(char c) {
		return (c == 'C' || c == 'W');
	}
	
	/**************************************************************************************************
	 * These methods are based on data at http://www.elmhurst.edu/~chm/vchembook/561aminostructure.html
	 * ************************************************************************************************
	 */
	
	private boolean is_hydrophobic(char c) {
		return (c == 'L' || c == 'I' || c == 'F' || c == 'W' || c == 'V' || 
				c == 'M' || c == 'C' || c == 'Y' || c == 'A');
	}
	
	private boolean is_hydrophilic(char c) {
		return (c == 'R' || c == 'K' || c == 'N' || c == 'H' || c == 'P' || c == 'D');
	}
}
