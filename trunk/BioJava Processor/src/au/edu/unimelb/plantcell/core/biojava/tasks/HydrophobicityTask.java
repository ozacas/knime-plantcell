package au.edu.unimelb.plantcell.core.biojava.tasks;

import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Computes six different hydrophobicity values from published authors and adds them to the table.
 * 
 * @author andrew.cassin
 *
 */
public class HydrophobicityTask extends BioJavaProcessorTask { 
    private static HashMap<String, Double> kyteDoolittle = new HashMap<String, Double>();
    private static HashMap<String, Double> hoppWoods = new HashMap<String, Double>();
    private static HashMap<String, Double> cornette = new HashMap<String, Double>();
    private static HashMap<String, Double> eisenberg = new HashMap<String, Double>();
    private static HashMap<String, Double> janin = new HashMap<String, Double>();
    private static HashMap<String, Double> engelman = new HashMap<String, Double>();

    // WARNING HACK TODO: these values have not been manually checked! Are they right?
    static {
        kyteDoolittle.put("A", 1.80);
        hoppWoods.put("A", -0.50);
        cornette.put("A", 0.20);
        eisenberg.put("A", 0.62);
        janin.put("A", 0.74);
        engelman.put("A", 0.30);
        kyteDoolittle.put("C", 2.50);
        hoppWoods.put("C", -1.00);
        cornette.put("C", 4.10);
        eisenberg.put("C", 0.29);
        janin.put("C", 0.91);
        engelman.put("C", 0.90);
        kyteDoolittle.put("D", -3.50);
        hoppWoods.put("D", 3.00);
        cornette.put("D", -3.10);
        eisenberg.put("D", -0.90);
        janin.put("D", 0.62);
        engelman.put("D", -0.60);
        kyteDoolittle.put("E", -3.50);
        hoppWoods.put("E", 3.00);
        cornette.put("E", -1.80);
        eisenberg.put("E", -0.74);
        janin.put("E", 0.62);
        engelman.put("E", -0.70);
        kyteDoolittle.put("F", 2.80);
        hoppWoods.put("F", -2.50);
        cornette.put("F", 4.40);
        eisenberg.put("F", 1.19);
        janin.put("F", 0.88);
        engelman.put("F", 0.50);
        kyteDoolittle.put("G", -0.40);
        hoppWoods.put("G", 0.00);
        cornette.put("G", 0.00);
        eisenberg.put("G", 0.48);
        janin.put("G", 0.72);
        engelman.put("G", 0.30);
        kyteDoolittle.put("H", -3.20);
        hoppWoods.put("H", -0.50);
        cornette.put("H", 0.50);
        eisenberg.put("H", -0.40);
        janin.put("H", 0.78);
        engelman.put("H", -0.10);
        kyteDoolittle.put("I", 4.50);
        hoppWoods.put("I", -1.80);
        cornette.put("I", 4.80);
        eisenberg.put("I", 1.38);
        janin.put("I", 0.88);
        engelman.put("I", 0.70);
        kyteDoolittle.put("K", -3.90);
        hoppWoods.put("K", 3.00);
        cornette.put("K", -3.10);
        eisenberg.put("K", -1.50);
        janin.put("K", 0.52);
        engelman.put("K", -1.80);
        kyteDoolittle.put("L", 3.80);
        hoppWoods.put("L", -1.80);
        cornette.put("L", 5.70);
        eisenberg.put("L", 1.06);
        janin.put("L", 0.85);
        engelman.put("L", 0.50);
        kyteDoolittle.put("M", 1.90);
        hoppWoods.put("M", -1.30);
        cornette.put("M", 4.20);
        eisenberg.put("M", 0.64);
        janin.put("M", 0.85);
        engelman.put("M", 0.40);
        kyteDoolittle.put("N", -3.50);
        hoppWoods.put("N", 0.20);
        cornette.put("N", -0.50);
        eisenberg.put("N", -0.78);
        janin.put("N", 0.63);
        engelman.put("N", -0.50);
        kyteDoolittle.put("P", -1.60);
        hoppWoods.put("P", 0.00);
        cornette.put("P", -2.20);
        eisenberg.put("P", 0.12);
        janin.put("P", 0.64);
        engelman.put("P", -0.30);
        kyteDoolittle.put("Q", -3.50);
        hoppWoods.put("Q", 0.20);
        cornette.put("Q", -2.80);
        eisenberg.put("Q", -0.85);
        janin.put("Q", 0.62);
        engelman.put("Q", -0.70);
        kyteDoolittle.put("R", -4.50);
        hoppWoods.put("R", 3.00);
        cornette.put("R", 1.40);
        eisenberg.put("R", -2.53);
        janin.put("R", 0.64);
        engelman.put("R", -1.40);
        kyteDoolittle.put("S", -0.80);
        hoppWoods.put("S", 0.30);
        cornette.put("S", -0.50);
        eisenberg.put("S", -0.18);
        janin.put("S", 0.66);
        engelman.put("S", -0.10);
        kyteDoolittle.put("T", -0.70);
        hoppWoods.put("T", -0.40);
        cornette.put("T", -1.90);
        eisenberg.put("T", -0.05);
        janin.put("T", 0.70);
        engelman.put("T", -0.20);
        kyteDoolittle.put("V", 4.20);
        hoppWoods.put("V", -1.50);
        cornette.put("V", 4.70);
        eisenberg.put("V", 1.08);
        janin.put("V", 0.86);
        engelman.put("V", 0.60);
        kyteDoolittle.put("W", -0.90);
        hoppWoods.put("W", -3.40);
        cornette.put("W", 1.00);
        eisenberg.put("W", 0.81);
        janin.put("W", 0.85);
        engelman.put("W", 0.30);
        kyteDoolittle.put("Y", -1.30);
        hoppWoods.put("Y", -2.30);
        cornette.put("Y", 3.20);
        eisenberg.put("Y", 0.26);
        janin.put("Y", 0.76);
        engelman.put("Y", -0.40);
    }

	@Override
	public String getCategory() {
		return "Common";
	}
	
	@Override
	public boolean canWindow() {
		return true;
	}
	
	@Override 
	public String getHTMLDescription(String task) {
		return "<html>Adds six different measures of hydrophobicity from Eisenberg, Hopp-Woods and many others" +
				" to the given protein sequence. If the sequences contains invalid AA, this calculation" +
				" will not emit hydrophobicity data as it is likely meaningless.";
	}
	
	@Override
	public String[] getNames() {
		return new String[] { "Add six different measures of hydrophobicity (eg. Eisenberg, Hopp-Woods)" };
	}
	
	@Override
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] cols = new DataColumnSpec[7];
		cols[0] = new DataColumnSpecCreator("Eisenberg scale", DoubleCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Kyte-Doolittle scale", DoubleCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Hopp-Woods scale", DoubleCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Cornette scale", DoubleCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("Janin scale", DoubleCell.TYPE).createSpec();
		cols[5] = new DataColumnSpecCreator("Engelman scale", DoubleCell.TYPE).createSpec();
		cols[6] = new DataColumnSpecCreator("Number of invalid AA", IntCell.TYPE).createSpec();

		return cols;
	}
	
	@SuppressWarnings("static-access")
	@Override
	public DataCell[] getCells(DataRow row) {
		SequenceValue sv = getSequenceForRow(row);
		if (sv == null || !sv.getSequenceType().isProtein()) 
			return missing_cells(getColumnSpecs().length);
		
		String      prot = sv.getStringValue();
		DataCell[] cells = missing_cells(getColumnSpecs().length);
		double kyte = 0.0;
		double hopp = 0.0;
		double cornette = 0.0;
		double eisenberg = 0.0;
		double janin = 0.0;
		double engelman = 0.0;
		int invalid = 0;
		for (int i=0; i<prot.length(); i++) {
			String aa = ""+prot.charAt(i);
			if (!kyteDoolittle.containsKey(aa)) {
				invalid++;
				continue;
			}
			kyte      += this.kyteDoolittle.get(aa);
			hopp      += this.hoppWoods.get(aa);
			cornette  += this.cornette.get(aa);
			eisenberg += this.eisenberg.get(aa);
			janin     += this.janin.get(aa);
			engelman  += this.engelman.get(aa);
		}
		
		if (invalid > 0) {
			cells[6] = new IntCell(invalid);
		} else {
			cells[1] = new DoubleCell(kyte);
			cells[2] = new DoubleCell(hopp);
			cells[3] = new DoubleCell(cornette);
			cells[0] = new DoubleCell(eisenberg);
			cells[4] = new DoubleCell(janin);
			cells[5] = new DoubleCell(engelman);
			cells[6] = new IntCell(0);
		}
		return cells;
	}

}
