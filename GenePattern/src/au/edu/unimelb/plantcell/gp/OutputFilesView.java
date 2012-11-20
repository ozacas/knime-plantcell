package au.edu.unimelb.plantcell.gp;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.core.node.ExternalApplicationNodeView;

/**
 * A genepattern analysis will typically generate many files, each of which needs
 * to be accessible to a knime user. This view is the primary way to do this. Many
 * nodes use it, so it must be unaware of the specifics of GenePattern KNIME nodes.
 * 
 * @author andrew.cassin
 *
 */
public class OutputFilesView<T extends AbstractGPNodeModel> extends ExternalApplicationNodeView<T> {
	private final JFrame m_jf = new JFrame("Output files");
	private AbstractGPNodeModel m_mdl = null;
	private JList  m_list = null;
	
	protected OutputFilesView(T mdl) {
		super(mdl);
		m_mdl = mdl;
	}

	@Override
	protected void onClose() {
		
	}

	@Override
	protected void onOpen(String s) {
		m_jf.setLayout(new BorderLayout());
		JPanel button_panel = new JPanel();
		ArrayList<String> items = new ArrayList<String>();
		if (m_mdl != null && m_mdl.hasOutputFiles()) {
			try {
				m_mdl.getOutputFiles(items);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		m_jf.setTitle("Output files: "+s);
		m_list = new JList(items.toArray(new String[0]));
		button_panel.setLayout(new GridLayout(3,1));
		final JButton view_button = new JButton("View");
		view_button.addActionListener(new ActionListener() {

			@SuppressWarnings("unused")
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String sel = (String) m_list.getSelectedValue();
				if (sel == null)
					return;
				
				// first must fetch file from the GP server
				if (sel.lastIndexOf("/") >= 0) {
					sel = sel.substring(sel.lastIndexOf("/"));
				}
				Pattern p = Pattern.compile("^(.*)(\\.\\w+)$");
				Matcher m = p.matcher(sel);
				String prefix = "genepattern_download";
				String suffix = ".tmp";
				if (m.matches()) {
					prefix = m.group(1);
					suffix = m.group(2);
				} 
				
				try {
					File f = m_mdl.downloadFile(sel);
					if (Desktop.isDesktopSupported()) {
						Desktop.getDesktop().open(f);
						f.deleteOnExit();		// NB: user must save temp file themselves...
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		});
		button_panel.add(view_button);
		button_panel.add(Box.createVerticalGlue());

		final JButton save_as_button = new JButton("Save as...");
		save_as_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String sel = (String) m_list.getSelectedValue();
				if (sel == null)
					return;
				try {
					File f = m_mdl.downloadFile(sel);
					JFileChooser fc = new JFileChooser();
					fc.setDialogType(JFileChooser.SAVE_DIALOG);
					fc.setDialogTitle("Save "+sel+" as...");
					if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
						FileInputStream in = new FileInputStream(f);
						FileOutputStream out = new FileOutputStream(fc.getSelectedFile());
						int in_cnt;
						byte[] tmp = new byte[8192];
						while ((in_cnt = in.read(tmp)) > 0) {
							out.write(tmp, 0, in_cnt);
						}
						out.close();
						in.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
			
		});
		button_panel.add(save_as_button);
		
		final JButton save_all_button = new JButton("Save all...");
		save_all_button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					File[] files = m_mdl.downloadFiles();
					JFileChooser fc = new JFileChooser();
					fc.setDialogType(JFileChooser.SAVE_DIALOG);
					fc.setDialogTitle("Save files as...");
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
						for (File f : files) {
							FileInputStream in = new FileInputStream(f);
							FileOutputStream out = new FileOutputStream(
									new File(fc.getSelectedFile(), f.getName()));
							int in_cnt;
							byte[] tmp = new byte[8192];
							while ((in_cnt = in.read(tmp)) > 0) {
								out.write(tmp, 0, in_cnt);
							}
							out.close();
							in.close();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}

		});
		button_panel.add(Box.createVerticalGlue());
		button_panel.add(save_all_button);
		
		
		m_jf.add(button_panel, BorderLayout.WEST);
		m_jf.add(new JScrollPane(m_list), BorderLayout.CENTER);
		m_jf.pack();
		m_jf.setVisible(true);
	}

	@Override
	protected void modelChanged() {
		m_list.setListData(new ArrayList<String>().toArray());
	}
	

}
