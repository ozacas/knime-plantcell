/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 *
 */
package au.edu.unimelb.plantcell.io.ws.uniprot;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * This is the eclipse bundle activator.
 * Note: KNIME node developers probably won't have to do anything in here, 
 * as this class is only needed by the eclipse platform/plugin mechanism.
 * If you want to move/rename this file, make sure to change the plugin.xml
 * file in the project root directory accordingly.
 *
 * @author Andrew Cassin
 */
public class UniProtAccessorNodePlugin extends Plugin {

    /** Make sure that this *always* matches the ID in plugin.xml. */
    public static final String PLUGIN_ID = "au.edu.unimelb.plantcell.io.ws.uniprot";

    // The shared instance.
    private static UniProtAccessorNodePlugin plugin;

    /**
     * The constructor.
     */
    public UniProtAccessorNodePlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     * 
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be started
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        try {
            Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );
            super.start(context);
        } catch (Throwable th) {
        	th.printStackTrace();
            if (th instanceof Exception) {
                    throw (Exception) th;
            }
        }
    }

    /**
     * This method is called when the plug-in is stopped.
     * 
     * @param context The OSGI bundle context
     * @throws Exception If this plugin could not be stopped
     */
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
    public static UniProtAccessorNodePlugin getDefault() {
        return plugin;
    }

}

