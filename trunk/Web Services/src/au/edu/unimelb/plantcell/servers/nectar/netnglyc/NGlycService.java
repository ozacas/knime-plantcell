
package au.edu.unimelb.plantcell.servers.nectar.netnglyc;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.9-b130926.1035
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "NGlycService", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", wsdlLocation = "file:/C:/work/src/knime-plantcell/Web%20Services/wsdl/netnglyc@nectar.wsdl")
public class NGlycService
    extends Service
{

    private final static URL NGLYCSERVICE_WSDL_LOCATION;
    private final static WebServiceException NGLYCSERVICE_EXCEPTION;
    private final static QName NGLYCSERVICE_QNAME = new QName("http://nectar.plantcell.unimelb.edu.au", "NGlycService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("file:/C:/work/src/knime-plantcell/Web%20Services/wsdl/netnglyc@nectar.wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        NGLYCSERVICE_WSDL_LOCATION = url;
        NGLYCSERVICE_EXCEPTION = e;
    }

    public NGlycService() {
        super(__getWsdlLocation(), NGLYCSERVICE_QNAME);
    }

    public NGlycService(WebServiceFeature... features) {
        super(__getWsdlLocation(), NGLYCSERVICE_QNAME, features);
    }

    public NGlycService(URL wsdlLocation) {
        super(wsdlLocation, NGLYCSERVICE_QNAME);
    }

    public NGlycService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, NGLYCSERVICE_QNAME, features);
    }

    public NGlycService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public NGlycService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns NGlycServicePortType
     */
    @WebEndpoint(name = "NGlycServiceHttpSoap11Endpoint")
    public NGlycServicePortType getNGlycServiceHttpSoap11Endpoint() {
        return super.getPort(new QName("http://nectar.plantcell.unimelb.edu.au", "NGlycServiceHttpSoap11Endpoint"), NGlycServicePortType.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns NGlycServicePortType
     */
    @WebEndpoint(name = "NGlycServiceHttpSoap11Endpoint")
    public NGlycServicePortType getNGlycServiceHttpSoap11Endpoint(WebServiceFeature... features) {
        return super.getPort(new QName("http://nectar.plantcell.unimelb.edu.au", "NGlycServiceHttpSoap11Endpoint"), NGlycServicePortType.class, features);
    }

    private static URL __getWsdlLocation() {
        if (NGLYCSERVICE_EXCEPTION!= null) {
            throw NGLYCSERVICE_EXCEPTION;
        }
        return NGLYCSERVICE_WSDL_LOCATION;
    }

}