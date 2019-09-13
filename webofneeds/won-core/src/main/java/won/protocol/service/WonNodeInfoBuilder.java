package won.protocol.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder so that {@link WonNodeInfo} can be kept immutable Created by
 * hfriedrich on 21.02.2017.
 */
public class WonNodeInfoBuilder {
    private String wonNodeURI;
    private String eventURIPrefix;
    private String connectionURIPrefix;
    private String atomURIPrefix;
    private String atomListURI;
    private Map<String, Map<String, String>> supportedProtocolImpl;

    public WonNodeInfoBuilder() {
        supportedProtocolImpl = new HashMap<>();
    }

    public WonNodeInfoBuilder setWonNodeURI(final String wonNodeURI) {
        this.wonNodeURI = wonNodeURI;
        return this;
    }

    public WonNodeInfoBuilder setEventURIPrefix(final String eventURIPrefix) {
        this.eventURIPrefix = eventURIPrefix;
        return this;
    }

    public WonNodeInfoBuilder setConnectionURIPrefix(final String connectionURIPrefix) {
        this.connectionURIPrefix = connectionURIPrefix;
        return this;
    }

    public WonNodeInfoBuilder setAtomURIPrefix(final String atomURIPrefix) {
        this.atomURIPrefix = atomURIPrefix;
        return this;
    }

    public WonNodeInfoBuilder setAtomListURI(final String atomListURI) {
        this.atomListURI = atomListURI;
        return this;
    }

    public WonNodeInfoBuilder addSupportedProtocolImplParamValue(String protocol, String paramName, String paramValue) {
        Map<String, String> protocolMap = supportedProtocolImpl.computeIfAbsent(protocol, k -> new HashMap<>());
        protocolMap.put(paramName, paramValue);
        return this;
    }

    public WonNodeInfo build() {
        return new WonNodeInfo(wonNodeURI, eventURIPrefix, connectionURIPrefix, atomURIPrefix, atomListURI,
                        supportedProtocolImpl);
    }
}
