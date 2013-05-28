
package au.edu.unimelb.plantcell.servers.bigpi;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.4-b01
 * Generated source version: 2.2
 * 
 */
@WebService(name = "BIGPIServicePortType", targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface BIGPIServicePortType {


    /**
     * 
     * @param organismType
     * @param proteinSequenceAsFasta
     * @return
     *     returns java.lang.String
     */
    @WebMethod(action = "urn:submit")
    @WebResult(targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
    @RequestWrapper(localName = "submit", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", className = "au.edu.unimelb.plantcell.servers.bigpi.Submit")
    @ResponseWrapper(localName = "submitResponse", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", className = "au.edu.unimelb.plantcell.servers.bigpi.SubmitResponse")
    public String submit(
        @WebParam(name = "protein_sequence_as_fasta", targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
        String proteinSequenceAsFasta,
        @WebParam(name = "organism_type", targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
        String organismType);

    /**
     * 
     * @param jobID
     * @return
     *     returns java.lang.String
     */
    @WebMethod(action = "urn:getStatus")
    @WebResult(targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
    @RequestWrapper(localName = "getStatus", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", className = "au.edu.unimelb.plantcell.servers.bigpi.GetStatus")
    @ResponseWrapper(localName = "getStatusResponse", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", className = "au.edu.unimelb.plantcell.servers.bigpi.GetStatusResponse")
    public String getStatus(
        @WebParam(name = "jobID", targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
        String jobID);

    /**
     * 
     * @param jobId
     * @return
     *     returns java.lang.String
     */
    @WebMethod(action = "urn:getResult")
    @WebResult(targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
    @RequestWrapper(localName = "getResult", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", className = "au.edu.unimelb.plantcell.servers.bigpi.GetResult")
    @ResponseWrapper(localName = "getResultResponse", targetNamespace = "http://nectar.plantcell.unimelb.edu.au", className = "au.edu.unimelb.plantcell.servers.bigpi.GetResultResponse")
    public String getResult(
        @WebParam(name = "job_id", targetNamespace = "http://nectar.plantcell.unimelb.edu.au")
        String jobId);

}