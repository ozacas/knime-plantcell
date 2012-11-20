
package dk.dtu.cbs.ws.wstmhmm_2_0b;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;
import dk.dtu.cbs.ws.ws_common.FetchResult;
import dk.dtu.cbs.ws.ws_common.PollQueue;
import dk.dtu.cbs.ws.ws_common.PollQueueResponse;
import dk.dtu.cbs.ws.ws_common.RunServiceResponse;
import dk.dtu.cbs.ws.ws_tmhmm.FetchResultResponse;
import dk.dtu.cbs.ws.ws_tmhmm.RunService;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the dk.dtu.cbs.ws.wstmhmm_2_0b package. 
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

    private final static QName _PollQueue_QNAME = new QName("http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", "pollQueue");
    private final static QName _RunService_QNAME = new QName("http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", "runService");
    private final static QName _RunServiceResponse_QNAME = new QName("http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", "runServiceResponse");
    private final static QName _FetchResultResponse_QNAME = new QName("http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", "fetchResultResponse");
    private final static QName _FetchResult_QNAME = new QName("http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", "fetchResult");
    private final static QName _PollQueueResponse_QNAME = new QName("http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", "pollQueueResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: dk.dtu.cbs.ws.wstmhmm_2_0b
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PollQueue }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", name = "pollQueue")
    public JAXBElement<PollQueue> createPollQueue(PollQueue value) {
        return new JAXBElement<PollQueue>(_PollQueue_QNAME, PollQueue.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RunService }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", name = "runService")
    public JAXBElement<RunService> createRunService(RunService value) {
        return new JAXBElement<RunService>(_RunService_QNAME, RunService.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RunServiceResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", name = "runServiceResponse")
    public JAXBElement<RunServiceResponse> createRunServiceResponse(RunServiceResponse value) {
        return new JAXBElement<RunServiceResponse>(_RunServiceResponse_QNAME, RunServiceResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FetchResultResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", name = "fetchResultResponse")
    public JAXBElement<FetchResultResponse> createFetchResultResponse(FetchResultResponse value) {
        return new JAXBElement<FetchResultResponse>(_FetchResultResponse_QNAME, FetchResultResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FetchResult }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", name = "fetchResult")
    public JAXBElement<FetchResult> createFetchResult(FetchResult value) {
        return new JAXBElement<FetchResult>(_FetchResult_QNAME, FetchResult.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PollQueueResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.cbs.dtu.dk/ws/WSTMHMM_2_0b", name = "pollQueueResponse")
    public JAXBElement<PollQueueResponse> createPollQueueResponse(PollQueueResponse value) {
        return new JAXBElement<PollQueueResponse>(_PollQueueResponse_QNAME, PollQueueResponse.class, null, value);
    }

}
