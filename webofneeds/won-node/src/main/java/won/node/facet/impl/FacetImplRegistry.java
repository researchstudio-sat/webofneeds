package won.node.facet.impl;

import won.protocol.model.FacetType;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 16.09.13
 * Time: 18:43
 * To change this template use File | Settings | File Templates.
 */
public class FacetImplRegistry {
    private HashMap<FacetType, FacetImpl> map;

    public FacetImpl get(FacetType ft) {
        return map.get(ft);
    }

    public void register(FacetType ft, FacetImpl fi) {
        map.put(ft, fi);
    }

    public void setMap(HashMap<FacetType, FacetImpl> map) {
        this.map = map;
    }
}
