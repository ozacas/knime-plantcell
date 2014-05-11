package au.edu.unimelb.plantcell.io.ws.signalp_nectar;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.knime.core.data.DataType;
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
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.UniqueID;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.core.regions.RegionsAnnotation;
import au.edu.unimelb.plantcell.core.regions.ScoredRegion;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.io.ws.tmhmm.AbstractWebServiceNodeModel;
import au.edu.unimelb.plantcell.servers.nectar.signalp.SignalPService;
import au.edu.unimelb.plantcell.servers.nectar.signalp.SignalPServicePortType;



public class SignalPAccessorNodeModel extends AbstractWebServiceNodeModel {
	 // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("SignalP@Nectar");
    
   
    // dialog configuration & model settings
	public static final String CFGKEY_SEQUENCE    = "sequence-column";
	public static final String CFGKEY_URL         = "endpoint-url";
	public static final String CFGKEY_ORGANISM_TYPE="organism-type";
	public static final String CFGKEY_LENGTH_CUTOFF="length-cutoff";
	public static final String CFGKEY_METHOD      = "method";
	public static final String CFGKEY_TM_CUTOFF   = "tm-cutoff";
	public static final String CFGKEY_NOTM_CUTOFF = "notm-cutoff";
	
	public static final String[] METHODS = new String[] { "best", "no-TM" };
	public static final String[] ORGANISM_TYPES = new String[] { "eukaryotic", "gram+", "gram-" };
    
	private final SettingsModelString m_seq_col   = new SettingsModelString(CFGKEY_SEQUENCE, "Sequence");
	private final SettingsModelString m_url       = new SettingsModelString(CFGKEY_URL, getDefaultEndpoint());
	private final SettingsModelString m_organism  = new SettingsModelString(CFGKEY_ORGANISM_TYPE, "eukaryotic");
	private final SettingsModelIntegerBounded m_length = new SettingsModelIntegerBounded(CFGKEY_LENGTH_CUTOFF, 70, 0, 10000);
	private final SettingsModelString m_method    = new SettingsModelString(CFGKEY_METHOD, "best");
	private final SettingsModelDoubleBounded m_tm_cutoff = new SettingsModelDoubleBounded(CFGKEY_TM_CUTOFF, -1, -1.0, 1.0);
	private final SettingsModelDoubleBounded m_notm_cutoff=new SettingsModelDoubleBounded(CFGKEY_NOTM_CUTOFF, -1, -1.0, 1.0);
	private int   m_seq_idx = -1;
	
	public SignalPAccessorNodeModel() {
		super(1, 2);
	}
	 
	public DataTableSpec[] make_output_spec(DataTableSpec dataTableSpec) throws InvalidSettingsException {
		// summary port
		DataColumnSpec[] cols = new DataColumnSpec[12];
		//# SignalP-4.0 euk predictions
		//# name                     Cmax  pos  Ymax  pos  Smax  pos  Smean   D     ?  Dmaxcut    Networks-used
		//IPI:IPI00000001.2          0.143 42   0.123  42  0.142  39  0.100   0.110 N  0.450      SignalP-noTM
		cols[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("CMax", DoubleCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("CMax Position (starts at 1)", IntCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("YMax", DoubleCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("YMax Position (starts at 1)", IntCell.TYPE).createSpec();
		cols[5] = new DataColumnSpecCreator("SMax", DoubleCell.TYPE).createSpec();
		cols[6] = new DataColumnSpecCreator("SMax Position (starts at 1)", IntCell.TYPE).createSpec();
		cols[7] = new DataColumnSpecCreator("Smean", DoubleCell.TYPE).createSpec();
		cols[8] = new DataColumnSpecCreator("D", DoubleCell.TYPE).createSpec();
		cols[9] = new DataColumnSpecCreator("Predicted signal peptide?", BooleanCell.TYPE).createSpec();
		cols[10]= new DataColumnSpecCreator("Dmaxcut", DoubleCell.TYPE).createSpec();
		cols[11]= new DataColumnSpecCreator("Networks used", StringCell.TYPE).createSpec();
		
		DataColumnSpec[] seq_cols = new DataColumnSpec[1];
		DataColumnSpecCreator my_annot_spec = new DataColumnSpecCreator("Annotated Sequence", SequenceCell.TYPE);
	
		// we must keep the list of tracks as a property associated with the column. We must do this
		// so that annotation dialogs can correctly display the tracks for the user
		DataColumnProperties isp = new DataColumnProperties();
		int seq_idx = dataTableSpec.findColumnIndex(m_seq_col.getStringValue());
		if (seq_idx > 0) {
			isp = dataTableSpec.getColumnSpec(seq_idx).getProperties();
		}
		TrackColumnPropertiesCreator tcpc = new TrackColumnPropertiesCreator(isp,
										new Track(Track.SIGNALP_TRACK, getTrackCreator())
									);
		my_annot_spec.setProperties(tcpc.getProperties());
		seq_cols[0] = my_annot_spec.createSpec();
		return new DataTableSpec[] { new DataTableSpec(cols), new DataTableSpec(seq_cols) };
	}
	
	public SignalPServicePortType getClientProxy() {
  		 // NB: need to use the local WSDL copy rather than go online for it... so...
  		 try {
  			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws");
  			 URL u = FileLocator.find(bundle, new Path("/wsdl/signalp@nectar.wsdl"), null);
  			 
  			 // must not call default constructor for local WSDL... so...
  			 SignalPService srv = new SignalPService(u,
  					new QName("http://nectar.plantcell.unimelb.edu.au", "SignalPService"));
  			 return srv.getSignalPServiceHttpSoap11Endpoint();
  		 } catch (Exception e) {
  			 e.printStackTrace();
  			 Logger.getAnonymousLogger().warning("Unable to get NetPhos proxy: "+e.getMessage());
  			 return null;
  		 }
  	}
	
	public String getStatus(String jobId) {
		return getClientProxy().getStatus(jobId);
	}
	
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
	            final ExecutionContext exec) throws Exception {
	
    	logger.info("Predicting signal peptides in "+m_seq_col.getStringValue());
    	DataTableSpec[] outSpecs = make_output_spec(inData[0].getSpec());
    	MyDataContainer c1 = new MyDataContainer(exec.createDataContainer(outSpecs[0]), "Row");
    	MyDataContainer c2 = new MyDataContainer(exec.createDataContainer(outSpecs[1]), "Seq");
    	
    	if (m_seq_idx < 0)
    		throw new InvalidSettingsException("Cannot find column: "+m_seq_col.getStringValue()+": reconfigure?");
    	
		BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(inData[0].iterator(), m_seq_idx, 
				50, 200 * 1024, new SequenceProcessor() 
		{

			@Override
			public SequenceValue process(SequenceValue sv) {
				if (!sv.getSequenceType().equals(SequenceType.AA)) {
					logger.warn("Skipping sequence: "+sv.getID()+" - must be protein sequence!");
					return null;
				}
				return sv;
			}
			
		});
		
		SignalPServicePortType proxy = getClientProxy();
		
		int done = 0;
		while (bsi.hasNext()) {
			Map<UniqueID,SequenceValue> batch = bsi.nextAsMap();
			StringBuffer sb = new StringBuffer();
			for (UniqueID uid : batch.keySet()) {
				sb.append(">"+uid.toString()+"\n");
				sb.append(batch.get(uid).getStringValue());
				sb.append("\n");
			}
			
			String type = "euk";
			if (m_organism.getStringValue().startsWith("gram+"))
				type = "plus";
			else if (m_organism.getStringValue().startsWith("gram-")) 
				type = "neg";
			
			String jobId = null;
			for (int i=0; i<MAX_RETRIES; i++) {
				try {
					jobId = proxy.submit(sb.toString(), m_tm_cutoff.getDoubleValue(), m_notm_cutoff.getDoubleValue(), 
							m_method.getStringValue().equals("best"), m_length.getIntValue(), type);
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
			String line = null;
			boolean in_gff = false;
			Map<UniqueID,List<ScoredRegion>> features = new HashMap<UniqueID,List<ScoredRegion>>();
			while ((line = rdr.readLine()) != null) {
				if (line.startsWith("#")) {
					if (line.startsWith("##gff-version")) {
						in_gff = true;
					}
					continue;
				}
				String[] fields = line.split("\\s+");
				
				// short-style output from signalp?
				if (!in_gff) {
					if (fields.length != 12)
						continue;
					
					DataCell[] cells = new DataCell[fields.length];
					grok_line(cells, fields, batch);
					c1.addRow(cells);
				} else {
					// gff format for predicted signal peptides?
					if (fields.length != 9)
						continue;
					List<ScoredRegion> l = features.get(new UniqueID(fields[0]));
					if (l == null) {
						l = new ArrayList<ScoredRegion>();
						features.put(new UniqueID(fields[0]), l);
					}
					
					ScoredRegion sr = make_region(fields);
					l.add(sr);
				}
			}
			rdr.close();
			
			for (UniqueID uid : batch.keySet()) {
				SequenceValue sv= batch.get(uid);
				SequenceCell sc = new SequenceCell(sv);
				List<ScoredRegion> l = features.get(uid);
				if (l != null) {
					Track t = sc.addTrack(Track.SIGNALP_TRACK, getTrackCreator());
					((RegionsAnnotation)t.getAnnotation()).addAll(l, false);	// signalp does not duplicate so false is ok here
				}
				c2.addRow(new DataCell[] { sc });
			}
			
		
		
			done += batch.size();
			exec.checkCanceled();
			exec.setProgress(((double)done) / inData[0].getRowCount());
		}
		
		return new BufferedDataTable[] { c1.close(), c2.close()};
	}
	
	private ScoredRegion make_region(String[] fields) {
		assert(fields != null && fields.length == 9);
		try {
			int start = Integer.parseInt(fields[3]);
			int end   = Integer.parseInt(fields[4]);
			double score = Double.NaN;
			if (fields[5].length() > 0 && !fields[5].equals("."))
				score = Double.parseDouble(fields[5]);
			return new ScoredRegion(1, start, end, fields[2], score);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private void grok_line(DataCell[] cells, String[] fields, Map<UniqueID,SequenceValue> batch) throws InvalidSettingsException {
		/*
		cols[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("CMax", DoubleCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("CMax Position (starts at 1)", IntCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("YMax", DoubleCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("YMax Position (starts at 1)", IntCell.TYPE).createSpec();
		cols[5] = new DataColumnSpecCreator("SMax", DoubleCell.TYPE).createSpec();
		cols[6] = new DataColumnSpecCreator("SMax Position (starts at 1)", IntCell.TYPE).createSpec();
		cols[7] = new DataColumnSpecCreator("Smean", DoubleCell.TYPE).createSpec();
		cols[8] = new DataColumnSpecCreator("D", DoubleCell.TYPE).createSpec();
		cols[9] = new DataColumnSpecCreator("Predicted signal peptide?", BooleanCell.TYPE).createSpec();
		cols[10]= new DataColumnSpecCreator("Dmaxcut", DoubleCell.TYPE).createSpec();
		*/
		assert(fields.length == cells.length && fields.length == 12);
		cells[0] = new StringCell(batch.get(new UniqueID(fields[0])).getID());
		cells[1] = safe_double(fields[1]);
		cells[2] = safe_int(fields[2]);
		cells[3] = safe_double(fields[3]);
		cells[4] = safe_int(fields[4]);
		cells[5] = safe_double(fields[5]);
		cells[6] = safe_int(fields[6]);
		cells[7] = safe_double(fields[7]);
		cells[8] = safe_double(fields[8]);
		cells[9] = fields[9].startsWith("Y") ? BooleanCell.TRUE : BooleanCell.FALSE;
		cells[10]= safe_double(fields[10]);
		cells[11]= new StringCell(fields[11]);
	}

	private DataCell safe_int(String val) {
		try {
			return new IntCell(Integer.parseInt(val));
		} catch (NumberFormatException nfe) {
			return DataType.getMissingCell();
		}
	}
	
	private DataCell safe_double(String dbl) {
		try {
			return new DoubleCell(Double.parseDouble(dbl));
		} catch (NumberFormatException nfe) {
			return DataType.getMissingCell();
		}
	}

	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
	            throws InvalidSettingsException {
		m_seq_idx = inSpecs[0].findColumnIndex(m_seq_col.getStringValue());
		if (m_seq_idx  < 0 && hasSequenceColumn(inSpecs[0])) {
			m_seq_idx = useSequenceColumnIndex(inSpecs[0], logger);
		}
		return make_output_spec(inSpecs[0]);
	}
	
	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		m_seq_col.saveSettingsTo(settings);
		m_url.saveSettingsTo(settings);
		m_organism.saveSettingsTo(settings);
		m_length.saveSettingsTo(settings);
		m_method.saveSettingsTo(settings);
		m_tm_cutoff.saveSettingsTo(settings);
		m_notm_cutoff.saveSettingsTo(settings);	
	}

	@Override
	protected void validateSettings(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_seq_col.validateSettings(settings);
		m_url.validateSettings(settings);
		m_organism.validateSettings(settings);
		m_length.validateSettings(settings);
		m_method.validateSettings(settings);
		m_tm_cutoff.validateSettings(settings);
		m_notm_cutoff.validateSettings(settings);	
	}

	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException {
		m_seq_col.loadSettingsFrom(settings);
		m_url.loadSettingsFrom(settings);
		m_organism.loadSettingsFrom(settings);
		m_length.loadSettingsFrom(settings);
		m_method.loadSettingsFrom(settings);
		m_tm_cutoff.loadSettingsFrom(settings);
		m_notm_cutoff.loadSettingsFrom(settings);	
	}
}
