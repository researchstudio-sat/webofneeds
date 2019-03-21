package won.matcher.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import won.matcher.protocol.impl.MatcherProtocolNeedServiceClient;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * User: gabriel Date: 14.02.13 Time: 15:00
 */
public class MatcherCLI implements CommandLineRunner {

  private static final Logger logger = LoggerFactory.getLogger(MatcherCLI.class);

  @Autowired
  private MatcherProtocolNeedServiceClient client;
  @Autowired
  private WonNodeInformationService wonNodeInformationService;
  @Autowired
  private LinkedDataSource linkedDataSource;

  @Override
  public void run(String... args) throws Exception {
    String need1 = "http://localhost:8080/won/resource/need/1";
    String need2 = "http://localhost:8080/won/resource/need/2";
    String org = "http://localhost:8080/matcher";
    double score = 1;

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-h")) {
        System.out.println("USAGE: java MatcherCLI [-n1 need1] [-n2 need2] [-o originator] [-s score] [-h]");
        System.exit(0);
      } else if (args[i].equals("-n1")) {
        need1 = args[++i];
      } else if (args[i].equals("-n2")) {
        need2 = args[++i];
      } else if (args[i].equals("-o")) {
        org = args[++i];
      } else if (args[i].equals("-s")) {
        score = Double.parseDouble(args[++i]);
      }
    }

    try {
      // TODO: Add rdf content
      client.hint(new URI(need1), new URI(need2), score, new URI(org), null,
          createWonMessage(URI.create(need1), URI.create(need2), score, URI.create(org)));
    } catch (URISyntaxException e) {
      logger.error("Exception caught:", e);
    } catch (IllegalMessageForNeedStateException e) {
      logger.error("Exception caught:", e);
    } catch (NoSuchNeedException e) {
      logger.error("Exception caught:", e);
    } catch (Exception e) {
      e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
    }
  }

  public void setClient(MatcherProtocolNeedServiceClient client) {
    this.client = client;
  }

  private WonMessage createWonMessage(URI needURI, URI otherNeedURI, double score, URI originator)
      throws WonMessageBuilderException {
    URI wonNode = WonLinkedDataUtils.getWonNodeURIForNeedOrConnection(needURI,
        linkedDataSource.getDataForResource(needURI));

    return WonMessageBuilder
        .setMessagePropertiesForHint(wonNodeInformationService.generateEventURI(wonNode), needURI, Optional.empty(),
            wonNode, otherNeedURI, Optional.empty(), originator, score)
        .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL).build();
  }
}
