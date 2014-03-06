package won.node.facet.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.model.FacetType;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 5.3.14.
 * Time: 10.25
 * To change this template use File | Settings | File Templates.
 */
public class BAAtomicOutcomeFacetImpl extends Facet {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public FacetType getFacetType() {
        return FacetType.BAAtomicOutcome;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
