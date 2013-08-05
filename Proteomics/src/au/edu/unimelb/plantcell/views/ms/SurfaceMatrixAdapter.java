package au.edu.unimelb.plantcell.views.ms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.la4j.matrix.Matrix;

/**
 * we dont implement interface <code>Matrix</code> as it just has too many methods. Just provide a method for
 * getting at the underlying Matrix instead. This adapter stores MS surface state apart from the binned
 * histogram itself.
 * 
 * @author andrew.cassin
 *
 */
public class SurfaceMatrixAdapter  {
	private Matrix m_matrix;
	private double m_xmin, m_xmax, m_ymin, m_ymax;
	private String m_key;
	private boolean m_isms2;
	
	public SurfaceMatrixAdapter(Matrix m) {
		this(m, 0.0, 1.0, 0.0, 1.0, false);
	}
	
	public SurfaceMatrixAdapter(Matrix m, double x_min, double x_max, double y_min, double y_max, boolean is_ms2) {
		assert(m != null);
		m_matrix = m;
		m_isms2  = is_ms2;
		setBounds(x_min, x_max, y_min, y_max);
		setKey(null);
	}
	
	public SurfaceMatrixAdapter(Matrix m, boolean b) {
		this(m);
		m_isms2 = b;
	}

	/***************************** Surface Matrix methods ***********************************/
	
	public void setBounds(double x_min, double x_max, double y_min, double y_max) {
		m_xmin = x_min;
		m_xmax = x_max;
		m_ymin = y_min;
		m_ymax = y_max;
	}

	public void setBounds(final SurfaceMatrixAdapter in) {
		assert(in != null);
		setBounds(in.getXMin(), in.getXMax(), in.getYMin(), in.getYMax());
	}

	
	public double getXMin() {
		return m_xmin;
	}
	
	public double getXMax() {
		return m_xmax;
	}
	
	public double getYMin() {
		return m_ymin;
	}
	
	public double getYMax() {
		return m_ymax;
	}
	
	public String getKey() {
		return m_key;
	}
	
	public void setKey(String new_key) {
		if (new_key == null) {
			new_key = makeKey(getYMin(), getYMax(), getXMin(), getXMax());
		}
		m_key = new_key;
	}
	
	public Matrix getMatrix() {
		return m_matrix;
	}
	
	/**
	 * Compute a hash key for a <code>this</code> matrix over the specified bounds. Silly parameters which need to be fixed at some stage.
	 * @param rt_min
	 * @param rt_max
	 * @param mz_min
	 * @param mz_max
	 * @param is_ms2
	 * @return
	 */
	public String makeKey(double rt_min, double rt_max, double mz_min,double mz_max) {
		return "matrix@"+this.hashCode()+"-"+this.rows()+"x"+this.columns()+" ms2="+m_isms2+" "+rt_min+","+rt_max+","+mz_min+","+mz_max;
	}
	
	/******** convenience Matrix delegates ********/
	public int rows() {
		return m_matrix.rows();
	}
	
	public int columns() {
		return m_matrix.columns();
	}

	public double get(int r, int c) {
		return m_matrix.get(r,c);
	}
	
	public void set(int r, int c, double val) {
		m_matrix.set(r,  c, val);
	}

	public Matrix copy() {
		return m_matrix.copy();
	}

	public void saveInternals(PrintWriter pw) throws IOException {
		assert(pw != null);
		// save the key
		pw.println(getKey());
		
		// save the bounds
		pw.println(getXMin());
		pw.println(getXMax());
		pw.println(getYMin());
		pw.println(getYMax());
	}

	public void readInternals(BufferedReader rdr) throws NumberFormatException,IOException {
		// load the key
		String key = rdr.readLine();
		setKey(key);
		// load the bounds
		Double x_min = Double.valueOf(rdr.readLine());
		Double x_max = Double.valueOf(rdr.readLine());
		Double y_min = Double.valueOf(rdr.readLine());
		Double y_max = Double.valueOf(rdr.readLine());
		setBounds(x_min.doubleValue(), x_max.doubleValue(), y_min.doubleValue(), y_max.doubleValue());
	}
}
