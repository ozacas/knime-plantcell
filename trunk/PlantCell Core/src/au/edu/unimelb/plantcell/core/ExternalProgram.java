package au.edu.unimelb.plantcell.core;

import java.io.File;

/**
 * Some helper methods for dealing with external programs in a mostly platform-agnostic way
 * 
 * @author andrew.cassin
 *
 */
public class ExternalProgram {
	public ExternalProgram() {
	}
	
	public static File find(final String[] filenames, final File[] folder_names) {
		for (File f : folder_names) {
			for (String p : filenames) {
				File tmp = new File(f, p);
				if (tmp.canExecute() && tmp.isFile()) {
					return tmp;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Tries various platform-specific tricks to find the specified executable <code>progname</code>
	 * in the specified folder <code>s_folder</code>
	 * 
	 * @param s_folder
	 * @param progname
	 * @return null if nothing is found which is executable and accessible
	 */
	public static File find(String s_folder, String progname) {
		String[] prognames = new String[] { progname, progname + ".exe" };
		File folder = new File(s_folder);
		File[] folders = new File[] { folder, new File(folder, "bin") };
		return find(prognames, folders);
	}
}
