
package dk.dtu.cbs.ws.ws_tmhmm;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the dk.dtu.cbs.ws.ws_tmhmm package. 
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


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: dk.dtu.cbs.ws.ws_tmhmm
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link RunService.Parameters }
     * 
     */
    public RunService.Parameters createRunServiceParameters() {
        return new RunService.Parameters();
    }

    /**
     * Create an instance of {@link FetchResultResponse.Output }
     * 
     */
    public FetchResultResponse.Output createFetchResultResponseOutput() {
        return new FetchResultResponse.Output();
    }

    /**
     * Create an instance of {@link FetchResultResponse.Output.Entries.Entry }
     * 
     */
    public FetchResultResponse.Output.Entries.Entry createFetchResultResponseOutputEntriesEntry() {
        return new FetchResultResponse.Output.Entries.Entry();
    }

    /**
     * Create an instance of {@link RunService }
     * 
     */
    public RunService createRunService() {
        return new RunService();
    }

    /**
     * Create an instance of {@link FetchResultResponse.Output.Entries }
     * 
     */
    public FetchResultResponse.Output.Entries createFetchResultResponseOutputEntries() {
        return new FetchResultResponse.Output.Entries();
    }

    /**
     * Create an instance of {@link FetchResultResponse.Output.Entries.Entry.Topology }
     * 
     */
    public FetchResultResponse.Output.Entries.Entry.Topology createFetchResultResponseOutputEntriesEntryTopology() {
        return new FetchResultResponse.Output.Entries.Entry.Topology();
    }

    /**
     * Create an instance of {@link FetchResultResponse.Output.Entries.Entry.Topology.Entry }
     * 
     */
    public FetchResultResponse.Output.Entries.Entry.Topology.TEntry createFetchResultResponseOutputEntriesEntryTopologyEntry() {
        return new FetchResultResponse.Output.Entries.Entry.Topology.TEntry();
    }

    /**
     * Create an instance of {@link FetchResultResponse }
     * 
     */
    public FetchResultResponse createFetchResultResponse() {
        return new FetchResultResponse();
    }

}
