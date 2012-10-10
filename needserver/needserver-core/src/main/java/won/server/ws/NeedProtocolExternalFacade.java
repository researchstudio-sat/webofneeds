package won.server.ws;

import java.net.URI;

/**
 * Interface to be used from inside the web service implementation. Used to propagate WS calls to the business logic.
 */
public interface NeedProtocolExternalFacade {
    public NeedTransaction receiveConnectionRequest(URI remoteNeedUri,URI remoteNeedProtocolEndpointUri, URI transactionID);
    public void compensate(URI transactionID);
    public void closed(URI transactionID);
    public void faulted(URI transactionID);
}
