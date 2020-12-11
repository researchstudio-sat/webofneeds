package won.shacl2java.sourcegen.typegen.mapping;

import com.squareup.javapoet.TypeSpec;
import org.apache.jena.graph.Node;
import org.apache.jena.shacl.parser.Shape;
import won.shacl2java.sourcegen.typegen.UpdateableTypeSpecMapping;
import won.shacl2java.sourcegen.typegen.support.ProducerConsumerMap;

import java.util.Map;
import java.util.stream.Collectors;

public class ShapeTargetClasses extends ProducerConsumerMap<Node, Shape> {
}
