package au.edu.unimelb.plantcell.networks;

import org.knime.core.data.DataValue;

import edu.uci.ics.jung.graph.Graph;

/**
 * Specification to which all network cells must conform
 * @author andrew.cassin
 *
 */
public interface NetworkValue extends DataValue, Comparable<NetworkValue> {
	public Graph<MyVertex, MyEdge> getGraph();
}
