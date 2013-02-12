package won.node.ws;

import won.protocol.ws.OwnerProtocolOwnerWebServiceEndpoint;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 20.12.12
 * Time: 09:22
 */
@WebServiceClient(name = "ownerProtocol", targetNamespace = "http://www.webofneeds.org/protocol/owner/soap/1.0/")
public class OwnerProtocolOwnerWebServiceClient extends Service {
    private final static QName OWNERPROTOCOL_QNAME = new QName("http://www.webofneeds.org/protocol/owner/soap/1.0/", "ownerProtocol");


    /**
     * TODO: We want to be able to pass the web service URI directly here... We already know the content of the wsdl file... right?
     * @param wsdlLocation
     */
    public OwnerProtocolOwnerWebServiceClient(URL wsdlLocation) {
        super(wsdlLocation, OWNERPROTOCOL_QNAME);
    }

    /**
     *
     * @return
     *     returns NeedProtocolNeedWebServiceEndpoint
     */
    @WebEndpoint(name = "OwnerProtocolNeedWebServiceEndpointPort")
    public OwnerProtocolOwnerWebServiceEndpoint getOwnerProtocolOwnerWebServiceEndpointPort() {
        return super.getPort(new QName("http://www.webofneeds.org/protocol/owner/soap/1.0/",
                "OwnerProtocolOwnerWebServiceEndpointPort"), OwnerProtocolOwnerWebServiceEndpoint.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.
     *     Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OwnerProtocolOwnerWebServiceEndpointPort
     */
    @WebEndpoint(name = "OwnerProtocolNeedWebServiceEndpointPort")
    public OwnerProtocolOwnerWebServiceEndpoint getOwnerProtocolOwnerWebServiceEndpointPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://www.webofneeds.org/protocol/owner/soap/1.0/",
                "OwnerProtocolOwnerWebServiceEndpointPort"), OwnerProtocolOwnerWebServiceEndpoint.class, features);
    }

}
