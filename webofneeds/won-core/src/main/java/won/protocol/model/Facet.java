package won.protocol.model;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 28.08.13
 * Time: 16:03
 * To change this template use File | Settings | File Templates.
 */

import javax.persistence.*;
import java.net.URI;

@Entity
@Table(name = "facet")
public class Facet {
    @Id
    @GeneratedValue
    @Column( name = "id" )
    private Long id;
    /* The uri of the facet's need object */
    @Column( name = "needURI")
    private URI needURI;
    /* The uri of the facet's type */
    @Column( name = "typeURI")
    private URI typeURI;

    public Long getId() {
        return id;
    }

    public URI getNeedURI() {
        return needURI;
    }

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
