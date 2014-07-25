package won.protocol.ws;

import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.ws.fault.*;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 12.12.12
 * Time: 17:09
 */

@WebService (
        serviceName = "ownerProtocol",
        targetNamespace = "http://www.webofneeds.org/protocol/owner/soap/1.0/",
        portName="OwnerProtocolOwnerWebServiceEndpointPort"
)
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface OwnerProtocolOwnerWebServiceEndpoint {

    @WebMethod
    public void hint(
            @WebParam(name = "ownNeedURI") final URI ownNeedURI,
            @WebParam(name = "otherNeedURI") final URI otherNeedURI,
            @WebParam(name = "score") final double score,
            @WebParam(name = "originatorURI") final URI originatorURI,
            @WebParam(name = "content") final String content)
            throws NoSuchNeedFault, IllegalMessageForNeedStateFault;

    @WebMethod
    public void connect(
            @WebParam(name = "ownNeedURI") final URI ownNeedURI,
            @WebParam(name = "otherNeedURI") final URI otherNeedURI,
            @WebParam(name = "ownConnectionURI") final URI ownConnectionURI,
            @WebParam(name = "content") final String content)
            throws NoSuchNeedFault, ConnectionAlreadyExistsFault, IllegalMessageForNeedStateFault;

    @WebMethod
    public void open(
            @WebParam(name = "connectionURI") final URI connectionURI,
            @WebParam(name = "content") final String content)
      throws NoSuchConnectionFault, IllegalMessageForConnectionStateFault, IllegalMessageForNeedStateException;

    @WebMethod
    public void close(
            @WebParam(name = "connectionURI") final URI connectionURI,
            @WebParam(name = "content") final String content)
            throws NoSuchConnectionFault, IllegalMessageForConnectionStateFault;

    @WebMethod
    public void sendMessage(
      @WebParam(name = "connectionURI") final URI connectionURI,
      @WebParam(name = "content") final String message)
            throws NoSuchConnectionFault, IllegalMessageForConnectionStateFault;
}
