package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.ExternalProgram;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.TempDirectory;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.read.fasta.BatchFastaIterator;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.io.write.fasta.FastaWriter;


/**
 * This is the model implementation of EmbossPredictor.
 * Runs EMBOSS tools which take sequence(s) as input and provide a GFF output for inclusion as a annotation track on the output sequences.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class EmbossSequenceNodeModel extends EmbossPredictorNodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("EMBOSS Sequences");
       
    // configuration settings
    public static final String CFGKEY_SEQUENCE = "sequence-column";
    public static final String CFGKEY_PROGRAM  = "emboss-program";
    public static final String CFGKEY_ARGS     = "command-line-args";
    public static final String CFGKEY_USER_FIELDS = UserSettingsPanel.CFGKEY_USER_FIELDS;
    
    private SettingsModelString m_sequence = new SettingsModelString(CFGKEY_SEQUENCE, "");
    private SettingsModelString m_program  = new SettingsModelString(CFGKEY_PROGRAM, "");
    private SettingsModelString m_args     = new SettingsModelString(CFGKEY_ARGS, "");
    private SettingsModelString m_user_fields = new SettingsModelString(CFGKEY_USER_FIELDS, "");
    	
    /**
     * Constructor for the node model.
     */
    protected EmbossSequenceNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	String       s = m_program.getStringValue();
    	int idx = s.indexOf(':');
    	if (idx < 0) 
    		throw new InvalidSettingsException("Please configure the node first!");
    	String program = s.substring(0, idx);
    	logger.info("Running EMBOSS sequence program: "+program);
    	String emboss_dir = ACDApplication.getEmbossDir();
    	
    	logger.info("Running EMBOSS software using preference: "+emboss_dir);
    	File prog = ExternalProgram.find(emboss_dir, program);
    	if (prog == null)
    		throw new InvalidSettingsException("Unable to locate: "+program);
    	logger.info("Found: "+prog.getAbsolutePath());
    	   
    	int seq_idx = inData[0].getSpec().findColumnIndex(m_sequence.getStringValue());
    	if (seq_idx < 0)
    		throw new InvalidSettingsException("Cannot find column: "+m_sequence.getStringValue()+" - reconfigure?");

    	ACDApplication appl = ACDApplication.find(program);
    	
    	DataTableSpec[] outSpecs = make_output_spec(inData[0].getSpec());
    	MyDataContainer c = new MyDataContainer(exec.createDataContainer(outSpecs[0]), "Seq");
    	RowIterator    it = inData[0].iterator();
    	
    	BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(it, seq_idx, 100, 100 * 1000, new SequenceProcessor() {

			@Override
			public SequenceValue process(SequenceValue sv) {
				return sv;
			}
    		
    	});
    	
    	Pattern p = Pattern.compile("^([^-_\\s]+?)([-_\\s].*)$");
    	
    	int done = 0;
    	while (bsi.hasNext()) {
    		Map<UniqueID,SequenceValue> batch_map = bsi.nextAsMap();
    		
    		CommandLine cl = new CommandLine(prog);
    		String[] args = m_args.getStringValue().split("\\s+");
    		for (String arg : args) {
    			cl.addArgument(arg);
    		}
    		
    		// run via commons-exec
    		DefaultExecutor exe = new DefaultExecutor();
        	exe.setExitValues(new int[] {0});

        	try {
    	    	TempDirectory td = new TempDirectory();
    	    	File tmp_fasta = File.createTempFile("fasta_input", ".fasta", td.asFile());
    	    	
    	    	String seq_arg = "-sequence";
    	    	if (appl.hasFieldType("seqall", "seqall")) 
    	    		seq_arg = "-seqall";
    	    		
    	    	cl.addArgument(seq_arg);
    			cl.addArgument(tmp_fasta.getAbsolutePath());

    			new FastaWriter(tmp_fasta, batch_map).write();
    			logger.info("Saved batch to "+tmp_fasta.getAbsolutePath());
    			ACDField output_sequence = appl.getFirstOutput();
    			cl.addArgument("-"+output_sequence.getName());
    			File tmp_out_file = File.createTempFile("tmp_out", ".fasta", td.asFile());
    			cl.addArgument(tmp_out_file.getAbsolutePath());
    			
    			addUserArgs(cl, m_user_fields.getStringValue());
    			
    	    	exe.setStreamHandler(new PumpStreamHandler(new IgnoreReader(), new StringLogger(logger, true)));
    	    	
    	    	// determine the sequence type for downstream nodes
    	    	SequenceType st = SequenceType.UNKNOWN;
    	    	if (output_sequence.hasProperty("type")) {
    	    		String val = output_sequence.getProperty("type").toLowerCase().trim();
    	    		if (val.indexOf("prot") >= 0)
    	    			st = SequenceType.AA;
    	    		if (val.indexOf("nucl") >= 0)
    	    			st = SequenceType.Nucleotide;
    	    		if (val.indexOf("rna") >= 0)
    	    			st = SequenceType.RNA;
    	    		if (val.indexOf("dna") >= 0)
    	    			st = SequenceType.DNA;
    	    	}
    	    	
    	    	// must place files in here: remember emboss invocations can create many files
    	    	// we will cleanup this folder at the end of each run SO BE CAREFUL what path you use!
    	    	exe.setWorkingDirectory(td.asFile());
    	    	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
    	    	
    	    	logger.info("Running: "+cl.toString());
    	    	int exitCode = exe.execute(cl);
    	    	logger.info("got exit code: "+exitCode);
    	    	
    	    	if (exe.isFailure(exitCode)) {
    	    		throw new IOException("Aborting run. Got failed exit code "+exitCode);
    	    	}
    	    	
    	    	if (!tmp_out_file.exists() || tmp_out_file.length() <= 0) {
    	    		throw new IOException("EMBOSS program did not create data!");
    	    	}
    	    	
    	    	BatchFastaIterator bfi = new BatchFastaIterator(tmp_out_file, st, 100);
    	    	while (bfi.hasNext()) {
    	    		List<SequenceValue> l = bfi.next();
    	    		for (SequenceValue sv : l) {
    	    			SequenceCell         sc = new SequenceCell(sv);
    	    			SequenceValue input_seq = batch_map.get(new UniqueID(sc.getID()));
    	    			if (input_seq == null) {		// try really hard to match the ID from the emboss program back to a batch sequence ID
    	    				Matcher m = p.matcher(sc.getID());
    	    				if (m.find()) {
    	    					String id2 = m.group(1);
    	    					String rest = m.group(2);
    	    					input_seq = batch_map.get(id2);
    	    					if (input_seq != null) {
    	    						sc.setID(input_seq.getID()+rest);
    	    					}
    	    				}
    	    			} else {
    	    				sc.setID(input_seq.getID());
    	    			}
    	    			c.addRow(new DataCell[] { sc });
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
    		
    		exec.checkCanceled();
    		done += batch_map.size();
    		exec.setProgress(((double)done)/inData[0].getRowCount());
    	}
     
        // return it
        return new BufferedDataTable[]{c.close()};	
    }
   
    private void addUserArgs(CommandLine cl, String user_fields) {
    	Pattern p = Pattern.compile("(\\S+)\\s+=\\s+(.*)$");
    	
    	// add user defined fields from m_user_fields
		String[] fields = user_fields.split("[\\r\\n]+");
		for (String line : fields) {
			Matcher m = p.matcher(line);
			if (m.matches()) {
				cl.addArgument("-"+m.group(1));
				cl.addArgument(m.group(2));
			}
		}
	}

	protected DataTableSpec[] make_output_spec(DataTableSpec inSpec) {
    	DataColumnSpec[] cols = new DataColumnSpec[1];
    	cols[0] = new DataColumnSpecCreator("Output Sequence", SequenceCell.TYPE).createSpec();
    	
    	return new DataTableSpec[] { new DataTableSpec(cols) };
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return make_output_spec(inSpecs[0]);
    }
    
	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_sequence.saveSettingsTo(settings);
    	m_program.saveSettingsTo(settings);
    	m_args.saveSettingsTo(settings);
    	m_user_fields.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.loadSettingsFrom(settings);
    	m_program.loadSettingsFrom(settings);
    	m_args.loadSettingsFrom(settings);
    	m_user_fields.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_sequence.validateSettings(settings);
    	m_program.validateSettings(settings);
    	m_args.validateSettings(settings);
    	m_user_fields.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
    }

}

