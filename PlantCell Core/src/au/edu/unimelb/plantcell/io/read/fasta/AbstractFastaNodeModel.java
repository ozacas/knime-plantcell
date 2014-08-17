package au.edu.unimelb.plantcell.io.read.fasta;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.URIUtil;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.cells.Comment;
import au.edu.unimelb.plantcell.core.cells.CommentType;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;

/**
 * Common methods for reading of sequence files
 * @author acassin
 *
 */
public abstract class AbstractFastaNodeModel extends NodeModel {
	  /** the settings key which is used to retrieve and 
          store the settings (from the dialog or from a settings file)    
          (package visibility to be usable from the dialog). */
	 static final String CFGKEY_FASTA    = "fasta-file";
	 static final String CFGKEY_ACCSN_RE = "accsn-regexp";
	 static final String CFGKEY_DESCR_RE = "description-regexp";
	 static final String CFGKEY_ENTRY_HANDLER = "entry-handler";
	 static final String CFGKEY_MAKESTATS= "make-statistics?";
	 static final String CFGKEY_SEQTYPE = "sequence-type";
	 static final String CFGKEY_FILE_COLUMN = "file-column";
	 static final String CFGKEY_ACCSN_COLUMN = "accession-column";
	 static final String CFGKEY_USE_ACCSN_AS_ROWID = "accsn-as-rowid?";
	    
	 protected static final String DEFAULT_ACCSN_RE = "^(\\S+)\\b";
	 protected static final String DEFAULT_DESCR_RE = "^\\S+\\s*(.*)$";
	 protected static final String DEFAULT_ENTRY_HANDLER = "single";
	 protected static final Boolean DEFAULT_MAKESTATS = Boolean.FALSE;	// dont waste memory and performance by default

	 private boolean m_single_entry_only;
	 private boolean m_use_accession_as_rid;
	 private URL     m_url;
	 
	/**
	 * Must use this constructor which specifies how many input and output ports the node has
	 * 
	 * @param nrInDataPorts
	 * @param nrOutDataPorts
	 */
	protected AbstractFastaNodeModel(int nrInDataPorts, int nrOutDataPorts) {
		super(nrInDataPorts, nrOutDataPorts);
		setSingleEntry(true);
		setCurrentURL(null);
		setUseAccessionAsRowID(true);
	}

	/**
	 * Only required for those nodes which read data from files/URLs. Must throw if no suitable files are available.
	 * 
	 * @return
	 * @throws InvalidSettingsException
	 */
	public abstract List<URL> getURLList() throws InvalidSettingsException;
		
	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// NO-OP
	}

	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// NO-OP
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		// NO-OP
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		// NO-OP
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		// NO-OP
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
	protected void reset() {
		// NO-OP
	}

	protected boolean isSingleEntry() {
		return m_single_entry_only;
	}
	
	protected void setSingleEntry(boolean yes) {
		m_single_entry_only = yes;
	}
	
	protected void setCurrentURL(final URL current_url) {
		m_url = current_url;
	}
	
	protected URL getCurrentURL() {
		return m_url;
	}
	
	protected DataCell getDataSourceCell() {
		URL u = getCurrentURL();
		if (u == null)
			return DataType.getMissingCell();
		return new StringCell(u.toString());
	}
	
	protected boolean useAccessionAsRowID() {
		return m_use_accession_as_rid;
	}
	
	protected void setUseAccessionAsRowID(boolean yes) {
		m_use_accession_as_rid = yes;
	}
	
    protected boolean save_sequence(MyDataContainer c1, SequenceType st, String[] accsn, String[] descr,
    		StringBuffer seq, SequenceStatistics stats) throws InvalidSettingsException
    {
    	if (accsn != null && descr != null && seq != null && accsn.length > 0) {
            // the cells of the current row, the types of the cells must match
            // the column spec (see above)
            SequenceCell sc  = new SequenceCell(st, accsn[0], seq.toString());
        	for (int i=0; i<descr.length; i++) {
        		 if (i == 0) {
        			 sc.addComment(new Comment(CommentType.Description, descr[i]));
        			 if (isSingleEntry())
        				 break;
        		 } else {
        			 // HACK: additional annotations are prefixed with the ID (eg. from NR)
        			 // what is right way?
        			 sc.addComment(new Comment(CommentType.Description, accsn[i] + " " + descr[i]));
        		 }
        	}
            
        	DataCell[] cells = new DataCell[] { sc, this.getDataSourceCell() };
        	if (useAccessionAsRowID()) {
        		c1.addRowWithID(accsn[0], cells);
        	} else {
        		c1.addRow(cells);
        	}
            if (stats != null) {
            	stats.grokSequence(sc);
            }
            return true;
  	    } else {
  	    	// NB: do not update stats object if bogus parameters...
  	    	return false;
  	    }
    }
    
    
    protected Collection<StringCell> toDataCells(String[] vec) {
    	ArrayList<StringCell> al = new ArrayList<StringCell>();
    	for (String s : vec) {
    		if (s == null)		// terminate add early if only a few entries valid
    			break;
    		al.add(new StringCell(s));
    	}
    	return al;
    }
    
    protected String[] parse_accession(Pattern matcher, String[] entries) throws Exception {
    	int cnt = 0;
    	String[] accsns = new String[entries.length];
    	for (String entry : entries) {
    		Matcher m = matcher.matcher(entry);
	    	if (m.find()) {
	    		if (m.groupCount() != 1) {
	    			throw new Exception("You must use capturing parentheses () to match an accession only once!");
	    		}
	    		accsns[cnt] = m.group(1);
	    		cnt++;
	    	} 
    	}
    	if (cnt < entries.length) {
    		accsns[cnt] = null; // make sure array has null after last match
    	}
     	return (cnt > 0) ? accsns : null;
    }
    
    protected String[] parse_description(Pattern matcher, String[] entries) throws Exception {
    	int cnt = 0;
    	String[] descrs = new String[entries.length];
    	for (String entry : entries) {
    		Matcher m = matcher.matcher(entry);
    		if (m.find()) {
    			if (m.groupCount() != 1) {
        			throw new Exception("You must use capturing parentheses() to match a sequence description only once!");
        		}
    			descrs[cnt] = m.group(1);
    			cnt++;
    		}
    	}
    	if (cnt < entries.length) {
    		descrs[cnt] = null;
    	}
    	return (cnt > 0) ? descrs : null;
    }
  
    
 	/**
 	 * If the specified url represents a file, without a host part, we can shorten the URL for
 	 * user convenience. The url is not modified, but the short string version is returned for display purposes.
 	 * 
 	 * @param u must not be null
 	 * @return never null
 	 */
 	public static String shortenURLForDisplay(final URL u) {
 		assert(u != null);
 		String host = u.getHost();
 		if (host != null && host.length() > 0)
 			return u.toString();
 		String proto = u.getProtocol();
 		if (proto.startsWith("file")) {
 			try {
 				// recommended by http://wiki.eclipse.org/Eclipse/UNC_Paths
				return URIUtil.toFile(URIUtil.toURI(u)).getAbsolutePath();
			} catch (URISyntaxException e) {
				e.printStackTrace();
				// fallback to this, but somewhat buggy...
				return u.getPath();
			}
 		}
 		else
 			return u.toString();
 	}
 	
}
