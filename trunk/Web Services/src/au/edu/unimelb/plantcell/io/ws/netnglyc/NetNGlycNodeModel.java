package au.edu.unimelb.plantcell.io.ws.netnglyc;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.JoinedRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.io.ws.tmhmm.AbstractWebServiceNodeModel;
import au.edu.unimelb.plantcell.servers.nectar.netnglyc.NGlycService;
import au.edu.unimelb.plantcell.servers.nectar.netnglyc.NGlycServicePortType;

/**
 * 
 * @author acassin
 *
 */
public class NetNGlycNodeModel extends AbstractWebServiceNodeModel {
	private final static NodeLogger logger = NodeLogger.getLogger("NetNGlyc");
	
	public static final String CFGKEY_SEQUENCE    = "sequence-column";
	private final SettingsModelString m_seq_col   = new SettingsModelString(CFGKEY_SEQUENCE, "Sequence");
	private int m_seq_idx;
	
	protected NetNGlycNodeModel() {
		this(1,1);
	}
	
	protected NetNGlycNodeModel(int inPorts, int outPorts) {
		super(inPorts, outPorts);
	}

	public NGlycServicePortType getClientProxy() {
 		 // NB: need to use the local WSDL copy rather than go online for it... so...
 		 try {
 			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws");
 			 URL u = FileLocator.find(bundle, new Path("/wsdl/netnglyc@nectar.wsdl"), null);
 			 
 			 // must not call default constructor for local WSDL... so...
 			 NGlycService srv = new NGlycService(u, new QName("http://nectar.plantcell.unimelb.edu.au", "NGlycService"));
 			 return srv.getNGlycServiceHttpSoap11Endpoint();
 		 } catch (Exception e) {
 			 e.printStackTrace();
 			 Logger.getAnonymousLogger().warning("Unable to get NGlyc proxy: "+e.getMessage());
 			 return null;
 		 }
 	}
	
	@Override
	public String getStatus(String jobId) {
		return getClientProxy().getStatus(jobId);
	}


	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
		logger.info("Predicting N-Glycosylation sites in "+m_seq_col.getStringValue());
    	DataTableSpec outSpec = make_output_spec(inData[0].getSpec());
    	MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(new DataTableSpec(inData[0].getSpec(), outSpec)), "Row");
    	if (m_seq_idx < 0)
    		throw new InvalidSettingsException("Cannot find column: "+m_seq_col.getStringValue()+": reconfigure?");
    	
    	BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(inData[0].iterator(), m_seq_idx, 
				50, 200 * 1024, new SequenceProcessor() {

			@Override
			public SequenceValue process(SequenceValue sv) {
				if (!sv.getSequenceType().equals(SequenceType.AA)) {
					logger.warn("Skipping sequence: "+sv.getID()+" - must be protein sequence!");
					return null;
				}
				return sv;
			}
			
		});
		
		NGlycServicePortType proxy = getClientProxy();
		
		int done = 0;
		bsi.setSaveRows(true);
		while (bsi.hasNext()) {
			Map<UniqueID,SequenceValue> batch = bsi.nextAsMap();
			StringBuilder sb = new StringBuilder();
			for (UniqueID uid : batch.keySet()) {
				sb.append(">"+uid.toString()+"\n");
				sb.append(batch.get(uid).getStringValue());
				sb.append("\n");
			}
			
			String jobId = null;
			for (int i=0; i<MAX_RETRIES; i++) {
				try {
					jobId = proxy.submit(sb.toString());
					if (jobId != null) {
						logger.info("Got job id: "+jobId+" for batch of "+batch.size()+" sequences.");
						break;
					}
				} catch (Exception e) {
					if (e instanceof CanceledExecutionException) 
						throw e;
					if (i == MAX_RETRIES-1)
						throw e;
					int delay = i * 120 + 120;
					logger.warn("Job submission failed: "+e.getMessage()+", retrying in "+delay+" seconds.");
					Thread.sleep(delay * 1000);
				}
			}
			
			wait_for_completion(logger, exec, jobId);
			String result = proxy.getResult(jobId);
			BufferedReader rdr = new BufferedReader(new StringReader(result));
			String line;
			GlycResults.reset();
			while ((line = rdr.readLine()) != null) {
				/*
				 * interactive test that the node is not missing reported hits. Logs suspicious lines which could be hits
				 * but the regexp has failed to match...
				 */
				if (GlycResults.grokLine(line) == null && line.matches("^\\s*S\\d+\\b")) {
					logger.warn("Possible missing hit: programmer error? Line: "+line);
				}
			}
			Map<String,UniqueID> rev_lookup = new HashMap<String,UniqueID>();
			for (UniqueID uid : batch.keySet()) {
				rev_lookup.put(batch.get(uid).getID(), uid);
			}
			
			for (DataRow row : bsi.lastBatchRows()) {
				DataCell[] cells = missing_cells(outSpec.getNumColumns());
				SequenceValue sv = (SequenceValue) row.getCell(m_seq_idx);
				UniqueID     uid = rev_lookup.get(sv.getID());
				GlycResults   gr = GlycResults.lookup(uid);
				if (gr != null) {
					cells[0] = new IntCell(gr.getNumGlycosylationSites());
					List<Integer> sites = gr.getSites();
					HashSet<Integer> hashed_sites = new HashSet<Integer>();
					for (Integer i : sites) {
						hashed_sites.add(i);
					}
					sb = new StringBuilder();
					char[] prot = sv.getStringValue().toCharArray();
					int     idx = 1;
					for (char c : prot) {
						if (hashed_sites.contains(idx)) {
							sb.append("<font color=\"red\"><b>"+c+"</b></font>");
						} else {
							sb.append(c);
						}
						if (idx % 60 == 0) {
							sb.append("<br/>\n");
						}
						idx++;
					}
					cells[1] = new StringCell("<html>"+sb.toString());
					
					List<Double>          potentials = gr.getPotentials();
					List<DoubleCell> potential_cells = new ArrayList<DoubleCell>();
					for (Double d : potentials) {
						potential_cells.add(new DoubleCell(d));
					}
					cells[2] = CollectionCellFactory.createListCell(potential_cells);
					
					List<StringCell> jury_agreement_cells = new ArrayList<StringCell>();
					for (String s : gr.getJuryAgreement()) {
						jury_agreement_cells.add(new StringCell(s));
					}
					cells[3] = CollectionCellFactory.createListCell(jury_agreement_cells);
				} else {
					cells[0] = new IntCell(0);		// user convenience: n sites = 0 rather than missing value
				}
				
				c1.addRow(new JoinedRow(row, new DefaultRow(row.getKey(), cells)));
			}
			done += batch.size();
			exec.checkCanceled();
			exec.setProgress(((double)done) / inData[0].getRowCount());
		}
		
		return new BufferedDataTable[] { c1.close() };
	}
	
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) {
		m_seq_idx = inSpecs[0].findColumnIndex(m_seq_col.getStringValue());
		if (m_seq_idx  < 0 && hasSequenceColumn(inSpecs[0])) {
			m_seq_idx = useSequenceColumnIndex(inSpecs[0], logger);
		}
		return new DataTableSpec[] { new DataTableSpec(inSpecs[0], make_output_spec(inSpecs[0])) };
	}
	
	private DataTableSpec make_output_spec(DataTableSpec inSpec) {
		DataColumnSpec[] cols = new DataColumnSpec[4];
		cols[0] = new DataColumnSpecCreator("NetNGlyc: number of predicted N-Glycosylation sites", IntCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("NetNGlyc: highlighted sequence (HTML)", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("NetNGlyc: Potential (score for each site)", ListCell.getCollectionType(DoubleCell.TYPE)).createSpec();
		cols[3] = new DataColumnSpecCreator("NetNGlyc: Jury Agreement (for each site)", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
		
		return new DataTableSpec(cols);
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_seq_col.saveSettingsTo(settings);
	}
	
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_seq_col.loadSettingsFrom(settings);
	}
	
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_seq_col.validateSettings(settings);
	}
}
