package au.edu.unimelb.plantcell.ensembl;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import au.edu.unimelb.plantcell.core.MyDataContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import uk.ac.roslin.ensembl.config.DBConnection.DataSource;
import uk.ac.roslin.ensembl.config.RegistryConfiguration;
import uk.ac.roslin.ensembl.dao.database.DBCollection;
import uk.ac.roslin.ensembl.dao.database.DBRegistry;
import uk.ac.roslin.ensembl.dao.database.DBSpecies;
import uk.ac.roslin.ensembl.datasourceaware.compara.DAHomologyPairRelationship;
import uk.ac.roslin.ensembl.datasourceaware.core.DADNASequence;
import uk.ac.roslin.ensembl.datasourceaware.core.DAGene;
import uk.ac.roslin.ensembl.exception.DAOException;
import uk.ac.roslin.ensembl.model.Mapping;
import uk.ac.roslin.ensembl.model.MappingSet;


/**
 * This is the model implementation of EnsembleAddHomologue.
 * Adds homologues for the input data to the output table
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AddHomologueNodeModel extends NodeModel {
    
	public final static String CFGKEY_SPECIES = "species";
	public final static String CFGKEY_SEQUENCE_ID = "sequence-id";
	
	private final SettingsModelString m_species = new SettingsModelString(CFGKEY_SPECIES, "Human");
	private final SettingsModelString m_id      = new SettingsModelString(CFGKEY_SEQUENCE_ID, "");
	
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

    	 DBRegistry eReg = new DBRegistry(DataSource.ENSEMBLGENOMES);
         DBSpecies sp =  eReg.getSpeciesByEnsemblName(m_species.getStringValue());
         if (sp == null)
        	 throw new InvalidSettingsException("Invalid ENSEMBL GENOME: "+m_species.getStringValue());
         
         DataTableSpec outSpec = make_output_spec(inData[0].getSpec());
         MyDataContainer c = new MyDataContainer(exec.createDataContainer(outSpec), "Homologue"); 
         int seq_idx = inData[0].getSpec().findColumnIndex(m_id.getStringValue());
         if (seq_idx < 0)
        	 throw new InvalidSettingsException("Unable to find column: "+m_id.getStringValue()+" - reconfigure?");
         RowIterator it = inData[0].iterator();
         
         while (it.hasNext()) {
        	 DataRow r = it.next();
        	 DataCell id_cell = r.getCell(seq_idx);
        	 if (id_cell == null || id_cell.isMissing())
        		 continue;
        	 report_homologous_genes(c, sp, id_cell.toString());
         }
        
        return new BufferedDataTable[]{c.close()};
    }

    private void report_homologous_genes(MyDataContainer c, DBSpecies sp, String id) throws DAOException {
    	 DAGene g = sp.getGeneByStableID(id);
    	 if (g == null)
    		 return;
    	 
         List<DAHomologyPairRelationship> out = g.getHomologies();
         System.out.println(g.getSpecies().getCommonName() + " gene " + g.getStableID() + " has " + out.size() + " homologies.");
         System.out.println("_____________________________________________\r\n");
         for (DAHomologyPairRelationship h : out) {

             System.out.print(h.getTargetProperties().getSpeciesName());
             System.out.print(" gene: " + h.getTarget().getStableID());
             System.out.println(" [" + h.getType().toString() + "] (last common ancestor: " + h.getLastCommonAncestor() + ")");

             System.out.println("MAPPING DATA IN COMPARA");
             System.out.println("'chromosome' name: " + h.getTargetProperties().getSequenceName()
                     + " [" + h.getTargetProperties().getCoords().toString() + "]");

             System.out.println("MAPPING DATA LAZY LOADED FROM CORE");

             MappingSet m = null;

             try {
                 m = h.getTarget().getAnnotationLevelMappings();
             } catch (DAOException dAOException) {
             }

             System.out.print("ANNOTATION LEVEL: ");
             if (m != null && !m.isEmpty()) {
                 for (Mapping mp : m) {
                     System.out.println(mp.getTarget().getClass().getSimpleName()
                             + " name: "
                             + ((DADNASequence) mp.getTarget()).getName()
                             + " id: " + mp.getTarget().getId() + " ["
                             + mp.getTargetCoordinates().toString() + "]");

                     if (!h.getTargetProperties().getSequenceName().contentEquals(((DADNASequence) mp.getTarget()).getName())) {
                         System.out.println("\n\n\n*********ERROR in name");
                     }
                     if (h.getTargetProperties().getCoords().getStart() - mp.getTargetCoordinates().getStart() != 0) {
                         System.out.println("\n\n\n*********ERROR in start coord");
                     }
                     if (h.getTargetProperties().getCoords().getEnd() - mp.getTargetCoordinates().getEnd() != 0) {
                         System.out.println("\n\n\n*********ERROR in end coord");
                     }
                     if (!h.getTargetProperties().getCoords().getStrand().equals(mp.getTargetCoordinates().getStrand())) {
                         System.out.println("\n\n\n*********ERROR in strande");
                     }

                 }
             } else {
                 System.out.println("");
             }

             try {
                 m = h.getTarget().getBuildLevelMappings();
             } catch (DAOException dAOException) {
             }


             System.out.print("BUILD LEVEL: ");
             if (m != null && !m.isEmpty()) {

                 for (Mapping mp : m) {
                     System.out.println(mp.getTarget().getClass().getSimpleName()
                             + " name: "
                             + ((DADNASequence) mp.getTarget()).getName()
                             + " id: " + mp.getTarget().getId() + " ["
                             + mp.getTargetCoordinates().toString() + "]");

                     if (!h.getTargetProperties().getSequenceName().contentEquals(((DADNASequence) mp.getTarget()).getName())) {
                         System.out.println("\n\n\n*********ERROR in name");
                     }
                     if (h.getTargetProperties().getCoords().getStart() - mp.getTargetCoordinates().getStart() != 0) {
                         System.out.println("\n\n\n*********ERROR in start coord");
                     }
                     if (h.getTargetProperties().getCoords().getEnd() - mp.getTargetCoordinates().getEnd() != 0) {
                         System.out.println("\n\n\n*********ERROR in end coord");
                     }
                     if (!h.getTargetProperties().getCoords().getStrand().equals(mp.getTargetCoordinates().getStrand())) {
                         System.out.println("\n\n\n*********ERROR in strande");
                     }

                 }
             } else {
                 System.out.println("");
             }

             try {
                 m = h.getTarget().getTopLevelMappings();
             } catch (DAOException dAOException) {
             }
             System.out.print("TOP LEVEL: ");
             if (m != null && !m.isEmpty()) {
                 for (Mapping mp : m) {
                     System.out.println(mp.getTarget().getClass().getSimpleName()
                             + " name: "
                             + ((DADNASequence) mp.getTarget()).getName()
                             + " id: " + mp.getTarget().getId() + " ["
                             + mp.getTargetCoordinates().toString() + "]");

                     if (!h.getTargetProperties().getSequenceName().contentEquals(((DADNASequence) mp.getTarget()).getName())) {
                         System.out.println("\n\n\n*********ERROR in name");
                     }
                     if (h.getTargetProperties().getCoords().getStart() - mp.getTargetCoordinates().getStart() != 0) {
                         System.out.println("\n\n\n*********ERROR in start coord");
                     }
                     if (h.getTargetProperties().getCoords().getEnd() - mp.getTargetCoordinates().getEnd() != 0) {
                         System.out.println("\n\n\n*********ERROR in end coord");
                     }
                     if (!h.getTargetProperties().getCoords().getStrand().equals(mp.getTargetCoordinates().getStrand())) {
                         System.out.println("\n\n\n*********ERROR in strande");
                     }

                 }
             } else {
                 System.out.println("");
             }
             System.out.println("___________________________________________________");
             System.out.println("");
         }
	}

	private DataTableSpec make_output_spec(DataTableSpec spec) {
		DataColumnSpec[] cols = new DataColumnSpec[1];
		cols[0] = new DataColumnSpecCreator("Sequence ID", StringCell.TYPE).createSpec();
		
		return new  DataTableSpec(cols);
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

        // TODO: generated method stub
        return new DataTableSpec[]{null};
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
			 rc.setDBByFile(new File("c:/temp/ensembledb.properties"));
			 rc.setSchemaByFile(null);
			 DBRegistry eReg = new DBRegistry(rc, true);
			 Collection<DBSpecies> sp =  eReg.getSpecies();
			 ArrayList<String> ret = new ArrayList<String>();
			 for (DBSpecies s : sp) {
				 ret.add(s.getDisplayName());
			 }
			 Collections.sort(ret);
			 return ret.toArray(new String[0]);
		 } catch (Exception e) {
			 e.printStackTrace();
			 return new String[] { "Homo Sapiens", "Arabidopsis thaliana" };
		 }
	}

}

