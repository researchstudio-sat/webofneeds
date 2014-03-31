package won.node.facet.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.model.FacetType;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 16.09.13
 * Time: 18:42
 * To change this template use File | Settings | File Templates.
 */
public class OwnerFacetImpl extends AbstractFacet
{
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public FacetType getFacetType() {
        return FacetType.OwnerFacet;
    }
}
