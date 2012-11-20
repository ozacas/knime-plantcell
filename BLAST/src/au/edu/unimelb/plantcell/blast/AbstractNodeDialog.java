package au.edu.unimelb.plantcell.blast;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentFileChooser;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringListSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;


public abstract class AbstractNodeDialog extends DefaultNodeSettingsPane {
	  
    protected void init() {
    	init(true, true, ".fasta|.fa");
    }
    
    protected void init(boolean show_program, boolean show_matrix, String file_extensions) {
    	createNewGroup("What database to BLAST against?");
        addDialogComponent(new DialogComponentFileChooser(
        		new SettingsModelString(BLASTPlusNodeModel.CFGKEY_DATABASE, "c:/temp/uniprot.fasta"), 
        		"blast-database", 0, file_extensions));
        
        createNewGroup("Important settings");
        if (show_program) {
        	addDialogComponent(new DialogComponentStringSelection(new SettingsModelString(BLASTPlusNodeModel.CFGKEY_BLAST_PROG, "blastp"), "Program", new String[] { "blastp", "blastx", "blastn", "tblastn", "tblastx" }));
        }
        addDialogComponent(new DialogComponentNumber(new SettingsModelDouble(BLASTPlusNodeModel.CFGKEY_EVAL, 0.00001), "E-Value cutoff", 0.01));
        addDialogComponent(new DialogComponentNumber(new SettingsModelIntegerBounded(BLASTPlusNodeModel.CFGKEY_NUM_THREADS, 1, 1, 8), "Number of threads", 1));
        
        
        createNewTab("Advanced");
        if (show_matrix) {
        	addDialogComponent(new DialogComponentStringSelection(
        		new SettingsModelString(BLASTPlusNodeModel.CFGKEY_MATRIX, "PAM250"),
        		"Scoring matrix", new String[] { "BLOSUM62", "BLOSUM45", "BLOSUM80", "BLOSUM50", "BLOSUM90", "PAM250", "PAM30", "PAM70" }
        		));
        }
        addDialogComponent(new DialogComponentBoolean(
        		new SettingsModelBoolean(BLASTPlusNodeModel.CFGKEY_FILTER, true),
        		"Filter low complexity sequence (yes if checked)"
        		));
        
        String[] result_types   = BLASTPlusNodeModel.getResultOptions();
        String[] result_default = BLASTPlusNodeModel.getResultDefaults();
        
        addDialogComponent(new DialogComponentStringListSelection(
        		new SettingsModelStringArray(BLASTPlusNodeModel.CFGKEY_RESULTS, result_default),
        		"Results to put in output", result_types
        		));
        
        addDialogComponent(new DialogComponentString(
        		new SettingsModelString(BLASTPlusNodeModel.CFGKEY_OTHER, ""),
        		"Additional command line arguments (optional)"
        		));
        
        addDialogComponent(new DialogComponentButtonGroup(
        		new SettingsModelString(BLASTPlusNodeModel.CFGKEY_ANNOTATE_WHAT, "All"),
        		"Which sequences to report?", true, BLASTPlusNodeModel.ANNOTATION_GROUP, BLASTPlusNodeModel.ANNOTATION_GROUP
        		));
    }
}
