package au.edu.unimelb.plantcell.core;

import java.io.File;
import java.io.IOException;

public class TempDirectory {
	private File m_f;
	
	public TempDirectory() throws IOException {
		this(File.createTempFile("temp", "_directory.dir"));
	}
	
	public TempDirectory(File f) throws IOException {
		assert(f != null);
		f.delete();		// HACK: race condition
		if (!f.mkdir())
			throw new IOException("Cannot create temporary directory");
		m_f = f;
	}
	
	public File asFile() {
		return m_f;
	}
	
	public final void deleteRecursive() throws IOException {
		// safety first: throw if m_f does not end in .dir
		if (!m_f.getName().toLowerCase().endsWith(".dir"))
			throw new IOException("SAFETY FAILURE: .dir extension not present on temp folder!");
		deleteRecursive(m_f);
	}
	
	public void deleteRecursive(File file) throws IOException {
		if (file.isDirectory()) {	 
    		if(file.list().length==0){
    		   file.delete();
    		} else {
         	   String files[] = file.list();
        	   for (String s : files) {
        	   		File kid = new File(file, s);
        	   		deleteRecursive(kid);
        	   }
        	   if(file.list().length==0){
           	     file.delete();
        	   }
    		}
    	} else {
    		file.delete();
    	}
    }

}
