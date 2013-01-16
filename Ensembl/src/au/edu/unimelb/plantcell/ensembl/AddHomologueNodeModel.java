package au.edu.unimelb.plantcell.ensembl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowIterator;
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
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import uk.ac.roslin.ensembl.config.RegistryConfiguration;
import uk.ac.roslin.ensembl.dao.database.DBRegistry;
import uk.ac.roslin.ensembl.dao.database.DBSpecies;
import uk.ac.roslin.ensembl.datasourceaware.compara.DAHomologyPairRelationship;
import uk.ac.roslin.ensembl.datasourceaware.core.DADNASequence;
import uk.ac.roslin.ensembl.datasourceaware.core.DAGene;
import uk.ac.roslin.ensembl.exception.DAOException;
import uk.ac.roslin.ensembl.model.Mapping;
import uk.ac.roslin.ensembl.model.MappingSet;
import uk.ac.roslin.ensembl.model.compara.HomologyAlignmentProperties;
import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.cells.SequenceValue;


/**
 * This is the model implementation of EnsembleAddHomologue.
 * Adds homologues for the input data to the output table
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AddHomologueNodeModel extends NodeModel {
    private final static String db_properties     = "c:/temp/au_ensembl_genomes_config.properties";
    private final static String[] homology_column_order = new String[] { "stable-id", "common-species-name", 
    	"homology-type", "gene-id", "last-common-ancestor", "chromosome-name", "chromosome-coords",
    	"% identity", "% similarity", "% coverage", "db-version"};
    
	public final static String CFGKEY_SPECIES     = "species";
	public final static String CFGKEY_SEQUENCE_ID = "sequence-id";
	
	private final static NodeLogger   logger    = NodeLogger.getLogger("Add Homologues");
	private final SettingsModelString m_species = new SettingsModelString(CFGKEY_SPECIES, "Human");
	private final SettingsModelColumnName m_id      = new SettingsModelColumnName(CFGKEY_SEQUENCE_ID, "");
	
	// map for execute() to use to locate species 
	private final static Map<String,DBSpecies> m_map = new HashMap<String,DBSpecies>();
	
    /**
     * Constructor for the node model.
     */
    protected AddHomologueNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
    	
    	RegistryConfiguration conf = new RegistryConfiguration();
    	conf.setDBByFile(new File(db_properties));
    
    	if (m_map.size() < 1) {
    		String[] species = getGenomeSpecies();
    		logger.info("Loaded "+species.length+" species.");
    	}
		DBSpecies sp =  m_map.get(m_species.getStringValue());
		if (sp == null)
			throw new InvalidSettingsException("Invalid ENSEMBL GENOME: "+m_species.getStringValue());
 
		DataTableSpec[] outSpecs = make_output_spec(inData[0].getSpec());
		MyDataContainer c = new MyDataContainer(exec.createDataContainer(outSpecs[0]), "Homologue"); 
		MyDataContainer c2= new MyDataContainer(exec.createDataContainer(outSpecs[1]), "Entry");
		int seq_idx = -1;
		if (! m_id.useRowID()) {
			seq_idx = inData[0].getSpec().findColumnIndex(m_id.getStringValue());
			if (seq_idx < 0)
				throw new InvalidSettingsException("Unable to find column: "+m_id.getStringValue()+" - reconfigure?");
		}
		RowIterator it = inData[0].iterator();
 
		int done = 0;
		boolean use_row_id = m_id.useRowID();
		while (it.hasNext()) {
			 DataRow r = it.next();
			 String id = r.getKey().getString();
			 if (!use_row_id) {
				 DataCell id_cell = r.getCell(seq_idx);
				 if (id_cell == null || id_cell.isMissing())
					 continue;
				 
				 id = get_id(id_cell);
			 }
			 
			 try {
				 report_homologous_genes(c, c2, sp, id);
			 } catch (DAOException dao) {
				 logger.warn("Error getting record for gene "+id+": ignored.");
				 dao.printStackTrace();
			 }
			 
			 if (done++ % 100 == 0) {
				 exec.checkCanceled();
				 exec.setProgress(((double)done) / inData[0].getRowCount());
			 }
		}
		
		return new BufferedDataTable[]{c.close(), c2.close()};
    }

    private String get_id(DataCell id_cell) {
		if (id_cell == null || id_cell.isMissing())
			return null;
		
		// return the primary ID if its a SequenceCell (ie. not the AA/NA sequence!) or
		// else assume the string cell contents are what is needed
		if (id_cell instanceof SequenceValue) {
			return ((SequenceValue)id_cell).getID();
		} else {
			return id_cell.toString();
		}
	}

	private void report_homologous_genes(MyDataContainer c, MyDataContainer c2, DBSpecies sp, String id) throws DAOException {
		
    	 DAGene g = sp.getGeneByStableID(id);
    	 if (g == null) {
    		 logger.warn("Unable to locate gene by stable ID: "+id+"... ignoring!");
    		 return;
    	 }
    	 
         List<DAHomologyPairRelationship> out = g.getHomologies();
         HashMap<String,String> props = new HashMap<String,String>();

         props.put("stable-id",                     g.getStableID());
         props.put("common-species-name",           g.getSpecies().getCommonName());
         //props.put("number-of-reported-homologies", String.valueOf(out.size()));
         
         for (DAHomologyPairRelationship h : out) {
        	 HomologyAlignmentProperties hap = h.getTargetProperties();
        	 props.put("species-name",         hap.getSpeciesName());
             props.put("gene-id",              h.getTarget().getStableID());
             props.put("last-common-ancestor", h.getLastCommonAncestor());
             props.put("homology-type",        h.getType().toString());
             props.put("chromosome-name",      hap.getSequenceName());
             props.put("chromosome-coords",    hap.getCoords().toString());
             props.put("cigar", hap.getCigarLine());
             props.put("peptide-id", hap.getPeptideID());
             props.put("db-version", h.getDBVersion());
             
             Integer i = hap.getPercentIdentity();
             if (i != null)
            	 props.put("% identity", i.toString());
             i = hap.getPercentSimilar();
             if (i != null)
            	 props.put("% similarity", i.toString());
             i = hap.getPercentCovered();
             if (i != null) 
            	 props.put("% coverage", i.toString());
             
             save_homology(c, props);
             
             //report_mappings("annotation-level", props, h.getTarget().getAnnotationLevelMappings(), h, c2);
            
            
             //report_mappings("build-level", props, h.getTarget().getBuildLevelMappings(), h);

             //report_mappings("top-level", props, h.getTarget().getTopLevelMappings(), h);
         }
	}

	private void save_homology(MyDataContainer c, HashMap<String, String> props) {
		DataCell[] cells = new DataCell[homology_column_order.length];
		int idx = 0;
		for (String key : homology_column_order) {
			String val = props.get(key);
			if (val == null) {
				cells[idx++] = DataType.getMissingCell();
			} else {
				cells[idx++] = new StringCell(val);
			}
		}
		c.addRow(cells);
	}

	private DataTableSpec[] make_output_spec(DataTableSpec spec) {
		DataColumnSpec[] cols = new DataColumnSpec[11];
		cols[0] = new DataColumnSpecCreator("Gene Stable ID", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("Target Species Name", StringCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("Homology type", StringCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("Target Gene ID", StringCell.TYPE).createSpec();
		cols[4] = new DataColumnSpecCreator("Last common ancestor", StringCell.TYPE).createSpec();
		cols[5] = new DataColumnSpecCreator("Target Chromosome name", StringCell.TYPE).createSpec();
		cols[6] = new DataColumnSpecCreator("Target Chromosome co-ordinates", StringCell.TYPE).createSpec();
		cols[7] = new DataColumnSpecCreator("% identity (no fraction)", StringCell.TYPE).createSpec();
		cols[8] = new DataColumnSpecCreator("% similarity (no fraction)", StringCell.TYPE).createSpec();
		cols[9] = new DataColumnSpecCreator("% coverage (no fraction)", StringCell.TYPE).createSpec();
		cols[10]= new DataColumnSpecCreator("Database version", StringCell.TYPE).createSpec();
		
		return new DataTableSpec[] { new DataTableSpec(cols) };
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return make_output_spec(inSpecs[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_species.saveSettingsTo(settings);
    	m_id.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_species.loadSettingsFrom(settings);
    	m_id.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_species.validateSettings(settings);
    	m_id.validateSettings(settings);
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

    /**
     * Returns the display names for available genomes from Ensembl
     * @return
     */
	public static String[] getGenomeSpecies() {
		 try {
			 RegistryConfiguration rc = new RegistryConfiguration();
			 logger.info("Loading EnsemblDB properties from (may take a minute or two): "+db_properties);
			 rc.setDBByFile(new File(db_properties));
			 
			 DBRegistry eReg = new DBRegistry(rc, true);
			 Collection<DBSpecies> sp =  eReg.getSpecies();
			 ArrayList<String> ret = new ArrayList<String>();
			 for (DBSpecies s : sp) {
				 String dname = s.getDisplayName();
				 ret.add(dname);
				 m_map.put(dname, s);
			 }
			 Collections.sort(ret);
			 logger.info("Loaded EnsemblDB species");
			 return ret.toArray(new String[0]);
		 } catch (Exception e) {
			 e.printStackTrace();
			 return new String[] { "Homo Sapiens", "Arabidopsis thaliana" };
		 }
	}

}

