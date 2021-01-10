package won.shacl2java.runtime.model;

import com.github.jsonldjava.utils.Obj;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

import java.net.URI;
import java.util.Objects;

public class EntityTriple {
    private Node predicate;
    private Node object;

    public EntityTriple(Node predicate, Node object) {
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(object);
        if (!predicate.isURI()) {
            throw new IllegalArgumentException("predicate must be an URI");
        }
        this.predicate = predicate;
        this.object = object;
    }

    public EntityTriple(String predicate, Node object) {
        this(NodeFactory.createURI(predicate), object);
    }

    public EntityTriple(URI predicate, Node object) {
        this(NodeFactory.createURI(predicate.toString()), object);
    }

    public Node getPredicate() {
        return predicate;
    }

    public Node getObject() {
        return object;
    }
}
