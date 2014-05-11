package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.ExecutorUtils;
import au.edu.unimelb.plantcell.core.ExternalProgram;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.TempDirectory;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.CoordinateSystem;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.core.cells.TrackCreator;
import au.edu.unimelb.plantcell.core.regions.RegionsAnnotation;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.io.write.fasta.FastaWriter;


/**
 * This is the model implementation of EmbossPredictor.
 * Runs EMBOSS tools which take sequence(s) as input and provide a GFF output for inclusion as a annotation track on the output sequences.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class EmbossPredictorNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("EMBOSS Prediction");
       
    // configuration settings
    public static final String CFGKEY_SEQUENCE = "sequence-column";
    public static final String CFGKEY_PROGRAM  = "emboss-program";
    public static final String CFGKEY_ARGS     = "command-line-args";
    public static final String CFGKEY_USER_FIELDS = UserSettingsPanel.CFGKEY_USER_FIELDS;

    
    private SettingsModelString m_sequence = new SettingsModelString(CFGKEY_SEQUENCE, "");
    private SettingsModelString m_program  = new SettingsModelString(CFGKEY_PROGRAM, "iep");
    private SettingsModelString m_args     = new SettingsModelString(CFGKEY_ARGS, "");
    private SettingsModelString m_user_fields = new SettingsModelString(CFGKEY_USER_FIELDS, "");

    /**
     * Constructor for the node model.
     */
    protected EmbossPredictorNodeModel() {
        this(1, 2);
    }

    public EmbossPredictorNodeModel(int n_in, int n_out) {
		super(n_in, n_out);
	}
    
    public static List<ACDApplication> getEmbossPrograms(EmbossProgramSelector sel) {
    	String emboss_dir = ACDApplication.getEmbossDir();
    	logger.info("Scanning ACD programs from: "+emboss_dir);
     	File[] acd_files = new File(emboss_dir+"/acd").listFiles();
    	int failed = 0;
    	int ok = 0;
    	List<ACDApplication> progs = new ArrayList<ACDApplication>();
    	for (File f : acd_files) {
    		if (!f.getName().endsWith(".acd"))
    			continue;
    		BufferedReader rdr = null;
    		try {
	    		//logger.info("Scanning "+f.getName());
	    		rdr = BufferedFileReader.createNewReader(new FileInputStream(f));
	        	ACDApplication appl = new ACDApplication(new ACDStreamReader(rdr));
	        	rdr.close();
	        	ok++;
	        	if (sel == null || (sel != null && sel.accept(appl))) {
	        		//logger.info("Found suitable program: "+appl.getName());
	        		progs.add(appl);
	        	}
    		} catch (Exception e) {
    			failed++;
    		} finally {
    			if (rdr != null)
					try {
						rdr.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    		}
    	}
    	logger.info("Processed "+(failed+ok)+" ACD files: "+failed+" could not be parsed.");
    	logger.info("Found "+progs.size()+" suitable EMBOSS programs");
    	return progs;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

    	String       s = m_program.getStringValue();
    	String program = s.substring(0, s.indexOf(':'));
    	logger.info("Running EMBOSS prediction program: "+program);
    	String emboss_dir = ACDApplication.getEmbossDir();
    	
    	logger.info("Running EMBOSS software using preference: "+emboss_dir);
    	File prog = ExternalProgram.find(emboss_dir, program);
    	if (prog == null)
    		throw new InvalidSettingsException("Unable to locate: "+program);
    	logger.info("Running: "+prog.getAbsolutePath());
    	
    	DataTableSpec[] outSpecs = make_output_spec(inData[0].getSpec());
    	MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(outSpecs[0]), "Pred");
    	MyDataContainer c2 = new MyDataContainer(exec.createDataContainer(outSpecs[1]), "AS");
    	
    	int seq_idx = inData[0].getSpec().findColumnIndex(m_sequence.getStringValue());
    	if (seq_idx  < 0)
    		throw new InvalidSettingsException("Cannot find column: "+m_sequence.getStringValue());
    	
    	// max 50 sequences per batch or 1MB whichever comes first
    	BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(inData[0].iterator(), seq_idx, 50, 1000 * 1000, 
    			new SequenceProcessor() {

					@Override
					public SequenceValue process(SequenceValue sv) {
						if (!sv.isValid()) {
							logger.warn("Skipping sequence: "+sv.getID()+", it is not valid!");
							return null;
						}
						return sv;
					}
    	});
    	
    	int done = 0;
    	while (bsi.hasNext()) {
    		Map<UniqueID,SequenceValue> batch_map = bsi.nextAsMap();
    		if (batch_map == null)
    			break;
    		
    		// build command line arguments
    		CommandLine cl = new CommandLine(prog);
    		String[] args = m_args.getStringValue().split("\\s+");
    		for (String arg : args) {
    			cl.addArgument(arg);
    		}
    		
    		// run via commons-exec
    		DefaultExecutor exe = new DefaultExecutor();
	    	exe.setExitValues(new int[] {0});
	    	TempDirectory td = new TempDirectory();
	    	File tmp_fasta = File.createTempFile("fasta_input", ".fasta", td.asFile());
	    	cl.addArgument("-sequence");
    		cl.addArgument(tmp_fasta.getAbsolutePath());
    		cl.addArgument("-auto");
    		cl.addArgument("-stdout");
    		cl.addArgument("-rformat");
    		cl.addArgument("gff");
    		
	    	logger.info("Got batch of "+batch_map.size()+" sequences.");
    		new FastaWriter(tmp_fasta, batch_map).write();
    		logger.info("Saved batch to "+tmp_fasta.getAbsolutePath());
    		
    		GFFReader r_gff = new GFFReader(exec, c1, batch_map);
    		StringLogger sl = new StringLogger(logger, true);	// true == log as error rather than warning
	    	exe.setStreamHandler(new PumpStreamHandler(r_gff, sl));
	    	// must place files in here: remember emboss invocations can create many files
	    	// we will cleanup this folder at the end of each run SO BE CAREFUL what path you use!
	    	exe.setWorkingDirectory(td.asFile());
	    	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
	    	
	    	int exitCode = new ExecutorUtils(exe, logger).run(cl);
	    	td.deleteRecursive();
	    	
	    	if (exe.isFailure(exitCode)) {
	    		if (exe.getWatchdog().killedProcess())
	    			throw new Exception("EMBOSS failed - watchdog says no...");
	    		else
	    			throw new Exception("EMBOSS failed - check console messages and input data");
	    	}
	    	
	    	// if all went well with augmenting the tabular output, then we must save the annotations
	    	// to the respective sequences for this batch
	    	r_gff.save_annotations(logger, program, c2, getTrackCreator());
	    	
    		// update user progress
    		exec.checkCanceled();
    		done += batch_map.size();
    		exec.setProgress(((double)done) / inData[0].getRowCount());
    	}
        return new BufferedDataTable[]{c1.close(), c2.close()};
    }

    private TrackCreator getTrackCreator() {
    	return new TrackCreator() {

			@Override
			public Track createTrack(String name)
					throws InvalidSettingsException {
				return new Track(name, new RegionsAnnotation(), CoordinateSystem.OFFSET_FROM_START);
			}
			
		};
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	
        return make_output_spec(inSpecs[0]);
    }

    private DataTableSpec[] make_output_spec(DataTableSpec inSpec) throws InvalidSettingsException {
    	// from http://www.sanger.ac.uk/resources/software/gff/spec.html
    	// <seqname> <source> <feature> <start> <end> <score> <strand> <frame> [attributes] [comments] 
    	DataColumnSpec[] c1 = new DataColumnSpec[9];
    	c1[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
    	c1[1] = new DataColumnSpecCreator("Source", StringCell.TYPE).createSpec();
    	c1[2] = new DataColumnSpecCreator("Feature", StringCell.TYPE).createSpec();
    	c1[3] = new DataColumnSpecCreator("Start", IntCell.TYPE).createSpec();
    	c1[4] = new DataColumnSpecCreator("End", IntCell.TYPE).createSpec();
    	c1[5] = new DataColumnSpecCreator("Score", DoubleCell.TYPE).createSpec();
    	c1[6] = new DataColumnSpecCreator("Strand", StringCell.TYPE).createSpec();
    	c1[7] = new DataColumnSpecCreator("Frame", IntCell.TYPE).createSpec();
    	c1[8] = new DataColumnSpecCreator("Attributes", StringCell.TYPE).createSpec();
    	
    	DataColumnSpec[] c2 = new DataColumnSpec[1];
    	DataColumnSpecCreator my_annot_spec = new DataColumnSpecCreator("Annotated Sequence (incl. EMBOSS predictions)", SequenceCell.TYPE);
    	int seq_idx = inSpec.findColumnIndex(m_sequence.getStringValue());
    	DataColumnProperties isp = new DataColumnProperties();
    	if (seq_idx >= 0) 
    		isp = inSpec.getColumnSpec(seq_idx).getProperties();
    	String s = m_program.getStringValue();
    	int idx = s.indexOf(":");
    	if (idx >= 0) {
	    	String prog = s.substring(0, idx);
	    	TrackColumnPropertiesCreator tcpc = new TrackColumnPropertiesCreator(isp,
					new Track(Track.EMBOSS_TRACKS+prog, getTrackCreator())
				);
	    	my_annot_spec.setProperties(tcpc.getProperties());
    	}
    	c2[0] = my_annot_spec.createSpec();

		return new DataTableSpec[] { new DataTableSpec(c1), new DataTableSpec(c2) };
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

    /**
     * Suitable for running small stdout programs only, this routine runs the specified
     * program with arguments and returns a string of the stdout.
     * 
     * @param cmdArgs cannot be null
     * @param appl cannot be null
     * @return
     */
	public static String run_emboss_command(ACDApplication appl, String[] cmdArgs, boolean want_stderr) 
							throws IOException, InvalidSettingsException {
		assert(appl != null && cmdArgs != null);
    	String emboss_dir = ACDApplication.getEmbossDir();
    	
    	File prog = ExternalProgram.find(emboss_dir, appl.getName());
    	if (prog == null)
    		throw new InvalidSettingsException("Unable to locate: "+appl.getName());
    	
    	CommandLine cl = new CommandLine(prog);
		for (String arg : cmdArgs) {
			cl.addArgument(arg);
		}
		
		// run via commons-exec
		DefaultExecutor exe = new DefaultExecutor();
    	exe.setExitValues(new int[] {0});
    	StringLogger sl = new StringLogger();
    	PumpStreamHandler psh = new PumpStreamHandler(sl, new StringLogger());
    	if (want_stderr)
    		psh = new PumpStreamHandler(new StringLogger(), sl);
    	exe.setStreamHandler(psh);
    	// dont change the working (current) directory for now...
    	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
    	
    	int exitCode = exe.execute(cl);  
    	
    	if (exe.isFailure(exitCode))
    		throw new InvalidSettingsException("Command failed!");
		return sl.toString();
	}

}

