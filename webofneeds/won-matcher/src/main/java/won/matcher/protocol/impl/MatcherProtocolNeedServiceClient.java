package won.matcher.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.matcher.MatcherProtocolNeedService;
import won.protocol.model.Connection;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.vocabulary.WON;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.ws.MatcherProtocolNeedWebServiceEndpoint;
import won.matcher.ws.MatcherProtocolNeedWebServiceClient;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 12.02.13
 * Time: 17:26
 * To change this template use File | Settings | File Templates.
 */
public class MatcherProtocolNeedServiceClient implements MatcherProtocolNeedService {
    final Logger logger = LoggerFactory.getLogger(getClass());

    private LinkedDataRestClient linkedDataRestClient;

    public void setLinkedDataRestClient(LinkedDataRestClient linkedDataRestClient) {
        this.linkedDataRestClient = linkedDataRestClient;
    }

    @Override
    public void hint(URI needURI, URI otherNeed, double score, URI originator)
            throws NoSuchNeedException, IllegalMessageForNeedStateException {
        logger.info(MessageFormat.format("need-facing: HINT called for needURI {0} and otherNeed {1} " +
                "with score {2} from originator {3}.", needURI, otherNeed, score, originator));
        try {
            MatcherProtocolNeedWebServiceEndpoint proxy = getMatcherProtocolEndpointForNeed(needURI);
            proxy.hint(needURI, otherNeed, score, originator);
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public Collection<URI> listNeedURIs() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<URI> listNeedURIs(int page) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<URI> listConnectionURIs() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<URI> listConnectionURIs(int page) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<URI> listConnectionURIs(URI needURI, int page) throws NoSuchNeedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Need readNeed(URI needURI) throws NoSuchNeedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Model readNeedContent(URI needURI) throws NoSuchNeedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Connection readConnection(URI connectionURI) throws NoSuchConnectionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Model readConnectionContent(URI connectionURI) throws NoSuchConnectionException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<Match> listMatches(URI needURI, int page) throws NoSuchNeedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<Match> listMatches(URI needURI) throws NoSuchNeedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    private MatcherProtocolNeedWebServiceEndpoint getMatcherProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
    {
        //TODO: fetch endpoint information for the need and store in db?
        URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(needURI, WON.MATCHER_PROTOCOL_ENDPOINT);
        if (needProtocolEndpoint == null) throw new NoSuchNeedException(needURI);
        logger.info("need won.matcher.protocol endpoint of need {} is {}", needURI.toString(), needProtocolEndpoint.toString());
        MatcherProtocolNeedWebServiceClient client = new MatcherProtocolNeedWebServiceClient(URI.create(needProtocolEndpoint.toString() + "?wsdl").toURL());
        return client.getOwnerProtocolOwnerWebServiceEndpointPort();
    }
}
