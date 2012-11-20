package au.edu.unimelb.plantcell.io.jemboss.settings;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;

/**
 * Setting to represent a multiple-sequence alignment (or pairwise) in the usual formats eg. CLUSTAL 
 * @author andrew.cassin
 *
 */
public class AlignmentSetting extends OutputFileSetting {
	public AlignmentSetting(HashMap<String,String> attrs) {
		super(attrs);
	}

	@Override
	public void marshal(String id, DataCell c, PrintWriter fw)
			throws IOException, InvalidSettingsException {
		// TODO Auto-generated method stub

	}
	
	public static boolean canEmboss(String acd_type) {
		return (acd_type.equals("align"));
	}

}
