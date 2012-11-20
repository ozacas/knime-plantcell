package au.edu.unimelb.plantcell.io.read.multiexcel;


import java.io.File;
import java.io.IOException;

import org.knime.core.data.*;
import org.knime.core.data.def.*;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.*;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import jxl.*;


/**
 * This is the model implementation of MultiExcelReader.
 * Reads all Microsoft-Excel (2003 and earlier) *.xls documents in a folder and creates a unified table representing all rows from all sheets in all Excel files contained in this folder. Does not search subfolders.
 *
 * @author Andrew Cassin
 */
public class MultiExcelReaderNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(MultiExcelReaderNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_FOLDER = "Folder";
    static final String CFGKEY_DELIMITER="Delimiter";
    /** initial default count value. */
    static final String DEFAULT_FOLDER = "/tmp";
    static final String DEFAULT_DELIMITER="**-**";

    // example value: the models count variable filled from the dialog 
    // and used in the models execution method. The default components of the
    // dialog work with "SettingsModels".
    private final SettingsModelString m_folder =
        new SettingsModelString(MultiExcelReaderNodeModel.CFGKEY_FOLDER,
                    MultiExcelReaderNodeModel.DEFAULT_FOLDER);
    private final SettingsModelString m_delimiter = 
    	new SettingsModelString(MultiExcelReaderNodeModel.CFGKEY_DELIMITER, 
    			MultiExcelReaderNodeModel.DEFAULT_DELIMITER);
    
    

    /**
     * Constructor for the node model.
     */
    protected MultiExcelReaderNodeModel() {
        // one outgoing port only
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

       
    	String dir = m_folder.getStringValue();
        logger.info("Reading files from "+dir);
        
        // the data table spec of the single output table, 
        // the table will have three columns:
        DataColumnSpec[] allColSpecs = new DataColumnSpec[4];
        allColSpecs[0] = 
            new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        allColSpecs[1] = 
            new DataColumnSpecCreator("Sheet Name", StringCell.TYPE).createSpec();
        allColSpecs[2] = 
            new DataColumnSpecCreator("Row ID", IntCell.TYPE).createSpec();
        allColSpecs[3] = 
        	new DataColumnSpecCreator("Row (separated by chosen delimiter)", StringCell.TYPE).createSpec();
        DataTableSpec outputSpec = new DataTableSpec(allColSpecs);
        // the execution context will provide us with storage capacity, in this
        // case a data container to which we will add rows sequentially
        // Note, this container can also handle arbitrary big data tables, it
        // will buffer to disc if necessary.
        BufferedDataContainer container = exec.createDataContainer(outputSpec);
        File tmp_dir = new File(dir);
        String delim = m_delimiter.getStringValue();
        File[] list_files = tmp_dir.listFiles();
        int row_id =1;
        for (int i=0; i<list_files.length; i++) {
        	if (list_files[i].getName().endsWith(".xls") || list_files[i].getName().endsWith(".XLS")) {
        		row_id = process_excel(container, exec, 1.0/list_files.length, i, row_id, list_files[i], delim);
        	}
        	exec.checkCanceled();
        }
        // once we are done, we close the container and return its table
        container.close();
        BufferedDataTable out = container.getTable();
        return new BufferedDataTable[]{out};
    }

    protected int process_excel(BufferedDataContainer container, ExecutionContext exec, double frac, int nth_file, int row_id, File in_file, String delim) throws Exception {
    	 Workbook wb = Workbook.getWorkbook(in_file);
         int n_sheets = wb.getNumberOfSheets();
     
         for (int i=0; i<n_sheets; i++) {
                 row_id = process_sheet(container, row_id, in_file, wb.getSheet(i), delim);
                 double pc = nth_file * frac + ((double)i)/n_sheets*frac;
                 exec.setProgress(pc);
         }
         return row_id;
    }
    
    protected int process_sheet(BufferedDataContainer container, int row_id, File excel_file, Sheet s, String delim) throws Exception {
    	int         n_rows = s.getRows();                                                              
        String wb_name     = excel_file.getName();                                                 
        String s_name      = s.getName();              
        StringCell cell_wb = new StringCell(wb_name);
        StringCell cell_s  = new StringCell(s_name);
        for (int r=0; r<n_rows; r++) {
                Cell[] cells = s.getRow(r);
                int max_non_empty_cell = cells.length-1;
                while (max_non_empty_cell >= 0) {
                        if (cells[max_non_empty_cell].getType() != CellType.EMPTY) {
                                break;
                        }
                        max_non_empty_cell--;
                }
                if (max_non_empty_cell >= 0) {
                        StringBuffer line = new StringBuffer(1024);
                        for (int c=0; c<=max_non_empty_cell; c++) {
                                line.append(cells[c].getContents());
                                if (c < max_non_empty_cell) {
                                        line.append(delim);
                                }
                        }
                        
                        IntCell    cell_rid= new IntCell(r);
                        StringCell cell_line=new StringCell(line.toString());
                       
                        DataRow new_row = new DefaultRow("Row"+row_id, cell_wb, cell_s, cell_rid, cell_line);
                        row_id++;
                        container.addRowToTable(new_row);
                      
                }
        }
        return row_id;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Code executed on reset.
        // Models build during execute are cleared here.
        // Also data handled in load/saveInternals will be erased here.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        // TODO: check if user settings are available, fit to the incoming
        // table structure, and the incoming types are feasible for the node
        // to execute. If the node can execute in its current state return
        // the spec of its output data table(s) (if you can, otherwise an array
        // with null elements), or throw an exception with a useful user message

        return new DataTableSpec[]{null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        // TODO save user settings to the config object.
        
        m_folder.saveSettingsTo(settings);
        m_delimiter.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        // TODO load (valid) settings from the config object.
        // It can be safely assumed that the settings are valided by the 
        // method below.
        
        m_folder.loadSettingsFrom(settings);
        m_delimiter.loadSettingsFrom(settings);    
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        // TODO check if the settings could be applied to our model
        // e.g. if the count is in a certain range (which is ensured by the
        // SettingsModel).
        // Do not actually set any values of any member variables.

	    m_folder.validateSettings(settings);
	    m_delimiter.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
        // TODO load internal data. 
        // Everything handed to output ports is loaded automatically (data
        // returned by the execute method, models loaded in loadModelContent,
        // and user settings set through loadSettingsFrom - is all taken care 
        // of). Load here only the other internals that need to be restored
        // (e.g. data used by the views).

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
        // TODO save internal models. 
        // Everything written to output ports is saved automatically (data
        // returned by the execute method, models saved in the saveModelContent,
        // and user settings saved through saveSettingsTo - is all taken care 
        // of). Save here only the other internals that need to be preserved
        // (e.g. data used by the views).

    }

}

