package won.matcher.ws;

import won.protocol.ws.MatcherProtocolNeedWebServiceEndpoint;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 12.02.13
 * Time: 17:31
 * To change this template use File | Settings | File Templates.
 */
@WebServiceClient(name = "matcherProtocol", targetNamespace = "http://www.webofneeds.org/protocol/matcher/soap/1.0/")
public class MatcherProtocolNeedWebServiceClient extends Service {

    private final static QName MATCHERPROTOCOL_QNAME = new QName("http://www.webofneeds.org/protocol/matcher/soap/1.0/", "matcherProtocol");



    /**
     * @param wsdlLocation
     */
    public MatcherProtocolNeedWebServiceClient(URL wsdlLocation) {
        super(wsdlLocation, MATCHERPROTOCOL_QNAME);
    }

    /**
     *
     * @return
     *     returns NeedProtocolNeedWebServiceEndpoint
     */
    @WebEndpoint(name = "MatcherProtocolMatcherWebServiceEndpointPort")
    public MatcherProtocolNeedWebServiceEndpoint getOwnerProtocolOwnerWebServiceEndpointPort() {
        return super.getPort(new QName("http://www.webofneeds.org/protocol/matcher/soap/1.0/",
                "MatcherProtocolNeedWebServiceEndpointImplPort"), MatcherProtocolNeedWebServiceEndpoint.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.
     *     Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OwnerProtocolOwnerWebServiceEndpointPort
     */
    @WebEndpoint(name = "MatcherProtocolMatcherWebServiceEndpointPort")
    public MatcherProtocolNeedWebServiceEndpoint getOwnerProtocolOwnerWebServiceEndpointPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://www.webofneeds.org/protocol/matcher/soap/1.0/",
                "MatcherProtocolNeedWebServiceEndpointImplPort"), MatcherProtocolNeedWebServiceEndpoint.class, features);
    }

}
