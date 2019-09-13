package won.protocol.message;

import java.net.URI;

/**
 * User: syim Date: 05.03.2015
 */
public interface WonMessageDynamicRouter {
    void route(URI socketType, URI direction, URI messageType, WonMessage wonMessage);
}
