package au.edu.unimelb.plantcell.proteomics.proteowizard.convert;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;

/**
 * <code>NodeDialog</code> for the "AnalystWiffConverter" Node.
 * Using a JAX-WS web service, this node converts a wiff file (optionally a .wiff.scan file too) to an open-format and then loads it as per Spectra Reader.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AnalystWiffConverterNodeDialog extends XCaliburRawConverterNodeDialog {

 
    protected AnalystWiffConverterNodeDialog() {
         super();
    }
    
    @Override
    protected String getPrintableFilename() {
    	return "Analyst(tm) WIFF files";
    }
    
    @Override
    public FileFilter getFileFilter() {
    	return new FileFilter() {

			@Override
			public boolean accept(final File arg0) {
				if (arg0.isDirectory())
					return true;
				String fname = arg0.getName().toLowerCase();
				if (fname.endsWith(".wiff")) {
					return true;
				}
				return false;
			}

			@Override
			public String getDescription() {
				return getPrintableFilename();
			}
    		
    	};
    }
}

