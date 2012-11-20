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

import au.edu.unimelb.plantcell.io.ws.multialign.AlignmentValue.AlignmentType;

import uk.ac.ebi.jdispatcher.soap.clustalo.InputParameters;
import uk.ac.ebi.jdispatcher.soap.clustalo.JDispatcherService;
import uk.ac.ebi.jdispatcher.soap.clustalo.ObjectFactory;
import uk.ac.ebi.jdispatcher.soap.clustalo.JDispatcherService_Service;
import uk.ac.ebi.jdispatcher.soap.clustalo.WsRawOutputParameters;


public class ClustalOProxy extends AbstractProxy {
	private uk.ac.ebi.jdispatcher.soap.clustalo.ObjectFactory m_of = new ObjectFactory();
	private JDispatcherService m_proxy;
	private InputParameters m_params;
	
	public ClustalOProxy(NodeLogger l) {
		super(l);
	}

	@Override
	public void prepare(String fasta) {
		m_params = m_of.createInputParameters();
		JAXBElement<String> seq_elem = m_of.createInputParametersSequence(fasta);
		// TODO: hardcoded defaults only are supported for now...
		JAXBElement<Boolean> dealign_seq_elem = m_of.createInputParametersDealign(new Boolean(false));
		JAXBElement<String> outfmt_elem = m_of.createInputParametersOutfmt("fa");
		JAXBElement<Boolean> mbed_out_elem = m_of.createInputParametersGuidetreeout(new Boolean(true));
		JAXBElement<Boolean> mbed_iteration_elem = m_of.createInputParametersMbediteration(new Boolean(true));
		JAXBElement<Integer> num_iterations_elem = m_of.createInputParametersIterations(new Integer(0));
		JAXBElement<Integer> max_guide_tree_elem = m_of.createInputParametersGtiterations(new Integer(0));
		JAXBElement<Integer> max_hmm_iterations_elem = m_of.createInputParametersHmmiterations(new Integer(0));
		
		m_params.setSequence(seq_elem);
		m_params.setDealign(dealign_seq_elem);
		m_params.setOutfmt(outfmt_elem);
		m_params.setMbed(mbed_out_elem);
		m_params.setMbediteration(mbed_iteration_elem);
		m_params.setIterations(num_iterations_elem);
		m_params.setGtiterations(max_guide_tree_elem);
		m_params.setHmmiterations(max_hmm_iterations_elem);
		
		 // instantiate ClustalO client
        m_proxy = getClientProxy();
	}

	public JDispatcherService getClientProxy() {
		 // NB: need to use the local WSDL copy rather than go online for it... so...
		 try {
			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws.multialign");
			 URL u = FileLocator.find(bundle, new Path("META-INF/wsdl/clustalo.wsdl"), null);
			 
			 // must not call default constructor for local WSDL... so...
			 JDispatcherService_Service cli = new JDispatcherService_Service(u,
					 new QName("http://soap.jdispatcher.ebi.ac.uk", "JDispatcherService"));
			 return cli.getJDispatcherServiceHttpPort();
		 } catch (Exception e) {
			 e.printStackTrace();
			 Logger.getAnonymousLogger().warning("Unable to get Clustal Omega proxy: "+e.getMessage());
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
	public DataCell[] get_results(ExecutionContext exec, String jobid)
			throws Exception {
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
	
		// cells[1] is the alignment cell (taken from the ClustalO alignment from EBI)
		byte[] aln_bytes = m_proxy.getResult(jobid, "aln-fasta", params);
		if (aln_bytes != null) {
			String fasta_alignment = new String(aln_bytes);
			cells[1] = AlignmentCellFactory.createCell(fasta_alignment, AlignmentType.AL_AA);
		} 
		return cells;
	}

}
