package won.owner.ws;

import won.protocol.ws.OwnerProtocolNeedWebServiceEndpoint;

import javax.xml.namespace.QName;
import javax.xml.ws.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 12.12.12
 * Time: 14:51
 */
@WebServiceClient(name = "ownerProtocol", targetNamespace = "http://www.webofneeds.org/protocol/owner/soap/1.0/")
public class OwnerProtocolNeedWebServiceClient extends Service {
    private final static URL OWNERPROTOCOL_WSDL_LOCATION;
    private final static WebServiceException OWNERPROTOCOL_EXCEPTION;
    private final static QName OWNERPROTOCOL_QNAME = new QName("http://www.webofneeds.org/protocol/owner/soap/1.0/", "ownerProtocol");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("http://localhost:8080/owner/needProtocol?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        OWNERPROTOCOL_WSDL_LOCATION = url;
        OWNERPROTOCOL_EXCEPTION = e;
    }


    /**
     * TODO: We want to be able to pass the web service URI directly here... We already know the content of the wsdl file... right?
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
        return super.getPort(new QName("http://www.webofneeds.org/protocol/need/soap/1.0/",
                "NeedProtocolNeedWebServiceEndpointPort"), OwnerProtocolNeedWebServiceEndpoint.class);
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
        return super.getPort(new QName("http://www.webofneeds.org/protocol/need/soap/1.0/",
                "NeedProtocolNeedWebServiceEndpointPort"), OwnerProtocolNeedWebServiceEndpoint.class, features);
    }

    private static URL __getWsdlLocation() {
        if (OWNERPROTOCOL_EXCEPTION!= null) {
            throw OWNERPROTOCOL_EXCEPTION;
        }
        return OWNERPROTOCOL_WSDL_LOCATION;
    }
}
