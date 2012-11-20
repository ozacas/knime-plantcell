package au.edu.unimelb.plantcell.io.ws.netsurfp;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.knime.base.node.io.filereader.InterruptedExecutionException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.osgi.framework.Bundle;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.SequenceProcessor;
import au.edu.unimelb.plantcell.core.cells.CoordinateSystem;
import au.edu.unimelb.plantcell.core.cells.NumericAnnotation;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;
import au.edu.unimelb.plantcell.core.cells.SequenceType;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;
import au.edu.unimelb.plantcell.core.cells.Track;
import au.edu.unimelb.plantcell.core.cells.TrackColumnPropertiesCreator;
import au.edu.unimelb.plantcell.core.cells.TrackCreator;
import au.edu.unimelb.plantcell.io.read.fasta.BatchSequenceRowIterator;
import au.edu.unimelb.plantcell.servers.netsurfp.NetSurfPService;
import au.edu.unimelb.plantcell.servers.netsurfp.NetSurfPServicePortType;


/**
 * This is the model implementation of NetPhosAccessor.
 * Implements support for the NetPhos PTM software
 *
 * @author Andrew Cassin
 */
public class NetSurfPAccessorNodeModel extends NodeModel {
	public final int MAX_RETRIES = 5;
	
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("CBS NetSurfP");
   
	// configuration keys between model and dialog
    static final String CFGKEY_SEQUENCE_COL = "sequence-column";
  
    // persisted node configuration state
    private final SettingsModelString m_seq  = new SettingsModelString(CFGKEY_SEQUENCE_COL, "Sequence");
    private int m_seq_idx = -1;
   	
    /**
     * Constructor for the node model.
     */
    protected NetSurfPAccessorNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        logger.info("Searching "+inData[0].getRowCount()+" AA sequences for secondary structure prediction and relative solvent accessibility.");
       
        if (m_seq_idx < 0) {
        	throw new InvalidSettingsException("Cannot find sequence column (re-configure): "+m_seq.getStringValue());
        }
        if (!inData[0].getSpec().getColumnSpec(m_seq_idx).getType().isCompatible(SequenceValue.class))
			throw new InvalidSettingsException("Sequence column must a SequenceCell (see Sequence 2 String node)!");
        DataTableSpec[] outSpecs = make_output_spec(inData[0].getSpec().getColumnSpec(m_seq_idx).getProperties());
        MyDataContainer out = new MyDataContainer(exec.createDataContainer(outSpecs[0]), "Row");
        MyDataContainer out2= new MyDataContainer(exec.createDataContainer(outSpecs[1]), "Seq");
        
        NetSurfPServicePortType proxy = getClientProxy();
        
        // create iterator (small batches due to output size per sequence)
        RowIterator it = inData[0].iterator();
        BatchSequenceRowIterator bsi = new BatchSequenceRowIterator(it, m_seq_idx, 
        									20, 			// max 20 seqs per batch
        									10 * 1024, 		// max 10K residues per batch
        									new SequenceProcessor() {

				@Override
				public SequenceValue process(SequenceValue sv) {
					if (sv.getSequenceType().equals(SequenceType.AA)) {
						if (!sv.isValid()) {
							logger.warn("Ignoring invalid sequence: "+sv.getID());
							return null;
						}
						return sv;
					}
					logger.warn("Skipping non-protein sequence: "+sv.getID());
					return null;
				}
        	
        });
        
        int done = 0;
        while (bsi.hasNext()) {
        	List<SequenceValue> batch = bsi.next();
        	
        	logger.info("Submitting batch of "+batch.size()+ " sequences.");
        	HashMap<String,SequenceValue> batch_map = new HashMap<String,SequenceValue>();
        	for (int i=1; i<=batch.size(); i++) {
        		batch_map.put("Seq"+i, batch.get(i-1));
        	}
        	for (int i=0; i<MAX_RETRIES; i++) {
        		try {
		        	// by submitting a small batch we can check less frequently since it will take x times (typically) one sequence
		        	String jobId     = submit_job(proxy, batch_map);
		        	wait_for_completion(exec, jobId, proxy);
		        	String   results = proxy.getResult(jobId);
		        	//logger.info(results);
		        	if (results == null) {
		        		logger.warn("Got no results from NetSurfP server, skipping results for "+jobId);
		        		break;
		        	}
		        	String[]   lines = results.split("\\n");
		    		DataCell[] cells = new DataCell[10];
		
		    		HashMap<String, SequenceCell> seqs = new HashMap<String,SequenceCell>(batch.size()+20);
		    		HashMap<String, NumericAnnotation> alpha_helices = new HashMap<String,NumericAnnotation>();
		    		HashMap<String, NumericAnnotation> beta_strand   = new HashMap<String,NumericAnnotation>();
		    		HashMap<String, NumericAnnotation> coil          = new HashMap<String,NumericAnnotation>();
		    		
		    		for (String id : batch_map.keySet()) {
		    			SequenceValue sv = batch_map.get(id);
		    			seqs.put(id, new SequenceCell(sv));
		    			if (!alpha_helices.containsKey(id)) {
		    				int len = sv.getLength();
		    				alpha_helices.put(id, new NumericAnnotation(len));
		    				beta_strand.put(id,   new NumericAnnotation(len));
		    				coil.put(id,          new NumericAnnotation(len));
		    			}
		    		}
		    		
		        	for (String line : lines) {
		        		if (line.startsWith("#"))
		        			continue;
		        		String[] fields = line.split("\\s+");
		        		if (fields.length != 10)
		        			continue;
		        		
		        		cells[0] = new StringCell(batch_map.get(fields[2]).getID());
		        		cells[1] = new StringCell(fields[0]);
		        		cells[2] = new StringCell(fields[1]);
		        		int pos = Integer.parseInt(fields[3]);
		        		cells[3] = new IntCell(pos);
		        		cells[4] = new DoubleCell(Double.parseDouble(fields[4]));
		        		cells[5] = new DoubleCell(Double.parseDouble(fields[5]));
		        		cells[6] = new DoubleCell(Double.parseDouble(fields[6]));
		        		double prob_alpha = Double.parseDouble(fields[7]);
		        		double prob_beta  = Double.parseDouble(fields[8]);
		        		double prob_coil  = Double.parseDouble(fields[9]);
		        		
		        		NumericAnnotation na = alpha_helices.get(fields[2]);
		        		if (na == null) {
		        			logger.warn("Unable to locate vector for sequence "+fields[2]);
		        			continue;
		        		}
		        		na.setPosition(pos-1, prob_alpha);
		        		beta_strand.get(fields[2]).setPosition(pos-1, prob_beta);
		        		coil.get(fields[2]).setPosition(pos-1, prob_coil);
		        		
		        		cells[7] = new DoubleCell(prob_alpha);
		        		cells[8] = new DoubleCell(prob_beta);
		        		cells[9] = new DoubleCell(prob_coil);
		        		out.addRow(cells);
		        	}
		        	for (String id : batch_map.keySet()) {
			        	cells           = new DataCell[1];
			        	SequenceCell sc = seqs.get(id);
			        	sc.addTrack(new Track(Track.NETSURFP_ALPHA, alpha_helices.get(id), CoordinateSystem.OFFSET_FROM_START));
			        	sc.addTrack(new Track(Track.NETSURFP_BETA, beta_strand.get(id), CoordinateSystem.OFFSET_FROM_START));
			        	sc.addTrack(new Track(Track.NETSURFP_COIL, coil.get(id), CoordinateSystem.OFFSET_FROM_START));
			        	cells[0] = sc;
			        	out2.addRow(cells);
		        	}
		        	break;
        		} catch (Exception e) {
        			if (e instanceof CanceledExecutionException || 
        					e instanceof InterruptedExecutionException) 
        				throw e;
        			e.printStackTrace();

        			int delay = 200 + (i * 200);
        			logger.warn("Failed to process NetSurfP sequences: "+e.getMessage());
        			if (i<MAX_RETRIES-1) {
        				logger.warn("Retrying in "+delay+" seconds...");
        				Thread.sleep(delay * 1000);
        			}
        		}
        	}
        	
        	exec.checkCanceled();
        	done += batch.size();
        	exec.setProgress(((double)done) / inData[0].getRowCount());
        }
        
        // once we are done, we close the container and return its table
       
        return new BufferedDataTable[]{out.close(), out2.close()};
    }


	public NetSurfPServicePortType getClientProxy() {
   		 // NB: need to use the local WSDL copy rather than go online for it... so...
   		 try {
   			 Bundle bundle = Platform.getBundle("au.edu.unimelb.plantcell.io.ws");
   			 URL u = FileLocator.find(bundle, new Path("/wsdl/netsurfp@nectar.wsdl"), null);
   			 
   			 // must not call default constructor for local WSDL... so...
   			 NetSurfPService srv = new NetSurfPService(u,
   					new QName("http://nectar.plantcell.unimelb.edu.au", "NetSurfPService"));
   			 return srv.getNetSurfPServiceHttpSoap11Endpoint();
   		 } catch (Exception e) {
   			 e.printStackTrace();
   			 Logger.getAnonymousLogger().warning("Unable to get netsurfp proxy: "+e.getMessage());
   			 return null;
   		 }
   	}
	
    public String submit_job(NetSurfPServicePortType proxy, Map<String,SequenceValue> batch) {
    	StringBuilder sb = new StringBuilder(100 * 1024);
    	for (String id : batch.keySet()) {
    		SequenceValue sv = batch.get(id);
    		sb.append(">");
    		sb.append(id);
    		sb.append(" ");
    		sb.append(sv.getID());		// need to put this here, otherwise we can't identify which sequence a given line from results relates to
    		sb.append('\n');
    		sb.append(sv.getStringValue());
    		sb.append('\n');
    	}
    	return proxy.submit(sb.toString());
    }
    
    private void wait_for_completion(final ExecutionContext exec, String jobId, NetSurfPServicePortType proxy) throws Exception {
    	String status = "QUEUED";
		int try_cnt = 0;
		while (status.equals("QUEUED") || status.equals("WAITING") || status.equals("RUNNING")) {
			// delay for 30s + 6s per attempt to get the finished job (give up if excessive delay)
			for (int i=0; i<6; i++) {
				exec.checkCanceled();
				Thread.sleep((try_cnt+5) * 1000);
			}
			status = proxy.getStatus(jobId);
			logger.info("Got status "+status+" for job "+jobId);
			try_cnt++;
			if (try_cnt > 100) {
				throw new Exception("Aborting... NetSurfP job not completed after excessive delay!");
			}
		}
		if (!status.startsWith("COMPLETE")) {
			throw new Exception("NetSurfP for job "+jobId+" failed, message: "+status);
		}
    }


	private TrackCreator getTrackCreator() {
		return new TrackCreator() {

			@Override
			public Track createTrack(String name)
					throws InvalidSettingsException {
				return new Track(name, new NumericAnnotation(), CoordinateSystem.OFFSET_FROM_START);
			}
			
		};
	}
	
	private DataTableSpec[] make_output_spec(DataColumnProperties isp) throws InvalidSettingsException {
		DataColumnSpec[] cols = new DataColumnSpec[10];
		cols[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Buried/Exposed?", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Amino Acid", StringCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("AA Position", IntCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("Relative Surface Accessibility (RSA)", DoubleCell.TYPE).createSpec();
		cols[5] = new DataColumnSpecCreator("Absolute Surface Accessibility", DoubleCell.TYPE).createSpec();
		cols[6] = new DataColumnSpecCreator("Z-Fit Score", DoubleCell.TYPE).createSpec();
		cols[7] = new DataColumnSpecCreator("Probability of Alpha-Helix [0..1]", DoubleCell.TYPE).createSpec();
		cols[8] = new DataColumnSpecCreator("Probability of Beta-Strand [0..1]", DoubleCell.TYPE).createSpec();
		cols[9] = new DataColumnSpecCreator("Probability of Coil [0..1]", DoubleCell.TYPE).createSpec();

		
		DataColumnSpec[] cols2 = new DataColumnSpec[1];
		DataColumnSpecCreator my_annot_spec = new DataColumnSpecCreator("Annotated Sequence (incl. NetSurfP tracks)", SequenceCell.TYPE);
		
		// we must keep the list of tracks as a property associated with the column. We must do this
		// so that annotation dialogs can correctly display the tracks for the user
		TrackColumnPropertiesCreator tcpc = new TrackColumnPropertiesCreator(isp,
										new Track[] { 
												new Track(Track.NETSURFP_ALPHA, getTrackCreator()),
												new Track(Track.NETSURFP_BETA, getTrackCreator()),
												new Track(Track.NETSURFP_COIL, getTrackCreator())
		});
		
		my_annot_spec.setProperties(tcpc.getProperties());
		cols2[0] = my_annot_spec.createSpec();
		return new DataTableSpec[] { new DataTableSpec(cols), new DataTableSpec(cols2) };
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	
    	m_seq_idx = inSpecs[0].findColumnIndex(m_seq.getStringValue());
    	
    	// dont know the column properties (ie. tracks) yet, so just pass null for until execute()...
    	return make_output_spec(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_seq.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_seq.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_seq.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

}

