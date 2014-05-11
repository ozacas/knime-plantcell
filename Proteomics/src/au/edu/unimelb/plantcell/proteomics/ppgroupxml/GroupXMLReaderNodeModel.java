package au.edu.unimelb.plantcell.proteomics.ppgroupxml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

import au.edu.unimelb.plantcell.core.MyDataContainer;
import au.edu.unimelb.plantcell.core.cells.SequenceCell;


/**
 * This is the model implementation of FastaReader.
 * This nodes reads sequences from the user-specified FASTA file and outputs three columns per sequence: 
 * * n1) Accession 
 * * n2) Description - often not accurate in practice 
 * * n3) Sequence data * n * n
 * Neither line breaks or leading/trailing whitespace are preserved.
 *
 * @author Andrew Cassin
 */
public class GroupXMLReaderNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger.getLogger("ProteinPilot GroupXML");
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
	static final String CFGKEY_XML_FILES    = "group-xml-files";
   
 
    // settings for this node: regular expressions to process the ">" lines, and the fasta sequence filename
    private final SettingsModelStringArray m_xml_files = new SettingsModelStringArray(CFGKEY_XML_FILES, new String[] {});
    

    /**
     * Constructor for the node model.
     */
    protected GroupXMLReaderNodeModel() {
        super(0, 3); // output ports only
    }

    protected DataTableSpec[] make_output_spec() {   
        // 1. create the column specification in accordance with the as_single parameter
        DataColumnSpec[] peptide_cols = new DataColumnSpec[13];
        peptide_cols[0] = new DataColumnSpecCreator("Spectrum ID", StringCell.TYPE).createSpec();
        peptide_cols[1] = new DataColumnSpecCreator("Spectrum Elution Time", DoubleCell.TYPE).createSpec();
        peptide_cols[2] = new DataColumnSpecCreator("Spectrum Precursor mass", DoubleCell.TYPE).createSpec();
        peptide_cols[3] = new DataColumnSpecCreator("Charge", IntCell.TYPE).createSpec();
        peptide_cols[4] = new DataColumnSpecCreator("DA Delta", DoubleCell.TYPE).createSpec();
        peptide_cols[5] = new DataColumnSpecCreator("E-Value", DoubleCell.TYPE).createSpec();
        peptide_cols[6] = new DataColumnSpecCreator("Modified Peptide", StringCell.TYPE).createSpec();
        peptide_cols[7] = new DataColumnSpecCreator("Unmodified Peptide Sequence", StringCell.TYPE).createSpec();
        peptide_cols[8] = new DataColumnSpecCreator("Confidence", DoubleCell.TYPE).createSpec();
        peptide_cols[9] = new DataColumnSpecCreator("M/Z", DoubleCell.TYPE).createSpec();
        peptide_cols[10]= new DataColumnSpecCreator("Score", DoubleCell.TYPE).createSpec();
        peptide_cols[11]= new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        peptide_cols[12]= new DataColumnSpecCreator("Type of peptide match (advanced users only)", IntCell.TYPE).createSpec();
        
        DataColumnSpec[] protein_cols = new DataColumnSpec[12];
        protein_cols[0] = new DataColumnSpecCreator("Name", StringCell.TYPE).createSpec();
        protein_cols[1] = new DataColumnSpecCreator("Coverage@0% confidence", DoubleCell.TYPE).createSpec();
        protein_cols[2] = new DataColumnSpecCreator("Coverage@95% confidence", DoubleCell.TYPE).createSpec();
        protein_cols[3] = new DataColumnSpecCreator("Coverage@50% confidence", DoubleCell.TYPE).createSpec();
        protein_cols[4] = new DataColumnSpecCreator("Sequence", SequenceCell.TYPE).createSpec();
        protein_cols[5] = new DataColumnSpecCreator("Protein Score", DoubleCell.TYPE).createSpec();
        protein_cols[6] = new DataColumnSpecCreator("ID", StringCell.TYPE).createSpec();
        protein_cols[7] = new DataColumnSpecCreator("Peptide IDs", ListCell.getCollectionType(StringCell.TYPE)).createSpec();
        protein_cols[8] = new DataColumnSpecCreator("Unique peptides", SetCell.getCollectionType(StringCell.TYPE)).createSpec();
        protein_cols[9] = new DataColumnSpecCreator("Filename", StringCell.TYPE).createSpec();
        protein_cols[10]= new DataColumnSpecCreator("Use Quant?", StringCell.TYPE).createSpec();	// perhaps boolean would be better?
        protein_cols[11]= new DataColumnSpecCreator("Use Type", StringCell.TYPE).createSpec();

        DataColumnSpec[] quant_cols = new DataColumnSpec[4];
        quant_cols[0] = new DataColumnSpecCreator("Spectrum ID", StringCell.TYPE).createSpec();
        quant_cols[1] = new DataColumnSpecCreator("Centroid m/z", DoubleCell.TYPE).createSpec();
        quant_cols[2] = new DataColumnSpecCreator("Peak Area", DoubleCell.TYPE).createSpec();
        quant_cols[3] = new DataColumnSpecCreator("Peak Error (%)", DoubleCell.TYPE).createSpec();
        
        return new DataTableSpec[] { new DataTableSpec(peptide_cols), 
        							 new DataTableSpec(protein_cols), new DataTableSpec(quant_cols) };
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

   
      DataTableSpec[] spec = make_output_spec();
      BufferedDataContainer c_peptide = exec.createDataContainer(spec[0]);
      BufferedDataContainer c_protein = exec.createDataContainer(spec[1]);
      BufferedDataContainer c_quant   = exec.createDataContainer(spec[2]);
      MyDataContainer my_peptides = new MyDataContainer(c_peptide, "Peptide");
      MyDataContainer my_proteins = new MyDataContainer(c_protein, "Protein");
      MyDataContainer my_quant    = new MyDataContainer(c_quant, "Quant");
      
      ArrayList<File> input_files = new ArrayList<File>();
      for (String s : m_xml_files.getStringArrayValue()) {
    	  File f = new File(s);
    	  if (f.isFile() && f.canRead()) {
    		  input_files.add(f);
    	  } else {
    		  logger.warn("Unable to read "+f.getAbsolutePath());
    	  }
      }
      
      /**
       * elements which are added to the start_map must also be added to end_map so that
       * the object stack is correctly managed
       */
      HashMap<String,ProteinPilotMatcher> start_map = new HashMap<String,ProteinPilotMatcher>();
      PSMMap         psm = new PSMMap();
      PeptideMap pep_map = new PeptideMap(psm);
      ProteinMap prot_map= new ProteinMap(pep_map);
      start_map.put("SPECTRUM", new SpectrumMatcher());
      start_map.put("MATCH",    new PeptideSpectrumMatcher(psm));
      start_map.put("PEPTIDE",  new PeptideMatcher(pep_map));
      start_map.put("MSMSPEAKS", new MSMSPeakListMatcher());
      start_map.put("ITRAQPEAKS", new iTRAQPeakMatcher());
      start_map.put("PROTEIN",   new ProteinMatcher(prot_map));
      start_map.put("PROTEIN2MATCH", new P2MMatcher());
      start_map.put("COVERAGE", new CoverageMatcher());
      HashSet<String> end_map = new HashSet<String>();
      end_map.add("SPECTRUM");
      end_map.add("MATCH");
      end_map.add("MSMSPEAKS");
      end_map.add("PEPTIDE");
      end_map.add("PROTEIN");
      end_map.add("PROTEIN2MATCH");
      end_map.add("COVERAGE");
      end_map.add("ITRAQPEAKS");

      if (input_files.size() < 1) {
    	  throw new InvalidSettingsException("No valid input XML files from ProteinPilot to read!");
      }
      
      for (File f : input_files) {
    	  logger.info("Processing file: "+f.getAbsolutePath());
    	  FileInputStream in = new FileInputStream(f);
          XMLInputFactory factory = XMLInputFactory.newInstance();
          factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
          XMLStreamReader parser = factory.createXMLStreamReader(in);
          
          /*
           * which XML elements are inside each other's scope?
           */
          Stack<ProteinPilotMatcher> object_stack = new Stack<ProteinPilotMatcher>();
          
          int done = 0;
          for (int event = parser.next();
                  event != XMLStreamConstants.END_DOCUMENT;
                  event = parser.next()) {
        	  
        	  /**
        	   * Cant call getLocalName() unless its an element so...
        	   */
        	  if (event != XMLStreamConstants.START_ELEMENT && event != XMLStreamConstants.END_ELEMENT)
        		  continue;
        	 
        	  String localName = parser.getLocalName();
        	  if (event == XMLStreamConstants.START_ELEMENT && start_map.containsKey(localName)) {
        		  ProteinPilotMatcher pm = start_map.get(localName);
        		  assert(pm != null);
        		  pm.processElement(logger, parser, object_stack);
        		  object_stack.push(pm);
        	  } else if (event == XMLStreamConstants.END_ELEMENT && end_map.contains(localName)) {
        		  ProteinPilotMatcher ppo = object_stack.pop();
        		  ppo.save(logger, my_peptides, my_proteins, my_quant, f);
        		  if (done++ % 1000 == 0) {
        			  System.gc();
        		  }
        	  }
          }
          
          for (ProteinPilotMatcher pm : start_map.values()) {
        	  pm.summary(logger);
          }
          
          parser.close();	// NB: does NOT close the input stream
          in.close();
      }
      
      // once we are done, we close the containers and the return the tables
      return new BufferedDataTable[]{my_peptides.close(), my_proteins.close(), my_quant.close()};
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
        return make_output_spec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_xml_files.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_xml_files.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {    
    	m_xml_files.validateSettings(settings);
    }
   
    protected void saveInternals(final File internDir, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
   
    }
    
    protected void loadInternals(final File internDir, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
    
    }
}

