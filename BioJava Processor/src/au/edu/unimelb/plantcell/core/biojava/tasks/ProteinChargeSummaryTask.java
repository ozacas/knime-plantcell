package au.edu.unimelb.plantcell.core.biojava.tasks;

import java.io.InputStreamReader;
import java.util.HashMap;

import org.biojava.bio.proteomics.IsoelectricPointCalc;
import org.biojava.bio.proteomics.MassCalc;
import org.biojava.bio.proteomics.aaindex.AAindex;
import org.biojava.bio.proteomics.aaindex.AAindexStreamReader;
import org.biojava.bio.proteomics.aaindex.SimpleSymbolPropertyTableDB;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.ProteinTools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.SymbolList;
import org.biojava.bio.symbol.SymbolPropertyTable;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.def.DoubleCell;
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Computes the Mass, pI and hydrophobicity of the specified sequences, using the
 * parameters as specified by the model given to execute()
 * 
 * @author acassin
 *
 */
public class ProteinChargeSummaryTask extends BioJavaProcessorTask {
	private MassCalc mc, mc_mi;
	private AAindex hydrophobicity;
	private IsoelectricPointCalc ic;
	private int m_cols = 0;
	private HashMap<String,Double> grantham = new HashMap<String,Double>();
	private HashMap<String,Double> zimmerman_bulkiness = new HashMap<String,Double>();
	private HashMap<String,Double> zimmerman_polarity = new HashMap<String,Double>();
	private HashMap<String,Double> zimmerman_hydrophobicity = new HashMap<String,Double>();

	
	public ProteinChargeSummaryTask() {
		super();
		
		// from Grantham's paper Amino Acid Difference Formula to Help Explain Protein Evolution
		grantham.put("ser", new Double(9.2));
		grantham.put("arg", new Double(10.5));
		grantham.put("leu", new Double(4.9));
		grantham.put("pro", new Double(8.0));
		grantham.put("thr", new Double(8.6));
		grantham.put("ala", new Double(8.1));
		grantham.put("val", new Double(5.9));
		grantham.put("gly", new Double(9.0));
		grantham.put("ile", new Double(5.2));
		grantham.put("phe", new Double(5.2));
		grantham.put("tyr", new Double(6.2));
		grantham.put("cys", new Double(5.5));
		grantham.put("his", new Double(10.4));
		grantham.put("gln", new Double(10.5));
		grantham.put("asn", new Double(11.6));
		grantham.put("lys", new Double(11.3));
		grantham.put("asp", new Double(13.0));
		grantham.put("glu", new Double(12.3));
		grantham.put("met", new Double(5.7));
		grantham.put("trp", new Double(5.4));
		
		// from Zimmerman's paper: Amino Acid Sequences in Proteins (1968)
		zimmerman_bulkiness.put("ser", new Double(9.47));
		zimmerman_bulkiness.put("arg", new Double(14.28));
		zimmerman_bulkiness.put("leu", new Double(21.40));
		zimmerman_bulkiness.put("pro", new Double(17.43));
		zimmerman_bulkiness.put("thr", new Double(15.77));
		zimmerman_bulkiness.put("ala", new Double(11.50));
		zimmerman_bulkiness.put("val", new Double(21.57));
		zimmerman_bulkiness.put("gly", new Double(3.40));
		zimmerman_bulkiness.put("ile", new Double(21.40));
		zimmerman_bulkiness.put("phe", new Double(19.80));
		zimmerman_bulkiness.put("tyr", new Double(18.03));
		zimmerman_bulkiness.put("cys", new Double(13.46));
		zimmerman_bulkiness.put("his", new Double(13.69));
		zimmerman_bulkiness.put("gln", new Double(14.45));
		zimmerman_bulkiness.put("asn", new Double(12.82));
		zimmerman_bulkiness.put("lys", new Double(15.71));
		zimmerman_bulkiness.put("asp", new Double(11.68));
		zimmerman_bulkiness.put("glu", new Double(13.57));
		zimmerman_bulkiness.put("met", new Double(16.25));
		zimmerman_bulkiness.put("trp", new Double(21.67));
		
		zimmerman_polarity.put("ser", new Double(1.67));
		zimmerman_polarity.put("arg", new Double(52.00));
		zimmerman_polarity.put("leu", new Double(0.13));
		zimmerman_polarity.put("pro", new Double(1.58));
		zimmerman_polarity.put("thr", new Double(1.66));
		zimmerman_polarity.put("ala", new Double(0.00));
		zimmerman_polarity.put("val", new Double(0.13));
		zimmerman_polarity.put("gly", new Double(0.00));
		zimmerman_polarity.put("ile", new Double(0.13));
		zimmerman_polarity.put("phe", new Double(0.35));
		zimmerman_polarity.put("tyr", new Double(1.61));
		zimmerman_polarity.put("cys", new Double(1.48));
		zimmerman_polarity.put("his", new Double(51.60));
		zimmerman_polarity.put("gln", new Double(3.53));
		zimmerman_polarity.put("asn", new Double(3.38));
		zimmerman_polarity.put("lys", new Double(49.50));
		zimmerman_polarity.put("asp", new Double(49.70));
		zimmerman_polarity.put("glu", new Double(49.90));
		zimmerman_polarity.put("met", new Double(1.43));
		zimmerman_polarity.put("trp", new Double(2.10));
		
		zimmerman_hydrophobicity.put("ser", new Double(0.14));
		zimmerman_hydrophobicity.put("arg", new Double(0.83));
		zimmerman_hydrophobicity.put("leu", new Double(2.52));
		zimmerman_hydrophobicity.put("pro", new Double(2.70));
		zimmerman_hydrophobicity.put("thr", new Double(0.54));
		zimmerman_hydrophobicity.put("ala", new Double(0.83));
		zimmerman_hydrophobicity.put("val", new Double(1.79));
		zimmerman_hydrophobicity.put("gly", new Double(0.10));
		zimmerman_hydrophobicity.put("ile", new Double(3.07));
		zimmerman_hydrophobicity.put("phe", new Double(2.75));
		zimmerman_hydrophobicity.put("tyr", new Double(2.97));
		zimmerman_hydrophobicity.put("cys", new Double(1.48));
		zimmerman_hydrophobicity.put("his", new Double(1.10));
		zimmerman_hydrophobicity.put("gln", new Double(0.00));
		zimmerman_hydrophobicity.put("asn", new Double(0.09));
		zimmerman_hydrophobicity.put("lys", new Double(1.60));
		zimmerman_hydrophobicity.put("asp", new Double(0.64));
		zimmerman_hydrophobicity.put("glu", new Double(0.65));
		zimmerman_hydrophobicity.put("met", new Double(1.40));
		zimmerman_hydrophobicity.put("trp", new Double(0.31));
	}
	
	@Override
	public String getCategory() {
		return "Common";
	}
	
	@Override
	public boolean canWindow() {
		return true;
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new ProteinChargeSummaryTask();
	}
	
	@Override
	public void init(String task_name, int col) throws Exception {
		super.init(task_name, col);
		mc    = new MassCalc(SymbolPropertyTable.AVG_MASS, true);
		mc_mi = new MassCalc(SymbolPropertyTable.MONO_MASS, true);
		ic    = new IsoelectricPointCalc();
	
		Bundle plugin = Platform.getBundle("au.edu.unimelb.plantcell.misc.biojava");
		IPath p = new Path("lib/aaindex1");
		
		SimpleSymbolPropertyTableDB db = new SimpleSymbolPropertyTableDB(new AAindexStreamReader(new InputStreamReader(FileLocator.openStream(plugin, p, false))));
		hydrophobicity = (AAindex) db.table("CIDH920105");
		m_cols = getColumnSpecs().length;
	}
	
	/** {@inheritDoc} */
	@Override
	public String[] getNames() {
		 return new String[] { "Hydrophobicity, pI, Total mass, Grantham-scale polarity, Zimmerman bulkiness/polarity and hydrophobicity" }; 
	}
	
	/** {@inheritDoc} */
	@Override
	public String getHTMLDescription(String task) {
		return "<html>Compute hydrophobicity, isoelectric point and total mass of "+
		"the sequences. The sequences are converted to protein if necessary (without any "+
		"translation) before performing the calculation. Hydrophobicity calculations are performed using" +
		" CIDH920105 average along sequence as implemented in BioJava (http://www.biojava.org)." +
		"Presence of ambiguous amino acids (eg. X, B, J) will cause missing values (?) in cells to be returned.";
	}
	
	public DataCell[] getCells(DataRow row) {
		try {
			SequenceValue sv = getSequenceForRow(row);
			if (sv == null || sv.getLength() < 1)
				return missing_cells(m_cols);
			
			SymbolList syms  = asBioJava(sv);
			
			if (! sv.getSequenceType().isProtein()) {
				// need to translate it (by default assume 5' to 3' orientation)
				if (syms.getAlphabet() != RNATools.getRNA()) {
					syms = DNATools.transcribeToRNA(syms);
				}
				// truncate if not divisible by 3
				if (syms.length() % 3 != 0) {
					syms = syms.subList(1, syms.length() - (syms.length() % 3));
				}
				
				syms = RNATools.translate(syms);
			}
			
			// remove * if necessary
			if (syms.symbolAt(syms.length()) == ProteinTools.ter()) {
				syms = syms.subList(1, syms.length()-1);
			}
			
			
			// unknown residues? Dont calculate, leave user to figure it out...
			DataCell[] cells = missing_cells(m_cols);
			
			String seq = syms.seqString();
			// return missing value if mass cannot be precisely known
			if (seq.indexOf("X") >= 0 || seq.indexOf("B") >= 0 || seq.indexOf("J") >= 0) {
				return cells;
			}
		

			double pI       = 0.0;
			double mass_avg = 0.0;
			double mass_mi  = 0.0;
			double hyd      = 0.0;
			mass_avg = mc.getMass(syms);
			mass_mi  = mc_mi.getMass(syms);
			pI   = ic.getPI(syms, true, true); // assume a free NH and COOH
			double grantham_sum = 0.0;
			double z_bulkiness = 0.0;
			double z_polarity = 0.0;
			double z_hydrophobicity = 0.0;
			for (int i=1; i<= syms.length(); i++) {
				hyd += hydrophobicity.getDoubleValue(syms.symbolAt(i));
				String aa = syms.symbolAt(i).getName().toLowerCase();
				grantham_sum += grantham.get(aa);
				z_bulkiness  += zimmerman_bulkiness.get(aa);
				z_polarity   += zimmerman_polarity.get(aa);
				z_hydrophobicity += zimmerman_hydrophobicity.get(aa);
			}
			
			cells[0]         = new DoubleCell(pI); 
			cells[1]         = new DoubleCell(mass_avg);
			cells[2]         = new DoubleCell(mass_mi);
			cells[3]         = new DoubleCell(hyd / syms.length());
			cells[4]		 = new DoubleCell(grantham_sum / syms.length());
			cells[5]         = new DoubleCell(z_bulkiness / syms.length());
			cells[6]         = new DoubleCell(z_polarity / syms.length());
			cells[7]         = new DoubleCell(z_hydrophobicity / syms.length());
			return cells;
		} catch (Exception e) {
			e.printStackTrace();
			return missing_cells(m_cols);
		}
	}

	@Override
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] allColSpecs = new DataColumnSpec[8];
        allColSpecs[0] = 
            new DataColumnSpecCreator("pI", DoubleCell.TYPE).createSpec();
        allColSpecs[1] = 
            new DataColumnSpecCreator("Mass (Average, Da, +MH)", DoubleCell.TYPE).createSpec();
        allColSpecs[2] =
        	new DataColumnSpecCreator("Mass (Monoisotopic, Da, +MH)", DoubleCell.TYPE).createSpec();
        allColSpecs[3] = 
            new DataColumnSpecCreator("Mean AA Hydrophobicity (aaindex1/CIDH920105)", DoubleCell.TYPE).createSpec();
        allColSpecs[4] = 
        	new DataColumnSpecCreator("Mean Grantham-scale polarity", DoubleCell.TYPE).createSpec();
        allColSpecs[5] = 
        	new DataColumnSpecCreator("Mean Zimmerman Bulkiness", DoubleCell.TYPE).createSpec();
        allColSpecs[6] =
        	new DataColumnSpecCreator("Mean Zimmerman Polarity", DoubleCell.TYPE).createSpec();
        allColSpecs[7] =
        	new DataColumnSpecCreator("Mean Zimmerman Hydrophobicity", DoubleCell.TYPE).createSpec();
        
        return allColSpecs;
	}

}
