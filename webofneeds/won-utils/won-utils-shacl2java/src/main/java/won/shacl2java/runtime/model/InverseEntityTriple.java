package won.shacl2java.runtime.model;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;

import java.net.URI;
import java.util.Objects;

public class InverseEntityTriple {
    private Node predicate;
    private Node subject;

    public InverseEntityTriple(Node subject, Node predicate) {
        Objects.requireNonNull(subject);
        Objects.requireNonNull(predicate);
        if (!(subject.isURI() || subject.isBlank())) {
            throw new IllegalArgumentException("subject must be an URI or a blank node");
        }
        this.subject = subject;
        if (!predicate.isURI()) {
            throw new IllegalArgumentException("predicate must be an URI");
        }
        this.predicate = predicate;
    }

    public InverseEntityTriple(String subject, String predicate) {
        this(NodeFactory.createURI(subject), NodeFactory.createURI(predicate));
    }

    public InverseEntityTriple(URI subject, URI predicate) {
        this(NodeFactory.createURI(subject.toString()), NodeFactory.createURI(predicate.toString()));
    }

    public Node getPredicate() {
        return predicate;
    }

    public Node getSubject() {
        return subject;
    }
}
