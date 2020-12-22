package won.shacl2java.model;

public interface GraphEntityVisitor {
    default void visit(GraphEntity host) {
        host.accept(this);
    }
}
