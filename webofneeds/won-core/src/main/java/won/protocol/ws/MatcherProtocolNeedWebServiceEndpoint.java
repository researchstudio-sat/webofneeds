package won.protocol.ws;

import com.hp.hpl.jena.rdf.model.Model;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.matcher.MatcherProtocolNeedService;
import won.protocol.model.Connection;
import won.protocol.model.Need;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 14.02.13
 * Time: 14:19
 * To change this template use File | Settings | File Templates.
 */
@WebService(serviceName = "matcherProtocol", targetNamespace = "http://www.webofneeds.org/protocol/matcher/soap/1.0/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface MatcherProtocolNeedWebServiceEndpoint {
    @WebMethod
    void hint(
            @WebParam(name = "needURI") URI needURI,
            @WebParam(name = "otherNeedURI") URI otherNeedURI,
            @WebParam(name = "score") double score,
            @WebParam(name = "originatorURI") URI originatorURI,
            @WebParam(name = "content") final String content) throws NoSuchNeedException, IllegalMessageForNeedStateException;

    @WebMethod(exclude = true)
    void setMatcherProtocolNeedService(MatcherProtocolNeedService matcherProtocolNeedService);
}
