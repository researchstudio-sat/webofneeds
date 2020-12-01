package won.shacl2java.sourcegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.apache.jena.graph.Node;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class IndividualPropertySpec {
    private Node predicate;
    private Set<Node> objects = new HashSet<>();
    private ClassName className;

    public IndividualPropertySpec(Node predicate, ClassName className) {
        this.predicate = predicate;
        this.className = className;
    }

    public Node getPredicate() {
        return predicate;
    }

    public Set<Node> getObjects() {
        return objects;
    }

    public void addObject(Node object) {
        objects.add(object);
    }

    public void addObjects(Collection<Node> objects) {
        objects.addAll(objects);
    }

    public static IndividualPropertySpec merge(IndividualPropertySpec left, IndividualPropertySpec right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        if (!left.getPredicate().equals(right.predicate)) {
            throw new IllegalArgumentException("cannot merge");
        }
        if (!left.getClassName().equals(right.getClassName())) {
            throw new IllegalArgumentException("cannot merge");
        }
        IndividualPropertySpec spec = new IndividualPropertySpec(left.getPredicate(), left.getClassName());
        spec.addObjects(left.getObjects());
        spec.addObjects(right.getObjects());
        return spec;
    }

    public ClassName getClassName() {
        return className;
    }

    public boolean isSingletonProperty() {
        return objects.size() == 1;
    }
}
