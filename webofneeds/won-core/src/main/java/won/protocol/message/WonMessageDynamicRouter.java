package won.protocol.message;

import java.net.URI;

/**
 * User: syim Date: 05.03.2015
 */
public interface WonMessageDynamicRouter {
    public void route(URI socketType, URI direction, URI messageType, WonMessage wonMessage);
}
