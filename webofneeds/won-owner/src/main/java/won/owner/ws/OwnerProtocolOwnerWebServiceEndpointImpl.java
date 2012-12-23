package won.owner.ws;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import won.protocol.exception.*;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.util.LazySpringBeanAutowiringSupport;
import won.protocol.ws.OwnerProtocolOwnerWebServiceEndpoint;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 03.12.12
 * Time: 13:53
 */
@WebService (
        serviceName = "ownerProtocol",
        targetNamespace = "http://www.webofneeds.org/protocol/owner/soap/1.0/",
        portName="OwnerProtocolOwnerWebServiceEndpointPort"
)
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class OwnerProtocolOwnerWebServiceEndpointImpl extends LazySpringBeanAutowiringSupport implements OwnerProtocolOwnerWebServiceEndpoint {
    @Autowired
    private OwnerProtocolOwnerService ownerProtocolOwnerService;

    @Override
    public void hintReceived(@WebParam(name = "ownNeedURI") URI ownNeedURI, @WebParam(name = "otherNeedURI") URI otherNeedURI, @WebParam(name = "score") double score, @WebParam(name = "originatorURI") URI originatorURI) throws NoSuchNeedException, IllegalMessageForNeedStateException {
        wireDependenciesLazily();
        ownerProtocolOwnerService.hintReceived(ownNeedURI, otherNeedURI, score, originatorURI);
    }

    @Override
    public void connectionRequested(@WebParam(name = "ownNeedURI") URI ownNeedURI, @WebParam(name = "otherNeedURI") URI otherNeedURI, @WebParam(name = "ownConnectionURI") URI ownConnectionURI, @WebParam(name = "message") String message) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException {
        wireDependenciesLazily();
        ownerProtocolOwnerService.connectionRequested(ownNeedURI, otherNeedURI, ownConnectionURI, message);
    }

    @Override
    public void accept(@WebParam(name = "connectionURI") URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        wireDependenciesLazily();
        ownerProtocolOwnerService.accept(connectionURI);
    }

    @Override
    public void deny(@WebParam(name = "connectionURI") URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        wireDependenciesLazily();
        ownerProtocolOwnerService.deny(connectionURI);
    }

    @Override
    public void close(@WebParam(name = "connectionURI") URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        wireDependenciesLazily();
        ownerProtocolOwnerService.close(connectionURI);
    }

    @Override
    public void sendTextMessage(@WebParam(name = "connectionURI") URI connectionURI, @WebParam(name = "message") String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        wireDependenciesLazily();
        ownerProtocolOwnerService.sendTextMessage(connectionURI, message);
    }

    @WebMethod(exclude = true)
    public void setOwnerProtocolOwnerService(final OwnerProtocolOwnerService ownerProtocolOwnerService) {
        this.ownerProtocolOwnerService = ownerProtocolOwnerService;
    }

    @Override
    protected boolean isWired() {
        return this.ownerProtocolOwnerService != null;
    }
}
