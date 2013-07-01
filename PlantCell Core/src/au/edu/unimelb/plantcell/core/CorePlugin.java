package au.edu.unimelb.plantcell.core;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * the PlantCell core plugin manages preferences and other stuff in here. Blech...
 * 
 * @author andrew.cassin
 *
 */
public class CorePlugin extends AbstractUIPlugin {
	 /** Make sure that this *always* matches the ID in plugin.xml. */
    public static final String PLUGIN_ID = "au.edu.unimelb.plantcell.core";

    private static CorePlugin plugin;
   
    public CorePlugin() {
    	super();
    	plugin = this;
    }
    
    @Override
    public void start(final BundleContext context) throws Exception {
    	super.start(context);
    }
    
    public void addPreferenceListener(IPropertyChangeListener ipcl) {
    	if (ipcl != null)
    		getDefault().getPreferenceStore().addPropertyChangeListener(ipcl);
    }
    
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * Returns the shared instance.
     *
     * @return Singleton instance of the Plugin
     */
    public static CorePlugin getDefault() {
        return plugin;
    }


}
