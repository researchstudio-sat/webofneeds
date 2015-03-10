package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.protocol.MatcherProtocolMatcherServiceClientSide;
import won.node.service.DataAccessService;
import won.protocol.message.WonMessageProcessor;
import won.protocol.repository.*;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.LinkedDataService;
import won.protocol.service.NeedManagementService;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;

/**
 * User: syim
 * Date: 02.03.2015
 */
public abstract class AbstractInOnlyMessageProcessor implements WonMessageProcessor
{
  protected Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

  @Autowired
  protected DataAccessService dataService;
  @Autowired
  protected RDFStorageService rdfStorage;

  @Autowired
  protected NeedManagementService needManagementService;

  @Autowired
  protected NeedRepository needRepository;
  @Autowired
  protected ConnectionRepository connectionRepository;
  @Autowired
  FacetRepository facetRepository;
  @Autowired
  protected OwnerApplicationRepository ownerApplicationRepository;
  @Autowired
  protected MessageEventRepository messageEventRepository;
  @Autowired
  protected LinkedDataService linkedDataService;
  @Autowired
  protected WonNodeInformationService wonNodeInformationService;
  @Autowired
  protected LinkedDataSource linkedDataSource;
  @Autowired
  protected MatcherProtocolMatcherServiceClientSide matcherProtocolMatcherClient;


  public abstract void process(final Exchange exchange) throws Exception;
}
