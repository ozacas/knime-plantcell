package au.edu.unimelb.plantcell.io.read.multiexcel;


import org.knime.core.node.defaultnodesettings.*;
import javax.swing.JFileChooser;

/**
 * <code>NodeDialog</code> for the "MultiExcelReader" Node.
 * Reads all Microsoft-Excel (2003 and earlier) *.xls documents in a folder and creates a unified table representing all rows from all sheets in all Excel files contained in this folder. Does not search subfolders.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Andrew Cassin
 */
public class MultiExcelReaderNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring MultiExcelReader node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
    protected MultiExcelReaderNodeDialog() {
        super();
        
        addDialogComponent(new DialogComponentFileChooser(
                new SettingsModelString(
                    MultiExcelReaderNodeModel.CFGKEY_FOLDER,
                    MultiExcelReaderNodeModel.DEFAULT_FOLDER),
                    "folder-history", JFileChooser.OPEN_DIALOG, true, ""));
        
        addDialogComponent(new DialogComponentString(new SettingsModelString(MultiExcelReaderNodeModel.CFGKEY_DELIMITER,
        		MultiExcelReaderNodeModel.DEFAULT_DELIMITER), "Cell Delimiter"));
                    
    }
}

