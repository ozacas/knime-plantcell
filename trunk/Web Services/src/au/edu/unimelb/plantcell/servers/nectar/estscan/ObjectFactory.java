
package au.edu.unimelb.plantcell.servers.nectar.estscan;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the au.edu.unimelb.plantcell.io.servers.nectar.estscan package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _GetResultJobId_QNAME = new QName("http://nectar.plantcell.unimelb.edu.au", "job_id");
    private final static QName _GetStatusJobID_QNAME = new QName("http://nectar.plantcell.unimelb.edu.au", "jobID");
    private final static QName _SubmitResponseReturn_QNAME = new QName("http://nectar.plantcell.unimelb.edu.au", "return");
    private final static QName _SubmitScoringModel_QNAME = new QName("http://nectar.plantcell.unimelb.edu.au", "scoring_model");
    private final static QName _SubmitNucleotideSequenceAsFasta_QNAME = new QName("http://nectar.plantcell.unimelb.edu.au", "nucleotide_sequence_as_fasta");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: au.edu.unimelb.plantcell.io.servers.nectar.estscan
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetResult }
     * 
     */
    public GetResult createGetResult() {
        return new GetResult();
    }

    /**
     * Create an instance of {@link GetStatus }
     * 
     */
    public GetStatus createGetStatus() {
        return new GetStatus();
    }

    /**
     * Create an instance of {@link SubmitResponse }
     * 
     */
    public SubmitResponse createSubmitResponse() {
        return new SubmitResponse();
    }

    /**
     * Create an instance of {@link GetStatusResponse }
     * 
     */
    public GetStatusResponse createGetStatusResponse() {
        return new GetStatusResponse();
    }

    /**
     * Create an instance of {@link Submit }
     * 
     */
    public Submit createSubmit() {
        return new Submit();
    }

    /**
     * Create an instance of {@link GetResultResponse }
     * 
     */
    public GetResultResponse createGetResultResponse() {
        return new GetResultResponse();
    }

    /**
     * Create an instance of {@link GetAvailableScoreModelsResponse }
     * 
     */
    public GetAvailableScoreModelsResponse createGetAvailableScoreModelsResponse() {
        return new GetAvailableScoreModelsResponse();
    }

    /**
     * Create an instance of {@link GetAvailableScoreModels }
     * 
     */
    public GetAvailableScoreModels createGetAvailableScoreModels() {
        return new GetAvailableScoreModels();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nectar.plantcell.unimelb.edu.au", name = "job_id", scope = GetResult.class)
    public JAXBElement<String> createGetResultJobId(String value) {
        return new JAXBElement<String>(_GetResultJobId_QNAME, String.class, GetResult.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nectar.plantcell.unimelb.edu.au", name = "jobID", scope = GetStatus.class)
    public JAXBElement<String> createGetStatusJobID(String value) {
        return new JAXBElement<String>(_GetStatusJobID_QNAME, String.class, GetStatus.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nectar.plantcell.unimelb.edu.au", name = "return", scope = SubmitResponse.class)
    public JAXBElement<String> createSubmitResponseReturn(String value) {
        return new JAXBElement<String>(_SubmitResponseReturn_QNAME, String.class, SubmitResponse.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nectar.plantcell.unimelb.edu.au", name = "return", scope = GetStatusResponse.class)
    public JAXBElement<String> createGetStatusResponseReturn(String value) {
        return new JAXBElement<String>(_SubmitResponseReturn_QNAME, String.class, GetStatusResponse.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nectar.plantcell.unimelb.edu.au", name = "scoring_model", scope = Submit.class)
    public JAXBElement<String> createSubmitScoringModel(String value) {
        return new JAXBElement<String>(_SubmitScoringModel_QNAME, String.class, Submit.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nectar.plantcell.unimelb.edu.au", name = "nucleotide_sequence_as_fasta", scope = Submit.class)
    public JAXBElement<String> createSubmitNucleotideSequenceAsFasta(String value) {
        return new JAXBElement<String>(_SubmitNucleotideSequenceAsFasta_QNAME, String.class, Submit.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nectar.plantcell.unimelb.edu.au", name = "return", scope = GetResultResponse.class)
    public JAXBElement<String> createGetResultResponseReturn(String value) {
        return new JAXBElement<String>(_SubmitResponseReturn_QNAME, String.class, GetResultResponse.class, value);
    }

}
