package won.shacl2java.sourcegen.typegen.logic;

import com.squareup.javapoet.TypeSpec;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.vocabulary.RDF;
import won.shacl2java.Shacl2JavaConfig;
import won.shacl2java.sourcegen.typegen.support.NameClashDetector;
import won.shacl2java.sourcegen.typegen.mapping.ShapeTypeInterfaceTypes;
import won.shacl2java.sourcegen.typegen.TypesGenerator;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static javax.lang.model.element.Modifier.PUBLIC;
import static won.shacl2java.util.NameUtils.classNameForShapeURI;

public class ShapeTypeInterfaceGenerator implements TypesGenerator {
    private Shapes shapes;
    private Shacl2JavaConfig config;
    private ShapeTypeInterfaceTypes.Producer shapeTypeInterfaceTypes;
    private NameClashDetector nameClashDetector;

    public ShapeTypeInterfaceGenerator(Shapes shapes,
                    ShapeTypeInterfaceTypes.Producer shapeTypeInterfaceTypes,
                    NameClashDetector nameClashDetector,
                    Shacl2JavaConfig config) {
        this.shapes = shapes;
        this.config = config;
        this.shapeTypeInterfaceTypes = shapeTypeInterfaceTypes;
        this.nameClashDetector = nameClashDetector;
    }

    @Override
    public Set<TypeSpec> generate() {
        if (!config.isInterfacesForRdfTypes()) {
            return Collections.emptySet();
        }
        return StreamSupport
                        .stream(Spliterators.spliteratorUnknownSize(shapes.iteratorAll(), Spliterator.ORDERED), false)
                        .distinct()
                        .filter(s -> s.isNodeShape())
                        .flatMap(shape -> shape.getShapeGraph()
                                        .find(shape.getShapeNode(), RDF.type.asNode(), null)
                                        .toSet().stream()
                                        .map(Triple::getObject)
                                        .map(Node::getURI)
                                        .filter(u -> !u.startsWith(SHACL.getURI()))
                                        .map(u -> URI.create(u))
                                        .map(u -> shapeTypeInterfaceTypes.computeIfAbsent(u, typeUri -> {
                                            String name = classNameForShapeURI(typeUri, config);
                                            nameClashDetector.detectNameClash(shape, name);
                                            return TypeSpec
                                                            .interfaceBuilder(name)
                                                            .addModifiers(PUBLIC)
                                                            .build();
                                        })))
                        .collect(Collectors.toSet());
    }
}
