package au.edu.unimelb.plantcell.gene.prediction;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
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
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.CorePlugin;
import au.edu.unimelb.plantcell.core.ErrorLogger;
import au.edu.unimelb.plantcell.core.ExecutorUtils;
import au.edu.unimelb.plantcell.core.ExternalProgram;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.Preferences;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.CoordinateSystem;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.core.cells.TrackCreator;
import au.edu.unimelb.plantcell.core.regions.GeneRegionsAnnotation;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.io.write.fasta.FastaWriter;

/**
 * This is the model implementation of AugustusNodeModel.
 * Runs augustus (http://augustus.gobics.de) on the local computer and loads its predictions into a KNIME table.
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AugustusNodeModel extends NodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("Augustus (local)");
    
	public final static String CFGKEY_STRAND = "strand";
	public final static String CFGKEY_SINGLESTRAND = "predict-each-strand-independently?";
	public final static String CFGKEY_GENEMODEL = "gene-model-for-predictions";
	public final static String CFGKEY_OTHER = "other-arguments";
	public final static String CFGKEY_SEQUENCE = "sequence";
	public final static String CFGKEY_WANTED= "wanted-items";
	
	private final SettingsModelString  m_strand = new SettingsModelString(CFGKEY_STRAND, "both");
	private final SettingsModelBoolean m_independent = new SettingsModelBoolean(CFGKEY_SINGLESTRAND, Boolean.TRUE);
	private final SettingsModelString  m_species = new SettingsModelString(CFGKEY_GENEMODEL, "arabidopsis");
	private final SettingsModelString  m_other   = new SettingsModelString(CFGKEY_OTHER, "");
	private final SettingsModelString  m_sequence   = new SettingsModelString(CFGKEY_SEQUENCE, "");
	private final SettingsModelStringArray m_wanted =new SettingsModelStringArray(CFGKEY_WANTED, new String[] { "All" });
	
	// internal state (not persisted)
	private static String[] m_gene_models;
	
    /**
     * Constructor for the node model.
     */
    protected AugustusNodeModel() {
        super(1, 2);
    }

    /**
     * Returns a list of available gene models, based on the user-specified
     * PlantCell preference setting for the augustus software install directory
     * @return
     */
    public final synchronized static String[] getGeneModels() {
    	if (m_gene_models != null) {
    		return m_gene_models;
    	}
    	IPreferenceStore prefs = CorePlugin.getDefault().getPreferenceStore();
		String install_dir = prefs.getString(Preferences.PREFS_AUGUSTUS_FOLDER);
		
    	try {
    		File config_dir = new File(install_dir, "config");
    		if (!config_dir.isDirectory()) 
    			throw new InvalidSettingsException("Cannot locate AUGUSTUS config directory!");
    		File species_dir = new File(config_dir, "species");
    		if (!species_dir.isDirectory() && species_dir.canRead()) 
    			throw new InvalidSettingsException("Cannot locate AUGUSTUS species directory!");
    		File[] available_species = species_dir.listFiles(new FileFilter() {

				@Override
				public boolean accept(File arg0) {
					return (arg0.isDirectory() && arg0.canRead());
				}
    			
    		});
    		ArrayList<String> ret = new ArrayList<String>();
    		for (File gene_model_dir : available_species) {
    			ret.add(gene_model_dir.getName());
    		}
    		Collections.sort(ret);
    		return ret.toArray(new String[0]);
    	} catch (Exception e) {
    		e.printStackTrace();
    		logger.warn("Cannot load list of gene models, returning default list!");
    		logger.warn("Looked for species in "+install_dir+ "/config/species. Is this folder correct?");
    		return new String[] { "arabidopsis", "ce", "maize", "human", "fly", "toxoplasma" };
    	}
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	IPreferenceStore prefs = CorePlugin.getDefault().getPreferenceStore();
		String install_dir = prefs.getString(Preferences.PREFS_AUGUSTUS_FOLDER);
    	File augustus = ExternalProgram.find(install_dir, "augustus");
    	if (augustus == null) 
    		throw new InvalidSettingsException("Unable to locate augustus: please check the PlantCell preferences (File->Preferences)");
    	
    	logger.info("Running augustus: "+augustus.getAbsolutePath());
    	
    	CommandLine cmdLine = new CommandLine(augustus);
    	
    	cmdLine.addArgument("--species="+m_species.getStringValue());
    	cmdLine.addArgument("--strand="+m_strand.getStringValue());
    	String true_false = "true";
    	String other = m_other.getStringValue();
    	if (!m_independent.getBooleanValue()) {
    		true_false = "false";
    	} else {
    		// wanting independent strand predictions forces UTR prediction off (for all but a few gene models) 
    		// so we automatically add to other if not already specified by the user
    		if (other.indexOf("--UTR=") < 0) {
    			other += " --UTR=off";
    		}
    	}
    	cmdLine.addArgument("--singlestrand="+true_false);
    	cmdLine.addArgument(other);
    	
    	int seq_idx = inData[0].getSpec().findColumnIndex(m_sequence.getStringValue());
    	if (seq_idx < 0)
    		throw new InvalidSettingsException("Cannot find column: "+m_sequence.getStringValue()+" - reconfigure?");
    	
    	RowIterator it = inData[0].iterator();
    	BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(it, seq_idx, 1000, 1000 * 1000, 
    			new SequenceProcessor() {
    				private boolean warned = false;
					@Override
					public SequenceValue process(SequenceValue sv) {
						if (!sv.isValid()) {
							if (!warned)
								logger.warn("Ignored invalid residue(s) in "+sv.getID());
							warned = true;
							return null;
						}
						if (sv.getSequenceType().equals(SequenceType.AA)) {
							if (!warned) 
								logger.warn("Ignoring protein sequence: "+sv.getID());
							warned = true;
							return null;
						}
						return sv;
					}
    		
    	});
    	
    	DataTableSpec[] outSpecs = make_output_spec(inData[0].getSpec());
    	MyDataContainer c  = new MyDataContainer(exec.createDataContainer(outSpecs[0]), "Seq");		// nucleotide predictions
    	MyDataContainer c2 = new MyDataContainer(exec.createDataContainer(outSpecs[1]), "PredProt");		// predicted proteins

    	// set AUGUSTUS_CONFIG_PATH to ensure augustus.exe can find the gene models
    	File config_dir = new File(install_dir, "config");
    	if (config_dir == null || !config_dir.isDirectory()) {
    		throw new InvalidSettingsException("Cannot locate AUGUSTUS_CONFIG_DIR/config!");
    	}
    	Map<String,String> env = new HashMap<String,String>();
    	env.put("AUGUSTUS_CONFIG_PATH", config_dir.getAbsolutePath());
    	env.put("PATH", System.getenv("PATH"));
    	env.put("CYGWIN", "nodosfilewarning");	// no harm if not using cygwin under win32/64
    	
    	// process augustus output obtained via apache commons-exec loggers
		File                          tmp_fasta = File.createTempFile("tmp_fasta", ".fasta");
		cmdLine.addArgument(tmp_fasta.getName());
    	logger.info("Setting working directory to: "+tmp_fasta.getParentFile().getAbsolutePath());
    	int done = 0;
    	while (bsi.hasNext()) {
    	    Map<UniqueID,SequenceValue> batch_map = bsi.nextAsMap();
    		if (batch_map == null)		// handle the case where the SP rejects all sequences in the last batch
    			break;
    		
    		// the file is opened and closed as part of the write() call
    		new FastaWriter(tmp_fasta, batch_map).write();
    		
    		DefaultExecutor exe = new DefaultExecutor();
        	exe.setExitValues(new int[] {0});
        	AugustusLogger tsv = new AugustusLogger(exec, c2, batch_map, m_wanted.getStringArrayValue());
        	exe.setStreamHandler(new PumpStreamHandler(tsv, new ErrorLogger(logger)));
        	exe.setWorkingDirectory(tmp_fasta.getParentFile());		// arbitrary choice
        	exe.setWatchdog(new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT));
        	
        	int exitCode = new ExecutorUtils(exe, logger).run(cmdLine);
        	if (exe.isFailure(exitCode)) {
        		if (exe.getWatchdog().killedProcess())
        			throw new Exception("Augustus failed - watchdog says no...");
        		else
        			throw new Exception("Augustus failed - check console messages and input data");
        	}
        	
        	tmp_fasta.delete();		// delete temp fasta file
        	
        	// dump out input sequence cells incl. annotations (order will be screwed up)
        	for (SequenceValue sv: batch_map.values()) {
        		c.addRow(new DataCell[] { (SequenceCell) sv }); 
        	}
        	exec.checkCanceled();
        	done += batch_map.size();
        	exec.setProgress(((double)done) / inData[0].getRowCount());
    	}
    	
    	// finish up...
        return new BufferedDataTable[]{c.close(), c2.close()};
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
    	DataColumnSpec[] cols = new DataColumnSpec[1];
    	DataColumnSpecCreator my_annot_spec = new DataColumnSpecCreator("Input Sequence (incl. augustus annotations)", SequenceCell.TYPE);
    	int seq_idx = inSpec.findColumnIndex(m_sequence.getStringValue());
    	DataColumnProperties isp = new DataColumnProperties();
    	if (seq_idx >= 0) {
    		isp = inSpec.getColumnSpec(seq_idx).getProperties();
    	}
    	TrackColumnPropertiesCreator tcpc = new TrackColumnPropertiesCreator(isp,
				new Track(Track.GENE_PREDICTION_AUGUSTUS, getTrackCreator())
			);
    	my_annot_spec.setProperties(tcpc.getProperties());
    	cols[0] = my_annot_spec.createSpec();
    	
    	DataColumnSpec[] cols2= new DataColumnSpec[1];
    	cols2[0] = new DataColumnSpecCreator("Predicted Proteins", SequenceCell.TYPE).createSpec();
    	
		return new DataTableSpec[] { new DataTableSpec(cols), new DataTableSpec(cols2) };
	}

	public static TrackCreator getTrackCreator() {
		return new TrackCreator() {

			@Override
			public Track createTrack(String name)
					throws InvalidSettingsException {
				return new Track(name, new GeneRegionsAnnotation(), CoordinateSystem.OFFSET_FROM_START);
			}
			
		};
	}
	
	/**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_strand.saveSettingsTo(settings);
    	m_independent.saveSettingsTo(settings);
    	m_species.saveSettingsTo(settings);
    	m_other.saveSettingsTo(settings);
    	m_sequence.saveSettingsTo(settings);
    	m_wanted.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_strand.loadSettingsFrom(settings);
    	m_independent.loadSettingsFrom(settings);
    	m_species.loadSettingsFrom(settings);
    	m_other.loadSettingsFrom(settings);
    	m_sequence.loadSettingsFrom(settings);
    	m_wanted.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_strand.validateSettings(settings);
    	m_independent.validateSettings(settings);
    	m_species.validateSettings(settings);
    	m_other.validateSettings(settings);
    	m_sequence.validateSettings(settings);
    	m_wanted.validateSettings(settings);
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

