package au.edu.unimelb.plantcell.core;

import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.knime.core.node.NodeLogger;

/**
 * Convenience wrapper around apache commons exec which ensures subcommands are run
 * in a separate thread to ensure correct interaction with the KNIME platform.
 * 
 * @author acassin
 *
 */
public class ExecutorUtils {
	private final NodeLogger logger;
	private MyRunnable r;
	
	public ExecutorUtils() {
		this(new DefaultExecutor(), NodeLogger.getLogger("ExecutorUtils"));
	}
	
	public ExecutorUtils(final NodeLogger l) {
		this(new DefaultExecutor(), l);
	}
	
	public ExecutorUtils(final DefaultExecutor de, final NodeLogger l) {
		assert(de != null && l != null);
		r = new MyRunnable(de);
		logger = l;
	}
	
	public int run(final CommandLine cl) {
		return run(cl, null);
	}
	
	public int run(final CommandLine cl, final DefaultExecuteResultHandler erh) {
		assert(cl != null && r != null);
		r.setCommandLine(cl);
		if (erh != null) {
			r.setResultsHandler(erh);
		}
		Thread t = new Thread(r);
		try {
			t.start();
			t.wait();
			return r.getExitStatus();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	private class MyRunnable implements Runnable {
		private DefaultExecutor exec;
		private CommandLine cl;
		private DefaultExecuteResultHandler results_handler;
		private int exit_status = -1;
		
		public MyRunnable(final DefaultExecutor de) {
			assert(de != null);
			exec = de;
			results_handler = null;
		}
		
		public void setCommandLine(final CommandLine c) {
			assert(c != null);
			cl = c;
		}
		
		public void setResultsHandler(final DefaultExecuteResultHandler erh) {
			results_handler = erh;
		}
		
		public int getExitStatus() {
			return exit_status;
		}
		
		@Override
		public void run() {
			exit_status = -1;
			logger.info("Running: "+cl.toString());
			try {
				if (results_handler != null) {
					exec.execute(cl, results_handler);
					results_handler.waitFor();
					exit_status = -1;
				} else {
					exit_status = exec.execute(cl);
				}
				logger.info("Got exit status: "+exit_status);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
		}
		
	}
}
