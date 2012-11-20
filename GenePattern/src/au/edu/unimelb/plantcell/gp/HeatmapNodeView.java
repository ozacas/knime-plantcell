package au.edu.unimelb.plantcell.gp;

import java.io.File;
import java.io.FileWriter;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.genepattern.client.GPClient;
import org.genepattern.webservice.Parameter;
import org.knime.core.node.ExternalApplicationNodeView;

/**
 * <code>NodeView</code> for the "GenePattern" Node.
 * Nodes to support remote invocation of a GenePattern instance for key analyses (heatmap, clustering etc.)
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class HeatmapNodeView<T extends HeatmapNodeModel> extends ExternalApplicationNodeView<T> {
	
    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link HeatmapNodeModel})
     */
    protected HeatmapNodeView(final T nodeModel) {
        super(nodeModel);
    }

	@Override
	protected void onOpen(String title) {
		final T mdl = super.getNodeModel();
		if (mdl.hasDataset()) {
			if (! ((T) mdl).accessGPServer()) {
				Logger.getAnonymousLogger().warning("Unable to access GenePattern server: network problem?");
				return;
			}
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					Logger l = Logger.getAnonymousLogger();
					try {
						if (!mdl.hasDataset()) {
							l.warning("No dataset available, please execute the node again.");
							return;
						}
						final File tmpfile = File.createTempFile("genepattern", ".gct", new File("c:/temp"));
						l.info("Saving heatmap dataset to (GCT format): "+tmpfile.getAbsolutePath());

						FileWriter pw = new FileWriter(tmpfile);
						pw.write(mdl.getDatasetAsGCT());
						pw.close();
						
						GPClient gp = mdl.make_gp_client();
					    l.info("*** Starting Heatmap (may take a minute to download and start)...");

						gp.runVisualizer("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.visualizer:00010:10", 
								new Parameter[]{new Parameter("dataset", tmpfile)});
					    tmpfile.deleteOnExit();
					} catch (Exception e) {
						e.printStackTrace();
						Logger.getAnonymousLogger().warning("Unable to display heatmap: "+e.getMessage());
					} catch (NoClassDefFoundError err) {
						err.printStackTrace();
					}
				}
				
			});
		} else {
			Logger.getAnonymousLogger().warning("No dataset to present heatmap for!");
		}
	}

	@Override
	protected void onClose() {
		
	}

	@Override
	protected void modelChanged() {
		
	}

	


}

