package won.matcher.protocol.impl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.matcher.MatcherProtocolAtomServiceClientSide;
import won.protocol.message.WonMessage;

import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * User: gabriel Date: 12.02.13 Time: 17:26
 */
public class MatcherProtocolAtomServiceClient implements MatcherProtocolAtomServiceClientSide {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    MatcherProtocolAtomServiceClientSide delegate;

    public void hint(URI atomURI, URI otherAtom, double score, URI originator, Model content, WonMessage wonMessage)
                    throws Exception {
        logger.info("atom-facing: HINT called for atomURI {} and otherAtom {} " + "with score {} from originator {}.",
                        new Object[] { atomURI, otherAtom, score, originator });
        Model socketModel = ModelFactory.createDefaultModel();
        delegate.hint(atomURI, otherAtom, score, originator, socketModel, wonMessage);
    }

    public void initializeDefault() {
        // delegate = new MatcherProtocolAtomServiceClientJMSBased();
        delegate.initializeDefault();
    }

    public void setDelegate(MatcherProtocolAtomServiceClientSide delegate) {
        this.delegate = delegate;
    }
}
