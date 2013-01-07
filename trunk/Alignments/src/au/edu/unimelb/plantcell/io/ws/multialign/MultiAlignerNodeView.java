package au.edu.unimelb.plantcell.io.ws.multialign;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.ExternalApplicationNodeView;

import au.edu.unimelb.plantcell.core.CorePlugin;
import au.edu.unimelb.plantcell.core.ExternalProgram;
import au.edu.unimelb.plantcell.core.PreferenceConstants;

/**
 * <code>NodeView</code> for the "MultiAligner" Node.
 * 
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class MultiAlignerNodeView extends ExternalApplicationNodeView<MultiAlignerNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link MultiAlignerNodeModel})
     */
    protected MultiAlignerNodeView(final MultiAlignerNodeModel nodeModel) {
        super(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
    	// HACK: jalview isnt notified of a change in data...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // TODO: generated method stub
    }

	@Override
	protected void onOpen(String title) {
		final JFrame jf = new JFrame("Select row to display alignment from: ");
		jf.setLayout(new BorderLayout());
		final MultiAlignerNodeModel mdl = getNodeModel();
		String[] rows = mdl.getRowIDs();
		if (rows.length < 1) {		// nothing to display?
			Logger.getAnonymousLogger().warning("No rows to display!");
			return;
		}
		final JList    my_list = new JList(rows);
		final JButton b_start  = new JButton("Start JalView...");
		IPreferenceStore prefs = CorePlugin.getDefault().getPreferenceStore();
		String jalview_dir     = prefs.getString(PreferenceConstants.PREFS_JALVIEW_FOLDER); 
		final String jre_dir   = prefs.getString(PreferenceConstants.PREFS_JRE_FOLDER);
		final Logger l = Logger.getLogger("Alignment View");
		if (!new File(jre_dir).isDirectory()) {
			l.warning("Cannot find Java in: "+jre_dir+" - see KNIME PlantCell preferences!");
			return;
		}
		final JTextField tf = new JTextField(jalview_dir, 40);

		b_start.setEnabled(false);
		b_start.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				List<String> args = new ArrayList<String>();
				File java_prog = ExternalProgram.find(jre_dir, "java");
				if (java_prog == null) {
					l.warning("Unable to locate java in "+jre_dir+" - fix your preferences!");
					return;
				}
				args.add(java_prog.getAbsolutePath());
				File folder = new File(tf.getText());
				File lib_dir = new File(folder.getAbsolutePath(), "lib");
				args.add("-Djava.ext.dirs="+lib_dir.getAbsolutePath().replaceAll("\\\\", "/"));
				args.add("-jar");
				args.add(new File(folder, "jalview.jar").getAbsolutePath().replaceAll("\\\\", "/"));
				args.add("-open");
				AlignmentValue av = mdl.getAlignment((String)my_list.getSelectedValue());
				Process kid = null;

				try {
					File f = File.createTempFile("multialign", ".aln");
					FileWriter fw = new FileWriter(f);
					fw.write(av.getClustalAlignment());
					fw.close();
					f.deleteOnExit();
					args.add(f.getAbsolutePath());
					ProcessBuilder pb = new ProcessBuilder(args);
					pb.directory(f.getParentFile());
					pb.redirectErrorStream(true);
				
					kid = pb.start();
				} catch (Exception e) {
					e.printStackTrace();
					if (kid != null) {
						kid.destroy();
						kid = null;
					}
				} finally {
					jf.dispose();
					if (kid != null) {
						String line;
						BufferedReader stdout = new BufferedReader(new InputStreamReader(kid.getInputStream()));
						StringBuffer sb = new StringBuffer(100 * 1024);
						try {
							while ((line = stdout.readLine()) != null) {
								sb.append(line);
								sb.append('\n');
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
		});
		my_list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent ev) {
				String sel_row = (String) my_list.getSelectedValue();
				b_start.setEnabled(sel_row != null);
			}
			
		});
		if (rows.length == 1) {
			my_list.setSelectedIndex(0);
		}
		
		jf.add(new JScrollPane(my_list), BorderLayout.CENTER);
		JPanel south = new JPanel();
		south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
		JPanel jalview_panel = new JPanel();
		jalview_panel.setLayout(new BoxLayout(jalview_panel, BoxLayout.X_AXIS));
		jalview_panel.add(new JLabel("JalView folder:"));
		jalview_panel.add(tf);
		
		JButton b_browse = new JButton("Browse...");
		b_browse.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				File f = new File(tf.getText());
				JFileChooser jfc = null;
				if (f.exists() && f.isDirectory()) {
					jfc = new JFileChooser(f);
				} else {
					jfc = new JFileChooser();
				}
				jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (jfc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					File chosen = jfc.getSelectedFile();
					if (chosen.exists() && chosen.isDirectory()) {
						tf.setText(chosen.getAbsolutePath());
					}
				}
			}
			
		});
		jalview_panel.add(b_browse);
		south.add(jalview_panel);
		south.add(b_start);
		
		jf.add(south, BorderLayout.SOUTH);
		jf.pack();
		jf.setAlwaysOnTop(true);
		jf.setVisible(true);
	}

}

