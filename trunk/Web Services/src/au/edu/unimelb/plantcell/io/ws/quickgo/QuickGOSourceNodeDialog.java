package au.edu.unimelb.plantcell.io.ws.quickgo;


import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * <code>NodeDialog</code> for the "GoaSource" Node.
 * Provides an interface to GOA (Gene Ontology Annotation) websites (esp. EBI) to KNIME
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class QuickGOSourceNodeDialog extends DefaultNodeSettingsPane {
	private static SettingsModelString f_advquery = QuickGOSourceNodeModel.make_string(QuickGOSourceNodeModel.CFGKEY_ADVQUERY);
	private static SettingsModelString f_db = QuickGOSourceNodeModel.make_string(QuickGOSourceNodeModel.CFGKEY_FIELD_DB);
	private static SettingsModelString f_evidence = QuickGOSourceNodeModel.make_string(QuickGOSourceNodeModel.CFGKEY_FIELD_EVIDENCE);
	private static SettingsModelString f_source = QuickGOSourceNodeModel.make_string(QuickGOSourceNodeModel.CFGKEY_FIELD_SOURCE);
	private static SettingsModelString f_ancestor = QuickGOSourceNodeModel.make_string(QuickGOSourceNodeModel.CFGKEY_FIELD_ANCESTOR);
	private static SettingsModelString f_ref = QuickGOSourceNodeModel.make_string(QuickGOSourceNodeModel.CFGKEY_FIELD_REF);
	private static SettingsModelString f_with = QuickGOSourceNodeModel.make_string(QuickGOSourceNodeModel.CFGKEY_FIELD_WITH);
	private static SettingsModelString f_taxonomy = QuickGOSourceNodeModel.make_string(QuickGOSourceNodeModel.CFGKEY_FIELD_TAX);
	private static SettingsModelString f_protein = QuickGOSourceNodeModel.make_string(QuickGOSourceNodeModel.CFGKEY_FIELD_PROTEIN);
	private static SettingsModelString f_type= QuickGOSourceNodeModel.make_string(QuickGOSourceNodeModel.CFGKEY_TYPE);
    
    public static void set_controls() {
    	 String s_type = f_type.getStringValue();
    	 //System.err.println("got stype: "+s_type);
    	 boolean set_to_true = s_type.equals("Annotation");
    	 
    	 System.err.println("setting controls to "+set_to_true);
    	 f_advquery.setEnabled(set_to_true);
         f_db.setEnabled(set_to_true);
         f_evidence.setEnabled(set_to_true);
         f_source.setEnabled(set_to_true);
         f_ancestor.setEnabled(set_to_true);
         f_ref.setEnabled(set_to_true);
         f_with.setEnabled(set_to_true);
         f_taxonomy.setEnabled(set_to_true);
         f_protein.setEnabled(set_to_true);
    }
    
    /**
     * New pane for configuring GoaSource node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    @SuppressWarnings("unchecked")
	protected QuickGOSourceNodeDialog() {
        super();
        
        SettingsModelString f_url = QuickGOSourceNodeModel.make_string(QuickGOSourceNodeModel.CFGKEY_URL);
        SettingsModelString f_term_col = QuickGOSourceNodeModel.make_string(QuickGOSourceNodeModel.CFGKEY_TERMINFO_COL);
        
        set_controls();
             
        // fields each of which does not have to be set (only available for annotation searches)
        createNewGroup("1. Search for... ");
        String[] commands = new String[] {"Term Information", "Annotation"};
        String[] labels = new String[] {"I have GO terms (eg: GO:000057) and want to know more about them",
        		"I have UniProt (or similar eg. Q95X67) accessions and want to understand function/process or cellular component" };
        addDialogComponent(new DialogComponentButtonGroup(f_type, null, true, labels, commands));
        
        createNewGroup("2. Column to get term/accessions from... ");
        addDialogComponent(new DialogComponentColumnNameSelection(f_term_col, "Column (used for all searches):", 0, StringValue.class ));
       
        createNewGroup("3. Filter by... (advanced settings on right)");
        addDialogComponent(new DialogComponentStringSelection(f_db, "Database", "UniProtKB: both Swiss-Prot and TrEMBL", "UniGene", "UniProtKB/Swiss-Prot", "UniProtKB/TrEMBL", "IPI", "DictyBase", "FB", "GR", "MGI", "SPOM", "TIGR", "WB", "ZFIN", "NCBI RefSeq", "Ensembl", "EMBL", "NCBI GeneID", "TAIR"));
        addDialogComponent(new DialogComponentStringSelection(f_evidence, "Evidence", "All", "Manual Experimental", "Manual All", "IC", "IDA", "IEA", "EXP", "IEP", "IGI", "IGC", "IMP", "IPI", "RCA", "ISA", "ISM", "ISO", "ISS", "ND", "NAS", "NR", "TAS"));
        addDialogComponent(new DialogComponentStringSelection(f_source , "Source", "Any", "UniProt", "HGNC"));
        
        f_type.addChangeListener(new ChangeListener() {
        	public void stateChanged(final ChangeEvent e) {
        		 set_controls();
        	}
        });
        
        createNewTab("Advanced");
        addDialogComponent(new DialogComponentString(f_url, "URL for EBI QuickGO website:"));
        addDialogComponent(new DialogComponentNumber((SettingsModelNumber) QuickGOSourceNodeModel.make(QuickGOSourceNodeModel.CFGKEY_MAX_ENTRIES), "Max. results per search", 100, 7));
        addDialogComponent(new DialogComponentString(f_advquery, "Advanced Query (optional):"));
        addDialogComponent(new DialogComponentString(f_ancestor, "Ancestor"));
        addDialogComponent(new DialogComponentString(f_ref, "Reference"));
        addDialogComponent(new DialogComponentString(f_with, "With"));
        addDialogComponent(new DialogComponentString(f_taxonomy, "Taxonomy"));
        addDialogComponent(new DialogComponentString(f_protein, "Protein"));
    }
}



