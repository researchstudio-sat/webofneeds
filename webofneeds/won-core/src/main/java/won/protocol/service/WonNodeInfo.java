/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: fkleedorfer Date: 27.10.2014
 */
public class WonNodeInfo {
    private String wonNodeURI;
    private String messageURIPrefix;
    private String connectionURIPrefix;
    private String atomURIPrefix;
    private String atomListURI;
    private Map<String, Map<String, String>> supportedProtocolImpl;

    protected WonNodeInfo(String wonNodeURI, String messageURIPrefix, String connectionURIPattern,
                    String atomURIPattern,
                    String atomListURI, Map<String, Map<String, String>> supportedProtocolImpl) {
        this.wonNodeURI = wonNodeURI;
        this.messageURIPrefix = messageURIPrefix;
        this.connectionURIPrefix = connectionURIPattern;
        this.atomURIPrefix = atomURIPattern;
        this.atomListURI = atomListURI;
        this.supportedProtocolImpl = supportedProtocolImpl;
    }

    public String getWonNodeURI() {
        return wonNodeURI;
    }

    public String getAtomListURI() {
        return atomListURI;
    }

    public String getSupportedProtocolImplParamValue(String protocol, String paramName) {
        Map<String, String> protocolMap = supportedProtocolImpl.get(protocol);
        if (protocolMap != null) {
            return protocolMap.get(paramName);
        }
        return null;
    }

    public Set<String> getSupportedProtocolImpls() {
        return new HashSet<>(supportedProtocolImpl.keySet());
    }

    public Set<String> getSupportedProtocolImplParams(String protocol) {
        Map<String, String> protocolMap = supportedProtocolImpl.get(protocol);
        return protocolMap.keySet();
    }

    public String getMessageURIPrefix() {
        return messageURIPrefix;
    }

    public String getConnectionURIPrefix() {
        return connectionURIPrefix;
    }

    public String getAtomURIPrefix() {
        return atomURIPrefix;
    }

    @Override
    public String toString() {
        return "WonNodeInfo [wonNodeURI=" + wonNodeURI + ", messageURIPrefix=" + messageURIPrefix
                        + ", connectionURIPrefix=" + connectionURIPrefix + ", atomURIPrefix=" + atomURIPrefix
                        + ", atomListURI=" + atomListURI + ", supportedProtocolImpl=" + supportedProtocolImpl + "]";
    }
}
