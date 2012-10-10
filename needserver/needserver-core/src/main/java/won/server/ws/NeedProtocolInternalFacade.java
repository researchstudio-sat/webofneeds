package won.server.ws;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: fsalcher
 * Date: 10.10.12
 * Time: 16:07
 * To change this template use File | Settings | File Templates.
 */
public interface NeedProtocolInternalFacade {
    public NeedTransaction connectWithNeed(URI needProtocolEndpointUri);
    public void abort(URI transactionID);
    public void close(URI transactionID);
}
