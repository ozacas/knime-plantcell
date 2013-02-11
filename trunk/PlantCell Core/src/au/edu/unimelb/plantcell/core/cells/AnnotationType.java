package au.edu.unimelb.plantcell.core.cells;

/**
 * Supported built-in annotation types. Do not change an ENUM name as this will break compatibility
 * with saved workflows.
 * 
 * @author andrew.cassin
 *
 */
public enum AnnotationType implements TrackSummaryInterface {
	
	/*
	 * ******************* WARNING WARNING WARNING ******************
	 * Any change here has a profound impact on serialization/deserialization
	 * so be very careful what you change. In particular, SequenceAnnotation.make()
	 * must implement construction for each type listed here.
	 */
	COMMENTS { 
		public String toString() { return "Comments"; } 
		public String[] labels() { return Track.NO_LABELS; }
	}, 
	
	LABELLED_REGIONS { 
		public String toString() { return "Regions (with labels)"; }
		public String[] labels() { return Track.NO_LABELS; };
	},
	
	TMHMM_REGIONS { 		
		public String[] labels() { return Track.TMHMM_LABELS; };
		public String toString() { return "TMHMM predictions"; }
	},

	USER_DEFINED { 
		public String toString() { return "User defined"; } 
		public String[] labels() { return Track.NO_LABELS; };
	},
	
	PHOBIUS_REGIONS {
		public String[] labels() { return Track.PHOBIUS_LABELS; };
		public String toString() { return "Phobius predictions"; }
	}, 
	
	ALIGNED_REGIONS {
		public String[] labels() { return Track.NO_LABELS; };
		public String toString() { return "Alignments";    };		// often from BLAST or similar
	},
	
	INTERPRO_REGIONS {
		public String[] labels() { return Track.NO_LABELS; };
		public String toString() { return "InterPro predictions"; };
	}, 
	
	NUMERIC {
		public String[] labels() { return Track.NO_LABELS; };
		public String toString() { return "Quantitation (un-named)"; };
	},
	
	GENE_PREDICTION_REGIONS {
		public String[] labels()   { return Track.NO_LABELS; };
		public String   toString() { return "Gene Prediction"; };
	},
	
	PREDGPI_REGIONS {
		public String[] labels() { return Track.NO_LABELS; };
		public String   toString() { return "PredGPI prediction"; }
	};
	
	/**
	 * Returns true for any annotation with regions of interest, false otherwise
	 * @return
	 */
	public boolean isRegion() {
		switch (this) {
		case INTERPRO_REGIONS:
		case ALIGNED_REGIONS:
		case PHOBIUS_REGIONS:
		case LABELLED_REGIONS:
		case TMHMM_REGIONS:
		case GENE_PREDICTION_REGIONS:
		case PREDGPI_REGIONS:
			return true;
		default:
			return false;
		}
	}
	
	/**
	 * Returns true for a numeric vector annotation, false otherwise
	 * @return
	 */
	public boolean isNumeric() {
		return this.equals(NUMERIC);
	}
}
