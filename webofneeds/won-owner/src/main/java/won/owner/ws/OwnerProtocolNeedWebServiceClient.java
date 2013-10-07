package won.owner.ws;

import won.protocol.ws.OwnerProtocolNeedWebServiceEndpoint;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 12.12.12
 * Time: 14:51
 */
@WebServiceClient(name = "ownerProtocol", targetNamespace = "http://www.webofneeds.org/protocol/owner/soap/1.0/")
public class OwnerProtocolNeedWebServiceClient extends Service {

    private final static QName OWNERPROTOCOL_QNAME = new QName("http://www.webofneeds.org/protocol/owner/soap/1.0/", "ownerProtocol");

    /**
     * @param wsdlLocation
     */
    public OwnerProtocolNeedWebServiceClient(URL wsdlLocation) {
        super(wsdlLocation, OWNERPROTOCOL_QNAME);
    }

    /**
     *
     * @return
     *     returns NeedProtocolNeedWebServiceEndpoint
     */
    @WebEndpoint(name = "OwnerProtocolOwnerWebServiceEndpointPort")
    public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolOwnerWebServiceEndpointPort() {
        return super.getPort(new QName("http://www.webofneeds.org/protocol/owner/soap/1.0/",
                "OwnerProtocolNeedWebServiceEndpointImplPort"), OwnerProtocolNeedWebServiceEndpoint.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.
     *     Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OwnerProtocolOwnerWebServiceEndpointPort
     */
    @WebEndpoint(name = "OwnerProtocolOwnerWebServiceEndpointPort")
    public OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolOwnerWebServiceEndpointPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://www.webofneeds.org/protocol/owner/soap/1.0/",
                "OwnerProtocolNeedWebServiceEndpointImplPort"), OwnerProtocolNeedWebServiceEndpoint.class, features);
    }

}
