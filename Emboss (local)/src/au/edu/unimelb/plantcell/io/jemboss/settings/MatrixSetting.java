package au.edu.unimelb.plantcell.io.jemboss.settings;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.io.jemboss.local.JEmbossProcessorNodeModel;
import au.edu.unimelb.plantcell.io.jemboss.local.ProgramSettingsListener;

/**
 * Setting which presents a list of scoring matrices to the user from EMBOSS (eg. during alignments)
 * 
 * @author andrew.cassin
 *
 */
public class MatrixSetting extends StringSetting {
	
	protected MatrixSetting(HashMap<String, String> attrs) {
		super(attrs);
	}

	@Override
	public JComponent make_widget(DataTableSpec dt) {
		JList jl = new JList(JEmbossProcessorNodeModel.getMatrices());
		jl.setVisibleRowCount(5);
		jl.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				setValue(((JList)arg0.getSource()).getSelectedValue().toString());
			}
			
		});
		jl.setSelectedIndex(0);
		ListModel lm = jl.getModel();
		for (int i=0; i<lm.getSize(); i++) {
			if (lm.getElementAt(i).equals(getValue())) {
				jl.setSelectedIndex(i);
				break;
			}
		}
		return new JScrollPane(jl);
	}

	@Override
	public void getArguments(ProgramSettingsListener l) throws Exception {
		l.addArgument(this, new String[] {"-"+getName(), getValue() });
	}
	
	@Override
	public void marshal(String id, DataCell c, PrintWriter fw)
			throws IOException, InvalidSettingsException {
		// NO-OP
	}

	@Override
	public String getColumnName() {
		// reading a matrix from a column is not (yet) supported
		return null;
	}

	public static boolean canEmboss(String acd_type) {
		if (acd_type.equals("matrixf") || acd_type.equals("matrixfile") || acd_type.equals("matrix"))
			return true;
		return false;
	}
}
