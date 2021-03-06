
package au.edu.unimelb.plantcell.servers.netsurfp;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.1
 * 
 */
@WebServiceClient(name = "NetSurfPService", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", wsdlLocation = "file:/C:/cygwin/home/andrew.cassin/test.outoftheway/netsurfp@nectar/NetSurfP_service.wsdl")
public class NetSurfPService
    extends Service
{

    private final static URL NETSURFPSERVICE_WSDL_LOCATION;
    private final static Logger logger = Logger.getLogger(au.edu.unimelb.plantcell.servers.netsurfp.NetSurfPService.class.getName());

    static {
        URL url = null;
        try {
            URL baseUrl;
            baseUrl = au.edu.unimelb.plantcell.servers.netsurfp.NetSurfPService.class.getResource(".");
            url = new URL(baseUrl, "file:/C:/cygwin/home/andrew.cassin/test.outoftheway/netsurfp@nectar/NetSurfP_service.wsdl");
        } catch (MalformedURLException e) {
            logger.warning("Failed to create URL for the wsdl Location: 'file:/C:/cygwin/home/andrew.cassin/test.outoftheway/netsurfp@nectar/NetSurfP_service.wsdl', retrying as a local file");
            logger.warning(e.getMessage());
        }
        NETSURFPSERVICE_WSDL_LOCATION = url;
    }

    public NetSurfPService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public NetSurfPService() {
        super(NETSURFPSERVICE_WSDL_LOCATION, new QName("http://nectar.plantcell.unimelb.edu.au", "NetSurfPService"));
    }

    /**
     * 
     * @return
     *     returns NetSurfPServicePortType
     */
    @WebEndpoint(name = "NetSurfPServiceHttpSoap11Endpoint")
    public NetSurfPServicePortType getNetSurfPServiceHttpSoap11Endpoint() {
        return super.getPort(new QName("http://nectar.plantcell.unimelb.edu.au", "NetSurfPServiceHttpSoap11Endpoint"), NetSurfPServicePortType.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns NetSurfPServicePortType
     */
    @WebEndpoint(name = "NetSurfPServiceHttpSoap11Endpoint")
    public NetSurfPServicePortType getNetSurfPServiceHttpSoap11Endpoint(WebServiceFeature... features) {
        return super.getPort(new QName("http://nectar.plantcell.unimelb.edu.au", "NetSurfPServiceHttpSoap11Endpoint"), NetSurfPServicePortType.class, features);
    }

}
