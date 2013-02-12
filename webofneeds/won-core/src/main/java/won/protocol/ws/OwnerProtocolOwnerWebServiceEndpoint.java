package won.protocol.ws;

import org.springframework.util.ObjectUtils;
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.util.DataAccessUtils;

import java.net.URI;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

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
    public void hintReceived(
            @WebParam(name = "ownNeedURI") final URI ownNeedURI,
            @WebParam(name = "otherNeedURI") final URI otherNeedURI,
            @WebParam(name = "score") final double score,
            @WebParam(name = "originatorURI") final URI originatorURI)
            throws NoSuchNeedException, IllegalMessageForNeedStateException;

    @WebMethod
    public void connectionRequested(
            @WebParam(name = "ownNeedURI") final URI ownNeedURI,
            @WebParam(name = "otherNeedURI") final URI otherNeedURI,
            @WebParam(name = "ownConnectionURI") final URI ownConnectionURI,
            @WebParam(name = "message") final String message)
            throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException;

    @WebMethod
    public void accept(
            @WebParam(name = "connectionURI") final URI connectionURI)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

    @WebMethod
    public void deny(
            @WebParam(name = "connectionURI") final URI connectionURI)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

    @WebMethod
    public void close(
            @WebParam(name = "connectionURI") final URI connectionURI)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

    @WebMethod
    public void sendTextMessage(
            @WebParam(name = "connectionURI") final URI connectionURI,
            @WebParam(name = "message") final String message)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException;
}
