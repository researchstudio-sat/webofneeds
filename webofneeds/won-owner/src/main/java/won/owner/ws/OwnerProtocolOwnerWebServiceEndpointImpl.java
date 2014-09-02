package won.owner.ws;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.util.LazySpringBeanAutowiringSupport;
import won.protocol.util.RdfUtils;
import won.protocol.ws.OwnerProtocolOwnerWebServiceEndpoint;
import won.protocol.ws.fault.*;

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
    public void hint(@WebParam(name = "ownNeedURI") URI ownNeedURI, @WebParam(name = "otherNeedURI") URI otherNeedURI, @WebParam(name = "score") double score, @WebParam(name = "originatorURI") URI originatorURI, @WebParam(name = "content") String content) throws NoSuchNeedFault, IllegalMessageForNeedStateFault
    {
        wireDependenciesLazily();
      try {
        ownerProtocolOwnerService.hint(ownNeedURI.toString(), otherNeedURI.toString(),
                String.valueOf(score), originatorURI.toString(),
                RdfUtils.readRdfSnippet(content, FileUtils.langTurtle).toString(), null);
      } catch (NoSuchNeedException e) {
        throw NoSuchNeedFault.fromException(e);
      } catch (IllegalMessageForNeedStateException e) {
      throw IllegalMessageForNeedStateFault.fromException(e);
      }
    }

    @Override
    public void connect(@WebParam(name = "ownNeedURI") URI ownNeedURI, @WebParam(name = "otherNeedURI") URI otherNeedURI, @WebParam(name = "ownConnectionURI") URI ownConnectionURI, @WebParam(name = "content") String content) throws NoSuchNeedFault, ConnectionAlreadyExistsFault, IllegalMessageForNeedStateFault {
        wireDependenciesLazily();
      try {
        ownerProtocolOwnerService.connect(ownNeedURI.toString(), otherNeedURI.toString(),
                ownConnectionURI.toString(),
                RdfUtils.readRdfSnippet(content, FileUtils.langTurtle).toString(), null);
      } catch (NoSuchNeedException e) {
        throw NoSuchNeedFault.fromException(e);
      } catch (ConnectionAlreadyExistsException e) {
        throw ConnectionAlreadyExistsFault.fromException(e);
      } catch (IllegalMessageForNeedStateException e) {
        throw IllegalMessageForNeedStateFault.fromException(e);
      }
    }

    @Override
    public void open(@WebParam(name = "connectionURI") URI connectionURI, @WebParam(name = "content") String content)
      throws NoSuchConnectionFault, IllegalMessageForConnectionStateFault, IllegalMessageForNeedStateException {
        wireDependenciesLazily();
      try {
        ownerProtocolOwnerService.open(
                connectionURI, RdfUtils.readRdfSnippet(content, FileUtils.langTurtle), null);
      } catch (NoSuchConnectionException e) {
        throw NoSuchConnectionFault.fromException(e);
      } catch (IllegalMessageForConnectionStateException e) {
        throw IllegalMessageForConnectionStateFault.fromException(e);
      }
    }

    @Override
    public void close(@WebParam(name = "connectionURI") URI connectionURI, @WebParam(name = "content") String content) throws NoSuchConnectionFault, IllegalMessageForConnectionStateFault {
        wireDependenciesLazily();
      try {
        ownerProtocolOwnerService.close(
                connectionURI, RdfUtils.readRdfSnippet(content, FileUtils.langTurtle), null);
      } catch (NoSuchConnectionException e) {
        throw NoSuchConnectionFault.fromException(e);
      } catch (IllegalMessageForConnectionStateException e) {
        throw IllegalMessageForConnectionStateFault.fromException(e);
      }
    }

    @Override
    public void sendMessage(@WebParam(name = "connectionURI") URI connectionURI, @WebParam(
      name = "content") String content) throws NoSuchConnectionFault, IllegalMessageForConnectionStateFault {
        wireDependenciesLazily();
      try {
          Model contentConvert = RdfUtils.toModel(content);
        ownerProtocolOwnerService.sendMessage(connectionURI, contentConvert, null);
      } catch (NoSuchConnectionException e) {
        throw NoSuchConnectionFault.fromException(e);
      } catch (IllegalMessageForConnectionStateException e) {
        throw IllegalMessageForConnectionStateFault.fromException(e);
      }
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
