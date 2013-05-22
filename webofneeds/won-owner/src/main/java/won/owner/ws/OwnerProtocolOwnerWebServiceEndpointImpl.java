package won.owner.ws;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.util.LazySpringBeanAutowiringSupport;
import won.protocol.util.RdfUtils;
import won.protocol.ws.OwnerProtocolOwnerWebServiceEndpoint;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.StringReader;
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
    @Autowired
    private RdfUtils rdfUtils;

    @Override
    public void hint(@WebParam(name = "ownNeedURI") URI ownNeedURI, @WebParam(name = "otherNeedURI") URI otherNeedURI, @WebParam(name = "score") double score, @WebParam(name = "originatorURI") URI originatorURI, @WebParam(name = "content") String content) throws NoSuchNeedException, IllegalMessageForNeedStateException {
        wireDependenciesLazily();
        ownerProtocolOwnerService.hint(ownNeedURI, otherNeedURI, score, originatorURI, rdfUtils.toModel(content));
    }

    @Override
    public void connect(@WebParam(name = "ownNeedURI") URI ownNeedURI, @WebParam(name = "otherNeedURI") URI otherNeedURI, @WebParam(name = "ownConnectionURI") URI ownConnectionURI, @WebParam(name = "content") String content) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException {
        wireDependenciesLazily();
        ownerProtocolOwnerService.connect(ownNeedURI, otherNeedURI, ownConnectionURI, rdfUtils.toModel(content));
    }

    @Override
    public void open(@WebParam(name = "connectionURI") URI connectionURI, @WebParam(name = "content") String content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        wireDependenciesLazily();
        ownerProtocolOwnerService.open(connectionURI, rdfUtils.toModel(content));
    }

    @Override
    public void close(@WebParam(name = "connectionURI") URI connectionURI, @WebParam(name = "content") String content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        wireDependenciesLazily();
        ownerProtocolOwnerService.close(connectionURI, rdfUtils.toModel(content));
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
