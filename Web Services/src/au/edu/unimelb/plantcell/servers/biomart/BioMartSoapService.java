
package au.edu.unimelb.plantcell.servers.biomart;

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
 * JAX-WS RI 2.2.4-b01
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "BioMartSoapService", targetNamespace = "http://soap.api.biomart.org/", wsdlLocation = "http://central.biomart.org/martsoap?wsdl")
public class BioMartSoapService
    extends Service
{

    private final static URL BIOMARTSOAPSERVICE_WSDL_LOCATION;
    private final static WebServiceException BIOMARTSOAPSERVICE_EXCEPTION;
    private final static QName BIOMARTSOAPSERVICE_QNAME = new QName("http://soap.api.biomart.org/", "BioMartSoapService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://central.biomart.org/martsoap?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        BIOMARTSOAPSERVICE_WSDL_LOCATION = url;
        BIOMARTSOAPSERVICE_EXCEPTION = e;
    }

    public BioMartSoapService() {
        super(__getWsdlLocation(), BIOMARTSOAPSERVICE_QNAME);
    }

    public BioMartSoapService(WebServiceFeature... features) {
        super(__getWsdlLocation(), BIOMARTSOAPSERVICE_QNAME, features);
    }

    public BioMartSoapService(URL wsdlLocation) {
        super(wsdlLocation, BIOMARTSOAPSERVICE_QNAME);
    }

    public BioMartSoapService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, BIOMARTSOAPSERVICE_QNAME, features);
    }

    public BioMartSoapService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public BioMartSoapService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns PortalServiceImpl
     */
    @WebEndpoint(name = "PortalServiceImplPort")
    public PortalServiceImpl getPortalServiceImplPort() {
        return super.getPort(new QName("http://soap.api.biomart.org/", "PortalServiceImplPort"), PortalServiceImpl.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns PortalServiceImpl
     */
    @WebEndpoint(name = "PortalServiceImplPort")
    public PortalServiceImpl getPortalServiceImplPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://soap.api.biomart.org/", "PortalServiceImplPort"), PortalServiceImpl.class, features);
    }

    private static URL __getWsdlLocation() {
        if (BIOMARTSOAPSERVICE_EXCEPTION!= null) {
            throw BIOMARTSOAPSERVICE_EXCEPTION;
        }
        return BIOMARTSOAPSERVICE_WSDL_LOCATION;
    }

}
