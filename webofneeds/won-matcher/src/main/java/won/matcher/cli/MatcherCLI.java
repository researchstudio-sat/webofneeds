package won.matcher.cli;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

import won.matcher.protocol.impl.MatcherProtocolAtomServiceClient;
import won.protocol.exception.IllegalMessageForAtomStateException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * User: gabriel Date: 14.02.13 Time: 15:00
 */
public class MatcherCLI implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private MatcherProtocolAtomServiceClient client;
    @Autowired
    private WonNodeInformationService wonNodeInformationService;
    @Autowired
    private LinkedDataSource linkedDataSource;

    @Override
    public void run(String... args) throws Exception {
        String atom1 = "http://localhost:8080/won/resource/atom/1";
        String atom2 = "http://localhost:8080/won/resource/atom/2";
        String org = "http://localhost:8080/matcher";
        double score = 1;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h":
                    System.out.println(
                                    "USAGE: java MatcherCLI [-n1 atom1] [-n2 atom2] [-o originator] [-s score] [-h]");
                    System.exit(0);
                case "-n1":
                    atom1 = args[++i];
                    break;
                case "-n2":
                    atom2 = args[++i];
                    break;
                case "-o":
                    org = args[++i];
                    break;
                case "-s":
                    score = Double.parseDouble(args[++i]);
                    break;
            }
        }
        try {
            // TODO: Add rdf content
            client.hint(new URI(atom1), new URI(atom2), score, new URI(org), null,
                            createWonMessage(URI.create(atom1), URI.create(atom2), score, URI.create(org)));
        } catch (URISyntaxException e) {
            logger.error("Exception caught:", e);
        } catch (IllegalMessageForAtomStateException e) {
            logger.error("Exception caught:", e);
        } catch (NoSuchAtomException e) {
            logger.error("Exception caught:", e);
        } catch (Exception e) {
            e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void setClient(MatcherProtocolAtomServiceClient client) {
        this.client = client;
    }

    private WonMessage createWonMessage(URI atomURI, URI otherAtomURI, double score, URI originator)
                    throws WonMessageBuilderException {
        URI wonNode = WonLinkedDataUtils.getWonNodeURIForAtomOrConnection(atomURI,
                        linkedDataSource.getDataForResource(atomURI));
        return WonMessageBuilder
                        .atomHint(wonNodeInformationService.generateEventURI(wonNode))
                        .atom(atomURI)
                        .hintTargetAtom(otherAtomURI)
                        .hintScore(score)
                        .direction().fromExternal()
                        .build();
    }
}
