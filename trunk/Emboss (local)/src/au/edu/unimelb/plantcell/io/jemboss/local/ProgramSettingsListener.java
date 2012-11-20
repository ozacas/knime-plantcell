package au.edu.unimelb.plantcell.io.jemboss.local;

import java.io.File;

import au.edu.unimelb.plantcell.io.jemboss.settings.OutputFileSetting;
import au.edu.unimelb.plantcell.io.jemboss.settings.ProgramSetting;

/**
 * Responsible for building a command line to run an emboss program and commencing marshalling of input/output data
 * 
 * @author andrew.cassin
 *
 */
public interface ProgramSettingsListener {
	/**
	 *	Invoked for constant arguments (same for each emboss program invocation)
	 *  @param args fully formed command line argument(s) (must not be <code>null</code>)
	 */
	public void addArgument(final ProgramSetting ps, String[] args);
	public void addArgument(ProgramSetting ps, String[] args, boolean needs_unmarshalling);
	
	/**
	 *  Invoked for an output file argument, the framework calls this method instead of <code>addArgument()</code>
	 *  @param ops the setting which contains the details of the required output file, which is produced by an EMBOSS program
	 *  @param opt the argument name (fully formed command line argument)
	 */
	public void addOutputFileArgument(final OutputFileSetting ops, String opt);
	
	/**
	 *  Invoked for an input file argument, the framework calls this method instead of <code>addArgument()</code>
	 *  @param opt the argument name (fully formed command line argument)
	 *  @param file the input file (which is not yet created)
	 */
	public void addInputFileArgument(final ProgramSetting ps, String opt, File in_file);

	
}
