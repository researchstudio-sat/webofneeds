package won.matcher.protocol.impl;

import java.net.URI;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.matcher.protocol.MatcherProtocolMatcherService;
import won.matcher.protocol.MatcherProtocolMatcherServiceCallback;
import won.matcher.protocol.NopMatcherProtocolMatcherServiceCallback;

/**
 * Created with IntelliJ IDEA. User: Gabriel Date: 03.12.12 Time: 14:12
 */
// TODO: refactor service interfaces.
public class MatcherProtocolMatcherServiceImpl implements MatcherProtocolMatcherService {
    final Logger logger = LoggerFactory.getLogger(getClass());
    // handler for incoming won protocol messages. The default handler does nothing.
    @Autowired(required = false)
    private MatcherProtocolMatcherServiceCallback matcherServiceCallback = new NopMatcherProtocolMatcherServiceCallback();

    // TODO: refactor this to use DataAccessService
    @Override
    public void onMatcherRegistration(final URI wonNodeUri) {
        logger.debug("matcher registration complete on {} ", wonNodeUri);
        matcherServiceCallback.onRegistered(wonNodeUri);
    }

    @Override
    public void onNewNeed(final URI wonNodeURI, URI needURI, Dataset content) {
        logger.debug("matcher from need: need created event for needURI {}", needURI);
        if (needURI == null)
            throw new IllegalArgumentException("needURI is not set");
        matcherServiceCallback.onNewNeed(wonNodeURI, needURI, content);
    }

    @Override
    public void onNeedModified(final URI wonNodeURI, final URI needURI) {
        logger.debug("matcher from need: need modified event for needURI {}", needURI);
        if (needURI == null)
            throw new IllegalArgumentException("needURI is not set");
        matcherServiceCallback.onNeedModified(wonNodeURI, needURI);
    }

    @Override
    public void onNeedActivated(final URI wonNodeURI, final URI needURI) {
        logger.debug("matcher from need: need activated event for needURI {}", needURI);
        if (needURI == null)
            throw new IllegalArgumentException("needURI is not set");
        matcherServiceCallback.onNeedActivated(wonNodeURI, needURI);
    }

    @Override
    public void onNeedDeactivated(final URI wonNodeURI, final URI needURI) {
        logger.debug("matcher from need: need deactivated event for needURI {}", needURI);
        if (needURI == null)
            throw new IllegalArgumentException("needURI is not set");
        matcherServiceCallback.onNeedDeactivated(wonNodeURI, needURI);
    }
}
