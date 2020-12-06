package won.shacl2java.sourcegen.typegen.support;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Shape;

import java.util.HashMap;
import java.util.Map;

public class NameClashDetector {
    private Map<String, Node> classNames = new HashMap<>();

    public NameClashDetector() {
    }

    public void detectNameClash(Shape shape, String name) {
        if (classNames.containsKey(name)) {
            Node conflictingShape = classNames.get(name);
            if (!shape.getShapeNode().equals(conflictingShape)) {
                throw new IllegalStateException(
                                String.format("Name clash: %s is generated twice, once for shape %s, once for %s",
                                                name, classNames.get(name), shape.getShapeNode()));
            }
        }
        classNames.put(name, shape.getShapeNode());
    }
}
