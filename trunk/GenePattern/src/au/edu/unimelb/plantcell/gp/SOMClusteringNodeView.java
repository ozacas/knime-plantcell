package au.edu.unimelb.plantcell.gp;

import java.io.File;
import java.util.logging.Logger;

import org.genepattern.client.GPClient;
import org.genepattern.webservice.Parameter;
import org.knime.core.node.ExternalApplicationNodeView;

public class SOMClusteringNodeView
		extends ExternalApplicationNodeView<au.edu.unimelb.plantcell.gp.SOMClusteringNodeModel> {

	protected SOMClusteringNodeView(SOMClusteringNodeModel model) {
		super(model);
	}

	@Override
	protected void onOpen(String title) {
		SOMClusteringNodeModel mdl = getNodeModel();
		Logger l = Logger.getAnonymousLogger();
		if (mdl.hasOutputFiles()) {
			try {
				File[] items = mdl.downloadFiles();
				assert(items.length > 0);
			
				GPClient gpClient = mdl.make_gp_client();
				
				l.info("Starting SOM Viewer... please wait this may take some time");
				gpClient.runVisualizer("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.visualizer:00023:5", 
							new Parameter[]{new Parameter("som.cluster.filename", items[0])});
			} catch (Exception e) {
				l.warning("Unable to run SOM viewer - network problem?\n"+e.getMessage());
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onClose() {
	}

	@Override
	protected void modelChanged() {
		// NO-OP since a *copy* of the dataset is created, we do nothing in response to user-action within KNIME
	}

}
