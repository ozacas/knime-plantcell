
package au.edu.unimelb.plantcell.servers.netphos;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.1
 * 
 */
@WebService(name = "NetPhosServicePortType", targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface NetPhosServicePortType {


    /**
     * 
     * @param generic
     * @param kinase
     * @param fasta
     * @param cutoff
     * @param bestOnly
     * @return
     *     returns java.lang.String
     */
    @WebMethod(action = "urn:submit")
    @WebResult(targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
    @RequestWrapper(localName = "submit", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", className = "au.edu.unimelb.plantcell.servers.netphos.Submit")
    @ResponseWrapper(localName = "submitResponse", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", className = "au.edu.unimelb.plantcell.servers.netphos.SubmitResponse")
    public String submit(
        @WebParam(name = "fasta", targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
        String fasta,
        @WebParam(name = "generic", targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
        Boolean generic,
        @WebParam(name = "best_only", targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
        Boolean bestOnly,
        @WebParam(name = "kinase", targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
        Boolean kinase,
        @WebParam(name = "cutoff", targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
        Double cutoff);

    /**
     * 
     * @param jobId
     * @return
     *     returns java.lang.String
     */
    @WebMethod(action = "urn:getStatus")
    @WebResult(targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
    @RequestWrapper(localName = "getStatus", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", className = "au.edu.unimelb.plantcell.servers.netphos.GetStatus")
    @ResponseWrapper(localName = "getStatusResponse", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", className = "au.edu.unimelb.plantcell.servers.netphos.GetStatusResponse")
    public String getStatus(
        @WebParam(name = "job_id", targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
        String jobId);

    /**
     * 
     * @param jobId
     * @return
     *     returns java.lang.String
     */
    @WebMethod(action = "urn:getResult")
    @WebResult(targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
    @RequestWrapper(localName = "getResult", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", className = "au.edu.unimelb.plantcell.servers.netphos.GetResult")
    @ResponseWrapper(localName = "getResultResponse", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", className = "au.edu.unimelb.plantcell.servers.netphos.GetResultResponse")
    public String getResult(
        @WebParam(name = "job_id", targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
        String jobId);

}