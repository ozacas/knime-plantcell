package au.edu.unimelb.plantcell.io.read.spectra;

import javax.swing.Icon;

import au.edu.unimelb.plantcell.io.read.spectra.SpectraValue;

import org.expasy.jpl.core.ms.spectrum.PeakList;
import org.expasy.jpl.core.ms.spectrum.peak.Peak;
import org.expasy.jpl.io.ms.jrap.Scan;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.data.renderer.DefaultDataValueRendererFamily;

/**
 * Contains important methods related to spectra cells used widely. Care must be taken to understand the KNIME platform
 * and backward compatibility before changing these methods.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 *
 */
public class SpectraUtilityFactory extends UtilityFactory {
	 private static int spectra_id = 1;
	 
     private static final Icon ICON =
             loadIcon(SpectraValue.class, "spectra-icon-16x16.png");
     
     public static DataCell createCell(BasicPeakList bpl) {
    	 return new BasicSpectraCell(bpl);
     }
     
     /**
      * Preferred constructor if you have a valid <code>PeakList</code> instance. This method copies the headers
      * (title, pepmass, charge etc.) from the input <code>sv</code> instance, to ensure that the newly
      * created spectra cell has similar fields. Copy constructor.
      * @param pl
      * @param sv
      * @return
      */
     public static DataCell createCell(PeakList pl, SpectraValue sv) {
    	 DataCell dc = createCell(pl);
    	 if (dc instanceof BasicSpectraCell && sv instanceof BasicSpectraCell) {
    		 ((BasicSpectraCell)dc).initHeaders((BasicSpectraCell)sv);
    	 }
    	 return dc;
     }
     
     /**
      * Use of this factory method is discouraged: it creates an "annotation free" spectra cell, which generally
      * is not what the user will want. It is provided for completeness.
      * 
      * @param pl
      * @return
      */
 	 public static DataCell createCell(PeakList pl) {
 		BasicPeakList bpl = new BasicPeakList(pl);
 		Peak precursor = pl.getPrecursor();
 		if (precursor != null) {
 			int charge = precursor.getCharge();
 			if (charge > 0)
 				bpl.setCharge(String.valueOf(precursor)+"+");
 			double mz = precursor.getMz();
 			if (mz > 0.0)
 				bpl.setPepMass(String.valueOf(mz));
 			String title = "";
 			if (precursor.getRT() != null)
 				title += "RT="+precursor.getRT()+" ";
 			bpl.setTitle(title);
 		}
 		
 		return createCell(bpl);
 	 }
 	 
 	
     public static DataCell createCell(Scan scn, String id) {
    	 if (id == null) {
    		 id = "Spectra" + spectra_id;
    		 spectra_id++;
    	 }
    	 return (scn.getHeader().getMsLevel() > 1) ? 
    				 new mzMLSpectraCell(scn, id) : DataType.getMissingCell();
     }
     
     /** {@inheritDoc} */
     @Override
     public Icon getIcon() {
         return ICON;
     }
     
     /** {@inheritDoc} */
     @Override
     protected DataValueRendererFamily getRendererFamily(
             final DataColumnSpec spec) {
         return new DefaultDataValueRendererFamily(
                 new SpectraStringRenderer(),
                 new SpectraTop10Renderer(),
                 new SpectraVisualRenderer(),
                 new SpectraPeakIntensityHistogramRenderer(),
                 new SpectraBitVectorRenderer("Spectra M/Z map (iTRAQ 8-plex region, no thres., 0.05u)", 113.0, 121.2, 0.0, 0.05),
                 new SpectraBitVectorRenderer("Spectra M/Z map (iTRAQ 8-plex region, thres. > 20, 0.05u)", 113.0, 121.2, 20.0, 0.05),
                 new SpectraBitVectorRenderer("Spectra M/Z map (low region, no thres., 0.1u)", 100.0, 600.0, 0.0, 0.1),
                 new SpectraBitVectorRenderer("Spectra M/Z map (low region, thres. > 20, 0.1u)", 100.0, 600.0, 20.0, 0.1),
                 new SpectraBitVectorRenderer("Spectra M/Z map (entire spectrum, no thres., 1u)", 0.0, 2000.0, 0.0, 1.0),
                 new SpectraBitVectorRenderer("Spectra M/Z map (entire spectrum, thres. > 20, 1u)", 0.0, 2000.0, 20.0, 1.0)
         );
                 
     }
 
     /** {@inheritDoc} */
     @Override
     protected DataValueComparator getComparator() {
         return new DataValueComparator() {
             /** {@inheritDoc} */
             @Override
             protected int compareDataValues(final DataValue v1,
                     final DataValue v2) {
                 // TODO... how to compare spectra... number of peaks?
            	 return 0;
             }
         };
     }

}
