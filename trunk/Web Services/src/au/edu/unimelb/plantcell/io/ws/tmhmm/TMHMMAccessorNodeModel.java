package au.edu.unimelb.plantcell.io.ws.tmhmm;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
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
import au.edu.unimelb.plantcell.core.cells.CoordinateSystem;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.core.cells.TrackCreator;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.servers.tmhmm.TMHMMService;
import au.edu.unimelb.plantcell.servers.tmhmm.TMHMMServicePortType;
import au.edu.unimelb.plantcore.core.regions.Region;
import au.edu.unimelb.plantcore.core.regions.RegionsAnnotation;
import au.edu.unimelb.plantcore.core.regions.TMHMMRegionsAnnotation;



public class TMHMMAccessorNodeModel extends AbstractWebServiceNodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("TMHMM Accessor");
    
    // dialog configuration & model settings
	public static final String CFGKEY_SEQUENCE    = "sequence-column";
	public static final String CFGKEY_URL         = "endpoint-url";

	private static final int MAX_RETRIES = 5;
	
	private final SettingsModelString m_seq_col   = new SettingsModelString(CFGKEY_SEQUENCE, "Sequence");
	private final SettingsModelString m_url       = new SettingsModelString(CFGKEY_URL, getDefaultEndpoint());
	private int m_seq_idx = -1;
	
	public TMHMMAccessorNodeModel() {
		super(1, 2);
	}
	
	public TMHMMServicePortType getClientProxy() {
   		 // NB: need to use the local WSDL copy rather than go online for it... so...
   		 try {
   			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws");
   			 URL u = FileLocator.find(bundle, new Path("/wsdl/tmhmm@nectar.wsdl"), null);
   			 
   			 // must not call default constructor for local WSDL... so...
   			 TMHMMService srv = new TMHMMService(u,
   					new QName("http://nectar.plantcell.unimelb.edu.au", "TMHMMService"));
   			 return srv.getTMHMMServiceHttpSoap11Endpoint();
   		 } catch (Exception e) {
   			 e.printStackTrace();
   			 Logger.getAnonymousLogger().warning("Unable to get TMHMM proxy: "+e.getMessage());
   			 return null;
   		 }
   	}
	
	@Override
	public String getStatus(String jobId) {
		return getClientProxy().getStatus(jobId);
	}
	 
	public DataTableSpec[] make_output_spec(DataColumnProperties isp) throws InvalidSettingsException {
		// summary port
		DataColumnSpec[] summary_cols = new DataColumnSpec[3];
		summary_cols[0] = new DataColumnSpecCreator("Accession", StringCell.TYPE).createSpec();
		summary_cols[1] = new DataColumnSpecCreator("Field", StringCell.TYPE).createSpec();
		summary_cols[2] = new DataColumnSpecCreator("Value", DoubleCell.TYPE).createSpec();
		
		DataColumnSpec[] domain_cols = new DataColumnSpec[1];
		DataColumnSpecCreator my_annot_spec = new DataColumnSpecCreator("Annotated Sequence", SequenceCell.TYPE);
	
		// we must keep the list of tracks as a property associated with the column. We must do this
		// so that annotation dialogs can correctly display the tracks for the user
		TrackColumnPropertiesCreator tcpc = new TrackColumnPropertiesCreator(isp,
										new Track(Track.TMHMM_TRACK, getTrackCreator())
									);
		my_annot_spec.setProperties(tcpc.getProperties());
		domain_cols[0] = my_annot_spec.createSpec();
		return new DataTableSpec[] { new DataTableSpec(summary_cols), new DataTableSpec(domain_cols) };
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
	
    	logger.info("Predicting transmembrane helixes in "+m_seq_col.getStringValue());
    	
		int n_rows     = inData[0].getRowCount();
		if (m_seq_idx < 0)
			throw new InvalidSettingsException("Cannot find column: "+m_seq_col.getStringValue()+" - reconfigure the node?");
		if (!inData[0].getSpec().getColumnSpec(m_seq_idx).getType().isCompatible(SequenceValue.class))
			throw new InvalidSettingsException("No suitable Sequence column (see Columns to Sequence node)!");
		
		DataTableSpec[] outputSpecs = make_output_spec(inData[0].getSpec().getColumnSpec(m_seq_idx).getProperties());
		
    	final MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(outputSpecs[0]), "Row");
    	final MyDataContainer c2 = new MyDataContainer(exec.createDataContainer(outputSpecs[1]), "AS");
    	
		TMHMMServicePortType proxy = getClientProxy();
		
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
					String jobid = proxy.submit(toFasta(batch_map), "");
					logger.info("Got job id "+jobid+ ", now waiting for batch to complete.");
					
					wait_for_completion(logger, exec, jobid);
					String result = proxy.getResult(jobid);
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
						Thread.sleep(delay*1000);
					}
				}
			}
			exec.checkCanceled();
			exec.setProgress(((double)done)/n_rows);
		}
		
		return new BufferedDataTable[] { c1.close(), c2.close()};
	}
	
	@Override
	protected TrackCreator getTrackCreator() {
		return new TrackCreator() {

			@Override
			public Track createTrack(String name)
					throws InvalidSettingsException {
				return new Track(name, new TMHMMRegionsAnnotation(), CoordinateSystem.OFFSET_FROM_START);
			}
			
		};
	}
	
	public void grok_result(final BufferedReader br, final Map<UniqueID, SequenceValue> batch_map, final MyDataContainer c1, final MyDataContainer c2) 
					throws Exception {
		String line;
		Pattern summary_re = Pattern.compile("^#\\s+(\\S+)\\s+([^:]+?):\\s+(\\S+)\\s*$");
		Pattern domain_re  = Pattern.compile("^(\\S+)\\s+\\S+\\s+(\\S+)\\s+(\\d+)\\s+(\\d+)\\s*$");
		
		Map<String,SequenceCell> annotated_track = new HashMap<String,SequenceCell>();
		while ((line = br.readLine()) != null) {
			String tline = line.trim();
			if (tline.length() < 1) {
				continue;
			}
			// summary output port data?
			if (tline.startsWith("#")) {
				Matcher m = summary_re.matcher(tline);
				if (m.matches()) {
					DataCell[] cells = new DataCell[3];
					SequenceValue sv   = batch_map.get(new UniqueID(m.group(1)));
					if (sv == null)
						throw new InvalidSettingsException("Cannot find "+m.group(1)+" in batch!");
					cells[0] = new StringCell(sv.getID());
					cells[1] = new StringCell(m.group(2));
					cells[2] = new DoubleCell(new Double(m.group(3)));
					
					c1.addRow(cells);
				} else if (tline.toLowerCase().indexOf("possible n-term signal sequence") >= 0) {
					int space_after_accsn = tline.indexOf(' ', 2);
					if (space_after_accsn >= 0) {
						DataCell[] cells = new DataCell[3];
						String accsn = tline.substring(2,space_after_accsn).trim();
						SequenceValue sv   = batch_map.get(new UniqueID(accsn));
						if (sv == null)
							throw new InvalidSettingsException("Cannot find "+accsn+" in batch!");
						cells[0] = new StringCell(sv.getID());
						cells[1] = new StringCell("contains signal peptide? (1 if predicted)");
						cells[2] = new DoubleCell(1.0);
						c1.addRow(cells);
					} else {
						logger.warn("Unable to report signal peptide for "+tline.substring(2));
					}
				}
				continue;
			}
			// else domain details data
			Matcher m = domain_re.matcher(tline);
			if (m.matches()) {
				SequenceValue sv   = batch_map.get(new UniqueID(m.group(1)));
				if (sv == null)
					throw new InvalidSettingsException("Cannot find "+m.group(1)+" in batch!");
				String orientation = m.group(2);
				int start = Integer.parseInt(m.group(3));
				int end   = Integer.parseInt(m.group(4));
				SequenceCell sc = null;
				if (!annotated_track.containsKey(sv.getID())) {
					sc = new SequenceCell(sv);
					annotated_track.put(sv.getID(), sc);
				} else {
					sc = annotated_track.get(sv.getID());
				}
				assert(sc != null);
				Track              t = sc.getTrackByName(Track.TMHMM_TRACK, getTrackCreator());
				RegionsAnnotation ra = (RegionsAnnotation) t.getAnnotation();
				Region r = new Region(start-1, end-1, orientation);
				ra.addRegion(r);
			}
		}
		
		for (SequenceCell sc : annotated_track.values()) {
			c2.addRow(new DataCell[] { sc });
		}
		
		br.close();
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
	            throws InvalidSettingsException {
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
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_seq_col.validateSettings(settings);
		m_url.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_seq_col.loadSettingsFrom(settings);
		m_url.loadSettingsFrom(settings);
	}
}
