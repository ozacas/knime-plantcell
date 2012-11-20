package au.edu.unimelb.plantcell.io.ws.multialign;

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

import uk.ac.ebi.jdispatcher.soap.muscle.InputParameters;
import uk.ac.ebi.jdispatcher.soap.muscle.JDispatcherService;
import uk.ac.ebi.jdispatcher.soap.muscle.JDispatcherService_Service;
import uk.ac.ebi.jdispatcher.soap.muscle.ObjectFactory;
import uk.ac.ebi.jdispatcher.soap.muscle.WsRawOutputParameters;
import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue.AlignmentType;

public class MUSCLEProxy extends AbstractProxy {
	private JDispatcherService m_proxy;
	private InputParameters    m_params;
	private final uk.ac.ebi.jdispatcher.soap.muscle.ObjectFactory m_of = new ObjectFactory();
	
	public MUSCLEProxy(NodeLogger l) {
		super(l);
	}

	
	@Override
	public void prepare(String fasta_sequences_unaligned) {
          m_params = m_of.createInputParameters();
          JAXBElement<String> seq_elem = m_of.createInputParametersSequence(fasta_sequences_unaligned);
          // one of: fasta, clw, clwstrict, html, msf, phyi, phys
          JAXBElement<String> format_elem = m_of.createInputParametersFormat("fasta");
          JAXBElement<String> order_elem = m_of.createInputParametersOrder("aligned");
          // one of: none, tree1 or tree2
          JAXBElement<String> tree_elem = m_of.createInputParametersTree("none");
          m_params.setSequence(seq_elem);
          m_params.setFormat(format_elem);
          m_params.setOrder(order_elem);
          m_params.setTree(tree_elem);
          
          // instantiate MUSCLE client
          m_proxy = getClientProxy();
	}

	public JDispatcherService getClientProxy() {
		 // NB: need to use the local WSDL copy rather than go online for it... so...
		 try {
			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws.multialign");
			 URL u = FileLocator.find(bundle, new Path("META-INF/wsdl/muscle.wsdl"), null);
			 
			 // must not call default constructor for local WSDL... so...
			 JDispatcherService_Service cli = new JDispatcherService_Service(u,
					 new QName("http://soap.jdispatcher.ebi.ac.uk", "JDispatcherService"));
			 return cli.getJDispatcherServiceHttpPort();
		 } catch (Exception e) {
			 e.printStackTrace();
			 Logger.getAnonymousLogger().warning("Unable to get MUSCLE proxy: "+e.getMessage());
			 return null;
		 }
	}
	
	@Override
	public String run(final Properties props) {
		prepare(props.getProperty("sequences"));
		return m_proxy.run(props.getProperty("email"), 
					props.getProperty("id"),
					m_params
				);
	}

	
	@Override
	public DataCell[] get_results(ExecutionContext exec, String jobid) throws Exception {
		DataCell cells[] = new DataCell[3];
		for (int i=0; i<cells.length; i++) {
			cells[i] = DataType.getMissingCell();
		}
		cells[0] = new StringCell(jobid);
		// cells[2] is the stdout/stderr from the MUSCLE program
		WsRawOutputParameters params = m_of.createWsRawOutputParameters();
		byte[] out_bytes = m_proxy.getResult(jobid, "out", params);
		if (out_bytes != null) {
			cells[2] = new StringCell(new String(out_bytes));
		} 
	
		// cells[1] is the alignment cell (taken from the ClustalW alignment from MUSCLE@EBI)
		byte[] aln_bytes = m_proxy.getResult(jobid, "aln-fasta", params);
		if (aln_bytes != null) {
			String fasta_alignment = new String(aln_bytes);
			cells[1] = AlignmentCellFactory.createCell(fasta_alignment, AlignmentType.AL_AA);
		} 
		return cells;
	}


	@Override
	public boolean wait_for_completion(ExecutionContext exec, String jobid) throws Exception {
		return wait_for_completion(exec, new StatusGetter() {

			@Override
			public String getStatus(String jobid) {
				return m_proxy.getStatus(jobid);
			}
			
		}, jobid);
	}


}
