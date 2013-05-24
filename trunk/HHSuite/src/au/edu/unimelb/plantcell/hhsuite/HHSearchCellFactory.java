package au.edu.unimelb.plantcell.hhsuite;

import java.util.ArrayList;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.ExecutionMonitor;

public class HHSearchCellFactory implements CellFactory {

	@Override
	public DataCell[] getCells(DataRow row) {
		DataCell[] out = new DataCell[10];
		
		return null;
	}

	@Override
	public DataColumnSpec[] getColumnSpecs() {
		ArrayList<DataColumnSpec> cols = new ArrayList<DataColumnSpec>();
		cols.add(new DataColumnSpecCreator("Number of hits", IntCell.TYPE).createSpec());
		
		return cols.toArray(new DataColumnSpec[0]);
	}

	@Override
	public void setProgress(int curRowNr, int rowCount, RowKey lastKey,
			ExecutionMonitor exec) {
		exec.setProgress(((double)curRowNr) / rowCount);
	}

}
