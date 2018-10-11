package won.owner.pojo;

import java.util.List;

public class ConnectAction {
    private List<FacetToConnect> facets; 
    
    public ConnectAction() {}

    public List<FacetToConnect> getFacets() {
        return facets;
    }

    public void setFacets(List<FacetToConnect> facets) {
        this.facets = facets;
    }
}
