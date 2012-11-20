package au.edu.unimelb.plantcell.misc.biojava;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;

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
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Computes the Mass, pI and hydrophobicity of the specified sequences, using the
 * parameters as specified by the model given to execute()
 * 
 * @author acassin
 *
 */
public class HydrophobicityProcessor extends BioJavaProcessorTask {
	private final int NUM_COLUMNS = 4;
	
	public HydrophobicityProcessor() {
	}
	
	@Override
	public String getCategory() {
		return "Statistics";
	}
	
	public static BioJavaProcessorTask getInstance() {
		return new HydrophobicityProcessor();
	}
	
	public void init(BioJavaProcessorNodeModel owner, String task_name) {
		setOwner(owner);
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
	
	public void execute(ColumnIterator ci, final ExecutionContext exec, NodeLogger logger, final BufferedDataTable[] inData, BufferedDataContainer c1)
			throws Exception {
		
		int outPI       = c1.getTableSpec().findColumnIndex("pI");
		int outMass_avg = c1.getTableSpec().findColumnIndex("Mass (Average, Da, +MH)");
		int outMass_mi  = c1.getTableSpec().findColumnIndex("Mass (Monoisotopic, Da, +MH)");
		int outHyd      = c1.getTableSpec().findColumnIndex("Average AA Hydrophobicity (aaindex1/CIDH920105)");
		
		if (outPI < 0 || outMass_avg < 0 || outMass_mi < 0 || outHyd < 0) {
			throw new Exception("Cannot find output columns! Bug...");
		}
		MassCalc mc, mc_mi;
		IsoelectricPointCalc ic;
		try {
			mc    = new MassCalc(SymbolPropertyTable.AVG_MASS, true);
			mc_mi = new MassCalc(SymbolPropertyTable.MONO_MASS, true);
			ic    = new IsoelectricPointCalc();
		} catch (Throwable th) {
			//System.err.println(th);
			throw new Exception("Unable to compute calculators... aborting execution!");
		}
		
		Bundle plugin = Platform.getBundle("au.edu.unimelb.plantcell.misc.biojava");
		IPath p = new Path("lib/aaindex1");
		
		SimpleSymbolPropertyTableDB db = new SimpleSymbolPropertyTableDB(new AAindexStreamReader(new InputStreamReader(FileLocator.openStream(plugin, p, false))));
		AAindex hydrophobicity = (AAindex) db.table("CIDH920105");
		while (ci.hasNext()) {
			DataCell c = ci.next();
			if (c == null || c.isMissing() || !(c instanceof SequenceValue))
				continue;
			
			double pI       = 0.0;
			double mass_avg = 0.0;
			double mass_mi  = 0.0;
			double hyd      = 0.0;
			
			SequenceValue sv = (SequenceValue) c;
			if (sv.getLength() < 1)
				continue;
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
			DataCell[] cells = new DataCell[NUM_COLUMNS];
			for (int i=0; i<cells.length; i++) {
				cells[i] = DataType.getMissingCell();
			}
			
			if (syms.seqString().indexOf("X") >= 0) {
				DataRow row = new DefaultRow(ci.lastRowID(), cells);
				c1.addRowToTable(new JoinedRow(ci.lastRow(), row));
				continue;
			}
			
			try {
				mass_avg = mc.getMass(syms);
				mass_mi  = mc_mi.getMass(syms);
				pI   = ic.getPI(syms, true, true); // assume a free NH and COOH
				
				for (int i=1; i<= syms.length(); i++) {
					hyd += hydrophobicity.getDoubleValue(syms.symbolAt(i));
				}
				hyd /= syms.length();				
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e.getMessage());
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				pw.close();
				
				logger.warn(sw.toString());
				cells[4] = DataType.getMissingCell();
			}
			cells[0]         = new DoubleCell(pI); 
			cells[1]         = new DoubleCell(mass_avg);
			cells[2]         = new DoubleCell(mass_mi);
			cells[3]         = new DoubleCell(hyd);
			DataRow      row = new DefaultRow(ci.lastRowID(), cells);
			c1.addRowToTable(new JoinedRow(ci.lastRow(), row));
		}
	}

	public DataTableSpec get_table_spec() {
		DataColumnSpec[] allColSpecs = new DataColumnSpec[NUM_COLUMNS];
        allColSpecs[0] = 
            new DataColumnSpecCreator("pI", DoubleCell.TYPE).createSpec();
        allColSpecs[1] = 
            new DataColumnSpecCreator("Mass (Average, Da, +MH)", DoubleCell.TYPE).createSpec();
        allColSpecs[2] =
        	new DataColumnSpecCreator("Mass (Monoisotopic, Da, +MH)", DoubleCell.TYPE).createSpec();
        allColSpecs[3] = 
            new DataColumnSpecCreator("Average AA Hydrophobicity (aaindex1/CIDH920105)", DoubleCell.TYPE).createSpec();
     
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
		return outputSpec;
	}

	@Override
	public boolean isMerged() {
		return true;
	}

}
