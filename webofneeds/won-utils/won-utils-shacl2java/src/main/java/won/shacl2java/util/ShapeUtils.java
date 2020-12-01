package won.shacl2java.util;

import org.apache.jena.graph.Node;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.NodeShape;
import org.apache.jena.shacl.parser.PropertyShape;
import org.apache.jena.shacl.parser.Shape;
import won.shacl2java.sourcegen.PropertySpec;
import won.shacl2java.constraints.*;

import java.util.Set;
import java.util.stream.Collectors;

public class ShapeUtils {
    public static Set<Node> getShNodeShapeNodes(Shape shape) {
        return new ShNodeShapeCollector().collectFrom(shape);
    }

    public static Set<Shape> getShNodeShapes(Shape shape, Shapes shapes) {
        return new ShNodeShapeCollector().collectFrom(shape).stream().map(shapes::getShape)
                        .collect(Collectors.toSet());
    }

    public static Set<Node> getNodeShapeNodes(NodeShape shape) {
        return new NodeShapeCollector().collectFrom(shape);
    }

    public static Set<Shape> getNodeShapes(NodeShape shape, Shapes shapes) {
        return new NodeShapeCollector().collectFrom(shape).stream().map(shapes::getShape)
                        .collect(Collectors.toSet());
    }

    public static Set<Node> getShNodeKinds(Shape shape) {
        return new ShNodeKindCollector().collectFrom(shape);
    }

    /**
     * Checks if a node shape is closed, and specifies allowed values using sh:in
     * 
     * @param shape
     * @return
     */
    public static EnumShapeChecker checkForEnumShape(Shape shape) {
        return EnumShapeChecker.check(shape);
    }

    public static Set<PropertySpec> getPropertySpecs(Shape shape) {
        return PropertySpecCollector.collectFrom(shape);
    }

    public static Set<PropertyShape> getShPropertyShapes(Shape shape) {
        return ShPropertyShapeCollector.collectFrom(shape);
    }
}
