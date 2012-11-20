
package dk.dtu.cbs.ws.wstmhmm_2_0b;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;


/**
 *  
 * 			TMHMM is a method for prediction transmembrane helices based on a hidden Markov model and 
 * 			developed by Anders Krogh and Erik Sonnhammer. The method is described in detail in the 
 * 			following articles:
 * 
 * 			Predicting transmembrane protein topology with a hidden Markov model: Application 
 * 			to complete genomes. A. Krogh, B. Larsson, G. von Heijne, and E. L. L. Sonnhammer. 
 * 			J. Mol. Biol., 305(3):567-580, January 2001.
 * 			PDF: http://www.binf.ku.dk/krogh/publications/pdf/KroghEtal01.pdf
 * 			 
 * 			A hidden Markov model for predicting transmembrane helices in protein sequences. 
 * 			E. L.L. Sonnhammer, G. von Heijne, and A. Krogh.
 * 			In J. Glasgow, T. Littlejohn, F. Major, R. Lathrop, D. Sankoff, and C. Sensen, editors, 
 * 			Proceedings of the Sixth International Conference on Intelligent Systems for Molecular Biology, 
 * 			pages 175-182, Menlo Park, CA, 1998. AAAI Press. 
 * 			PDF: http://www.binf.ku.dk/krogh/publications/ps/SonnhammerEtal98.pdf
 * 			
 * 		Alongside this Web Service the TMHMM method is also implemented as
 * 		a traditional click-and-paste WWW server at:
 * 
 * 		  http://www.cbs.dtu.dk/services/TMHMM/
 * 
 * 		TMHMM is also available as a stand-alone software package to install
 * 		and run at the user's site, with the same functionality. For academic
 * 		users there is a download page at:
 * 
 * 		  http://www.cbs.dtu.dk/cgi-bin/nph-sw_request?tmhmm
 * 
 * 		Other users are requested to write to software@cbs.dtu.dk for details.
 * 
 * 		WEB SERVICE OPERATION
 * 
 * 		This Web Service is fully asynchronous; the usage is split into the
 * 		following three operations:
 * 
 * 		1. runService    
 * 
 * 		Input:  The following parameters and data:
 * 		        'graphics'     OPTIONAL. Can be 'yes' or 'no' indicating whether or not
 * 		                       graphical output should be added to the output. PLEASE BE AWARE
 * 		                       that this option adds significant compute time and it is therefor 
 * 		                       advised to apply this to smaller datasets (50-100 proteins). For 
 * 		                       larger data sets, an initial run can be submitted without graphical
 * 		                       output, and a job can subsequently be submitted based on the filtered 
 * 		                       results for the first run, now including graphics.
 * 		        'sequences'    protein sequences, with unique identifiers (mandatory) 
 * 		                       The sequences must be written using the one letter amino acid
 * 		                       code: `acdefghiklmnpqrstvwy' or `ACDEFGHIKLMNPQRSTVWY'. Other
 * 		                       letters will be converted to `X' and treated as unknown amino
 * 		                       acids. Other symbols, such as whitespace and numbers, will be
 * 		                       ignored. All the input sequences are truncated to 70 aa from
 * 		                       the N-terminal. Currently, at most 2,000 sequences are allowed
 * 		                       per submission.
 * 
 * 		Output: Unique job identifier
 * 
 * 		2. pollQueue
 * 
 * 		Input:  Unique job identifier
 * 
 * 		Output: 'jobstatus' - the status of the job
 * 		            Possible values are QUEUED, ACTIVE, FINISHED, WAITING,
 * 		            REJECTED, UNKNOWN JOBID or QUEUE DOWN
 * 
 * 		3. fetchResult
 * 
 * 		Input:  Unique job identifier of a FINISHED job
 * 
 * 		Output: 'output' - prediction results:
 * 
 * 		        For each input sequence a record is output consisting of the
 * 		        following fields:
 * 
 * 		        'len'          The length of the protein sequence
 * 		        'PredHel'      The number of predicted transmembrane helices.
 * 		        'ExpAA'        The expected number of amino acids intransmembrane helices. 
 * 		                       If this number is larger than 18 it is very likely to be a 
 * 		                       transmembrane protein (OR have a signal peptide).
 * 		        'First60'      The expected number of amino acids in transmembrane helices 
 * 		                       in the first 60 amino acids of the protein. If this number 
 * 		                       more than a few, you should be warned that a predicted
 * 		                       transmembrane helix in the N-term could be a signal peptide.
 * 		        'NinProb'      The total probability that the N-term is on the cytoplasmic side 
 * 		                       of the membrane.
 * 		        'NtermSignal'  (yes/no) A warning that is produced when 'First60' is larger than 10.
 * 		        'image'        OPTIONAL - common image data type, base64 encoded PNG image 
 * 		                       'comment'     Fixed: Posterior probabilities of inside/outside/TM helix
 * 		                       'encoding'    Fixed: base64
 * 		                       'MIMEtype'    Fixed: image/png
 * 		                       'content'     Base64 encoded image: iVBORw0KGgoAAAANS...
 * 		        'topology'     The topology of the helix prediction:
 * 		                      'location'   (inside/outside)
 * 		                      'begin'      Start postion
 * 		                      'end'        End position
 * 
 *     VERSIONS
 *     2.0  : initial
 *     2.0b : A change was made to version 2.0b to use a different service
 *            endpoint (simple.cgi instead of server.cgi). The change is transparent to the 
 *            to the user. 
 * 
 * 
 * 		CONTACT
 * 
 * 		Questions concerning the scientific aspects of the TMHMM method should
 * 		go to Anders Krogh, krogh@cbs.dtu.dk; technical questions concerning
 * 		the Web Service should go to Peter Fischer Hallin, pfh@cbs.dtu.dk or
 * 		Kristoffer Rapacki, rapacki@cbs.dtu.dk.
 * 
 * 		
 * 
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.0
 * 
 */
@WebServiceClient(name = "WSTMHMM_2_0b", targetNamespace = "http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", wsdlLocation = "file:/C:/cygwin/home/andrew.cassin/test/tmhmm/TMHMM_2_0b.wsdl")
public class WSTMHMM20B_Service
    extends Service
{

    private final static URL WSTMHMM20B_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(dk.dtu.cbs.ws.wstmhmm_2_0b.WSTMHMM20B_Service.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = dk.dtu.cbs.ws.wstmhmm_2_0b.WSTMHMM20B_Service.class.getResource(".");
            url = new URL(baseUrl, "file:/C:/cygwin/home/andrew.cassin/test/tmhmm/TMHMM_2_0b.wsdl");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'file:/C:/cygwin/home/andrew.cassin/test/tmhmm/TMHMM_2_0b.wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        WSTMHMM20B_WSDL_LOCATION = url;
    }

    public WSTMHMM20B_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public WSTMHMM20B_Service() {
        super(WSTMHMM20B_WSDL_LOCATION, new QName("http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", "WSTMHMM_2_0b"));
    }

    /**
     * 
     * @return
     *     returns WSTMHMM20B
     */
    @WebEndpoint(name = "WSTMHMM_2_0b")
    public WSTMHMM20B getWSTMHMM20B() {
        return super.getPort(new QName("http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", "WSTMHMM_2_0b"), WSTMHMM20B.class);
    }

}
