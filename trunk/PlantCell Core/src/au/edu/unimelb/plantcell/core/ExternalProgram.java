package au.edu.unimelb.plantcell.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.knime.core.node.NodeLogger;

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
				NodeLogger.getLogger("Find Program").debug(tmp.getAbsolutePath());
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

	public static boolean isLikelyWindows() {
		return (File.pathSeparatorChar == ';');
	}
	
	/**
	 * When you need to find a program or folder use this method to increase the number of locations
	 * searched. Only folders which exist will be returned. Tries to several standard locations cross-platform.
	 * The current directory is also appended to the end of the list if a folder of the suffix (or its lowercase equivalent)
	 * can be found there.
	 * 
	 * @param suffix the last path of the folder which is checked for existence (with File.exists())
	 * @return
	 */
	public static Collection<? extends File> addPlausibleFolders(String suffix) {
		ArrayList<File> ret = new ArrayList<File>();
		String[] try_paths;
		if (suffix == null)
			suffix = "";
		if (isLikelyWindows()) {		// try to guess windows
			try_paths = new String[] {"c:\\Program Files", "c:\\Program Files (x86)", suffix.toLowerCase() };
		} else {	// assume linux/unix/macosx
			
			try_paths = new String[] {"/opt", "/usr/local", "/usr", "/usr/share", ".", "./"+suffix.toLowerCase()};
		}
		
		for (String path : try_paths) {
			makeFolderWithSuffix(path, suffix, ret);
		}
		return ret;
	}

	private static void makeFolderWithSuffix(final String s, final String last_path_name, final List<File> ret) {
		assert(s != null);
		File root = new File(s);
		if (!root.exists())
			return;
		if (last_path_name != null && last_path_name.length() > 0) {
			ret.add(new File(s + File.separator + last_path_name));
		} else {
			ret.add(new File(s));
		}
	}

	public static Collection<? extends File> addSystemPathExecutablePaths() {
		String[] try_paths;
		ArrayList<File> ret = new ArrayList<File>();
		
		if (isLikelyWindows()) {
			try_paths = new String[] { "c:\\cygwin\\bin" };
		} else {
			try_paths = new String[] { "/opt/bin", "/bin", "/usr/bin", "/usr/local/bin", System.getenv("HOME")+"/bin" };
		}
		for (String s : try_paths) {
			makeFolderWithSuffix(s, null, ret);
		}
		return ret;
	}
}
