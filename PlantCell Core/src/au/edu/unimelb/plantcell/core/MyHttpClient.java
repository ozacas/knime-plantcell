package au.edu.unimelb.plantcell.core;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;

/**
 * Implements simple-minded rate limiting based on <code>setQueryLimit</code> calls
 * 
 * @author andrew.cassin
 *
 */
public class MyHttpClient extends HttpClient {
	private int m_one_query_per_x = 5;		// a query every five secs.
	private Date m_last = new Date();
	private Calendar cal = Calendar.getInstance();
	
	@Override 
	public int executeMethod(HostConfiguration hc, HttpMethod m) throws HttpException, IOException {
		m_last = delay_until(m_one_query_per_x, m_last);
		return super.executeMethod(hc, m);
	}
	
	@Override
	public int executeMethod(HttpMethod m) throws HttpException,IOException {
		m_last = delay_until(m_one_query_per_x, m_last);
		return super.executeMethod(m);
	}
	
	@Override
	public int executeMethod(HostConfiguration hc, HttpMethod m, HttpState s) throws HttpException,IOException {
		m_last = delay_until(m_one_query_per_x, m_last);
		return super.executeMethod(hc, m, s);
	}
	
	protected synchronized Date delay_until(int n, Date last_query) {
		assert(last_query != null && n > 0);
		cal.setTime(last_query);
		cal.add(Calendar.SECOND, n);
		Calendar now = Calendar.getInstance();
		if (now.before(cal)) {
			try {
				Thread.sleep(n * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return new Date();
	}
}
