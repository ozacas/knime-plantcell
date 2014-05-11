package au.edu.unimelb.plantcell.io.write.phyloxml;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.forester.phylogeny.PhylogenyNode;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.CollectionDataValue;

/**
 * Provides a list with appropriate support for PhyloXML vector data. The input data is assumed to
 * come from a KNIME collection cell (either Set or List although order is unpredictable from a Set)
 * @author acassin
 *
 */
public class VectorDataList {
	@SuppressWarnings("unused")
	private boolean is_int;	// either Integer or Double are supported for now
	private List<Double> vec;
	
	public VectorDataList(DataCell vector_data_cell) {
		vec = fromCell(vector_data_cell);
		is_int = false;	// assume double
	}

	private List<Double> fromCell(DataCell dc) {
		assert(dc != null && !dc.isMissing());
		CollectionDataValue cv = (CollectionDataValue) dc;
		List<Double> ret = new ArrayList<Double>();
		Iterator<DataCell> it = cv.iterator();
		while (it.hasNext()) {
			DataCell dv = it.next();
			DoubleValue d = (DoubleValue) dv;
			ret.add(new Double(d.getDoubleValue()));
		}
		
		return ret;
	}
	
	/**
	 * Establish the node vector associated with the node.
	 * 
	 * @param n
	 */
	public void setNodeVector(final PhylogenyNode n) {
		assert(n != null);
		if (vec.size() > 0) {
			n.getNodeData().setVector(vec);
		} else {
			n.getNodeData().setVector(null);
		}
	}
	
	

}
