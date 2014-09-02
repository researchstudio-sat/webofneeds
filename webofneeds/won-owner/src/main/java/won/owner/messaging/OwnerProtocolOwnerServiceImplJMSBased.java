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

package won.owner.messaging;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.camel.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.*;
import won.protocol.util.RdfUtils;

import java.net.URI;


/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 03.12.12
 * Time: 14:12
 */
//TODO copied from OwnerProtocolOwnerService... refactoring needed
    //TODO: refactor service interfaces.
public class OwnerProtocolOwnerServiceImplJMSBased {//implements OwnerProtocolOwnerService {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private WonNodeRepository wonNodeRepository;

    @Autowired
    OwnerProtocolOwnerService delegate;

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private OwnerProtocolNeedServiceClientJMSBased ownerService;


   // @Override
    public void hint(
            @Header("ownNeedUri") final String ownNeedURI,
            @Header("otherNeedUri")final String otherNeedURI,
            @Header("score") final String score,
            @Header("originatorUri")final String originatorURI,
            @Header("content")final String content,
            @Header("messageEvent") final String messageEvent)
            throws NoSuchNeedException, IllegalMessageForNeedStateException {

        if (ownNeedURI == null) throw new IllegalArgumentException("needURI is not set");
        if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
        if (score == null) throw new IllegalArgumentException("score is not in [0,1]");
        if (originatorURI == null) throw new IllegalArgumentException("originator is not set");
        if (ownNeedURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");
        logger.debug("owner from need (jms): HINT called for own need {}, other need {}, with score {} from originator {} and content {}",
          new Object[]{ownNeedURI, otherNeedURI, score, originatorURI, content});
        delegate.hint(ownNeedURI, otherNeedURI, score, originatorURI, content, RdfUtils.toDataset(messageEvent));
    }

    private boolean isNeedActive(final Need need) {
        return NeedState.ACTIVE == need.getState();
    }

    public void connect(@Header("ownNeedURI") final String ownNeedURI,
                        @Header("otherNeedURI")final String otherNeedURI,
                        @Header("ownConnectionURI")final String ownConnectionURI,
                        @Header("content")final String content,
                        @Header("messageEvent") final String messageEvent)
            throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
    {

      if (ownNeedURI == null) throw new IllegalArgumentException("needURI is not set");
      if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
      if (ownConnectionURI == null) throw new IllegalArgumentException("otherConnectionURI is not set");
      if (ownNeedURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");
      logger.debug("owner from need (jms): CONNECT called for own need {}, other need {}, own connection {} and content {}", new Object[]{ownNeedURI,otherNeedURI,ownConnectionURI, content});
      delegate.connect(ownNeedURI, otherNeedURI, ownConnectionURI, content, RdfUtils.toDataset(messageEvent));
    }

    public void open(@Header("connectionURI")String connectionURI,
                     @Header("content")String content,
                     @Header("messageEvent") final String messageEvent)
      throws NoSuchConnectionException, IllegalMessageForConnectionStateException, IllegalMessageForNeedStateException {
      if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
      logger.debug("owner from need (jms): OPEN called for connection {} with content {}.", connectionURI, content);
      delegate.open(URI.create(connectionURI), RdfUtils.toModel(content), RdfUtils.toDataset(messageEvent));
    }

    public void close(@Header("connectionURI")final String connectionURI,
                      @Header("content")String content,
                      @Header("messageEvent") final String messageEvent)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
      if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
      logger.debug("owner from need (jms): CLOSE called for connection {}", connectionURI);
      delegate.close(URI.create(connectionURI), RdfUtils.toModel(content), RdfUtils.toDataset(messageEvent));
    }

    public void sendMessage(@Header("connectionURI") final String connectionURI,
                            @Header("message") final String message,
                            @Header("messageEvent") final String messageEvent)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
        if (message == null) throw new IllegalArgumentException("message is not set");
        logger.debug("owner from need (jms): SEND_TEXT_MESSAGE called for connection {} with message {}", connectionURI, message);
        Model messageConvert = RdfUtils.toModel(message);
        delegate.sendMessage(URI.create(connectionURI), messageConvert, RdfUtils.toDataset(messageEvent));
    }


    public void setOwnerService(OwnerProtocolNeedServiceClientJMSBased ownerService) {
        this.ownerService = ownerService;
    }
}
