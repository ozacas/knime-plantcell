package au.edu.unimelb.plantcell.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.knime.core.node.NodeLogger;

/**
 * Convenience wrapper around apache commons exec which ensures subcommands are run
 * in a separate thread to ensure correct interaction with the KNIME platform. Supports:
 * 1) environment variables as specified at construction time
 * 2) logging via the supplied logger
 * 3) use of the specified executor instance for execution of command-line
 * 4) run with/without waiting for the invoked program to finish
 * 
 * @author acassin
 *
 */
public class ExecutorUtils {
	private final NodeLogger logger;
	private MyRunnable r;
	private Thread t;
	
	public ExecutorUtils() {
		this(new DefaultExecutor(), NodeLogger.getLogger("ExecutorUtils"), null);
	}
	
	public ExecutorUtils(final NodeLogger l) {
		this(new DefaultExecutor(), l, null);
	}
	
	public ExecutorUtils(final DefaultExecutor de, final NodeLogger l) {
		this(de, l, null);
	}
	
	public ExecutorUtils(final DefaultExecutor de, final NodeLogger l, Map<String,String> environment) {
		assert(de != null && l != null);
		r      = new MyRunnable(de);
		r.setEnvironment(environment);
		logger = l;
		t      = null;
	}
	
	public void runNoWait(final CommandLine cl) {
		r.setCommandLine(cl);
		r.setWaitForCompletion(false);
		r.setResultsHandler(null);
		t = new Thread(r);
		t.start();
	}
	
	public int run(final CommandLine cl) {
		return run(cl, null);
	}
	
	public int run(final CommandLine cl, final DefaultExecuteResultHandler erh) {
		return runWaitForCompletion(cl, erh);
	}
	
	/**
	 * Run the specified command with an optional results handler (if non-null)
	 * @param cl
	 * @param erh
	 * @return 
	 */
	public int runWaitForCompletion(final CommandLine cl, final DefaultExecuteResultHandler erh) {
		assert(cl != null && r != null);
		r.setCommandLine(cl);
		r.setWaitForCompletion(true);
		if (erh != null) {
			r.setResultsHandler(erh);
		}
		t = new Thread(r);
		t.start();
		while (true) {
			try {
				t.join();
				break;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return r.getExitStatus();
	}
	
	/**
	 * Copy an input stream to the specified destination. Tries somewhat hard to cleanup in the event of problems.
	 * 
	 * @param rdr what to read from
	 * @param save_here destination for input stream data
	 * @throws IOException
	 */
	public static void copyFile(final InputStream rdr, final File save_here) throws IOException {
		assert(rdr != null && save_here != null);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(save_here);
			byte[] buf = new byte[128 * 1024];
			int cnt;
			while ((cnt = rdr.read(buf)) >= 0) {
				out.write(buf, 0, cnt);
			}
		} catch (IOException ioe) {
			save_here.delete();
			throw ioe;
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}
	
	private class MyRunnable implements Runnable {
		private DefaultExecutor exec;
		private CommandLine cl;
		private DefaultExecuteResultHandler results_handler;
		private boolean waitForCompletion;
		private int     exit_status;
		private Map<String,String> environment_map;
		
		public MyRunnable(final DefaultExecutor de) {
			assert(de != null);
			exec = de;
			results_handler = null;
			setWaitForCompletion(true);
			setExitStatus(-1);
		}
		
		public void setEnvironment(final Map<String,String> environment) {
			environment_map = environment;
		}
		
		public void setWaitForCompletion(boolean wait) {
			waitForCompletion = wait;
		}
		
		public boolean waitForCompletion() {
			return waitForCompletion;
		}
		
		public void setCommandLine(final CommandLine c) {
			assert(c != null);
			cl = c;
		}
		
		public int getExitStatus() {
			return exit_status;
		}
		
		private void setExitStatus(int exitStatus) {
			exit_status = exitStatus;
		}
		
		public void setResultsHandler(final DefaultExecuteResultHandler erh) {
			results_handler = erh;
		}
		
		@Override
		public void run() {
			exit_status = -1;
			logger.info("Running: "+cl.toString());
			try {
				if (results_handler != null) {
					if (environment_map != null) {
						exec.execute(cl, environment_map, results_handler);
					} else {
						exec.execute(cl, results_handler);
					}
					if (waitForCompletion()) {
						results_handler.waitFor();
						exit_status = results_handler.getExitValue();
					} else {
						exit_status = -1;
					}
				} else {
					if (environment_map != null) {
						exit_status = exec.execute(cl, environment_map);
					} else {
						exit_status = exec.execute(cl);
					}
				}				
			} catch (IOException|InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
}
