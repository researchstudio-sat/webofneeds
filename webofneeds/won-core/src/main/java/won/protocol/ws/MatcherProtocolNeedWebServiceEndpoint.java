package won.protocol.ws;

import won.protocol.matcher.MatcherProtocolNeedService;
import won.protocol.ws.fault.IllegalMessageForNeedStateFault;
import won.protocol.ws.fault.NoSuchNeedFault;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.URI;

/**
 * User: gabriel
 * Date: 14.02.13
 * Time: 14:19
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
            @WebParam(name = "content") final String content) throws NoSuchNeedFault, IllegalMessageForNeedStateFault;

    @WebMethod(exclude = true)
    void setMatcherProtocolNeedService(MatcherProtocolNeedService matcherProtocolNeedService);
}
