package au.edu.unimelb.plantcell.io.ws.pfam;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;

import au.edu.unimelb.plantcell.core.cells.SequenceCell;

public class PFAMUniProtGetTask extends PFAMTask {

	@Override
	public DataTableSpec getTableSpec() {
		DataColumnSpec[] cols = new DataColumnSpec[7];
		cols[0] = new DataColumnSpecCreator("PFAM Release", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Title", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Taxonomy", StringCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("UniProt Sequence (from PFAM)", SequenceCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("PFAM matches (Accession list)", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
		cols[5] = new DataColumnSpecCreator("PFAM matches (ID list)", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
		cols[6] = new DataColumnSpecCreator("PFAM match evalues (list)", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec();
		
		return new DataTableSpec(cols);
	}

	@Override
	public DataCell[] get(DataRow input_row) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
