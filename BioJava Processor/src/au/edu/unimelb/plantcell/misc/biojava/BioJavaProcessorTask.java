package au.edu.unimelb.plantcell.misc.biojava;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.ProteinTools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.symbol.IllegalSymbolException;
import org.biojava.bio.symbol.SymbolList;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ColumnFilter;

import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;

/**
 * Abstract baseclass for all executable tasks that the node supports. One method
 * is used during configure, most are used during <code>execute()</code>
 * 
 * @author andrew.cassin
 *
 */
public abstract class BioJavaProcessorTask {
	private final Map<String,TaskParameter>        m_advanced = new HashMap<String,TaskParameter>();
	private BioJavaProcessorNodeModel m_owner;
	
	public BioJavaProcessorTask() {
	}
	
	/**
	 * Singleton pattern - subclasses must override and must not return <code>null</code>.
	 * The instance returned need not be fully initialised, the {@link BioJavaNodeModel}
	 * will call <code>init(this)</code> before the task is executed, once per execution.
	 * @return
	 */
	public static BioJavaProcessorTask getInstance() throws Throwable { 
		throw new InvalidSettingsException("Subclasses must override!"); 
	}
	
	/**
	 * Typically overridden by subclasses, this must return the category
	 * which the task should be displayed in, in the configure dialog. Default
	 * implementation has it appear in the 'all' category
	 */
	public String getCategory() {
		return "All";
	}
	
	/**
	 * Two-stage construction for task objects is required to support the singleton
	 * pattern and interaction with model & dialog code
	 * 
	 * @param owner
	 */
	public abstract void init(BioJavaProcessorNodeModel owner, String task_name);
	
	protected void setOwner(BioJavaProcessorNodeModel new_owner) {
		assert(new_owner != null);
		m_owner = new_owner;
	}
	
	/**
	 * Returns the owner (node model instance) of the task to the caller. Guaranteed not-<code>null</code>
	 * @return
	 */
	protected BioJavaProcessorNodeModel getOwner() {
		return m_owner;
	}
	
	/**
	 * Returns the human-readable names for the task as it should appear in the configure dialog.
	 * Every subclass is expected to override this method. Most tasks will only provide
	 * one name but if you want multiple list entries you need to specify them here.
	 * This method is called using {@link java.lang.reflect}
	 */
	public String[] getNames() { 
		return new String[] {""}; 
	}
	
	/**
	 * Returns a HTML fragment suitable for reporting what the currently selected
	 * task does. Method has not been instantiated, so this must be a static method
	 */
	public String getHTMLDescription(String task) {
		return "<html><b>No description available.</b>";
	}
	
	/**
	 * Returns the table spec needed by the processor to store the results it will produce. Must not return null.
	 * @return DataTableSpec
	 */
	public abstract DataTableSpec get_table_spec();
	
	/**
	 * Which input columns can be selected for this task?
	 */
	public ColumnFilter getColumnFilter() {
		return  new ColumnFilter() {

			@Override
			public boolean includeColumn(DataColumnSpec colSpec) {
				return (colSpec.getType().isCompatible(SequenceValue.class));
			}

			@Override
			public String allFilteredMsg() {
				return "No suitable Sequence columns available!";
			}
			
		};
	}
	/**
	 * Is the result table merged with the input columns?
	 */
	public abstract boolean isMerged();
	
	/**
	 * Retrieve the set of configurable parameters for this task, used by the dialog code to let the user edit the advanced settings
	 */
	public Collection<TaskParameter> getParameters() {
		return m_advanced.values();
	}
	
	public TaskParameter getParameter(String parameter_name) throws InvalidSettingsException {
		if (!m_advanced.containsKey(parameter_name)) {
			throw new InvalidSettingsException("No such parameter: "+parameter_name+" - probably programmer error.");
		}
		return m_advanced.get(parameter_name);
	}
	
	public TaskParameter getParameter(String parameter_name, String default_value) {
		TaskParameter ret = m_advanced.get(parameter_name);
		if (ret == null)
			return new TaskParameter(parameter_name, default_value);
		else
			return ret;
	}
	
	/**
	 * Called by the dialog code when advanced settings must be validated
	 * @return true if the parameters are ok, false otherwise
	 */
	public boolean validateParameters() throws InvalidSettingsException {
		for (TaskParameter tp : m_advanced.values()) {
			if (!tp.isValid())
				return false;
		}
		return true;
	}
	
	/**
	 * Saves the advanced task parameters to the specified settings instance
	 */
	public void saveSettingsTo(NodeSettingsWO settings) throws Exception {
		ArrayList<String> adv = new ArrayList<String>();
		for (TaskParameter tp : m_advanced.values()) {
			StringBuffer sb = new StringBuffer();
			sb.append(tp.getName());
			sb.append('=');
			sb.append(tp.getValue());
			adv.add(sb.toString());
		}
		
		settings.addStringArray("ADVANCED-PARAMS", adv.toArray(new String[0]));
	}
	
	public void loadSettingsFrom(NodeSettingsRO settings) throws Exception {
		String[] adv = settings.getStringArray("ADVANCED-PARAMS");
		m_advanced.clear();
		for (String s : adv) {
			String field = s.substring(0, s.indexOf('='));
			String value = s.substring(s.indexOf('=')+1);
			m_advanced.put(field, new TaskParameter(field, value));
		}
	}
	
	/**
	 * Processes the required task storing results into c, using the parameters as specified by m
	 * @param c the container to store results into
	 * @param seqs the iterator over the input sequences
	 * @param inData[] only the first element ie. 0 will be available with the necessary data to perform the calculation
	 * @throws Exception
	 */
	public abstract void execute(ColumnIterator seqs, final ExecutionContext exec, NodeLogger l, final BufferedDataTable[] inData, BufferedDataContainer c) throws Exception;

	public boolean hasCategory(String cat) {
		return getCategory().equalsIgnoreCase(cat);
	}

	public boolean hasName(String taskName) {
		for (String s : getNames()) {
			if (s.equals(taskName)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Converts <code>sv</code> to the specified type of sequence using BioJava. The only
	 * valid translations are: DNA -> Protein, DNA -> RNA, RNA -> Protein. Other types of
	 * conversions will result in an exception.
	 * 
	 * @param sv
	 * @return the converted sequence
	 * @throws InvalidSettingsException if the conversion is ill-defined.
	 */
	public SymbolList asBioJava(SequenceValue sv) throws InvalidSettingsException,IllegalSymbolException {
		SequenceType st = sv.getSequenceType();
		if (st.equals(SequenceType.DNA)) {
			return DNATools.createDNA(sv.getStringValue());
		} else if (st.equals(SequenceType.RNA)) {
			return RNATools.createRNA(sv.getStringValue());
		} else if (st.equals(SequenceType.AA)) {
			return ProteinTools.createProtein(sv.getStringValue());
		} else {
			throw new InvalidSettingsException("Unsupported sequence conversion: "+st);
		}
	}
}
