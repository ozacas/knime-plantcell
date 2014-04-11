package au.edu.unimelb.plantcell.io.ws.multialign;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.ExternalApplicationNodeView;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;

import au.edu.unimelb.plantcell.core.CorePlugin;
import au.edu.unimelb.plantcell.core.ExternalProgram;
import au.edu.unimelb.plantcell.core.Preferences;

/**
 * View for models which support the {@link AlignmentViewDataModel} interface
 * 
 *
 * @author http://www.plantcell.unimelb.edu.au/bioinformatics
 */
public class AlignmentNodeView extends ExternalApplicationNodeView<NodeModel>{
	private final NodeLogger logger = NodeLogger.getLogger("Alignment View");
	private final AlignmentViewDataModel m_model;
	
    /**
     * Creates a new view. This class cannot rely on the specific instance of the model model, but may rely
     * on the model implementing AlignmentViewDataModel, as there are many alignment related nodes.
     * 
     * @param <T>
     * 
     * @param nodeModel for example: {@link MultiAlignerNodeModel})
     */
    public AlignmentNodeView(final NodeModel nodeModel) {
        super(nodeModel);
        if (nodeModel != null && nodeModel instanceof AlignmentViewDataModel) {
        	m_model = (AlignmentViewDataModel) nodeModel;
        } else {
        	m_model = null;
        }
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
        // no-op, PlantCell doesnt talk to jalview in any way (separate processes)
    }

	@Override
	protected void onOpen(String title) {
		// nothing to do if no alignments are available...
		if (hasNoOrEmptyModel()) {
			logger.warn("No available data - re-execute node?");
			return;
		}
		List<String>       rows= getAlignmentRowIDs();
		
		// just display the only available alignment (no point asking the user which one)...
		if (hasExactlyOneAlignment()) {
			showAlignment(getAlignment(rows.get(0)));
			return;
		}
		
		// else...
		final JFrame jf = new JFrame("Select row to display alignment from: ");
		jf.setLayout(new BorderLayout());
		final JList<String>    my_list = new JList<String>(rows.toArray(new String[0]));
		final JButton b_start  = new JButton("Open alignment in JalView...");
		
		final JTextField tf = new JTextField(getJalViewRootFolder().getAbsolutePath(), 40);

		b_start.setEnabled(false);
		b_start.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				AlignmentValue av = getAlignment(my_list.getSelectedValue());
				showAlignment(av);
			}

		});
		my_list.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent ev) {
				String sel_row = (String) my_list.getSelectedValue();
				b_start.setEnabled(sel_row != null);
			}
			
		});
		if (rows.size() == 1) {
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
	
	protected void showAlignment(AlignmentValue av) {
		if (av == null)
			return;		// do nothing if no alignment produced from model...
		try {
			CommandLine cl = makeCommandLine();
			File f = makeTemporaryAlignmentFile(av);
			addJalViewOpenArguments(cl, f);
			DefaultExecutor de = new DefaultExecutor();
			logger.info("Running jalview: "+cl.toString());
			de.execute(cl);
		} catch (Exception ex) {
			logger.warn(ex.getMessage());
			ex.printStackTrace();
		} finally {
			// NO-OP for now...
		}
	}

	/**
	 * Creates a temporary file (which will be deleted on exit) to hold the specified alignment. Will throw
	 * an exception if the alignment isnt valid (eg. null or empty)
	 * @param av
	 * @return the File is clustal alignment format. Probably not the best format with character length restrictions on the ID's...
	 * @throws IOException
	 */
	protected File makeTemporaryAlignmentFile(final AlignmentValue av) throws IOException {
		 if (av == null || av.getSequenceCount() < 1)
			 throw new IOException("Invalid (empty) alignment!");
		 
		 File f = File.createTempFile("multialign", ".aln");
         FileWriter fw = new FileWriter(f);
         fw.write(av.getClustalAlignment());
         fw.close();
         f.deleteOnExit();
         return f;
	}

	protected void addJalViewOpenArguments(final CommandLine cl, final File f) {
		assert(cl != null && f != null);
		cl.addArgument("-open");
		cl.addArgument(f.getAbsolutePath());
	}

	private IPreferenceStore getPlantCellPreferences() {
		IPreferenceStore prefs = CorePlugin.getDefault().getPreferenceStore();
		return prefs;
	}
	
	/**
	 * returns a {@link CommandLine} instance ready for use with Apache Commons Exec with jalview (hopefully) found.
	 * This method tries two different ways to invoke jalview (and sets up the return result accordingly):
	 * 1) tries to locate a program jalview (or jalview.exe) in various locations in common use
	 * 2) if (1) fails, then it tries to locate jalview.jar and invoke java.exe on it
	 * In either case the command line is return for a -open <filename> argument to be added just prior to execution
	 */
	public CommandLine makeCommandLine() throws IOException {
		List<File> paths = new ArrayList<File>();
		paths.addAll(ExternalProgram.addSystemPathExecutablePaths());
		paths.addAll(ExternalProgram.addPlausibleFolders("Jalview"));
		
		File jalview = ExternalProgram.find(new String[] { "jalview", "jalview.exe", "Jalview", "JalView.exe"},
				paths.toArray(new File[0]) );
		
		if (jalview != null) {
			return makeJalViewExecutableCommandLine(jalview);
		} else {
			File jalview_jar = ExternalProgram.find(new String[] {"jalview.jar", "Jalview.jar" }, paths.toArray(new File[0]));
			if (jalview_jar == null) {
				throw new IOException("Cannot find either jalview (executable) or jalview.jar - is it installed?");
			}
			return makeJalViewJarCommandLine(jalview_jar);
		}
	}
	
	/**
	 * Given a valid File instance to jalview.jar, this returns a command line which invokes java -jar on it...
	 * @param jalview_jar
	 * @return
	 */
	private CommandLine makeJalViewJarCommandLine(final File jalview_jar) throws IOException {
		File java = getJavaExecutable();
		if (java == null) {
			throw new IOException("Cannot find java!");
		}
		CommandLine cl = new CommandLine(java);
		// make sure jalview.jar can find its dependent jars...
		File lib_dir = new File(jalview_jar.getParentFile(), "lib");
		cl.addArgument("-Djava.ext.dirs="+lib_dir.getAbsolutePath().replaceAll("\\\\", "/"));
		cl.addArgument("-jar");
		cl.addArgument(jalview_jar.getAbsolutePath());
		return cl;
	}

	/**
	 * We found a wrapper executable as specified by jalview and this returns the corresponding commandline instance
	 * 
	 * @param jalview
	 * @return
	 * @throws IOException
	 */
	private CommandLine makeJalViewExecutableCommandLine(final File jalview) throws IOException {
		if (jalview == null)
			throw new IOException("Cannot find main program for jalview");
		CommandLine cl = new CommandLine(jalview);
		return cl;
	}

	/**
	 * Return the root folder of the jalview installation eg. c:\program files\jalview or ./Jalview if it cannot be found
	 * 
	 * @return never null
	 */
	private File getJalViewRootFolder() {
		String jalview_dir     = getPlantCellPreferences().getString(Preferences.PREFS_JALVIEW_FOLDER);
		File ret = new File(jalview_dir);
		if (!ret.exists()) {
			// make up a default... rather than return something which is wrong and hope the user spots the problem
			return new File("./Jalview");
		}
		return ret;
	}
	
	private File getJavaExecutable() {
		final File java = ExternalProgram.find(new String[] {"java.exe", "java", "javaw"}, 
				new File[] { new File(getPlantCellPreferences().getString(Preferences.PREFS_JRE_FOLDER)), 
				getImputedJREFolder()});
	
		if (!java.exists()) {
			return null;
		}
		return java;
	}
	
	private File getImputedJREFolder() {
		File java_home         = new File(System.getProperty("java.home"));
		File imputed_jre_folder= new File(java_home, "bin");
		return imputed_jre_folder;
	}

	private AlignmentValue getAlignment(final String row_id) {
		assert(row_id != null);
		return m_model.getAlignment(row_id);
	}
	

	private List<String> getAlignmentRowIDs() {
		return m_model.getAlignmentRowIDs();
	}

	private boolean hasNoOrEmptyModel() {
		if (m_model == null)
			return true;
		return (m_model.getAlignmentRowIDs().size() < 1);
	}

	private boolean hasExactlyOneAlignment() {
		if (hasNoOrEmptyModel())
			return false;
		return (m_model.getAlignmentRowIDs().size() == 1);
	}
}

