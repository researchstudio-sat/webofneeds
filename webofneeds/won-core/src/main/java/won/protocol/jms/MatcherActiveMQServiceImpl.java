/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.jms;

import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.model.ProtocolType;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: sbyim
 * Date: 28.11.13
 */
public class MatcherActiveMQServiceImpl extends ActiveMQServiceImpl implements MatcherActiveMQService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private List<String> matcherProtocolTopicList;
    private String pathInformation;
  private static final String PATH_MATCHER_PROTOCOL_OUT_NEED_CREATED = "<"+WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_CREATED_TOPIC_NAME+">";
  private static final String PATH_MATCHER_PROTOCOL_OUT_NEED_ACTIVATED = "<"+WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_ACTIVATED_TOPIC_NAME+">";
  private static final String PATH_MATCHER_PROTOCOL_OUT_NEED_DEACTIVATED = "<"+WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_DEACTIVATED_TOPIC_NAME+">";
  private static final String PATH_MATCHER_PROTOCOL_QUEUE_NAME = "<" + WON.SUPPORTS_WON_PROTOCOL_IMPL + ">/<" + WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_QUEUE_NAME + ">";

  @Autowired
    private LinkedDataSource linkedDataSource;

    public MatcherActiveMQServiceImpl(ProtocolType type) {
      super(type);
      queueNamePath = PATH_MATCHER_PROTOCOL_QUEUE_NAME;
      //pathInformation = "/resource";
      matcherProtocolTopicList = new ArrayList<>();
      matcherProtocolTopicList.add(PATH_MATCHER_PROTOCOL_OUT_NEED_ACTIVATED);
      matcherProtocolTopicList.add(PATH_MATCHER_PROTOCOL_OUT_NEED_CREATED);
      matcherProtocolTopicList.add(PATH_MATCHER_PROTOCOL_OUT_NEED_DEACTIVATED);

    }

    public final Set<String> getMatcherProtocolTopicNamesWithResource(URI resourceURI){
        Set<String> activeMQMatcherProtocolTopicNames = new HashSet<>();
        resourceURI = URI.create(resourceURI.toString());
        for (int i = 0; i< matcherProtocolTopicList.size();i++){
            try{
                Path path = PathParser.parse(matcherProtocolTopicList.get(i),PrefixMapping.Standard);
                activeMQMatcherProtocolTopicNames.add(RdfUtils.getStringPropertyForPropertyPath(
                        linkedDataSource.getModelForResource(resourceURI),
                        resourceURI,
                        path
                ));
            }catch (UniformInterfaceException e){
                ClientResponse response = e.getResponse();
                if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()){
                    return null;
                }
                else throw e;
            }

        }
        return activeMQMatcherProtocolTopicNames;
    }


}
