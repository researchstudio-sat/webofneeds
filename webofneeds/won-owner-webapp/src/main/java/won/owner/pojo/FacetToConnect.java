package won.owner.pojo;

public class FacetToConnect {
    String facet;
    boolean pending = false;
    
    public FacetToConnect(){}

    public String getFacet() {
        return facet;
    }

    public void setFacet(String facet) {
        this.facet = facet;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }
    
    
}
