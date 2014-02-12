package au.edu.unimelb.plantcell.core.cells;

import java.util.Arrays;

/**
 * Denotes nucleotide or amino acid sequence (unknown is permissible but not recommended)
 * 
 * @author andrew.cassin
 *
 */
public enum SequenceType {
	// permits anything but user is responsible... ;)
	UNKNOWN {
		public String toString() {
			return "Unknown";
		}
	},
	
	// [ACGTN]+ and IUPAC codes where ambiguous
	DNA {
		public String toString() {
			return "DNA+IUPAC";
		}
		
	},
	
	// [ACGTNU]+ and IUPAC codes
	Nucleotide {
		public String toString() {
			return "Nucleotides+IUPAC";
		}
	},
	
	// [ACGUN]+ only
	RNA {
		public String toString() {
			return "RNA+IUPAC";
		}
	},
	
	// amino acids single letters only and ambiguous symbols [B, J, X]
	AA {
		public String toString() {
			return "Amino Acids";
		}
	};
	
	public static SequenceType getValue(String str) {
		for (SequenceType st : values()) {
			if (str.equals(st.toString())) {
				return st;
			}
		}
		return SequenceType.UNKNOWN;
	}

	public static String[] getSeqTypes() {
		String[] seqtypes = new String[SequenceType.values().length];
		int idx=0;
		for (SequenceType st : SequenceType.values()) {
			seqtypes[idx++] = st.toString();
		}
		Arrays.sort(seqtypes);
		return seqtypes;
	}

	/**
	 * Careful: this method returns true if it is explicitly specified as DNA *OR* if it is only nucleotides of unknown alphabet
	 * @return
	 */
	public boolean isDNA() {
		return (this.equals(DNA) || this.equals(Nucleotide));
	}
	
	public boolean isRNA() {
		return this.equals(RNA);
	}
	
	public boolean isProtein() {
		return (this.equals(AA));
	}

	public boolean isNucleotides() {
		return (isDNA() || isRNA());
	}
}
