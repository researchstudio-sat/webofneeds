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
    public void onNewAtom(final URI wonNodeURI, URI atomURI, Dataset content) {
        logger.debug("matcher from atom: atom created event for atomURI {}", atomURI);
        if (atomURI == null)
            throw new IllegalArgumentException("atomURI is not set");
        matcherServiceCallback.onNewAtom(wonNodeURI, atomURI, content);
    }

    @Override
    public void onAtomModified(final URI wonNodeURI, final URI atomURI) {
        logger.debug("matcher from atom: atom modified event for atomURI {}", atomURI);
        if (atomURI == null)
            throw new IllegalArgumentException("atomURI is not set");
        matcherServiceCallback.onAtomModified(wonNodeURI, atomURI);
    }

    @Override
    public void onAtomActivated(final URI wonNodeURI, final URI atomURI) {
        logger.debug("matcher from atom: atom activated event for atomURI {}", atomURI);
        if (atomURI == null)
            throw new IllegalArgumentException("atomURI is not set");
        matcherServiceCallback.onAtomActivated(wonNodeURI, atomURI);
    }

    @Override
    public void onAtomDeactivated(final URI wonNodeURI, final URI atomURI) {
        logger.debug("matcher from atom: atom deactivated event for atomURI {}", atomURI);
        if (atomURI == null)
            throw new IllegalArgumentException("atomURI is not set");
        matcherServiceCallback.onAtomDeactivated(wonNodeURI, atomURI);
    }
}
