package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import won.protocol.message.WonMessageProcessor;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class AbstractInOutMessageProcessor implements WonMessageProcessor
{

  protected Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
/*
  protected RDFStorageService rdfStorage;
  protected DataAccessService dataService;
  protected NeedManagementService needManagementService;
  protected NeedRepository needRepository;
  protected ConnectionRepository connectionRepository;
  protected FacetRepository facetRepository;
  protected OwnerApplicationRepository ownerApplicationRepository;
  protected MessageEventRepository messageEventRepository;
  protected LinkedDataService linkedDataService;
  protected WonNodeInformationService wonNodeInformationService;
  protected LinkedDataSource linkedDataSource;
  protected MatcherProtocolMatcherServiceClientSide matcherProtocolMatcherClient;
*/

  public  Object process(final Exchange exchange) throws Exception{return null;};

}
