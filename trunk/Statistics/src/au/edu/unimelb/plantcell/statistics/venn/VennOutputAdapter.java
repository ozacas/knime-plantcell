package au.edu.unimelb.plantcell.statistics.venn;

import java.util.List;
import java.util.Map;

/**
 * If you want output from the {@link VennModel} implement this interface and call <code>VennModel.outputToAdapter()</code>
 * 
 * {@see VennDataContainerAdapter}
 * 
 * @author acassin
 *
 */
public interface VennOutputAdapter {
	/**
	 * Called to record a single category and its corresponding count. 
	 * @param category subclass is responsible for adding category_name to the <code>getDoneCategories()</code>. Results will be unpredictable if you dont.
	 * @param count
	 * 
	 * Consider a two way venn diagram: there are three categories: A, B and AB. This method will be called three times (assuming
	 * there are data points for each category!) with category set to [A], [B] and [A, B] in turn. Order is not determinate.
	 */
	public void saveCategory(final List<String> category, int count);
	
	/**
	 * a map from CategoryX => PrintableCategoryNameForX which is maintained by the adapter. This must
	 * obey the contract of keeping track of categories saved so far, or results from the model will be unpredictable.
	 * 
	 * @return must not return null
	 */
	public Map<List<String>,String> getDoneCategories();
	
	/**
	 * Called to initialise adapater before run (may clear previous run state if desired)
	 */
	public void reset();
}