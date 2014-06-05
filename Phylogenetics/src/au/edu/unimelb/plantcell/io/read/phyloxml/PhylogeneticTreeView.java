package au.edu.unimelb.plantcell.io.read.phyloxml;

import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.NodeModel;
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.ExecutorUtils;

/**
 * Responsible for invoking Archaeopteryx to display the tree file as specified by the model. We use the archaeopteryx plugin
 * present in the distribution rather than a system-installed one (which might be very hard to find)
 * 
 * @author acassin@unimelb.edu.au
 *
 */
public class PhylogeneticTreeView extends AbstractNodeView<NodeModel> {
	private File m_tree_to_display;
	
	public PhylogeneticTreeView(final NodeModel nodeModel, final File tree_to_display) {
		super(nodeModel);
		m_tree_to_display = tree_to_display;
	}

	@Override
	protected void modelChanged() {
		// TODO FIXME no-op for now...
	}

	@Override
	protected void callOpenView(String title) {
		// locate forester.jar within the plugin and save it out to a temporary file in order to invoke it...
		Bundle plugin = Platform.getBundle("au.edu.unimelb.plantcell.phylogenetics");
        IPath       p = new Path("lib/forester.jar");
        IPath       p2= new Path("lib/archaeopteryx_conf.txt");

        File forester_jar = null;
        File config_txt = null;
        try {
        	forester_jar = File.createTempFile("forester", "_exe.jar");
        	config_txt   = File.createTempFile("archaeopteryx", "_config.txt");
        	
			ExecutorUtils.copyFile(FileLocator.openStream(plugin, p, false),  forester_jar);
			ExecutorUtils.copyFile(FileLocator.openStream(plugin, p2, false), config_txt);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

        // mark the forester.jar as delete on exit... TODO FIXME this isnt enough since the program may still be running when KNIME exits...
        if (forester_jar != null && forester_jar.exists()) {
        	forester_jar.deleteOnExit();
        }
      
        
		// run java -jar forester.jar <tree file> to display the requested tree...
        CommandLine cl = new CommandLine("java");
        cl.addArgument("-jar");
        cl.addArgument(forester_jar.getAbsolutePath());
        
        // order is important for these arguments!
        if (config_txt != null && config_txt.exists()) {
        	config_txt.deleteOnExit();
        	 cl.addArgument("-c");
             cl.addArgument(config_txt.getAbsolutePath());
        }
       
        cl.addArgument(m_tree_to_display.getAbsolutePath());
        new ExecutorUtils().runNoWait(cl);
	}

	@Override
	protected void callCloseView() {
		// no-op
	}

}
