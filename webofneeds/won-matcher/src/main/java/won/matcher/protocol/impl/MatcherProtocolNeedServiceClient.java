package won.matcher.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.matcher.MatcherProtocolNeedServiceClientSide;
import won.protocol.model.FacetType;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * User: gabriel
 * Date: 12.02.13
 * Time: 17:26
 */
public class MatcherProtocolNeedServiceClient implements MatcherProtocolNeedServiceClientSide {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    MatcherProtocolNeedServiceClientSide delegate;

    public void hint(URI needURI, URI otherNeed, double score, URI originator, Model content)
            throws Exception {
        logger.info("need-facing: HINT called for needURI {} and otherNeed {} " +
                "with score {} from originator {}.", new Object[]{needURI, otherNeed, score, originator});
        Model facetModel = WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(FacetType.OwnerFacet.getURI(), FacetType.OwnerFacet.getURI());
        delegate.hint(needURI, otherNeed, score, originator, facetModel);
    }


    public void initializeDefault() {
        //   delegate = new MatcherProtocolNeedServiceClientJMSBased();
        delegate.initializeDefault();
    }


    public void setDelegate(MatcherProtocolNeedServiceClientSide delegate) {
        this.delegate = delegate;
    }

}
