package au.edu.unimelb.plantcell.io.jemboss;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.biojava.bio.BioException;
import org.biojava.bio.program.gff.GFFEntrySet;
import org.biojava.bio.program.gff.GFFTools;
import org.biojava.utils.ParserException;
import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.io.jemboss.local.AbstractTableMapper;
import au.edu.unimelb.plantcell.io.jemboss.settings.ProgramSetting;

/**
 * Unmarshalls a Generic Feature Format (GFF) annotated file. See
 * http://en.wikipedia.org/wiki/GFF for more details.
 * Uses biojava to do the actual reading, so it must be compatible with
 * the underlying biojava implementation (which is currently v1.8 until biojava v3 is completed).
 * 
 * @author andrew.cassin
 *
 */
public class GFFUnmarshaller implements UnmarshallerInterface {

	@Override
	public void addColumns(AbstractTableMapper atm,
			ProgramSetting for_this_setting) {

	}

	@Override
	public void processKnown(ProgramSetting for_this,
			File f, AbstractTableMapper atm)
			throws IOException, InvalidSettingsException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		try {
			@SuppressWarnings("unused")
			GFFEntrySet es = GFFTools.readGFF(br);
		} catch (ParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BioException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void processUnknown(ProgramSetting for_this, List<File> filenames,
			AbstractTableMapper atm) throws IOException,
			InvalidSettingsException {
		// TODO Auto-generated method stub
		
	}

}
