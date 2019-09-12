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
package won.protocol.jms;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import won.protocol.model.ProtocolType;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: sbyim Date: 28.11.13
 */
public class MatcherActiveMQServiceImpl extends ActiveMQServiceImpl implements MatcherActiveMQService {
    private List<String> matcherProtocolTopicList;
    // private String pathInformation;
    private static final String PATH_MATCHER_PROTOCOL_OUT_ATOM_CREATED = "<" + WON.supportsWonProtocolImpl + ">/<"
                    + WON.atomCreatedTopic + ">";
    private static final String PATH_MATCHER_PROTOCOL_OUT_ATOM_ACTIVATED = "<" + WON.supportsWonProtocolImpl + ">/<"
                    + WON.atomActivatedTopic + ">";
    private static final String PATH_MATCHER_PROTOCOL_OUT_ATOM_DEACTIVATED = "<" + WON.supportsWonProtocolImpl + ">/<"
                    + WON.atomDeactivatedTopic + ">";
    private static final String PATH_MATCHER_PROTOCOL_QUEUE_NAME = "<" + WON.supportsWonProtocolImpl + ">/<"
                    + WON.matcherQueue + ">";

    public MatcherActiveMQServiceImpl(ProtocolType type) {
        super(type);
        queueNamePath = PATH_MATCHER_PROTOCOL_QUEUE_NAME;
        // pathInformation = "/resource";
        matcherProtocolTopicList = new ArrayList<>();
        matcherProtocolTopicList.add(PATH_MATCHER_PROTOCOL_OUT_ATOM_ACTIVATED);
        matcherProtocolTopicList.add(PATH_MATCHER_PROTOCOL_OUT_ATOM_CREATED);
        matcherProtocolTopicList.add(PATH_MATCHER_PROTOCOL_OUT_ATOM_DEACTIVATED);
    }

    public final Set<String> getMatcherProtocolTopicNamesWithResource(URI resourceURI) {
        Set<String> activeMQMatcherProtocolTopicNames = new HashSet<>();
        for (String s : matcherProtocolTopicList) {
            try {
                Path path = PathParser.parse(s, PrefixMapping.Standard);
                activeMQMatcherProtocolTopicNames.add(RdfUtils.getStringPropertyForPropertyPath(
                        linkedDataSource.getDataForResource(resourceURI), resourceURI, path));
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    return null;
                } else
                    throw e;
            }
        }
        return activeMQMatcherProtocolTopicNames;
    }
}
