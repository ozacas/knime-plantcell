package au.edu.unimelb.plantcell.core;



public class UniqueID {
	public static int cur = 1;
	private final int m_id;
	
	public UniqueID(boolean restart) {
		if (restart) 
			restart();
		m_id = cur++;
	}
	
	public UniqueID() {
		this(false);
	}
	
	public UniqueID(String str) {
		m_id = Integer.valueOf(str.substring(1));
	}

	private static synchronized void restart() {
		cur = 1;
	}
	
	public String toString() {
		return "S"+m_id;
	}

	public boolean equals(Object o) {
		if (o instanceof UniqueID) {
			return ((UniqueID)o).m_id == this.m_id;
		} else if (o instanceof String) {
			return o.toString().equals(this.toString());
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	public static boolean smellsOk(String str) {
		return str.startsWith("S");
	}
}
