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
package won.node.camel.processor;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;

import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.cryptography.service.RandomNumberService;
import won.node.camel.service.CamelWonMessageService;
import won.node.protocol.MatcherProtocolMatcherServiceClientSide;
import won.node.service.persistence.AtomService;
import won.node.service.persistence.ConnectionService;
import won.node.service.persistence.MessageService;
import won.node.service.persistence.SocketService;
import won.protocol.jms.MessagingService;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.ConnectionContainerRepository;
import won.protocol.repository.ConnectionMessageContainerRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.repository.SocketRepository;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;

/**
 * User: syim Date: 02.03.2015
 */
public abstract class AbstractCamelProcessor implements Processor {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    protected MessagingService messagingService;
    @Autowired
    protected DatasetHolderRepository datasetHolderRepository;
    @Autowired
    protected AtomRepository atomRepository;
    @Autowired
    protected ConnectionContainerRepository connectionContainerRepository;
    @Autowired
    protected AtomMessageContainerRepository atomMessageContainerRepository;
    @Autowired
    protected ConnectionRepository connectionRepository;
    @Autowired
    protected ConnectionMessageContainerRepository connectionMessageContainerRepository;
    @Autowired
    protected SocketRepository socketRepository;
    @Autowired
    protected OwnerApplicationRepository ownerApplicationRepository;
    @Autowired
    protected MessageEventRepository messageEventRepository;
    @Autowired
    protected WonNodeInformationService wonNodeInformationService;
    @Autowired
    protected LinkedDataSource linkedDataSource;
    @Autowired
    protected MatcherProtocolMatcherServiceClientSide matcherProtocolMatcherClient;
    @Autowired
    protected RandomNumberService randomNumberService;
    @Autowired
    protected ExecutorService executorService;
    @Autowired
    protected SocketService socketService;
    @Autowired
    protected AtomService atomService;
    @Autowired
    protected ConnectionService connectionService;
    @Autowired
    protected MessageService messageService;
    @Autowired
    protected CamelWonMessageService camelWonMessageService;
}
