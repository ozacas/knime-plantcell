package au.edu.unimelb.plantcell.io.read.phyloxml;

import java.io.File;

/**
 * NodeModel's which can display their state via Archaeopteryx (ie. tree nodes) must implement this interface in order
 * to use {@see PhylogeneticTreeView}
 * 
 * @author acassin@unimelb.edu.au
 *
 */
public interface FileTreeViewInterface {
	public File getTreeFileForDisplay();
}
