package au.edu.unimelb.plantcell.io.write.phyloxml.heat;

import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.knime.core.data.DataRow;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;

/**
 * Baseclass implementation just picks the maximum pairwise datapoint from the matrix
 * and sets only the branch colour connecting each leaf node to the tree to this colour.
 * 
 * @author acassin
 *
 */
public class DefaultHeatModel extends AbstractHeatModel {
	//private final Map<PhylogenyNode,Double> heat = new HashMap<PhylogenyNode,Double>();
	//private Double max, min;
	private NodeLogger logger;
	
	public DefaultHeatModel(final NodeLogger l, final ColourManager cm) {
		super(l, cm);
		assert(l != null);
		assert(cm  != null);
		logger = l;
	}
	
	@Override
	public void start(final Phylogeny p, final BufferedDataTable in, int a_col, int heat_col) throws InvalidSettingsException {
		int success = 0;
		int failed = 0;
		for (DataRow r : in) {
			String taxa_name = asTaxaName(r.getCell(a_col));
			Double heat_value= asNumber(r.getCell(heat_col));
			
			PhylogenyNode n = p.getNode(taxa_name);
			if (n == null)
				continue;
			if (getColourManager().addHeat(n, in.getSpec().getRowColor(r).getColor(), heat_value)) {
				success++;
			} else {
				failed++;
			}
		}
		logger.info("Failed to associate heat with "+failed+" rows, succeeded for "+success);
	}
}
