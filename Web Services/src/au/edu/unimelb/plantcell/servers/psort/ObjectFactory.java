
package au.edu.unimelb.plantcell.servers.psort;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the au.edu.unimelb.plantcell.servers.psort package. 
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

    private final static QName _SubmitResponseReturn_QNAME = new QName("http://nectar.plantcell.unimelb.edu.au", "return");
    private final static QName _SubmitOrganism_QNAME = new QName("http://nectar.plantcell.unimelb.edu.au", "organism");
    private final static QName _SubmitFastaSequences_QNAME = new QName("http://nectar.plantcell.unimelb.edu.au", "fasta_sequences");
    private final static QName _GetResultJobID_QNAME = new QName("http://nectar.plantcell.unimelb.edu.au", "jobID");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: au.edu.unimelb.plantcell.servers.psort
     * 
     */
    public ObjectFactory() {
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
     * Create an instance of {@link GetResult }
     * 
     */
    public GetResult createGetResult() {
        return new GetResult();
    }

    /**
     * Create an instance of {@link GetResultResponse }
     * 
     */
    public GetResultResponse createGetResultResponse() {
        return new GetResultResponse();
    }

    /**
     * Create an instance of {@link GetStatus }
     * 
     */
    public GetStatus createGetStatus() {
        return new GetStatus();
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
    @XmlElementDecl(namespace = "http://nectar.plantcell.unimelb.edu.au", name = "organism", scope = Submit.class)
    public JAXBElement<String> createSubmitOrganism(String value) {
        return new JAXBElement<String>(_SubmitOrganism_QNAME, String.class, Submit.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nectar.plantcell.unimelb.edu.au", name = "fasta_sequences", scope = Submit.class)
    public JAXBElement<String> createSubmitFastaSequences(String value) {
        return new JAXBElement<String>(_SubmitFastaSequences_QNAME, String.class, Submit.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nectar.plantcell.unimelb.edu.au", name = "jobID", scope = GetResult.class)
    public JAXBElement<String> createGetResultJobID(String value) {
        return new JAXBElement<String>(_GetResultJobID_QNAME, String.class, GetResult.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nectar.plantcell.unimelb.edu.au", name = "return", scope = GetResultResponse.class)
    public JAXBElement<String> createGetResultResponseReturn(String value) {
        return new JAXBElement<String>(_SubmitResponseReturn_QNAME, String.class, GetResultResponse.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://nectar.plantcell.unimelb.edu.au", name = "jobID", scope = GetStatus.class)
    public JAXBElement<String> createGetStatusJobID(String value) {
        return new JAXBElement<String>(_GetResultJobID_QNAME, String.class, GetStatus.class, value);
    }

}
