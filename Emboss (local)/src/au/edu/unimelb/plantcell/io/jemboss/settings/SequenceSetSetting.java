package au.edu.unimelb.plantcell.io.jemboss.settings;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionPanel;

import au.edu.unimelb.plantcell.io.jemboss.local.ProgramSettingsListener;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue;

/**
 * 
 * @author andrew.cassin
 *
 */
public class SequenceSetSetting extends SequenceSetting implements ActionListener {
	private JRadioButton b1, b2;
	private JButton      open_file_button = new JButton("   Select File...   ");
	private ColumnSelectionPanel csp;
	
	
	public SequenceSetSetting(HashMap<String,String> attrs) {
		super(attrs);
	}
	
	public static boolean canEmboss(String acd_type) {
		return (acd_type.equals("seqset"));
	}
	
	@Override
	public JComponent make_widget(DataTableSpec dt) {
		JPanel w = new JPanel();
		w.setLayout(new BoxLayout(w, BoxLayout.X_AXIS));
		String          where = isInput() ? "from" : "to";
		b1 = new JRadioButton(where+" column");
		b2 = new JRadioButton(where+" file");
		boolean disable_column_input = false;
		try {
			 csp = make_col_panel(dt);		// attempt construction which may fail if no suitable columns available
		} catch (Exception e) {
			 Logger.getAnonymousLogger().warning("No suitable columns found in input table for "+getName());
		}
		
		b1.addActionListener(this);
		b2.addActionListener(this);
		b1.setEnabled(!disable_column_input);
		b1.setSelected(isInputFromColumn());
		b2.setSelected(!isInputFromColumn());
		w.add(b2);
		w.add(csp);
		w.add(b1);
		w.add(open_file_button);
		return w;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		JRadioButton b = (JRadioButton) e.getSource();
		boolean is_b1 = b.equals(b1);	// file radio button?
		setSourceFromColumn(!is_b1);
		b1.setSelected(is_b1);
		b2.setSelected(!is_b1);
		if (is_b1) {
			open_file_button.setEnabled(true);
			if (csp != null)
				csp.setEnabled(false);
		} else {
			if (csp != null)
				csp.setEnabled(true);
			open_file_button.setEnabled(false);
		}
	}
	
	@Override
	protected ColumnSelectionPanel make_col_panel(DataTableSpec dt) {
        
		ColumnSelectionPanel csp = new ColumnSelectionPanel((Border)null, new ColumnFilter() {
			
			@Override
			public boolean includeColumn(DataColumnSpec colSpec) {
				if (colSpec.getType().isCompatible(AlignmentValue.class)) {
					return true;
				}
				if (colSpec.getType().isCollectionType() && colSpec.getType().getCollectionElementType().isCompatible(StringValue.class)) {
					return true;
				}
				return false;
			}

			@Override
			public String allFilteredMsg() {
				return "ERROR: no suitable String or Alignment columns available!";
			}
			
		}, false, false);
	
		csp.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Object o = ((JComboBox)arg0.getSource()).getSelectedItem();
				if (o instanceof DataColumnSpec) {
					setValue(((DataColumnSpec)o).getName());
				}
			}
			
		});
		
		String default_value = getDefaultValue();
		if (hasAttribute("value") && isInputFromColumn()) {
			default_value = getValue();
		}
		
		try {
			if (dt != null) {
				csp.update(dt, "");
			}
		} catch (NotConfigurableException nce) {
			nce.printStackTrace();
		}
		return csp;
	}
	
	@Override
	public void marshal(String id, DataCell c, PrintWriter fw) 
					throws IOException, InvalidSettingsException {
		if (c == null || c.isMissing()) {
			throw new InvalidSettingsException("Cannot create sequence set from missing cell: "+id);
		}
		
		if (c.getType().isCompatible(AlignmentValue.class)) {
			AlignmentValue av = (AlignmentValue) c;
			for (int i=0; i<av.getSequenceCount(); i++) {
				super.marshal(">Seq"+(i+1)+"_"+id+" "+av.getIdentifier(i).getName(), 
									new StringCell(av.getAlignedSequenceString(i)), fw);
			}
		} else if (c.getType().isCollectionType() && c.getType().getCollectionElementType().isCompatible(StringValue.class)) {
			CollectionDataValue cv = (CollectionDataValue) c;
			Iterator<DataCell> it = cv.iterator();
			int i = 1;
			while (it.hasNext()) {
				DataCell c2= it.next();
				if (c2.isMissing()) {
					throw new InvalidSettingsException("Cannot create sequence set from missing cell: "+id);
				}
				super.marshal("Seq"+i++, c2, fw);
			}
		} else {
			throw new InvalidSettingsException("Invalid/unsupported sequence cell for "+id);
		}
	}
	
	@Override
	public void getArguments(ProgramSettingsListener l) throws InvalidSettingsException,IOException {
	    
	    // input-by-file specified but no file chosen?
	    String v = getValue();
	    
	    if (!isInputFromColumn() && (v==null || v.length() < 1)) {
	    	throw new InvalidSettingsException("You must specify a filename for "+getName());
	    }
	    
	    if (isIgnored()) {
	    	return;
	    }
	    
		File f = File.createTempFile("infile", ".fasta");
    	l.addInputFileArgument(this, "-"+getName(), f);
	}
}
