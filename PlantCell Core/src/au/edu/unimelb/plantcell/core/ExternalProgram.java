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
	
	public static File find(String s_folder, String progname) {
		String[] prognames = new String[] { progname, progname + ".exe" };
		File folder = new File(s_folder);
		File[] folders = new File[] { folder, new File(folder, "bin") };
		for (File f : folders) {
			for (String p : prognames) {
				File tmp = new File(f, p);
				if (tmp.canExecute() && tmp.isFile()) {
					return tmp;
				}
			}
		}
		
		return null;
	}
}
