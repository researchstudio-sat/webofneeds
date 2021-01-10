package won.shacl2java.runtime.model;

public interface GraphEntityVisitor {
    default void visit(GraphEntity host) {
        host.accept(this);
    }
}
