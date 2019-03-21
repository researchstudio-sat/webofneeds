package won.protocol.model;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 28.08.13
 * Time: 16:03
 * To change this template use File | Settings | File Templates.
 */
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name = "facet", indexes = { @Index(name = "IDX_UNIQUE_FACET", columnList = "facetURI")})
public class Facet {
    @Id
    @GeneratedValue
    @Column( name = "id" )
    private Long id;

    /* The uri of the facet's need object */
    @Column( name = "needURI")
    @Convert( converter = URIConverter.class)
    private URI needURI;

    /* The uri of the facet's type */
    @Column( name = "typeURI")
    @Convert( converter = URIConverter.class)
    private URI typeURI;
    
    /* The uri of the facet - must be defined in the need's content */
    @Column( name = "facetURI")
    @Convert( converter = URIConverter.class)
    private URI facetURI;
    
    @Column( name = "defaultFacet")
    @Convert( converter = BooleanTFConverter.class)
    private boolean isDefaultFacet = false;

    public Long getId() {
        return id;
    }

    public URI getNeedURI() {
        return needURI;
    }

    /**
     * Not safe to use unless we know the typeURI is a known facet type. Use getTypeURI instead.
     * @Deprecated
     * */
    public FacetType getFacetType() {
        return FacetType.getFacetType(typeURI);
    }

    public void setNeedURI(URI needURI) {
        this.needURI = needURI;
    }

    public URI getTypeURI() {
        return typeURI;
    }

    public void setTypeURI(URI typeURI) {
        this.typeURI = typeURI;
    }

    public URI getFacetURI() {
        return facetURI;
    }

    public void setFacetURI(URI facetURI) {
        this.facetURI = facetURI;
    }

    public boolean isDefaultFacet() {
        return isDefaultFacet;
    }

    public void setDefaultFacet(boolean isDefaultFacet) {
        this.isDefaultFacet = isDefaultFacet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Facet facet = (Facet) o;

        if (id != null ? !id.equals(facet.id) : facet.id != null) return false;
        if (needURI != null ? !needURI.equals(facet.needURI) : facet.needURI != null) return false;
        if (typeURI != null ? !typeURI.equals(facet.typeURI) : facet.typeURI != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (needURI != null ? needURI.hashCode() : 0);
        result = 31 * result + (typeURI != null ? typeURI.hashCode() : 0);
        return result;
    }
}
