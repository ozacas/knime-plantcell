package au.edu.unimelb.plantcell.io.ws.netphos_nectar;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.core.regions.RegionsAnnotation;
import au.edu.unimelb.plantcell.core.regions.ScoredRegion;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.io.ws.tmhmm.AbstractWebServiceNodeModel;
import au.edu.unimelb.plantcell.servers.netphos.NetPhosService;
import au.edu.unimelb.plantcell.servers.netphos.NetPhosServicePortType;



public class NetPhosAccessorNodeModel extends AbstractWebServiceNodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("NetPhos@Nectar");
    
   
    // dialog configuration & model settings
	public static final String CFGKEY_SEQUENCE    = "sequence-column";
	public static final String CFGKEY_URL         = "endpoint-url";
	public static final String CFGKEY_PROGRAM     = "netphos-variant";
	public static final String CFGKEY_CUTOFF      = "netphos-score-cutoff";
	public static final String CFGKEY_BEST_ONLY   = "netphos-best-only";
	public static final String CFGKEY_GENERIC     = "netphos-generic";
	
	private static final int MAX_RETRIES = 5;
    static final String[] METHODS = new String[] { "NetPhos", "NetPhosK (kinase version)" };

    
	private final SettingsModelString m_seq_col   = new SettingsModelString(CFGKEY_SEQUENCE, "Sequence");
	private final SettingsModelString m_url       = new SettingsModelString(CFGKEY_URL, getDefaultEndpoint());
	private final SettingsModelString m_program   = new SettingsModelString(CFGKEY_PROGRAM, METHODS[0]);
	private final SettingsModelDoubleBounded m_cutoff = new SettingsModelDoubleBounded(CFGKEY_CUTOFF, 0.5, 0.0, 1.0);
	private final SettingsModelBoolean m_best         = new SettingsModelBoolean(CFGKEY_BEST_ONLY, false);
	private final SettingsModelBoolean m_generic      = new SettingsModelBoolean(CFGKEY_GENERIC, false);
	private int m_seq_idx = -1;
	
	public NetPhosAccessorNodeModel() {
		super(1, 2);
	}

	public NetPhosServicePortType getClientProxy() {
   		 // NB: need to use the local WSDL copy rather than go online for it... so...
   		 try {
   			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws");
   			 URL u = FileLocator.find(bundle, new Path("/wsdl/netphos@nectar.wsdl"), null);
   			 
   			 // must not call default constructor for local WSDL... so...
   			 NetPhosService srv = new NetPhosService(u,
   					new QName("http://nectar.plantcell.unimelb.edu.au", "NetPhosService"));
   			 return srv.getNetPhosServiceHttpSoap11Endpoint();
   		 } catch (Exception e) {
   			 e.printStackTrace();
   			 Logger.getAnonymousLogger().warning("Unable to get NetPhos proxy: "+e.getMessage());
   			 return null;
   		 }
   	}
	
	@Override
	public String getStatus(String jobId) {
		return getClientProxy().getStatus(jobId);
	}
	 
	public DataTableSpec[] make_output_spec(DataColumnProperties isp) throws InvalidSettingsException {
		// summary port
		DataColumnSpec[] gff_cols = new DataColumnSpec[6];
		// output from web service getResult():
		// # seqname            source        feature      start   end   score  N/A   ?
	    // # ---------------------------------------------------------------------------
		// EFTU_HUMAN           netphos-3.1b  phos-PKC         5     5   0.776  . .  YES
		gff_cols[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
		gff_cols[1] = new DataColumnSpecCreator("Source", StringCell.TYPE).createSpec();
		gff_cols[2] = new DataColumnSpecCreator("Feature", StringCell.TYPE).createSpec();
		gff_cols[3] = new DataColumnSpecCreator("Position (starts at 1)", IntCell.TYPE).createSpec();
		gff_cols[4] = new DataColumnSpecCreator("Score", DoubleCell.TYPE).createSpec();
		gff_cols[5] = new DataColumnSpecCreator("Above cutoff?", BooleanCell.TYPE).createSpec();
		
		DataColumnSpec[] seq_cols = new DataColumnSpec[1];
		DataColumnSpecCreator my_annot_spec = new DataColumnSpecCreator("Annotated Sequence", SequenceCell.TYPE);
	
		// we must keep the list of tracks as a property associated with the column. We must do this
		// so that annotation dialogs can correctly display the tracks for the user
		TrackColumnPropertiesCreator tcpc = new TrackColumnPropertiesCreator(isp,
										new Track(Track.NETPHOS_TRACK, getTrackCreator())
									);
		my_annot_spec.setProperties(tcpc.getProperties());
		seq_cols[0] = my_annot_spec.createSpec();
		return new DataTableSpec[] { new DataTableSpec(gff_cols), new DataTableSpec(seq_cols) };
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
	
    	logger.info("Predicting phosphorylation sites in "+m_seq_col.getStringValue());
    	
		int n_rows     = inData[0].getRowCount();
		if (m_seq_idx < 0)
			throw new InvalidSettingsException("Cannot find column: "+m_seq_col.getStringValue()+" - reconfigure the node?");
		if (!inData[0].getSpec().getColumnSpec(m_seq_idx).getType().isCompatible(SequenceValue.class))
			throw new InvalidSettingsException("Sequence column must a SequenceCell (see Sequence 2 String node)!");
		
		DataTableSpec[] outputSpecs = make_output_spec(inData[0].getSpec().getColumnSpec(m_seq_idx).getProperties());
		
    	final MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(outputSpecs[0]), "Site");
    	final MyDataContainer c2 = new MyDataContainer(exec.createDataContainer(outputSpecs[1]), "AS");
    	
		NetPhosServicePortType proxy = getClientProxy();
		
		int done = 0;
		BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(inData[0].iterator(), 
				m_seq_idx, 100, 1024 * 1024, new SequenceProcessor() {

					@Override
					public SequenceValue process(SequenceValue sv) {
						if (!sv.getSequenceType().isProtein()) {
							logger.warn("Skipping sequence "+sv.getID()+" - must be protein sequence!");
							return null;
						}
						if (sv.getLength() < 1) {
							logger.warn("Skipping empty sequence: "+sv.getID());
							return null;
						}
						return sv;
					}
			
		});
		while (bsi.hasNext()) {
			Map<UniqueID,SequenceValue> batch_map = bsi.nextAsMap();
			
			for (int i=0; i<MAX_RETRIES; i++) {
				exec.checkCanceled();
				try {
					logger.info("Predicting batch of "+batch_map.size()+" sequences.");
					String jobid = proxy.submit(toFasta(batch_map), 
										m_generic.getBooleanValue(), m_best.getBooleanValue(), 
											m_program.equals(METHODS[1]), m_cutoff.getDoubleValue());
					logger.info("Got job id "+jobid+ ", now waiting for batch to complete.");
					
					wait_for_completion(logger, exec, jobid);
					String result = proxy.getResult(jobid);
					
					if (result == null) {
						logger.warn("No results for batch (server problem?) -- continuing anyway!");
						continue;
					}
					//logger.info(result);
					grok_result(new BufferedReader(new StringReader(result)), batch_map, c1, c2);
					
					done += batch_map.size();
					break;
				} catch (Exception e) {
					if (e instanceof CanceledExecutionException || e instanceof InterruptedException)
						throw e;
					int delay = 200 + (200*i);
					if (i<MAX_RETRIES-1) {
						logger.warn("Unable to contact server (retrying in "+delay+" seconds): "+e.getMessage());
						e.printStackTrace();
						Thread.sleep(delay*1000);
					}
				}
			}
			exec.checkCanceled();
			exec.setProgress(((double)done)/n_rows);
		}
		
		return new BufferedDataTable[] { c1.close(), c2.close()};
	}
	
	
	public void grok_result(final BufferedReader br, Map<UniqueID, SequenceValue> batch_map, 
						final MyDataContainer c1, final MyDataContainer c2) throws Exception {
		String line;
		
		HashMap<String,SequenceCell> scells = new HashMap<String,SequenceCell>();
		
		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			String tline = line.trim();
			if (tline.length() < 1)
				continue;
			String[] fields = tline.split("\\s+");
			if (fields.length != 9)
				continue;
			
			//new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
			//new DataColumnSpecCreator("Source", StringCell.TYPE).createSpec();
			// new DataColumnSpecCreator("Feature", StringCell.TYPE).createSpec();
			// new DataColumnSpecCreator("Position (starts at 1)", IntCell.TYPE).createSpec();
			// new DataColumnSpecCreator("Score", DoubleCell.TYPE).createSpec();
			// new DataColumnSpecCreator("Above cutoff?", BooleanCell.TYPE).createSpec();
			//logger.info("Got field: "+fields[0]);
			SequenceValue sv = batch_map.get(new UniqueID(fields[0]));
			if (sv == null) {
				throw new InvalidSettingsException("Cannot find "+fields[0]+" in batch map!");
			}
			DataCell[] cells = new DataCell[6];
			cells[0] = new StringCell(sv.getID());
			cells[1] = new StringCell(fields[1]);
			String feature = fields[2];
			if (feature.startsWith("phos-")) {
				feature = feature.substring("phos-".length());
			}
			cells[2] = new StringCell(feature);
			int pos  = Integer.parseInt(fields[3]);
			cells[3] = new IntCell(pos);
			double score = Double.parseDouble(fields[5]);
			cells[4] = new DoubleCell(score);
			cells[5] = (fields[8].startsWith("Y")) ? BooleanCell.TRUE : BooleanCell.FALSE;
			c1.addRow(cells);
			
			if (!scells.containsKey(fields[0])) {
				scells.put(fields[0], new SequenceCell(sv));
			}
			SequenceCell sc = scells.get(fields[0]);
			Track t = sc.getTrackByName(Track.NETPHOS_TRACK, getTrackCreator());
			// since this class creates the track, we know its annotation is a RegionAnnotation
			RegionsAnnotation ra = (RegionsAnnotation) t.getAnnotation();
			ScoredRegion r = new ScoredRegion(1, pos, pos+1, feature, score);
			ra.addRegion(r);
		}
		
		if (scells.size() != batch_map.size()) {
			logger.warn("Did not get predictions for all sequences in batch!");
		}
		for (String s : scells.keySet()) {
			DataCell[] cells = new DataCell[1];
			cells[0] = scells.get(s);
			c2.addRow(cells);
		}
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
	            throws InvalidSettingsException {
		// we set this here to have the node auto-configure to the configured sequence column
		m_seq_idx = inSpecs[0].findColumnIndex(m_seq_col.getStringValue());
		if (m_seq_idx  < 0 && hasSequenceColumn(inSpecs[0])) {
			m_seq_idx = useSequenceColumnIndex(inSpecs[0], logger);
		}
		return make_output_spec(null);
	}

	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_seq_col.saveSettingsTo(settings);
		m_url.saveSettingsTo(settings);
		m_program.saveSettingsTo(settings);
		m_cutoff.saveSettingsTo(settings);
		m_best.saveSettingsTo(settings);
		m_generic.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_seq_col.validateSettings(settings);
		m_url.validateSettings(settings);
		m_program.validateSettings(settings);
		m_cutoff.validateSettings(settings);
		m_best.validateSettings(settings);
		m_generic.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_seq_col.loadSettingsFrom(settings);
		m_url.loadSettingsFrom(settings);
		m_program.loadSettingsFrom(settings);
		m_cutoff.loadSettingsFrom(settings);
		m_best.loadSettingsFrom(settings);
		m_generic.loadSettingsFrom(settings);
	}

}
