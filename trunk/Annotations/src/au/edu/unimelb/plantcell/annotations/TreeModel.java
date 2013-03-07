package au.edu.unimelb.plantcell.annotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;


/**
 * Implements a tree-element depth-balanced tree: root - task category - task name. This information
 * comes from the available <code>BioJavaProcessorTask</code> classes and their methods. This is used
 * in the dialog to display a categorised tree view of attributes to compute about biological sequences.
 * 
 * @author andrew.cassin
 *
 */
public class TreeModel extends DefaultTreeModel {
	/**
	 * not used
	 */
	private static final long serialVersionUID = 7908682211086428679L;
	
	/**
	 * root node
	 */
	private static final String m_root = "/";
	
	/**
	 * tree structure which is built up via <code>add()</code>
	 */
	private HashMap<String,List<String>> m_items = new HashMap<String,List<String>>();
	
	public TreeModel() {
		super(new DefaultMutableTreeNode(m_root));
	}
	
	public void clear() {
		m_items.clear();
	}
	
	public void add(String category, String item) {
		if (!m_items.containsKey(category)) {
			m_items.put(category, new ArrayList<String>());
		}
		List<String> l = m_items.get(category);
		assert(l != null);
		l.add(item);
		// disabled for performance
		//Collections.sort(l);
	}

	/**
	 * Return a list of <b>leaf node only</b> tree items from the chosen list (an empty list if none)
	 * @param sel
	 * @return
	 */
	public List<TreePath> paths2leaves(TreePath[] sel) {
		List<TreePath> ret = new ArrayList<TreePath>();
		if (sel == null || sel.length < 1)
			return ret;
		for (TreePath tp : sel) {
			if (tp.getPathCount() == 3) {		// leaf node?
				ret.add(tp);
			}
		}
		return ret;
	}

	/**
	 * Return the name of the task represented by <code>tp</code>
	 * @param tp
	 * @return <code>null</code> is returned if <code>tp</code> is not a leaf node in the tree
	 */
	public String findName(TreePath tp) {
		if (tp == null)
			return null;
		if (tp.getPathCount() < 3)
			return null;
		return tp.getLastPathComponent().toString();
	}

	public TreePath getPath(String name) {
		for (String category : m_items.keySet()) {
			List<String> l = m_items.get(category);
			for (String s : l) {
				if (name.equals(s)) {
					return new TreePath(new Object[] { m_root, category, s} );
				}
			}
		}
		
		return null;
	}
	
	/*************************** TreeModel methods *****************************/
	
	public Object getChild(Object parent, int idx) {
		if (parent == getRoot()) {
			Set<String> keys = m_items.keySet();
			int i = 0;
			for (String key : keys) {
				if (i++ == idx) {
					return key;
				}
			}
		} else {
			// parent must be category node
			for (String s : m_items.keySet()) {
				if (s.equals(parent)) {
					List<String> tasks = m_items.get(s);
					return tasks.get(idx);
				}
			}
		}
		
		return null;		// should not get here
	}
	
	public int getChildCount(Object o) {
		if (getRoot().equals(o)) {
			return m_items.size();
		} else {
			// check for category node
			List<String> l = m_items.get(o);
			if (l != null)
				return l.size();
			
			// else must be leaf node...
			return 0;
		}
	}
	
	public int getIndexOfChild(Object parent, Object child) {
		if (parent.equals(getRoot())) {
			Set<String> keys = m_items.keySet();
			int i = 0;
			for (String k : keys) {
				if (child.equals(k)) {
					return i;
				} else {
					i++;
				}
			}
		} else if (m_items.containsKey(parent)) {
			List<String> names = m_items.get(parent);
			assert(names != null);
			return names.indexOf(child);
		}
		// else
		return 0;
	}
	
	public boolean isLeaf(Object o) {
		if (getRoot().equals(o)) {
			return false;
		} else if (m_items.containsKey(o)) {
			return false;
		} else {
			return true;	// check to see if o is in the tree?
		}
	}
	
	public Object getRoot() {
		return m_root;
	}

	public void invalidate() {
		// signal listeners that the entire tree as needs to be updated
		fireTreeStructureChanged(this, new Object[] { getRoot() }, null, null);
	}
	
	
}
