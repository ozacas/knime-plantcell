package au.edu.unimelb.plantcell.io.jemboss;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.image.png.PNGImageCell;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.InvalidSettingsException;

import au.edu.unimelb.plantcell.io.jemboss.local.AbstractTableMapper;
import au.edu.unimelb.plantcell.io.jemboss.settings.GraphSetting;
import au.edu.unimelb.plantcell.io.jemboss.settings.ProgramSetting;


public class GraphUnmarshaller implements UnmarshallerInterface {
	private final HashMap<GraphSetting, String> setting2column = new HashMap<GraphSetting,String>();
	
	@Override
	public void addColumns(AbstractTableMapper atm,
			ProgramSetting for_this_setting) {
		
		/**
		 * EMBOSS programs with a "-graph png" option usually dont let you create the filename so
		 * you must dig thru the stdout of the invocation to see what the created file was...
		 */
		if (for_this_setting instanceof GraphSetting) {
			GraphSetting gs = (GraphSetting)for_this_setting;
			String   format = gs.getGraphFormat().toUpperCase();
			List<DataColumnSpec> al = new ArrayList<DataColumnSpec>();
			String colname = "File created by EMBOSS: "+format;
			if (format.equals("PNG")) {
				al.add(new DataColumnSpecCreator(colname, DataType.getType(PNGImageCell.class)).createSpec());
				atm.addFormattedColumns(for_this_setting, al);
			} else if (format.equals("SVG")) {
				al.add(new DataColumnSpecCreator(colname, DataType.getType(XMLCell.class)).createSpec());
				atm.addFormattedColumns(for_this_setting, al);
			} else {
				// no support for this format inside knime (for now), so...
				return;
			}
			setting2column.put(gs, colname);
		}
	}

	@Override
	public void processUnknown(ProgramSetting for_this, List<File> files, AbstractTableMapper atm)
			throws IOException, InvalidSettingsException {
		if (!(for_this instanceof GraphSetting)) {
			throw new InvalidSettingsException("BUG: cannot unmarshal a non-graphics file using GraphUnmarshaller::processUnknown()");
		}
		GraphSetting gs = (GraphSetting) for_this;
		
		Map<String, DataCell> created_files_map = new HashMap<String,DataCell>();
		created_files_map.put("RowID", new StringCell(atm.getCurrentRow()));
		
		for (File f : files) {
			if (f.getName().toLowerCase().endsWith("." + gs.getGraphFormat())) {
				FileInputStream fis = new FileInputStream(f);
				long len = f.length();
				byte[] bytes = new byte[(int) len];
				int got = fis.read(bytes);
				fis.close();
				
				if (gs.isPNG() && got >= len) {
					created_files_map.put(gs.getColumnName(), new PNGImageContent(bytes).toImageCell() );
					f.delete();
				} else if (gs.isSVG() && got >= len) {
					try {
						DataCell xc = XMLCellFactory.create(new String(bytes));
						created_files_map.put(gs.getColumnName(), xc);
					} catch (Exception e) {
						throw new IOException("Bad SVG (XML) data: "+e.getMessage());	
					}
				} else {
					created_files_map.put(gs.getColumnName(), DataType.getMissingCell());
				}
			}
		}
				
		atm.setFormattedCells(created_files_map);
		atm.emitFormattedRow();
	}

	@Override
	public void processKnown(ProgramSetting for_this, File out_file,
			AbstractTableMapper atm) throws IOException,
			InvalidSettingsException {
		// NO-OP for now (most graphs the filename is unknown)
	}
}
