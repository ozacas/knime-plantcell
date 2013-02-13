package au.edu.unimelb.plantcell.core.biojava.tasks;

import java.io.InputStreamReader;

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
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.misc.biojava.BioJavaProcessorNodeModel;

/**
 * Computes the Mass, pI and hydrophobicity of the specified sequences, using the
 * parameters as specified by the model given to execute()
 * 
 * @author acassin
 *
 */
public class HydrophobicityTask extends BioJavaProcessorTask {
	private int m_col = -1;
	private MassCalc mc, mc_mi;
	private AAindex hydrophobicity;
	private IsoelectricPointCalc ic;
	
	public HydrophobicityTask() {
		super();
	}
	
	@Override
	public String getCategory() {
		return "Statistics";
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new HydrophobicityTask();
	}
	
	public void init(BioJavaProcessorNodeModel owner, String task_name, int col) throws Exception {
		m_col = col;
		mc    = new MassCalc(SymbolPropertyTable.AVG_MASS, true);
		mc_mi = new MassCalc(SymbolPropertyTable.MONO_MASS, true);
		ic    = new IsoelectricPointCalc();
	
		Bundle plugin = Platform.getBundle("au.edu.unimelb.plantcell.misc.biojava");
		IPath p = new Path("lib/aaindex1");
		
		SimpleSymbolPropertyTableDB db = new SimpleSymbolPropertyTableDB(new AAindexStreamReader(new InputStreamReader(FileLocator.openStream(plugin, p, false))));
		hydrophobicity = (AAindex) db.table("CIDH920105");
	}
	
	/** {@inheritDoc} */
	@Override
	public String[] getNames() {
		 return new String[] { "Hydrophobicity, pI and total mass" }; 
	}
	
	/** {@inheritDoc} */
	@Override
	public String getHTMLDescription(String task) {
		return "<html>Compute hydrophobicity, isoelectric point and total mass of "+
		"the sequences. The sequences are converted to protein if necessary (without any "+
		"translation) before performing the calculation. Hydrophobicity calculations are performed using" +
		" CIDH920105 average along sequence as implemented in BioJava (http://www.biojava.org)</li>" +
		"</ol>";
	}
	
	public DataCell[] getCells(DataRow row) {
		try {
			DataCell c = row.getCell(m_col);
			if (c == null || c.isMissing() || !(c instanceof SequenceValue))
				return missing_cells(4);
			
			double pI       = 0.0;
			double mass_avg = 0.0;
			double mass_mi  = 0.0;
			double hyd      = 0.0;
			
			SequenceValue sv = (SequenceValue) c;
			if (sv.getLength() < 1)
				return missing_cells(4);
			
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
				DataCell[] cells = new DataCell[4];
				for (int i=0; i<cells.length; i++) {
					cells[i] = DataType.getMissingCell();
				}
				
				if (syms.seqString().indexOf("X") >= 0) {
					return missing_cells(cells.length);
				}
			
				mass_avg = mc.getMass(syms);
				mass_mi  = mc_mi.getMass(syms);
				pI   = ic.getPI(syms, true, true); // assume a free NH and COOH
				
				for (int i=1; i<= syms.length(); i++) {
					hyd += hydrophobicity.getDoubleValue(syms.symbolAt(i));
				}
				hyd /= syms.length();
				cells[3]         = new DoubleCell(hyd);
				cells[0]         = new DoubleCell(pI); 
				cells[1]         = new DoubleCell(mass_avg);
				cells[2]         = new DoubleCell(mass_mi);
				return cells;
			} catch (Exception e) {
				e.printStackTrace();
				return missing_cells(4);
			}
	}

	@Override
	public DataColumnSpec[] getColumnSpecs() {
		DataColumnSpec[] allColSpecs = new DataColumnSpec[4];
        allColSpecs[0] = 
            new DataColumnSpecCreator("pI", DoubleCell.TYPE).createSpec();
        allColSpecs[1] = 
            new DataColumnSpecCreator("Mass (Average, Da, +MH)", DoubleCell.TYPE).createSpec();
        allColSpecs[2] =
        	new DataColumnSpecCreator("Mass (Monoisotopic, Da, +MH)", DoubleCell.TYPE).createSpec();
        allColSpecs[3] = 
            new DataColumnSpecCreator("Average AA Hydrophobicity (aaindex1/CIDH920105)", DoubleCell.TYPE).createSpec();
     
        return allColSpecs;
	}

}
