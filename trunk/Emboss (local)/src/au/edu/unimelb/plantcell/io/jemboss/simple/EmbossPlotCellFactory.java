package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.NodeLogger;

import au.edu.unimelb.plantcell.core.ExecutorUtils;
import au.edu.unimelb.plantcell.core.TempDirectory;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.write.fasta.FastaWriter;

/**
 * Appends an image associated with the current sequence for each row of input
 * by running the chosen EMBOSS program 
 * 
 * @author andrew.cassin
 *
 */
public class EmbossPlotCellFactory extends SingleCellFactory {
	private int m_seqidx;
	private NodeLogger m_logger;
	private String m_prog, m_args;
	private ACDApplication m_appl;
	private final List<String> m_extra_args = new ArrayList<String>();
	
	public EmbossPlotCellFactory(DataColumnSpec new_col, int seq_idx, 
			File emboss_program, String args, ACDApplication appl, NodeLogger l) {
		super(new_col);
		assert(seq_idx >= 0);
		assert(emboss_program != null && args != null && l != null);
		m_seqidx = seq_idx;
		m_logger = l;
		m_prog   = emboss_program.getAbsolutePath();
		m_args   = args;
		m_appl   = appl;
		
		m_extra_args.add("-auto");
		m_extra_args.add("-stdout");
		
		if (m_appl.hasFieldType("plot", "toggle")) {
    		m_extra_args.add("-plot");
    		m_extra_args.add("Y");
    	}
    	if (m_appl.hasFieldType("graph", "xygraph") || 
    			m_appl.hasFieldType("graph", "graph")) {
    		m_extra_args.add("-graph");
    		m_extra_args.add("png");
    	}
	}
	
	@Override
	public DataCell getCell(DataRow row) {
		DataCell c = row.getCell(m_seqidx);
		if (c == null || c.isMissing())
			return DataType.getMissingCell();
		assert(c instanceof SequenceValue);
		SequenceValue sv = (SequenceValue) c;
		
		// build command line arguments
		CommandLine cl = new CommandLine(m_prog);
		if (m_args.indexOf("-gtitle") < 0) {
			cl.addArgument("-gtitle");
			cl.addArgument("\""+sv.getID()+"\"", false);
		}
		String[] args = m_args.split("\\s+");
		for (String arg : args) {
			cl.addArgument(arg);
		}
		
		// run via commons-exec
		DefaultExecutor exe = new DefaultExecutor();
    	exe.setExitValues(new int[] {0});
    	DataCell c_img = DataType.getMissingCell();

    	try {
	    	TempDirectory td = new TempDirectory();
	    	File tmp_fasta = File.createTempFile("fasta_input", ".fasta", td.asFile());
	    	
	    	String seq_arg = "-sequence";
	    	if (m_appl.hasFieldType("seqall", "seqall")) 
	    		seq_arg = "-seqall";
	    		
	    	cl.addArgument(seq_arg);
			cl.addArgument(tmp_fasta.getAbsolutePath());
	    	cl.addArguments(m_extra_args.toArray(new String[0]));

	    	HashMap<UniqueID,SequenceValue> batch_map = new HashMap<UniqueID,SequenceValue>();
	    	batch_map.put(new UniqueID(), sv);
			new FastaWriter(tmp_fasta, batch_map).write();
			m_logger.info("Saved batch to "+tmp_fasta.getAbsolutePath());
			
	    	exe.setStreamHandler(new PumpStreamHandler(new IgnoreReader(), new StringLogger(m_logger, true)));
	    	
	    	// must place files in here: remember emboss invocations can create many files
	    	// we will cleanup this folder at the end of each run SO BE CAREFUL what path you use!
	    	exe.setWorkingDirectory(td.asFile());
	    	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
	    	
	    	int exitCode = new ExecutorUtils(exe, m_logger).run(cl);
	    	if (exe.isFailure(exitCode)) {
	    		throw new IOException("Execution of EMBOSS program failed: exit code "+exitCode);
	    	}
	    	
	    	File[] results = td.asFile().listFiles();
	    	
	    	// locate first image file and read it into a cell
	    	for (File f : results) {
	    		if (f.getName().toLowerCase().endsWith(".png")) {
	    			FileInputStream fis = new FileInputStream(f);
	    			byte[] result = new byte[(int)f.length()];
	    			if (fis.read(result) >= f.length()) {
	    				c_img = new PNGImageContent(result).toImageCell();
	    			}
	    			fis.close();
	    		}
	    	}
		    	
		    td.deleteRecursive();
		} catch (ExecuteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
   
		return c_img;
	}

}
