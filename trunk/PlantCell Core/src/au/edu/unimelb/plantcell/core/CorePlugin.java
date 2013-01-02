package au.edu.unimelb.plantcell.core;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

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
