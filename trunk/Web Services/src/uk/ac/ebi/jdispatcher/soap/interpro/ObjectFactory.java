
package uk.ac.ebi.jdispatcher.soap.interpro;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the uk.ac.ebi.jdispatcher.soap.interpro package. 
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

    private final static QName _RunTitle_QNAME = new QName("", "title");
    private final static QName _GetResultParameters_QNAME = new QName("", "parameters");
    private final static QName _InputParametersGoterms_QNAME = new QName("", "goterms");
    private final static QName _InputParametersSequence_QNAME = new QName("", "sequence");
    private final static QName _InputParametersNocrc_QNAME = new QName("", "nocrc");
    private final static QName _InputParametersAppl_QNAME = new QName("", "appl");
    private final static QName _WsResultTypeDescription_QNAME = new QName("", "description");
    private final static QName _WsResultTypeLabel_QNAME = new QName("", "label");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: uk.ac.ebi.jdispatcher.soap.interpro
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Run }
     * 
     */
    public Run createRun() {
        return new Run();
    }

    /**
     * Create an instance of {@link WsRawOutputParameters }
     * 
     */
    public WsRawOutputParameters createWsRawOutputParameters() {
        return new WsRawOutputParameters();
    }

    /**
     * Create an instance of {@link GetStatus }
     * 
     */
    public GetStatus createGetStatus() {
        return new GetStatus();
    }

    /**
     * Create an instance of {@link GetResultTypesResponse }
     * 
     */
    public GetResultTypesResponse createGetResultTypesResponse() {
        return new GetResultTypesResponse();
    }

    /**
     * Create an instance of {@link WsParameters }
     * 
     */
    public WsParameters createWsParameters() {
        return new WsParameters();
    }

    /**
     * Create an instance of {@link WsParameterValues }
     * 
     */
    public WsParameterValues createWsParameterValues() {
        return new WsParameterValues();
    }

    /**
     * Create an instance of {@link WsParameterValue }
     * 
     */
    public WsParameterValue createWsParameterValue() {
        return new WsParameterValue();
    }

    /**
     * Create an instance of {@link GetStatusResponse }
     * 
     */
    public GetStatusResponse createGetStatusResponse() {
        return new GetStatusResponse();
    }

    /**
     * Create an instance of {@link WsProperty }
     * 
     */
    public WsProperty createWsProperty() {
        return new WsProperty();
    }

    /**
     * Create an instance of {@link GetParametersResponse }
     * 
     */
    public GetParametersResponse createGetParametersResponse() {
        return new GetParametersResponse();
    }

    /**
     * Create an instance of {@link RunResponse }
     * 
     */
    public RunResponse createRunResponse() {
        return new RunResponse();
    }

    /**
     * Create an instance of {@link ArrayOfString }
     * 
     */
    public ArrayOfString createArrayOfString() {
        return new ArrayOfString();
    }

    /**
     * Create an instance of {@link WsProperties }
     * 
     */
    public WsProperties createWsProperties() {
        return new WsProperties();
    }

    /**
     * Create an instance of {@link GetParameterDetails }
     * 
     */
    public GetParameterDetails createGetParameterDetails() {
        return new GetParameterDetails();
    }

    /**
     * Create an instance of {@link GetResult }
     * 
     */
    public GetResult createGetResult() {
        return new GetResult();
    }

    /**
     * Create an instance of {@link WsResultType }
     * 
     */
    public WsResultType createWsResultType() {
        return new WsResultType();
    }

    /**
     * Create an instance of {@link WsRawOutputParameter }
     * 
     */
    public WsRawOutputParameter createWsRawOutputParameter() {
        return new WsRawOutputParameter();
    }

    /**
     * Create an instance of {@link InputParameters }
     * 
     */
    public InputParameters createInputParameters() {
        return new InputParameters();
    }

    /**
     * Create an instance of {@link WsResultTypes }
     * 
     */
    public WsResultTypes createWsResultTypes() {
        return new WsResultTypes();
    }

    /**
     * Create an instance of {@link GetParameterDetailsResponse }
     * 
     */
    public GetParameterDetailsResponse createGetParameterDetailsResponse() {
        return new GetParameterDetailsResponse();
    }

    /**
     * Create an instance of {@link GetParameters }
     * 
     */
    public GetParameters createGetParameters() {
        return new GetParameters();
    }

    /**
     * Create an instance of {@link WsParameterDetails }
     * 
     */
    public WsParameterDetails createWsParameterDetails() {
        return new WsParameterDetails();
    }

    /**
     * Create an instance of {@link GetResultTypes }
     * 
     */
    public GetResultTypes createGetResultTypes() {
        return new GetResultTypes();
    }

    /**
     * Create an instance of {@link GetResultResponse }
     * 
     */
    public GetResultResponse createGetResultResponse() {
        return new GetResultResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "title", scope = Run.class)
    public JAXBElement<String> createRunTitle(String value) {
        return new JAXBElement<String>(_RunTitle_QNAME, String.class, Run.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link WsRawOutputParameters }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "parameters", scope = GetResult.class)
    public JAXBElement<WsRawOutputParameters> createGetResultParameters(WsRawOutputParameters value) {
        return new JAXBElement<WsRawOutputParameters>(_GetResultParameters_QNAME, WsRawOutputParameters.class, GetResult.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "goterms", scope = InputParameters.class)
    public JAXBElement<Boolean> createInputParametersGoterms(Boolean value) {
        return new JAXBElement<Boolean>(_InputParametersGoterms_QNAME, Boolean.class, InputParameters.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "sequence", scope = InputParameters.class)
    public JAXBElement<String> createInputParametersSequence(String value) {
        return new JAXBElement<String>(_InputParametersSequence_QNAME, String.class, InputParameters.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "nocrc", scope = InputParameters.class)
    public JAXBElement<Boolean> createInputParametersNocrc(Boolean value) {
        return new JAXBElement<Boolean>(_InputParametersNocrc_QNAME, Boolean.class, InputParameters.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ArrayOfString }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "appl", scope = InputParameters.class)
    public JAXBElement<ArrayOfString> createInputParametersAppl(ArrayOfString value) {
        return new JAXBElement<ArrayOfString>(_InputParametersAppl_QNAME, ArrayOfString.class, InputParameters.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "description", scope = WsResultType.class)
    public JAXBElement<String> createWsResultTypeDescription(String value) {
        return new JAXBElement<String>(_WsResultTypeDescription_QNAME, String.class, WsResultType.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "label", scope = WsResultType.class)
    public JAXBElement<String> createWsResultTypeLabel(String value) {
        return new JAXBElement<String>(_WsResultTypeLabel_QNAME, String.class, WsResultType.class, value);
    }

}
