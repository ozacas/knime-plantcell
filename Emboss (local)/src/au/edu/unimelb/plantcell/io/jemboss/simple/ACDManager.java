package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.knime.core.node.NodeLogger;

public class ACDManager {
	private NodeLogger logger;
	
	public ACDManager(final NodeLogger logger) {
		this.logger = logger;
	}
	
	public File[] getACDFiles() throws IOException {
		String emboss_dir = ACDApplication.getEmbossDir();
    	logger.info("EMBOSS root preference setting: "+emboss_dir);
    	
    	for (String path : ACDApplication.DEFAULT_EMBOSS_ACD_PATHS) {
    		File root = new File(emboss_dir, path);
    		if (!root.exists()) {
    			logger.info("Failed to find ACD files in: "+root.getAbsolutePath());
    			continue;
    		}
    		File[] acd = root.listFiles(new FileFilter() {

				@Override
				public boolean accept(File arg0) {
					return arg0.getName().toLowerCase().endsWith(".acd");
				}
    			
    		});
    		if (acd != null && acd.length > 0) {
    			logger.info("Found "+acd.length+" emboss ACD files. Using "+root.getAbsolutePath());
    			return acd;
    		}
    	}
    	throw new IOException("Unable to locate any ACD files. Is EMBOSS installed and PlantCell preference set?");
	}
}
