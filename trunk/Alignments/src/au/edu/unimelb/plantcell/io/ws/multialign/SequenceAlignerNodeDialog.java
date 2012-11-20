package au.edu.unimelb.plantcell.io.ws.multialign;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * <code>NodeDialog</code> for the "SequenceAligner" Node.
 * Performs an alignment, performed by http://jaligner.sourceforge.net of two sequences using the chosen parameters
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class SequenceAlignerNodeDialog extends DefaultNodeSettingsPane {
	private final SettingsModelString alignment_data  = (SettingsModelString) SequenceAlignerNodeModel.make(SequenceAlignerNodeModel.CFG_IS_PAIRWISE);
	private final SettingsModelString sequence        = (SettingsModelString) SequenceAlignerNodeModel.make(SequenceAlignerNodeModel.CFG_SEQ_COL);
	private final SettingsModelString sequence2       = (SettingsModelString) SequenceAlignerNodeModel.make(SequenceAlignerNodeModel.CFG_SEQ2_COL);
	private final SettingsModelString scoring_matrix  = (SettingsModelString) SequenceAlignerNodeModel.make(SequenceAlignerNodeModel.CFG_BUILTIN_MATRIX);
	private final SettingsModelString alignment_items = (SettingsModelString) SequenceAlignerNodeModel.make(SequenceAlignerNodeModel.CFG_ALIGN_TYPE);
	private final SettingsModelDouble gap_open        = (SettingsModelDouble) SequenceAlignerNodeModel.make(SequenceAlignerNodeModel.CFG_GAP_PENALTY_OPEN);
	private final SettingsModelDouble gap_extend      = (SettingsModelDouble) SequenceAlignerNodeModel.make(SequenceAlignerNodeModel.CFG_GAP_PENALTY_EXTEND);
	private final SettingsModelBoolean align_self     = (SettingsModelBoolean) SequenceAlignerNodeModel.make(SequenceAlignerNodeModel.CFG_ALIGN_SELF);
	
    /**
     * New pane for configuring SequenceAligner node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected SequenceAlignerNodeDialog() {
        super();
     
        Set<String> alignment_types = new HashSet<String>();
        String[] atypes = new String[] {"Local - SmithWaterman", "Local - CrochemoreLandauZivUkelson",
        		"Global - NeedlemanWunsch", "Global - CrochemoreLandauZivUkelson", "Local - JAligner (SmithWatermanGotoh)"};
        Arrays.sort(atypes);
        for (String s : atypes ) {
        	alignment_types.add(s);
        }
        String[] outputs = new String[] {
        		"Accessions", "Original Sequences", "Gapped Sequences", "Score", "Tag Line", "Total Gaps (Sequence1)",
        		"Total Gaps (Sequence2)", "Identities (%)", "Similarities (excluding identities, %)", "Extent (%sequence1)", "Extent (%sequence2)",
        		"Identical Regions (BitVector)", "Similar Regions (BitVector)", "Alignment in ClustalW format",
        		"Alignment in BLAST format", "Alignment in FASTA format", "Alignment Cell",
        		"Gap regions (Sequence1)", "Gap regions (Sequence2)", "Gap regions (union)", "Gap regions (intersection)"
        };
        Arrays.sort(outputs);
        
        DialogComponentStringSelection dcss = new DialogComponentStringSelection(alignment_items, "Alignment Type", alignment_types);
        addDialogComponent(dcss);
        
        // neobio does not support gap extension (at the moment) but JAligner does so a change listener (see above) disables widgets depending on the type of alignment
        alignment_items.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent arg0) {
				String algorithm = alignment_items.getStringValue();
				if (algorithm.toLowerCase().contains("jalign")) {
					gap_open.setEnabled(true);
					gap_extend.setEnabled(true);
				} else {
					gap_open.setEnabled(false);
					gap_extend.setEnabled(false);
				}
			}
        	
        });
        	
        createNewGroup("Input sequences to align");
        String[] radio_items = new String[] { "in two columns", "in one column (pairwise)" };
        String[] radio_actions = new String[] { "2cols", "1col" };
        DialogComponentButtonGroup bg = new DialogComponentButtonGroup(alignment_data, "Sequences to align are... ", false, radio_items, radio_actions );
       
        alignment_data.addChangeListener(new ChangeListener() {
        	public void stateChanged(final ChangeEvent e) {
        		sequence2.setEnabled(alignment_data.getStringValue().equals("2cols"));
        		align_self.setEnabled(!sequence2.isEnabled());
        	}
        });
        
        addDialogComponent(bg);
        addDialogComponent(new DialogComponentBoolean(align_self, "Align to self (pairwise only)?"));
              
        addDialogComponent(new DialogComponentColumnNameSelection(sequence, "Sequence Column", 0, true, SequenceValue.class));
        addDialogComponent(new DialogComponentColumnNameSelection(sequence2, "Sequence2 Column", 0, true, new ColumnFilter() {

			@Override
			public String allFilteredMsg() {
				return "No String or Collection columns available!";
			}

			@Override
			public boolean includeColumn(DataColumnSpec colSpec) {
				// a single string
				if (colSpec.getType().isCompatible(SequenceValue.class))
					return true;
				// is it a string collection?
				if (colSpec.getType().isCollectionType() && colSpec.getType().getCollectionElementType().isCompatible(SequenceValue.class))
					return true;
				// else...
				return false;
			}
        
        }));
        
        createNewTab("Required Output");
        addDialogComponent(new DialogComponentStringListSelection((SettingsModelStringArray) SequenceAlignerNodeModel.make(SequenceAlignerNodeModel.CFG_WANTED), "Desired output", outputs));
 
        // must ensure initial display of dialog corresponds to changelistener!
        sequence2.setEnabled(alignment_data.getStringValue().equals("2cols"));
        align_self.setEnabled(!sequence2.isEnabled());
        
        createNewTab("Scoring");
        String[] builtin_matrix_items = SequenceAlignerNodeModel.getBuiltinScoringMatrices();
        DialogComponentStringSelection matrix_widget = new DialogComponentStringSelection(scoring_matrix, "Builtin Matrices", builtin_matrix_items);
        addDialogComponent(matrix_widget);
        
        createNewGroup("Gap Cost (always non-negative)");
        addDialogComponent(new DialogComponentNumber(gap_open,   "Gap (Open)", 1.0));
        addDialogComponent(new DialogComponentNumber(gap_extend, "Gap Extension", 1.0));
        
    }
}

