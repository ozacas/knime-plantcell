package au.edu.unimelb.plantcell.io.ws.multialign;

import java.io.StringReader;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.osgi.framework.Bundle;

import pal.alignment.Alignment;
import uk.ac.ebi.jdispatcher.soap.tcoffee.InputParameters;
import uk.ac.ebi.jdispatcher.soap.tcoffee.JDispatcherService;
import uk.ac.ebi.jdispatcher.soap.tcoffee.JDispatcherService_Service;
import uk.ac.ebi.jdispatcher.soap.tcoffee.ObjectFactory;
import uk.ac.ebi.jdispatcher.soap.tcoffee.WsRawOutputParameters;

/**
 * Concrete subclass to handle T-Coffee multiple alignment requests (some advanced settings are hardcoded)
 * @author andrew.cassin
 *
 */
public class TCoffeeProxy extends AbstractProxy {
	private JDispatcherService m_proxy;
	private final uk.ac.ebi.jdispatcher.soap.tcoffee.ObjectFactory m_of = new ObjectFactory();
	private InputParameters m_params;
	
	public TCoffeeProxy(NodeLogger l) {
		super(l);
	}

	@Override
	public void prepare(String fasta) {
		m_params = m_of.createInputParameters();
		JAXBElement<String> seq_elem = m_of.createInputParametersSequence(fasta);
		JAXBElement<String> order    = m_of.createInputParametersOrder("aligned");
		JAXBElement<String> matrix   = m_of.createInputParametersMatrix("pam");
		m_params.setSequence(seq_elem);
		m_params.setMatrix(matrix);
		m_params.setOrder(order);
		
		m_proxy = getClientProxy();
	}
	
	public JDispatcherService getClientProxy() {
		 // NB: need to use the local WSDL copy rather than go online for it... so...
		 try {
			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws.multialign");
			 URL u = FileLocator.find(bundle, new Path("META-INF/wsdl/tcoffee.wsdl"), null);
			 
			 // must not call default constructor for local WSDL... so...
			 JDispatcherService_Service cli = new JDispatcherService_Service(u,
					 new QName("http://soap.jdispatcher.ebi.ac.uk", "JDispatcherService"));
			 return cli.getJDispatcherServiceHttpPort();
		 } catch (Exception e) {
			 e.printStackTrace();
			 Logger.getAnonymousLogger().warning("Unable to get TCoffee proxy: "+e.getMessage());
			 return null;
		 }
	}
	
	@Override
	public String run(Properties props) throws Exception {
		prepare(props.getProperty("sequences"));
		return m_proxy.run(props.getProperty("email"), 
					props.getProperty("id"),
					m_params
				);
	}

	@Override
	public boolean wait_for_completion(ExecutionContext exec, String jobid)
			throws Exception {
		return wait_for_completion(exec, new StatusGetter() {

			@Override
			public String getStatus(String jobid) {
				return m_proxy.getStatus(jobid);
			}
			
		}, jobid);
	}

	@Override
	public DataCell[] get_results(ExecutionContext exec, String jobid) throws Exception {
		DataCell cells[] = new DataCell[3];
		for (int i=0; i<cells.length; i++) {
			cells[i] = DataType.getMissingCell();
		}
		cells[0] = new StringCell(jobid);
		// cells[2] is the stdout/stderr from the T-Coffee program
		WsRawOutputParameters params = m_of.createWsRawOutputParameters();
		//WsResultTypes types = m_proxy.getResultTypes(jobid);
		byte[]    out_bytes = m_proxy.getResult(jobid, "out", params);
		if (out_bytes != null) {
			cells[2] = new StringCell(new String(out_bytes));
		} 
	
		// cells[1] is the alignment cell (taken from the ClustalW alignment from T-Coffee@EBI 
		// as T-Coffee does not offer fasta as an output format as at 16th April 2012)
		byte[] aln_bytes = m_proxy.getResult(jobid, "aln-clustalw", params);
		if (aln_bytes != null) {
			String clustalw_alignment = new String(aln_bytes);
			Alignment a = AlignmentCellFactory.readClustalAlignment(new StringReader(clustalw_alignment));
			cells[1] = AlignmentCellFactory.createCell(a);
		} 
		return cells;
	}

}
